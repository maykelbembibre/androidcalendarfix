package bembibre.alarmfix.core;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import bembibre.alarmfix.R;
import bembibre.alarmfix.ReminderEditActivity;
import bembibre.alarmfix.ReminderListActivity;
import bembibre.alarmfix.alarms.ReminderManager;
import bembibre.alarmfix.database.RemindersDbAdapter;
import bembibre.alarmfix.logging.Logger;
import bembibre.alarmfix.logic.DeleteAllReminders;
import bembibre.alarmfix.logic.exportimport.DataImport;
import bembibre.alarmfix.models.DataImportResultType;
import bembibre.alarmfix.models.ImportedReminder;
import bembibre.alarmfix.models.DataImportResult;
import bembibre.alarmfix.models.DateTime;
import bembibre.alarmfix.userinterface.UserInterfaceUtils;

/**
 * Class for all the delicate work that must be synchronized between threads.
 */
public class SynchronizedWork {

    /**
     * Synchronized method called when an alarm for a reminder has been triggered.
     *
     * @param context application context.
     * @param intent intent of the alarm.
     */
    public synchronized static void reminderAlarmReceived(Context context, Intent intent) {
        try {
            AlarmChainManager alarmChainManager = new AlarmChainManager(context);
            if ((intent.hasExtra(RemindersDbAdapter.KEY_ROWID)) && (intent.hasExtra(ReminderManager.EXTRA_ALARM_ID))) {
                Logger.log("The received alarm is for notifying a reminder and setting the next alarm.");
                long rowId = intent.getExtras().getLong(RemindersDbAdapter.KEY_ROWID);
                long alarmId = intent.getExtras().getLong(ReminderManager.EXTRA_ALARM_ID);
                CoreOperations.notifyReminder(context, rowId, alarmId);
            } else {
                Logger.log("The received alarm is only for checking if there is a near future reminder.");
            }
            // In any case, we have to set the next alarm.
            alarmChainManager.setNextAlarm();
        } catch (Throwable t) {
            Logger.log("CRITICAL ERROR: an alarm has been triggered and couldn't be handled. The application has failed to handle an alarm.", t);
        }
    }

    /**
     * Called when the phone has just been turned on, this method manages the situation of
     * notifying any alarm that couldn't be notified when the phone was off and of setting the
     * system alarms for any upcoming reminder.
     */
    public synchronized static void phoneHasJustBeenTurnedOn(Context context) {
        new AlarmChainManager(context).setNextAlarm();
    }

    /**
     * Called when a reminder is deleted.
     * @param listActivity
     * @param mDbHelper
     * @param reminderDatabaseId
     */
    public synchronized static void reminderDeleted(ReminderListActivity listActivity, RemindersDbAdapter mDbHelper, long reminderDatabaseId) {
        mDbHelper.deleteReminder(reminderDatabaseId);
        new AlarmChainManager(listActivity).setNextAlarm();
        listActivity.createSpinnersAndFillData();
    }

