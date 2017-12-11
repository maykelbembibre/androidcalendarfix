package bembibre.alarmfix.userinterface;

import android.app.Notification;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.support.v4.app.NotificationCompat;

import bembibre.alarmfix.R;
import bembibre.alarmfix.ReminderEditActivity;
import bembibre.alarmfix.ReminderListActivity;
import bembibre.alarmfix.database.RemindersDbAdapter;
import bembibre.alarmfix.logging.Logger;

import static android.content.Context.NOTIFICATION_SERVICE;

/**
 * Created by Max Power on 11/12/2017.
 */

public class NotificationManager {

    private Context context;

    public NotificationManager(Context context) {
        this.context = context;
    }

    public void notifySingleReminder(long rowId, String reminderTitle) {
        makeNotification(rowId, this.context.getResources().getString(R.string.notifiy_new_task_title), reminderTitle);

        Logger.log("A notification has just been thrown for a received alarm.");
    }

    public void notifyMultipleReminders(Iterable<String> reminderTitles) {
        StringBuilder notificationBody = new StringBuilder();
        int remindersNumber = 0;
        for (String title : reminderTitles) {
            if (remindersNumber > 0) {
                notificationBody.append("\n");
            }
            notificationBody.append(title);
            remindersNumber++;
        }
        if (remindersNumber > 0) {
            String notificationTitle = this.context.getResources().getString(R.string.notify_multiple_reminders_title, remindersNumber);
            this.makeNotification(null, notificationTitle, notificationBody.toString());
            Logger.log("A notification has just been thrown for multiple reminders.");
        }
    }

    private void makeNotification(Long rowId, String title, String body) {
        android.app.NotificationManager mgr = (android.app.NotificationManager)context.getSystemService(NOTIFICATION_SERVICE);
        Intent notificationIntent;

        long notificationId;
        PendingIntent pi;
        if (rowId == null) {
            notificationId = 0;
            notificationIntent = new Intent(context, ReminderListActivity.class);
            pi = PendingIntent.getActivity(context, 0, notificationIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT);
        } else {
            notificationId = rowId;
            notificationIntent = new Intent(context, ReminderEditActivity.class);
            notificationIntent.putExtra(RemindersDbAdapter.KEY_ROWID, rowId);
            long rowIdPrimitive = rowId;
            pi = PendingIntent.getActivity(context, (int)rowIdPrimitive, notificationIntent,
                    PendingIntent.FLAG_ONE_SHOT);
        }

        NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(context)
                .setSmallIcon(android.R.drawable.stat_sys_warning)
                .setContentTitle(title)
                .setContentText(body)
                .setContentIntent(pi)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(body));

        Notification note = mBuilder.build();

        note.defaults |= Notification.DEFAULT_SOUND;
        note.flags |= Notification.FLAG_AUTO_CANCEL;

        // An issue could occur if user ever enters over 2,147,483,647 tasks. (Max int value).
        // I highly doubt this will ever happen. But is good to note.
        int id = (int)((long)notificationId);
        mgr.notify(id, note);
        NotificationManager.setNotified(context, id);
    }

    /**
     * Marks the reminder in the database as it has already been notified to the user, so it won't
     * be needed to set alarms for it any longer.
     * @param rowId the database identifier of the reminder.
     */
    private static void setNotified(Context context, long rowId) {
        RemindersDbAdapter dbHelper = RemindersDbAdapter.getInstance(context);
        dbHelper.open();
        try {
            dbHelper.updateReminder(rowId, true);
        } finally {
            dbHelper.close();
        }
    }
}
