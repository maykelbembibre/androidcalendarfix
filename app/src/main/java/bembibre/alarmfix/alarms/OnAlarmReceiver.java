package bembibre.alarmfix.alarms;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

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
        if (firstIntent.hasExtra(RemindersDbAdapter.KEY_ROWID)) {
            long rowid = firstIntent.getExtras().getLong(RemindersDbAdapter.KEY_ROWID);
            secondIntent.putExtra(RemindersDbAdapter.KEY_ROWID, rowid);
        }
        if (firstIntent.hasExtra(ReminderManager.EXTRA_ALARM_ID)) {
            long alarmId = firstIntent.getExtras().getLong(ReminderManager.EXTRA_ALARM_ID);
            secondIntent.putExtra(ReminderManager.EXTRA_ALARM_ID, alarmId);
        }
        context.startService(secondIntent);
    }
}