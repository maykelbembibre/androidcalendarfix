package bembibre.alarmfix.alarms;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.support.v4.app.NotificationCompat;

import bembibre.alarmfix.R;
import bembibre.alarmfix.ReminderEditActivity;
import bembibre.alarmfix.database.RemindersDbAdapter;

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
        Long rowId = intent.getExtras().getLong(RemindersDbAdapter.KEY_ROWID);

        if (rowId != null) {
            doReminderWorkById(rowId);
        }
    }

    private void doReminderWorkById(long rowId) {
        // Status bar notification Code Goes here.
        NotificationManager mgr = (NotificationManager)getSystemService(NOTIFICATION_SERVICE);
        Intent notificationIntent = new Intent(this, ReminderEditActivity.class);
        notificationIntent.putExtra(RemindersDbAdapter.KEY_ROWID, rowId);
        PendingIntent pi = PendingIntent.getActivity(this, 0, notificationIntent,
                PendingIntent.FLAG_ONE_SHOT);

        NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(this)
                .setSmallIcon(android.R.drawable.stat_sys_warning)
                .setContentTitle(getString(R.string.notifiy_new_task_title))
                .setContentText(getString(R.string.notify_new_task_message))
                .setContentIntent(pi);

        Notification note = mBuilder.build();

        /*Notification note = new Notification(android.R.drawable.stat_sys_warning,
                getString(R.string.notify_new_task_message),
                System.currentTimeMillis());
        note.setLatestEventInfo(this, getString(R.string.notifiy_new_task_title),
                        getString(R.string.notify_new_task_message), pi);
                        */
        note.defaults |= Notification.DEFAULT_SOUND;
        note.flags |= Notification.FLAG_AUTO_CANCEL;

        // An issue could occur if user ever enters over 2,147,483,647 tasks. (Max int value).
        // I highly doubt this will ever happen. But is good to note.
        int id = (int)((long)rowId);
        mgr.notify(id, note);
    }
}