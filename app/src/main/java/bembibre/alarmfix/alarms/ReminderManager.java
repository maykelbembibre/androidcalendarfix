package bembibre.alarmfix.alarms;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import java.util.Calendar;

import bembibre.alarmfix.database.RemindersDbAdapter;
import bembibre.alarmfix.logging.Logger;
import bembibre.alarmfix.utils.GeneralUtils;

/**
 * Sets alarms in the operating system for the reminders of this application.
 */
public class ReminderManager {

    /**
     * This is the key that identifies a metadata item that is attached to the intent of an alarm of
     * a reminder for tracking it.
     */
    public static final String EXTRA_ALARM_ID = "extra_alarm_id";

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
     * @param taskId  data base identifier of the reminder.
     * @param alarmId number that helps distinguishing each one of the alarms set for a same reminder.
     * @param when    when.
     */
    public void setReminder(long taskId, long alarmId, Calendar when) throws AlarmException {
        Intent i = new Intent(mContext, OnAlarmReceiver.class);
        i.putExtra(RemindersDbAdapter.KEY_ROWID, taskId);
        i.putExtra(ReminderManager.EXTRA_ALARM_ID, alarmId);
        PendingIntent pi = getReminderPendingIntent(i);

        try {
            this.setAlarm(pi, when);
            Logger.log("An alarm has been set successfully for the reminder at " + GeneralUtils.format(when) + ". Reminder id: " + taskId);
        } catch (Throwable throwable) {
            Logger.log("The system doesn't let us to set an alarm for the reminder at " + GeneralUtils.format(when));
            throw new AlarmException(throwable);
        }
    }

    public void setNextAlarmCheckReminder(Calendar when) throws AlarmException {
        Intent i = new Intent(mContext, OnAlarmReceiver.class);
        PendingIntent pi = getReminderPendingIntent(i);
        try {
            this.setAlarm(pi, when);
            Logger.log("An alarm has been set successfully for the next reminder check at " + GeneralUtils.format(when) + ".");
        } catch (Throwable throwable) {
            Logger.log("The system doesn't let us to set an alarm for the next reminder check at " + GeneralUtils.format(when));
            throw new AlarmException(throwable);
        }
    }

    /**
     * Unsets the alarm, if it is set.
     */
    public void unsetAlarm() {
        Intent i = new Intent(mContext, OnAlarmReceiver.class);
        PendingIntent pi = getReminderPendingIntent(i);
        mAlarmManager.cancel(pi);
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
            mAlarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, when.getTimeInMillis(), operation);
        }
    }
}