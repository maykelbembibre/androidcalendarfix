package bembibre.alarmfix.core;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;

import java.util.Calendar;
import java.util.Date;

import bembibre.alarmfix.alarms.ReminderManager;
import bembibre.alarmfix.database.RemindersDbAdapter;
import bembibre.alarmfix.logging.Logger;
import bembibre.alarmfix.models.DateTime;
import bembibre.alarmfix.utils.GeneralUtils;

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
    public static Long createReminderAndAlarm(Context context, RemindersDbAdapter dbAdapter, String title, String body, Calendar reminderCalendar, Long updatedReminderId, Long currentAlarmId, boolean ignorePast) {
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
    public static long getNowDateTimeWithinAWhile() {
        Calendar nowCalendar = Calendar.getInstance();
        nowCalendar.add(Calendar.MINUTE, 10);
        return nowCalendar.getTime().getTime();
    }

    public static void deleteReminderAndItsAlarm(Context context, RemindersDbAdapter mDbHelper, long reminderDatabaseId, String dateAsString) {
        mDbHelper.deleteReminder(reminderDatabaseId);
        new ReminderManager(context).unsetReminder(reminderDatabaseId, dateAsString);
    }

    public static void deleteReminderAndItsAlarm(Context context, RemindersDbAdapter mDbHelper, long reminderDatabaseId) {
        Cursor cursor = mDbHelper.fetchReminder(reminderDatabaseId);
        Long date;
        String dateAsString;
        if ((cursor != null) && (cursor.moveToFirst())) {
            date = cursor.getLong(cursor.getColumnIndexOrThrow(RemindersDbAdapter.KEY_DATE_TIME));
        } else {
            date = null;
        }
        if (date == null) {
            dateAsString = "unknown";
        } else {
            dateAsString = new DateTime(date).toString();
        }
        deleteReminderAndItsAlarm(context, mDbHelper, reminderDatabaseId, dateAsString);
        cursor.close();
    }
}
