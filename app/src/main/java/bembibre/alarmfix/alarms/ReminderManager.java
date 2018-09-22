package bembibre.alarmfix.alarms;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import java.util.Calendar;
import java.util.Date;

import bembibre.alarmfix.ReminderListActivity;
import bembibre.alarmfix.database.RemindersDbAdapter;
import bembibre.alarmfix.logging.Logger;
import bembibre.alarmfix.models.Alarm;
import bembibre.alarmfix.utils.GeneralUtils;

/**
 * Sets alarms in the operating system for the reminders of this application.
 */
public class ReminderManager {

    /**
     * Maximum delay that we consider that an alarm can be delayed to go off without deeming it
     * as a failure.
     */
    private static final long ALARM_MAX_DELAY = 120000;

    /**
     * This is the key that identifies a metadata item that is attached to the intent of an alarm of
     * a reminder for telling apart different alarms that could be set for the same reminder.
     */
    public static final String EXTRA_REMINDER_ALARM_ID = "extra_alarm_id";

    /**
     * This is the key that identifies a metadata item that is attached to the intent of an alarm of
     * a reminder where that item represents the database identifier of a row of the alarms table,
     * in which this application saves a row for every alarm set in the operating system which is
     * pending to trigger. This table is used for detecting failures, because every alarm that
     * triggers properly deletes its associated row from this table, so, if there is a row associated
     * to a past alarm, that means that its alarm wasn't triggered.
     */
    public static final String EXTRA_PENDING_ALARM_REFERENCE_ID = "extra_set_alarm_id";

    private Context mContext;
    private AlarmManager mAlarmManager;

