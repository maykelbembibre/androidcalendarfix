package bembibre.alarmfix.alarms.reboot;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import bembibre.alarmfix.alarms.ReminderManager;
import bembibre.alarmfix.core.SynchronizedWork;
import bembibre.alarmfix.database.RemindersDbAdapter;

/**
 * Created by Max Power on 12/08/2017.
 */

/**
 * Re-creates all the alarms after a reboot.
 */
public class OnBootReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        SynchronizedWork.phoneHasJustBeenTurnedOn(context);
    }
}