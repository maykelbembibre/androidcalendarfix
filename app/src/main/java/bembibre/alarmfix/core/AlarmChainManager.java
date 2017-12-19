package bembibre.alarmfix.core;

import android.content.Context;
import android.database.Cursor;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import bembibre.alarmfix.alarms.AlarmException;
import bembibre.alarmfix.alarms.ReminderManager;
import bembibre.alarmfix.database.RemindersDbAdapter;
import bembibre.alarmfix.logging.Logger;
import bembibre.alarmfix.models.DateTime;
import bembibre.alarmfix.userinterface.NotificationManager;

/**
 * Class in charge of setting an alarm chain in which one single alarm is always set in the
 * operating system, either for the next reminder, or for checking further reminders when no
 * reminders are found in a near future.
 */
class AlarmChainManager {

    private static final int NEAR_FUTURE_MILLIS = 24 * 3600 * 1000;
    private static final int DOZE_LIMIT_MILLIS = 15 * 60 * 1000;

    private Context context;

    AlarmChainManager(Context context) {
        this.context = context;
    }

    void setNextAlarm() {
        boolean alarmSet = false;
        ReminderManager reminderManager = new ReminderManager(this.context);
        reminderManager.unsetAlarm();
        RemindersDbAdapter dbAdapter = RemindersDbAdapter.getInstance(this.context);
        dbAdapter.open();
        try {
            Cursor cursor = dbAdapter.fetchAllNotNotifiedReminders();
            if (cursor == null) {
                Logger.log("CRITICAL SITUATION: database returned a null cursor while trying to set the alarm for the next reminder. If the database is not empty of reminders then this is a critical failure and this application's code should be reviewed for corrections.");
            } else {
                List<String> pendingReminders = new ArrayList<>();
                cursor.moveToFirst();
                int rowIdColumnIndex = cursor.getColumnIndex(RemindersDbAdapter.KEY_ROWID);
                int dateTimeColumnIndex = cursor.getColumnIndex(RemindersDbAdapter.KEY_DATE_TIME);
                while (!cursor.isAfterLast()) {
                    long rowId = cursor.getLong(rowIdColumnIndex);
                    DateTime dateTime = new DateTime(cursor.getLong(dateTimeColumnIndex));

                    // Handle not notified reminder.
                    if (dateTime.toMillisecondsSinceTheEpoch() <= CoreOperations.getNowDateTimeWithinAWhile()) {
                        // Past reminder.
                        pendingReminders.add(cursor.getString(cursor.getColumnIndex(RemindersDbAdapter.KEY_TITLE)));

                        // All the pending reminders are going to be notified.
                        dbAdapter.updateReminder(rowId, true);
                    } else {
                        // There is at least one future reminder.
                        long nearFutureLimit = CoreOperations.getNowDateTimeWithinAWhile() + NEAR_FUTURE_MILLIS;
                        try {
                            if (dateTime.toMillisecondsSinceTheEpoch() < nearFutureLimit) {
                                /*
                                 * The first future reminder  date is before of what we consider the time
                                 * limit for a near future.
                                 */
                                Calendar cal = Calendar.getInstance();
                                cal.setTime(new Date(dateTime.toMillisecondsSinceTheEpoch()));
                                long alarmId = cursor.getLong(cursor.getColumnIndexOrThrow(RemindersDbAdapter.KEY_ALARM_ID));
                                reminderManager.setReminder(rowId, alarmId, cal);
                            } else {
                                // There is at least one future reminder but it is too far away.
                                Calendar when = Calendar.getInstance();
                                when.setTimeInMillis(nearFutureLimit - DOZE_LIMIT_MILLIS);
                                reminderManager.setNextAlarmCheckReminder(when);
                            }
                        } catch (AlarmException e) {
                            Logger.log("CRITICAL FAILURE: the next alarm cannot be set because of an unexpected exception", e.getCause());
                        }
                        alarmSet = true;
                        // Only the first future reminder is set.
                        break;
                    }

                    cursor.moveToNext();
                }
                try {
                    new NotificationManager(context).notifyMultipleReminders(pendingReminders);
                } catch (Exception e) {
                    Logger.log("Unable to notify pending reminders because there was an exception.", e);
                }
                cursor.close();
                if (!alarmSet) {
                    Logger.log("No need for setting any alarm for now. The user hasn't got any future reminder.");
                }
            }
        } finally {
            dbAdapter.close();
        }
    }
}
