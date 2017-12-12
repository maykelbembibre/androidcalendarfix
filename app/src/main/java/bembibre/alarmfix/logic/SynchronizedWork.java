package bembibre.alarmfix.logic;

/**
 * Created by Max Power on 08/12/2017.
 */

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Arrays;
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
import bembibre.alarmfix.models.DateTime;
import bembibre.alarmfix.userinterface.NotificationManager;
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
        Cursor cursor = mDbHelper.fetchReminder(reminderDatabaseId);
        Long date;
        Long reminderId;
        String dateAsString;
        if ((cursor != null) && (cursor.moveToFirst())) {
            date = cursor.getLong(cursor.getColumnIndexOrThrow(RemindersDbAdapter.KEY_DATE_TIME));
            reminderId = cursor.getLong(cursor.getColumnIndexOrThrow(RemindersDbAdapter.KEY_ALARM_ID));
        } else {
            date = null;
            reminderId = null;
        }
        if (date == null) {
            dateAsString = "unknown";
        } else {
            dateAsString = new DateTime(date).toString();
        }

        if (reminderId == null) {
            Logger.log("Database error while the user tried to delete a reminder.");
        } else {
            mDbHelper.deleteReminder(reminderDatabaseId);
            new ReminderManager(listActivity).unsetReminder(reminderDatabaseId, reminderId, dateAsString);
        }
        listActivity.createSpinnersAndFillData();
    }

    /**
     * Synchronized method called by the corresponding activity when the user uses that activity for
     * creating a reminder or for updating an existing one.
     * @param reminderEditActivity the activity where the reminders are created and updated.
     * @param title title of the reminder set by the user.
     * @param body body of the reminder set by the user.
     * @param reminderDateTime date and time of the reminder set by the user.
     * @param currentAlarmId the current alarm identifier associated at this moment to the reminder in the database.
     */
    public synchronized static void reminderCreatedOrUpdated(final ReminderEditActivity reminderEditActivity, String title, String body, DateTime reminderDateTime, long currentAlarmId) {
        long id;
        boolean ok;
        try {
            long alarmId;

            reminderEditActivity.mDbHelper.beginTransaction();

            if (reminderEditActivity.mRowId == null) {
                id = reminderEditActivity.mDbHelper.createReminder(title, body, reminderDateTime.toMillisecondsSinceTheEpoch());
                alarmId = RemindersDbAdapter.FIRST_ALARM_ID;

                if (id > 0) {
                    reminderEditActivity.mRowId = id;
                }
            } else {
                id = reminderEditActivity.mRowId;
                alarmId = currentAlarmId + 1;
                reminderEditActivity.mDbHelper.updateReminder(reminderEditActivity.mRowId, title, body, reminderDateTime.toMillisecondsSinceTheEpoch(), alarmId);
            }

            /*
             * Can throw exception.
             * If a reminder update has happened, the alarm for the old time is cancelled automatically.
             */
            new ReminderManager(reminderEditActivity).setReminder(id, alarmId, reminderEditActivity.mCalendar);

            // No exceptions, all okay.
            reminderEditActivity.mDbHelper.setTransactionSuccessful();

            ok = true;
        } catch (Exception e) {
            /*
             * AlarmException can be thrown when setting the alarm.
             *
             * Transaction is not set as successful here, so it will rollback and the
             * reminder won't be created.
             */
            ok = false;
        } finally {
            reminderEditActivity.mDbHelper.endTransaction();
        }

        // Exit activity.
        if (ok) {
            Toast.makeText(reminderEditActivity,
                    reminderEditActivity.getString(R.string.task_saved_message),
                    Toast.LENGTH_SHORT).show();
            reminderEditActivity.finish();
        } else {
            DialogInterface.OnClickListener listener = new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int which) {
                    dialog.dismiss();
                    reminderEditActivity.finish();
                }
            };
            UserInterfaceUtils.showDialog(reminderEditActivity, R.string.unable_to_set_alarm, listener);
        }
    }


    public static final String BROADCAST_BUFFER_SEND_CODE = "com.example.SEND_CODE";
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
     * Returns as milliseconds since the Epoch the date and time of an instant that is some minutes
     * away from now.
     */
    private static long getNowDateTimeWithinAWhile() {
        Calendar nowCalendar = Calendar.getInstance();
        nowCalendar.add(Calendar.MINUTE, 10);
        return nowCalendar.getTime().getTime();
    }
}
