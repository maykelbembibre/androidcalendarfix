package bembibre.alarmfix.logic;

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
import bembibre.alarmfix.alarms.AlarmException;
import bembibre.alarmfix.alarms.ReminderManager;
import bembibre.alarmfix.database.RemindersDbAdapter;
import bembibre.alarmfix.logging.Logger;
import bembibre.alarmfix.logic.exportimport.DataImport;
import bembibre.alarmfix.logic.models.DataImportResultType;
import bembibre.alarmfix.logic.models.ImportedReminder;
import bembibre.alarmfix.logic.models.DataImportResult;
import bembibre.alarmfix.models.DateTime;
import bembibre.alarmfix.userinterface.NotificationManager;
import bembibre.alarmfix.userinterface.UserInterfaceUtils;
import bembibre.alarmfix.utils.GeneralUtils;

/**
 * Class for all the delicate work that must be synchronized between threads.
 */
public class SynchronizedWork {

    public static final String BROADCAST_BUFFER_SEND_CODE = "com.example.SEND_CODE";

    /**
     * Synchronized method called when an alarm for a reminder has been triggered.
     *
     * @param context application context.
     * @param intent intent of the alarm.
     */
    public synchronized static void reminderAlarmReceived(Context context, Intent intent) {
        try {
            long rowId = intent.getExtras().getLong(RemindersDbAdapter.KEY_ROWID);
            long alarmId = intent.getExtras().getLong(ReminderManager.EXTRA_ALARM_ID);
            SynchronizedWork.notifyReminder(context, rowId, alarmId);
        } catch (Throwable t) {
            Logger.log("CRITICAL ERROR: an alarm has been triggered and couldn't be handled. The application has failed to notify an alarm.", t);
        }
    }

    /**
     * Called when the phone has just been turned on, this method manages the situation of
     * notifying any alarm that couldn't be notified when the phone was off and of setting the
     * system alarms for any upcoming reminder.
     * @param reminderManager
     * @param dbHelper
     */
    public synchronized static void phoneHasJustBeenTurnedOn(Context context, ReminderManager reminderManager, RemindersDbAdapter dbHelper) {
        dbHelper.open();
        try {
            Cursor cursor = dbHelper.fetchAllNotNotifiedReminders();
            if (cursor != null) {
                long reference = SynchronizedWork.getNowDateTimeWithinAWhile();
                List<String> pendingReminders = new ArrayList<>();
                cursor.moveToFirst();
                int rowIdColumnIndex = cursor.getColumnIndex(RemindersDbAdapter.KEY_ROWID);
                int dateTimeColumnIndex = cursor.getColumnIndex(RemindersDbAdapter.KEY_DATE_TIME);
                int remindersSet = 0;
                int remindersNotSetAlarmException = 0;
                while (!cursor.isAfterLast()) {
                    long rowId = cursor.getLong(rowIdColumnIndex);
                    DateTime dateTime = new DateTime(cursor.getLong(dateTimeColumnIndex));

                    // Handle not notified reminder.
                    if (dateTime.toMillisecondsSinceTheEpoch() < reference) {
                        // Past reminder.

                        pendingReminders.add(cursor.getString(cursor.getColumnIndex(RemindersDbAdapter.KEY_TITLE)));

                        // All the pending reminders are going to be notified.
                        dbHelper.updateReminder(rowId, true);
                    } else {
                        // Future reminder.

                        Calendar cal = Calendar.getInstance();
                        try {
                            cal.setTime(new Date(dateTime.toMillisecondsSinceTheEpoch()));
                            long alarmId = cursor.getLong(cursor.getColumnIndexOrThrow(RemindersDbAdapter.KEY_ALARM_ID));
                            reminderManager.setReminder(rowId, alarmId, cal);
                            remindersSet++;
                        } catch (AlarmException e) {
                            remindersNotSetAlarmException++;
                        }
                    }

                    cursor.moveToNext();
                }
                new NotificationManager(context).notifyMultipleReminders(pendingReminders);
                Logger.log("Telephone has been turned on. Reminders set for the future: " + remindersSet + ". Reminders not set because of an alarm problem: " + remindersNotSetAlarmException + ".");
                cursor.close();
            }
        } finally {
            dbHelper.close();
        }
    }

