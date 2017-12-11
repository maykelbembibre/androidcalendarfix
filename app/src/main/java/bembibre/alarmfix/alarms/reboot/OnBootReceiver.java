package bembibre.alarmfix.alarms.reboot;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import bembibre.alarmfix.alarms.ReminderManager;
import bembibre.alarmfix.logic.SynchronizedWork;
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
        ReminderManager reminderMgr = new ReminderManager(context);
        RemindersDbAdapter dbHelper = RemindersDbAdapter.getInstance(context);
        SynchronizedWork.phoneHasJustBeenTurnedOn(context, reminderMgr, dbHelper);
    }
}