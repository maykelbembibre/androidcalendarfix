package bembibre.alarmfix.alarms.intentservices;

import android.content.Intent;
import android.os.Handler;

import bembibre.alarmfix.R;
import bembibre.alarmfix.alarms.ReminderManager;
import bembibre.alarmfix.alarms.WakeReminderIntentService;
import bembibre.alarmfix.core.SynchronizedWork;
import bembibre.alarmfix.database.RemindersDbAdapter;

/**
 * Class that notifies past alarms and sets the next alarm when the phone has just been turned on,
 * while a wake lock is retained for making sure that the work is done.
 */
public class BootService extends WakeReminderIntentService {

    Handler mHandler;

    public BootService() {
        super("BootService");
        mHandler = new Handler();
    }

    @Override
    protected void doReminderWork(Intent intent) {
        ReminderManager reminderMgr = new ReminderManager(this);
        RemindersDbAdapter dbHelper = RemindersDbAdapter.getInstance(this);
        SynchronizedWork.phoneHasJustBeenTurnedOn(this, reminderMgr, dbHelper);
        mHandler.post(new DisplayToast(this, this.getString(R.string.boot_received)));
    }
}