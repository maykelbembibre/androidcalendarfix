package bembibre.alarmfix.userinterface;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationChannelGroup;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Build;
import android.support.v4.app.NotificationCompat;

import bembibre.alarmfix.R;
import bembibre.alarmfix.ReminderEditActivity;
import bembibre.alarmfix.ReminderListActivity;
import bembibre.alarmfix.database.RemindersDbAdapter;
import bembibre.alarmfix.logging.Logger;

import static android.content.Context.NOTIFICATION_SERVICE;

/**
 * Class that generates notifications.
 */
public class NotificationManager {

    private static final String NOTIFICATION_GROUP_ID = "bembibre.alarmfix.GROUP.ONE";
    private static final String NOTIFICATION_GROUP_NAME = "Group one";
    private static final String CHANNEL_ONE_ID = "bembibre.alarmfix.ONE";
    private static final String CHANNEL_ONE_NAME = "Channel One";
    private static final String NOTIFICATION_CHANNEL_DESCRIPTION = "The notification channel for Alarm Fix.";

    /*
     * In Android, when you launch two notifications with the same identifier, the second one
     * overrides the first one. So, we are using different identifiers for each notification.
     * If the notification is about a user reminder, it takes the identifier for the reminder, an
     * integer autoincremental number starting from 1.
     *
     * If the notification is for other purpose, it takes 0, -1, -2, -3 negative numbers and so on.
     * They will never match a reminder identifier (1, 2, 3, ...).
     */
    private static final int NOTIFICATION_REASON_MULTIPLE = 0;
    private static final int NOTIFICATION_REASON_BOOT_ERROR = -1;
    private static final int NOTIFICATION_REASON_ALARMS_CONFIGURED = -2;
    private static final int NOTIFICATION_REASON_ALARM_FIRE_ERROR = -3;

    private Context context;

    public NotificationManager(Context context) {
        this.context = context;
    }

    /**
     * Makes a notification for a single reminder.
     *
     * @param rowId database identifier of the reminder.
     * @param reminderTitle title of the reminder.
     *
     * @throws Exception when Android refuses to give access to the notifications service.
     */
    public void notifySingleReminder(long rowId, String reminderTitle) throws Exception {
        makeNotification(rowId, this.context.getResources().getString(R.string.notifiy_new_task_title), reminderTitle, true, 0);

        Logger.log("A notification has just been thrown for a received alarm.");
    }

    /**
     * Makes a notification for multiple reminders.
     *
     * @param reminderTitles the titles.
     *
     * @throws Exception when Android refuses to give access to the notifications service.
     */
    public void notifyMultipleReminders(Iterable<String> reminderTitles) throws Exception {
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
            int string;
            if (remindersNumber == 1) {
                string = R.string.notify_multiple_reminders_title_single;
            } else {
                string = R.string.notify_multiple_reminders_title_plural;
            }
            String notificationTitle = this.context.getResources().getString(string, remindersNumber);
            this.makeNotification(null, notificationTitle, notificationBody.toString(), true, NOTIFICATION_REASON_MULTIPLE);
            Logger.log("A notification has just been thrown for multiple reminders.");
        }
    }

    /**
     * Notifies an error that affects the application when the phone is turned on and it can't set
     * all the alarms that have been erased when the phone turned off.
     * @param title
     * @param body
     * @throws Exception
     */
    public void makeBootErrorNotification(String title, String body) throws Exception {
        this.makeGeneralNotification(title, body, NOTIFICATION_REASON_BOOT_ERROR);
    }

    /**
     * Notifies the user that the alarms have been properly configured when the phone has turned on.
     * @param title
     * @param body
     * @throws Exception
     */
    public void makeAlarmsConfiguredNotification(String title, String body) throws Exception {
        this.makeGeneralNotification(title, body, NOTIFICATION_REASON_ALARMS_CONFIGURED);
    }

    /**
     * Notifies the user that the application found alarms that weren't triggered on time.
     * @param title
     * @param body
     * @throws Exception
     */
    public void makeAlarmErrorNotification(String title, String body) throws Exception {
        this.makeGeneralNotification(title, body, NOTIFICATION_REASON_ALARM_FIRE_ERROR);
    }

    /**
     * Makes a general notification that won't be closed if the user pushes it, but only if he
     * deliberately closes it.
     *
     * @param title title.
     * @param body body.
     * @throws Exception
     */
    private void makeGeneralNotification(String title, String body, int reason) throws Exception {
        this.makeNotification(null, title, body, false, reason);
    }

    /**
     * Makes a notification in the notifications service of Android.
     *
     * @param rowId unique identifier for the notification, the identifier of the reminder in the database.
     * @param title title for the notification.
     * @param body body for the notification.
     *
     * @throws Exception when Android refuses to give access to the notifications service.
     */
    private void makeNotification(Long rowId, String title, String body, boolean autoCancel, int reason) throws Exception {
        android.app.NotificationManager mgr = (android.app.NotificationManager)context.getSystemService(NOTIFICATION_SERVICE);
        if (mgr == null) {
            throw new Exception("Android gives no access to the notifications service.");
        }
        Intent notificationIntent;

        long notificationId;
        PendingIntent pi;
        if (rowId == null) {
            notificationId = reason;
            notificationIntent = new Intent(context, ReminderListActivity.class);
            pi = PendingIntent.getActivity(context, 0, notificationIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT);
        } else {
            notificationId = rowId;
            notificationIntent = new Intent(context, ReminderEditActivity.class);
            notificationIntent.putExtra(RemindersDbAdapter.REMINDERS_COLUMN_ROWID, rowId);
            long rowIdPrimitive = rowId;
            pi = PendingIntent.getActivity(context, (int)rowIdPrimitive, notificationIntent,
                    PendingIntent.FLAG_ONE_SHOT);
        }

        NotificationCompat.Builder mBuilder;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Logger.log("Setting special notification for Android Oreo and upper.");

            NotificationChannel mChannel = new NotificationChannel
                    (CHANNEL_ONE_ID, CHANNEL_ONE_NAME, android.app.NotificationManager.IMPORTANCE_HIGH);

            mChannel.setDescription(NOTIFICATION_CHANNEL_DESCRIPTION);
            mChannel.enableLights(true);
            mChannel.setLightColor(Color.CYAN);
            mChannel.enableVibration(true);
            mgr.createNotificationChannel(mChannel);

            NotificationChannelGroup mGroup = new NotificationChannelGroup(NOTIFICATION_GROUP_ID, NOTIFICATION_GROUP_NAME);
            mgr.createNotificationChannelGroup(mGroup);

            mBuilder = new NotificationCompat.Builder(context, CHANNEL_ONE_ID);
        } else {
            mBuilder = new NotificationCompat.Builder(context);
        }

        mBuilder.setSmallIcon(android.R.drawable.stat_sys_warning)
                .setContentTitle(title)
                .setContentText(body)
                .setContentIntent(pi)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(body));

        if (Build.VERSION.SDK_INT >= 16) {
            mBuilder.setPriority(Notification.PRIORITY_HIGH);
        }

        Notification note = mBuilder.build();

        note.defaults |= Notification.DEFAULT_SOUND;
        if (autoCancel) {
            note.flags |= Notification.FLAG_AUTO_CANCEL;
        }

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
