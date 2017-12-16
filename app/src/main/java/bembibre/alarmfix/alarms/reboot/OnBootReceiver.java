package bembibre.alarmfix.alarms.reboot;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import bembibre.alarmfix.alarms.ReminderManager;
import bembibre.alarmfix.alarms.WakeReminderIntentService;
import bembibre.alarmfix.alarms.intentservices.BootService;
import bembibre.alarmfix.alarms.intentservices.ReminderService;
import bembibre.alarmfix.core.SynchronizedWork;
import bembibre.alarmfix.database.RemindersDbAdapter;
import bembibre.alarmfix.logging.Logger;

/**
 * Created by Max Power on 12/08/2017.
 */

/**
 * Re-creates all the alarms after a reboot.
 */
public class OnBootReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        Logger.log("The phone has just been turned on.");
        WakeReminderIntentService.acquireStaticLock(context);
        Intent i = new Intent(context, BootService.class);
        context.startService(i);
    }
}