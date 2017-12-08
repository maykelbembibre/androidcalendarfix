package bembibre.alarmfix.alarms;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.support.v4.app.NotificationCompat;

import bembibre.alarmfix.R;
import bembibre.alarmfix.ReminderEditActivity;
import bembibre.alarmfix.database.RemindersDbAdapter;
import bembibre.alarmfix.logging.Logger;

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