    /**
     * Synchronized method called by the corresponding activity when the user uses that activity for
     * creating a reminder or for updating an existing one.
     *
     * @param reminderEditActivity the activity where the reminders are created and updated.
     * @param title                title of the reminder set by the user.
     * @param body                 body of the reminder set by the user.
     * @param reminderDateTime     date and time of the reminder set by the user.
     * @param currentAlarmId       the current alarm identifier associated at this moment to the reminder in the database.
     */
    public synchronized static void reminderCreatedOrUpdated(final ReminderEditActivity reminderEditActivity, String title, String body, Calendar reminderDateTime, Long currentAlarmId) {
        Long createdOrUpdatedReminderId =
                CoreOperations.createReminder(
            reminderEditActivity.mDbHelper,
            title,
            body,
            reminderDateTime,
            false,
            reminderEditActivity.mRowId,
            currentAlarmId
        );
        new AlarmChainManager(reminderEditActivity).setNextAlarm();
        if (reminderEditActivity.mRowId == null) {
            // Save the new id of the newly created event.
            reminderEditActivity.mRowId = createdOrUpdatedReminderId;
        }

        // Exit activity.
        if (createdOrUpdatedReminderId == null) {
            DialogInterface.OnClickListener listener = new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int which) {
                    dialog.dismiss();
                    reminderEditActivity.finish();
                }
            };
            UserInterfaceUtils.showWarningDialog(reminderEditActivity, reminderEditActivity.getResources().getString(R.string.unable_to_set_alarm), listener);
        } else {
            Toast.makeText(reminderEditActivity,
                    reminderEditActivity.getString(R.string.task_saved_message),
                    Toast.LENGTH_SHORT).show();
            reminderEditActivity.finish();
        }
    }

    public synchronized static DataImportResult importData(Context context, DataImport whereToPublishProgress, String jsonDataToImport) {
        RemindersDbAdapter dbAdapter = RemindersDbAdapter.getInstance(context);
        dbAdapter.open();
        DataImportResultType resultType;
        DataImportResult result;
        try {
            // Import new reminders.
            JSONArray reminders = new JSONArray(jsonDataToImport);
            JSONObject reminder;
            ImportedReminder importedReminder;
            List<ImportedReminder> importedReminders = new ArrayList<>();
            int length = reminders.length();
            int index;
            long errors = 0;
            for (index = 0;index < length;index++) {
                try {
                    reminder = reminders.getJSONObject(index);
                    importedReminder = new ImportedReminder(reminder);
                    importedReminders.add(importedReminder);
                } catch (JSONException e) {
                    errors++;
                }
            }

            // JSON exception could be thrown before of here if the file format is bad.

            // Delete old reminders.
            dbAdapter.deleteAllReminders();
            whereToPublishProgress.publishProgressFromOutside(0.5F);

            // Effectively create the imported reminders.
            int processed = 0;
            int importedRemindersSize = importedReminders.size();
            long now = new Date().getTime();
            for (ImportedReminder importedReminder2 : importedReminders) {
                // Past alarms are deliberately ignored here. If not and there were too many, what a mess!
                if (importedReminder2.getCalendar().getTimeInMillis() < now) {
                    importedReminder2.setAsAlreadyNotified();
                }
                CoreOperations.createReminder(dbAdapter, importedReminder2.getTitle(), importedReminder2.getBody(), importedReminder2.getCalendar(), importedReminder2.isNotified(), null, null);
                processed++;
                whereToPublishProgress.publishProgressFromOutside(((float) processed) / importedRemindersSize / 2 + 0.5f);
            }

            // Set next alarm.
            new AlarmChainManager(context).setNextAlarm();

            resultType = DataImportResultType.OK;
            result = new DataImportResult(resultType, importedReminders.size(), errors);
        } catch (JSONException e) {
            Logger.log("A data import hasn't been made because the user selected a file bad formatted. The current reminders have not been changed.");
            result = new DataImportResult(DataImportResultType.FORMAT_ERROR, 0, 0);
        } catch (Throwable t) {
            Logger.log("Unexpected error while deleting all reminders in a data import process.", t);
            result = new DataImportResult(DataImportResultType.UNKNOWN_ERROR, 0, 0);
        } finally {
            dbAdapter.close();
        }

        /*
         * Notice the reminders list activity (just in case it is open right now) to update the
         * reminders list.
         */
        Intent bufferIntentSendCode = new Intent(CoreOperations.BROADCAST_BUFFER_SEND_CODE);
        context.sendBroadcast(bufferIntentSendCode);

        return result;
    }

    public synchronized static boolean deleteAllData(Context context, DeleteAllReminders whereToPublishProgress) {
        RemindersDbAdapter dbAdapter = RemindersDbAdapter.getInstance(context);
        dbAdapter.open();
        DataImportResultType resultType;
        boolean result;
        try {
            // Delete all reminders.
            new ReminderManager(context).unsetAlarm();
            whereToPublishProgress.publishProgressFromOutside(0.5F);
            dbAdapter.deleteAllReminders();
            result = true;
        } catch (Throwable t) {
            Logger.log("Unexpected error while deleting all reminders in a data deletion process.", t);
            result = false;
        } finally {
            dbAdapter.close();
        }

        /*
         * Notice the reminders list activity (just in case it is open right now) to update the
         * reminders list.
         */
        Intent bufferIntentSendCode = new Intent(CoreOperations.BROADCAST_BUFFER_SEND_CODE);
        context.sendBroadcast(bufferIntentSendCode);

        return result;
    }
}
