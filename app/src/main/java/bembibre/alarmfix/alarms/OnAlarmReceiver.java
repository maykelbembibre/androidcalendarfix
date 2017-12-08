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
    public void onReceive(Context context, Intent intent) {
        Logger.log("An alarm has been received right now.");
        long rowid =
                intent.getExtras().getLong(RemindersDbAdapter.KEY_ROWID);
        WakeReminderIntentService.acquireStaticLock(context);
        Intent i = new Intent(context, ReminderService.class);
        i.putExtra(RemindersDbAdapter.KEY_ROWID, rowid);
        context.startService(i);
    }
}