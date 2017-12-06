package bembibre.alarmfix.alarms;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import java.util.Calendar;

import bembibre.alarmfix.database.RemindersDbAdapter;

/**
 * Created by Max Power on 12/08/2017.
 */

/**
 * Sets alarms in the OS.
 */
public class ReminderManager {
    private Context mContext;
    private AlarmManager mAlarmManager;
    public ReminderManager(Context context) {
        mContext = context;
        mAlarmManager =
                (AlarmManager)context.getSystemService(Context.ALARM_SERVICE);
    }

    /**
     * Part of the code that is responsible for setting an alarm.
     * @param taskId data base identifier of the reminder.
     * @param when when.
     */
    public void setReminder(Long taskId, Calendar when) {
        Intent i = new Intent(mContext, OnAlarmReceiver.class);
        i.putExtra(RemindersDbAdapter.KEY_ROWID, (long)taskId);
        PendingIntent pi =
                PendingIntent.getBroadcast(mContext, 0, i,
                        PendingIntent.FLAG_ONE_SHOT);

        /*
         * The alarm must be set differently depending on the OS version. Anyway, we need the
         * pending intent in order to know what was the reminder for which the alarm was fired, so
         * then the correct notification will be shown.
         */
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            // Before Marshmallow, we can do this for setting a reliable alarm.
            mAlarmManager.set(AlarmManager.RTC_WAKEUP, when.getTimeInMillis(), pi);
        } else {
            /*
             * Starting from Marshmallow, it seems like this is the only way for setting a reliable
             * alarm.
             * If we use the "alarm clock" framework, the user will see a icon of an alarm clock.
             * If we use the setExactAndAllowWhileIdle the user will see nothing, but the OS can
             * delay alarms at some sort of situations.
             */
            mAlarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, when.getTimeInMillis(), pi);
        }
    }
}