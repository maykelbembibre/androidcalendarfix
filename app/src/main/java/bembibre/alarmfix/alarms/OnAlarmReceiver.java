package bembibre.alarmfix.alarms;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import bembibre.alarmfix.alarms.intentservices.ReminderService;
import bembibre.alarmfix.database.RemindersDbAdapter;
import bembibre.alarmfix.logging.Logger;

/**
 * Created by Max Power on 12/08/2017.
 */

/**
 * Receives alarms from the OS.
 */
public class OnAlarmReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent firstIntent) {
        Logger.log("An alarm has been received right now.");
        WakeReminderIntentService.acquireStaticLock(context);
        Intent secondIntent = new Intent(context, ReminderService.class);
        if (firstIntent.hasExtra(RemindersDbAdapter.REMINDERS_COLUMN_ROWID)) {
            long rowid = firstIntent.getExtras().getLong(RemindersDbAdapter.REMINDERS_COLUMN_ROWID);
            secondIntent.putExtra(RemindersDbAdapter.REMINDERS_COLUMN_ROWID, rowid);
        }
        if (firstIntent.hasExtra(ReminderManager.EXTRA_REMINDER_ALARM_ID)) {
            long alarmId = firstIntent.getExtras().getLong(ReminderManager.EXTRA_REMINDER_ALARM_ID);
            secondIntent.putExtra(ReminderManager.EXTRA_REMINDER_ALARM_ID, alarmId);
        }
        if (firstIntent.hasExtra(ReminderManager.EXTRA_SET_ALARM_ID)) {
            long extra = firstIntent.getExtras().getLong(ReminderManager.EXTRA_SET_ALARM_ID);
            secondIntent.putExtra(ReminderManager.EXTRA_SET_ALARM_ID, extra);
        }
        context.startService(secondIntent);
    }
}