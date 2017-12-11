package bembibre.alarmfix.alarms;

import android.content.Intent;

import bembibre.alarmfix.logic.SynchronizedWork;

/**
 * Created by Max Power on 12/08/2017.
 */

/**
 * Class for showing notifications of reminders while a wake lock is retained for making sure that
 * the notification is shown.
 */
public class ReminderService extends WakeReminderIntentService {
    public ReminderService() {
        super("ReminderService");
    }

    @Override
    void doReminderWork(Intent intent) {
        SynchronizedWork.reminderAlarmReceived(this, intent);
    }
}