    public ReminderManager(Context context) {
        mContext = context;
        mAlarmManager =
                (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
    }

    /**
     * Part of the code that is responsible for setting an alarm.
     *
     * @param taskId  database identifier of the reminder.
     * @param alarmId number that helps distinguishing each one of the alarms set for a same reminder.
     * @param when    when.
     */
    public void setReminder(long taskId, long alarmId, Calendar when) throws AlarmException {
        Intent i = new Intent(mContext, OnAlarmReceiver.class);
        i.putExtra(RemindersDbAdapter.REMINDERS_COLUMN_ROWID, taskId);
        i.putExtra(ReminderManager.EXTRA_REMINDER_ALARM_ID, alarmId);

        try {
            this.setAlarmInSystemAndDatabase(i, when, taskId);
            Logger.log("An alarm has been set successfully for the reminder at " + GeneralUtils.format(when) + ". Reminder id: " + taskId);
        } catch (Throwable throwable) {
            Logger.log("The system doesn't let us to set an alarm for the reminder at " + GeneralUtils.format(when));
            throw new AlarmException(throwable);
        }
    }

    public void setNextAlarmCheckReminder(Calendar when) throws AlarmException {
        Intent i = new Intent(mContext, OnAlarmReceiver.class);
        try {
            this.setAlarmInSystemAndDatabase(i, when, null);
            Logger.log("An alarm has been set successfully for the next reminder check at " + GeneralUtils.format(when) + ".");
        } catch (Throwable throwable) {
            Logger.log("The system doesn't let us to set an alarm for the next reminder check at " + GeneralUtils.format(when));
            throw new AlarmException(throwable);
        }
    }

    /**
     * Unsets any alarm either for reminder or for next alarm check that was set through this class.
     *
     * Also, deletes all references to those alarms that could be stored at database.
     */
    public void unsetAlarm() {
        RemindersDbAdapter dbAdapter = RemindersDbAdapter.getInstance(this.mContext);
        dbAdapter.open();

        try {
            dbAdapter.beginTransaction();

            // Delete alarm reference in the database.
            dbAdapter.deleteAllAlarms();

            // Cancel the alarm into the Android system.
            Intent i = new Intent(mContext, OnAlarmReceiver.class);
            PendingIntent pi = getReminderPendingIntent(i);
            mAlarmManager.cancel(pi);

            // No exceptions, all okay.
            dbAdapter.setTransactionSuccessful();

            Logger.log("Every alarm references have been deleted from the database.");
        } finally {
            dbAdapter.endTransaction();
            dbAdapter.close();
        }
    }

    /**
     * Returns true if and only if alarms health is allright, in other words, if there isn't any
     * past alarm that has failed to go off in time.
     *
     * Note that nowadays almost every phone includes a fucking power manager that prevents alarms
     * of applications to go off because it is thought that it saves battery.
     *
     * @return true if there isn't any past alarm that failed to go off.
     */
    public boolean checkAlarmsHealth() {
        boolean result;

        RemindersDbAdapter dbAdapter = RemindersDbAdapter.getInstance(this.mContext);
        dbAdapter.open();

        Logger.log("Checking alarms health...");

        try {
            Alarm earliestAlarm = dbAdapter.fetchEarliestAlarm();
            if (earliestAlarm == null) {
                Logger.log("There isn't any alarm stored in the database (alarms in the database can be either for reminders created by the user or for periodic tasks of the application itself to check for future user reminders).");
                result = true;
            } else {
                long limit = new Date().getTime() - ALARM_MAX_DELAY;
                if (earliestAlarm.getTime() < limit) {
                    Logger.log("There is a pending alarm reference in the database that is earlier than now so there has been a failure.");
                    result = false;
                } else {
                    Logger.log("The earliest alarm in the database is for the future so everything is all right.");
                    result = true;
                }
            }
        } finally {
            dbAdapter.close();
        }
        return result;
    }

    /**
     * Returns the <code>PendingIntent</code> object that must be used for calling this application
     * when a reminder's alarm triggers.
     *
     * @param i the intent to be used.
     * @return the <code>PendingIntent</code> object.
     */
    private PendingIntent getReminderPendingIntent(Intent i) {
        PendingIntent pi = PendingIntent.getBroadcast(mContext, 0, i, PendingIntent.FLAG_UPDATE_CURRENT);
        return pi;
    }

    /**
     * Sets the alarm in the operating system.
     *
     * @param operation
     * @param when
     */
    private void setAlarm(PendingIntent operation, Calendar when) throws Throwable {
        /*
         * The alarm must be set differently depending on the OS version. Anyway, we need the
         * pending intent in order to know what was the reminder for which the alarm was fired, so
         * then the correct notification will be shown.
         */
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            // Before Marshmallow, we can do this for setting a reliable alarm.
            mAlarmManager.set(AlarmManager.RTC_WAKEUP, when.getTimeInMillis(), operation);
        } else {
            /*
             * Starting from Marshmallow, it seems like this is the only way for setting a reliable
             * alarm.
             * If we use the "alarm clock" framework, the user will see a icon of an alarm clock.
             * If we use the setExactAndAllowWhileIdle the user will see nothing, but the OS can
             * delay alarms at some sort of situations.
             */
            //mAlarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, when.getTimeInMillis(), operation);
            Intent alarmClockIntent = new Intent(this.mContext, ReminderListActivity.class);
            PendingIntent showIntent = PendingIntent.getActivity(this.mContext, 0, alarmClockIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT);
            mAlarmManager.setAlarmClock(new AlarmManager.AlarmClockInfo(when.getTimeInMillis(), showIntent), operation);
        }
    }

    private void setAlarmInSystemAndDatabase(Intent intent, Calendar when, Long taskId) throws Throwable {
        RemindersDbAdapter dbAdapter = RemindersDbAdapter.getInstance(this.mContext);
        dbAdapter.open();

        try {
            dbAdapter.beginTransaction();

            long setAlarmIdentifier = dbAdapter.createAlarm(when.getTimeInMillis(), taskId);
            intent.putExtra(ReminderManager.EXTRA_PENDING_ALARM_REFERENCE_ID, setAlarmIdentifier);
            PendingIntent operation = getReminderPendingIntent(intent);
            this.setAlarm(operation, when);

            // No exceptions, all okay.
            dbAdapter.setTransactionSuccessful();

            Logger.log("An alarm reference has been stored at the database at " + GeneralUtils.format(when) + ". Alarm reference id: " + setAlarmIdentifier);
        } finally {
            dbAdapter.endTransaction();
            dbAdapter.close();
        }
    }
}