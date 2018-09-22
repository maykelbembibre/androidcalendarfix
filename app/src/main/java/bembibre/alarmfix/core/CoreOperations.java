package bembibre.alarmfix.core;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;

import java.util.Calendar;

import bembibre.alarmfix.ReminderListActivity;
import bembibre.alarmfix.database.RemindersDbAdapter;
import bembibre.alarmfix.logging.Logger;
import bembibre.alarmfix.models.DateTime;

/**
 * Class that has got the critical application core operations regarding reminders and alarms.
 */
public class CoreOperations {

    public static final String BROADCAST_BUFFER_SEND_CODE = "com.example.SEND_CODE";

    static void notifyReminder(Context context, long rowId, long receivedAlarmId) throws Exception {
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
                    reminderTitle = cursor.getString(cursor.getColumnIndexOrThrow(RemindersDbAdapter.REMINDERS_COLUMN_TITLE));
                    alarmId = cursor.getLong(cursor.getColumnIndexOrThrow(RemindersDbAdapter.REMINDERS_COLUMN_ALARM_ID));

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
        ReminderListActivity.updateActivity(context);
    }

    /**
     * Does the operation of creating or updating a reminder.
     *
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
    static Long createReminder(RemindersDbAdapter dbAdapter, String title, String body, Calendar reminderCalendar, boolean notified, Long updatedReminderId, Long currentAlarmId) {
        Long id;
        DateTime reminderDateTime = new DateTime(reminderCalendar);
        try {
            dbAdapter.beginTransaction();

            if (updatedReminderId == null) {
                id = dbAdapter.createReminder(title, body, reminderDateTime.toMillisecondsSinceTheEpoch(), notified);
            } else {
                id = updatedReminderId;
                dbAdapter.updateReminder(id, title, body, reminderDateTime.toMillisecondsSinceTheEpoch(), currentAlarmId + 1);
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
     * Returns as milliseconds since the Epoch the date and time of an instant that is slightly
     * later from now.
     */
    public static long getNowDateTimeWithinAWhile() {
        Calendar nowCalendar = Calendar.getInstance();
        nowCalendar.add(Calendar.SECOND, 30);
        return nowCalendar.getTime().getTime();
    }

}