    /**
     * Called when a reminder is deleted.
     * @param listActivity
     * @param mDbHelper
     * @param reminderDatabaseId
     */
    public synchronized static void reminderDeleted(ReminderListActivity listActivity, RemindersDbAdapter mDbHelper, long reminderDatabaseId) {
        SynchronizedWork.deleteReminderAndItsAlarm(listActivity, mDbHelper, reminderDatabaseId);
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
        SynchronizedWork.createReminderAndAlarm(
            reminderEditActivity,
            reminderEditActivity.mDbHelper,
            title,
            body,
            reminderDateTime,
            reminderEditActivity.mRowId,
            currentAlarmId,
            false
        );
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
            long totalReminders = dbAdapter.countAllReminders();
            String dateAsString;
            long processedReminders = 0;

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
            Cursor cursor = dbAdapter.fetchAllReminders(0);
            while ((cursor != null) && (cursor.moveToFirst())) {
                Logger.log("Data import: deleting " + cursor.getCount() + " reminders.");
                do {
                    long row_id = cursor.getLong(cursor.getColumnIndexOrThrow(RemindersDbAdapter.KEY_ROWID));
                    long alarm_id = cursor.getLong(cursor.getColumnIndexOrThrow(RemindersDbAdapter.KEY_ALARM_ID));
                    long dateTime = cursor.getLong(cursor.getColumnIndexOrThrow(RemindersDbAdapter.KEY_DATE_TIME));
                    dateAsString = new DateTime(dateTime).toString();

                    SynchronizedWork.deleteReminderAndItsAlarm(context, dbAdapter, row_id, alarm_id, dateAsString);

                    processedReminders++;
                    whereToPublishProgress.publishProgressFromOutside(((float) processedReminders) / totalReminders / 2);
                } while (cursor.moveToNext());
                cursor.close();

                /*
                 * Page is always the first because reminders are getting deleted one by one
                 * gingerly.
                 */
                cursor = dbAdapter.fetchAllReminders(0);
            }
            if (cursor != null) {
                cursor.close();
            }

            // Effectively create the imported reminders.
            int processed = 0;
            int importedRemindersSize = importedReminders.size();
            for (ImportedReminder importedReminder2 : importedReminders) {
                // Past alarms are deliberately ignored here. If not and there were too many, what a mess!
                SynchronizedWork.createReminderAndAlarm(context, dbAdapter, importedReminder2.getTitle(), importedReminder2.getBody(), importedReminder2.getCalendar(), null, null, true);
                processed++;
                whereToPublishProgress.publishProgressFromOutside(((float) processed) / importedRemindersSize / 2 + 0.5f);
            }

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
        Intent bufferIntentSendCode = new Intent(BROADCAST_BUFFER_SEND_CODE);
        context.sendBroadcast(bufferIntentSendCode);

        return result;
    }

    synchronized static boolean deleteAllData(Context context, DeleteAllReminders whereToPublishProgress) {
        RemindersDbAdapter dbAdapter = RemindersDbAdapter.getInstance(context);
        dbAdapter.open();
        DataImportResultType resultType;
        boolean result;
        try {
            // Delete all reminders.
            long totalReminders = dbAdapter.countAllReminders();
            String dateAsString;
            long processedReminders = 0;
            Cursor cursor = dbAdapter.fetchAllReminders(0);
            while ((cursor != null) && (cursor.moveToFirst())) {
                Logger.log("Data delete: deleting " + cursor.getCount() + " reminders.");
                do {
                    long row_id = cursor.getLong(cursor.getColumnIndexOrThrow(RemindersDbAdapter.KEY_ROWID));
                    long alarm_id = cursor.getLong(cursor.getColumnIndexOrThrow(RemindersDbAdapter.KEY_ALARM_ID));
                    long dateTime = cursor.getLong(cursor.getColumnIndexOrThrow(RemindersDbAdapter.KEY_DATE_TIME));
                    dateAsString = new DateTime(dateTime).toString();

                    SynchronizedWork.deleteReminderAndItsAlarm(context, dbAdapter, row_id, alarm_id, dateAsString);

                    processedReminders++;
                    whereToPublishProgress.publishProgressFromOutside(((float) processedReminders) / totalReminders);
                } while (cursor.moveToNext());
                cursor.close();

                /*
                 * Page is always the first because reminders are getting deleted one by one
                 * gingerly.
                 */
                cursor = dbAdapter.fetchAllReminders(0);
            }
            if (cursor != null) {
                cursor.close();
            }

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
        Intent bufferIntentSendCode = new Intent(BROADCAST_BUFFER_SEND_CODE);
        context.sendBroadcast(bufferIntentSendCode);

        return result;
    }

    private static void notifyReminder(Context context, long rowId, long receivedAlarmId) throws Exception {
        // Status bar notification Code Goes here.
        String reminderTitle;
        long alarmId;
        RemindersDbAdapter dbHelper = RemindersDbAdapter.getInstance(context);
        dbHelper.open();
        try {
            Cursor cursor = dbHelper.fetchReminder(rowId);
            if (cursor != null) {
                if (cursor.moveToFirst()) {
                    Logger.log("A reminder with identifier " + rowId + " is going to be notified to the user.");
                    reminderTitle = cursor.getString(cursor.getColumnIndexOrThrow(RemindersDbAdapter.KEY_TITLE));
                    alarmId = cursor.getLong(cursor.getColumnIndexOrThrow(RemindersDbAdapter.KEY_ALARM_ID));

                    /*
                     * If the alarm identified of the database doesn't match with the received one,
                     * then this would be an old alarm and shouldn't do anything.
                     */
                    if (alarmId == receivedAlarmId) {
                        // Send notification to the user.
                        new bembibre.alarmfix.userinterface.NotificationManager(context).notifySingleReminder(rowId, reminderTitle);

                        // Mark the reminder as notified.
                        dbHelper.updateReminder(rowId, true);
                    } else {
                        Logger.log("An alarm has been received with the alarm identifier " + receivedAlarmId + " but the alarm identifier that this reminder has got currently is " + alarmId + ", so no notification will be made.");
                    }
                } else {
                    throw new Exception("There's no way for accessing database for getting information about a reminder which has to be notified to the user.");
                }
                cursor.close();
            }
        } finally {
            dbHelper.close();
        }

        /*
         * Notice the reminders list activity (just in case it is open right now) to update the
         * reminders list.
         */
        Intent bufferIntentSendCode = new Intent(BROADCAST_BUFFER_SEND_CODE);
        context.sendBroadcast(bufferIntentSendCode);
    }

    /**
     * Does the operation of creating or updating a reminder.
     *
     * @param context application context.
     * @param dbAdapter object for accessing the database, the caller must open an close it properly.
     * @param title title for the reminder.
     * @param body body for the reminder.
     * @param reminderCalendar calendar representing date and time for the reminder.
     * @param updatedReminderId <code>null</code> creates a new reminder, otherwise the reminder
     * with the given identifier is updated.
     * @param currentAlarmId when updating a reminder, pass here the current alarm identifier that
     * it has got. It will be saved with an increased alarm identifier so that the old alarm does
     * nothing even if it was fired.
     * @return identifier of the created or updated reminder or <code>null</code> if there was a problem.
     */
    private static Long createReminderAndAlarm(Context context, RemindersDbAdapter dbAdapter, String title, String body, Calendar reminderCalendar, Long updatedReminderId, Long currentAlarmId, boolean ignorePast) {
        Long id;
        long alarmId;
        DateTime reminderDateTime = new DateTime(reminderCalendar);
        long now = new Date().getTime();
        try {
            dbAdapter.beginTransaction();

            if (updatedReminderId == null) {
                id = dbAdapter.createReminder(title, body, reminderDateTime.toMillisecondsSinceTheEpoch());
                alarmId = RemindersDbAdapter.FIRST_ALARM_ID;
            } else {
                id = updatedReminderId;
                alarmId = currentAlarmId + 1;
                dbAdapter.updateReminder(id, title, body, reminderDateTime.toMillisecondsSinceTheEpoch(), alarmId);
            }

            /*
             * Can throw exception.
             * If a reminder update has happened, the alarm for the old time is cancelled automatically.
             */
            if ((ignorePast) && (reminderDateTime.toMillisecondsSinceTheEpoch() <= now)) {
                Logger.log("An alarm has been ignored because it is past, for the reminder at " + GeneralUtils.format(reminderCalendar) + ". Reminder id: " + id);
            } else {
                new ReminderManager(context).setReminder(id, alarmId, reminderCalendar);
            }

            // No exceptions, all okay.
            dbAdapter.setTransactionSuccessful();
        } catch (Exception e) {
            /*
             * AlarmException can be thrown when setting the alarm.
             * Transaction is not set as successful here, so it will rollback and the
             * reminder won't be created.
             */
            id = null;
        } finally {
            dbAdapter.endTransaction();
        }
        return id;
    }

    /**
     * Returns as milliseconds since the Epoch the date and time of an instant that is some minutes
     * away from now.
     */
    private static long getNowDateTimeWithinAWhile() {
        Calendar nowCalendar = Calendar.getInstance();
        nowCalendar.add(Calendar.MINUTE, 10);
        return nowCalendar.getTime().getTime();
    }

    private static void deleteReminderAndItsAlarm(Context context, RemindersDbAdapter mDbHelper, long reminderDatabaseId) {
        Cursor cursor = mDbHelper.fetchReminder(reminderDatabaseId);
        Long date;
        Long alarmId;
        String dateAsString;
        if ((cursor != null) && (cursor.moveToFirst())) {
            date = cursor.getLong(cursor.getColumnIndexOrThrow(RemindersDbAdapter.KEY_DATE_TIME));
            alarmId = cursor.getLong(cursor.getColumnIndexOrThrow(RemindersDbAdapter.KEY_ALARM_ID));
        } else {
            date = null;
            alarmId = null;
        }
        if (date == null) {
            dateAsString = "unknown";
        } else {
            dateAsString = new DateTime(date).toString();
        }
        if (alarmId == null) {
            Logger.log("Database error while the user tried to delete a reminder.");
        } else {
            deleteReminderAndItsAlarm(context, mDbHelper, reminderDatabaseId, alarmId, dateAsString);
        }
        cursor.close();
    }

    private static void deleteReminderAndItsAlarm(Context context, RemindersDbAdapter mDbHelper, long reminderDatabaseId, long alarmId, String dateAsString) {
        mDbHelper.deleteReminder(reminderDatabaseId);
        new ReminderManager(context).unsetReminder(reminderDatabaseId, alarmId, dateAsString);
    }
}
