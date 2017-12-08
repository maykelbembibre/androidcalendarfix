package bembibre.alarmfix.alarms;

/**
 * Created by Max Power on 08/12/2017.
 */

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.support.v4.app.NotificationCompat;
import android.widget.Toast;

import bembibre.alarmfix.R;
import bembibre.alarmfix.ReminderEditActivity;
import bembibre.alarmfix.database.RemindersDbAdapter;
import bembibre.alarmfix.logging.Logger;
import bembibre.alarmfix.userinterface.UserInterfaceUtils;

import static android.content.Context.NOTIFICATION_SERVICE;

/**
 * Class for all the delicate work that must be synchronized between threads.
 */
public class SynchronizedWork {

    /**
     * Synchronized method called when an alarm for a reminder has been triggered.
     *
     * @param context application context.
     * @param intent intent of the alarm.
     */
    public synchronized static void reminderAlarmReceived(Context context, Intent intent) {
        long rowId = intent.getExtras().getLong(RemindersDbAdapter.KEY_ROWID);
        SynchronizedWork.doReminderWorkById(context, rowId);
    }

    /**
     * Synchronized method called by the corresponding activity when the user uses that activity for
     * creating a reminder or for updating an existing one.
     * @param reminderEditActivity the activity where the reminders are created and updated.
     * @param title title of the reminder set by the user.
     * @param body body of the reminder set by the user.
     * @param reminderDateTime date and time of the reminder set by the user.
     */
    public synchronized static void reminderCreatedOrUpdated(final ReminderEditActivity reminderEditActivity, String title, String body, String reminderDateTime) {
        long id;
        boolean ok;
        try {
            reminderEditActivity.mDbHelper.beginTransaction();

            // My SQL stuff.
            if (reminderEditActivity.mRowId == null) {
                id = reminderEditActivity.mDbHelper.createReminder(title, body, reminderDateTime);

                if (id > 0) {
                    reminderEditActivity.mRowId = id;
                }
            } else {
                id = reminderEditActivity.mRowId;
                reminderEditActivity.mDbHelper.updateReminder(reminderEditActivity.mRowId, title, body, reminderDateTime);
            }

            /*
             * Can throw exception.
             * If a reminder update has happened, the alarm for the old time is cancelled automatically.
             */
            new ReminderManager(reminderEditActivity).setReminder(id, reminderEditActivity.mCalendar);

            // No exceptions, all okay.
            reminderEditActivity.mDbHelper.setTransactionSuccessful();

            ok = true;
        } catch (Exception e) {
            /*
             * AlarmException can be thrown when setting the alarm.
             *
             * Transaction is not set as successful here, so it will rollback and the
             * reminder won't be created.
             */
            ok = false;
        } finally {
            reminderEditActivity.mDbHelper.endTransaction();
        }

        // Exit activity.
        if (ok) {
            Toast.makeText(reminderEditActivity,
                    reminderEditActivity.getString(R.string.task_saved_message),
                    Toast.LENGTH_SHORT).show();
            reminderEditActivity.finish();
        } else {
            DialogInterface.OnClickListener listener = new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int which) {
                    dialog.dismiss();
                    reminderEditActivity.finish();
                }
            };
            UserInterfaceUtils.showDialog(reminderEditActivity, R.string.unable_to_set_alarm, listener);
        }
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

    private static void doReminderWorkById(Context context, long rowId) {
        // Status bar notification Code Goes here.
        NotificationManager mgr = (NotificationManager)context.getSystemService(NOTIFICATION_SERVICE);
        Intent notificationIntent = new Intent(context, ReminderEditActivity.class);
        notificationIntent.putExtra(RemindersDbAdapter.KEY_ROWID, rowId);
        PendingIntent pi = PendingIntent.getActivity(context, 0, notificationIntent,
                PendingIntent.FLAG_ONE_SHOT);

        NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(context)
                .setSmallIcon(android.R.drawable.stat_sys_warning)
                .setContentTitle(context.getString(R.string.notifiy_new_task_title))
                .setContentText(context.getString(R.string.notify_new_task_message))
                .setContentIntent(pi);

        Notification note = mBuilder.build();

        note.defaults |= Notification.DEFAULT_SOUND;
        note.flags |= Notification.FLAG_AUTO_CANCEL;

        // An issue could occur if user ever enters over 2,147,483,647 tasks. (Max int value).
        // I highly doubt this will ever happen. But is good to note.
        int id = (int)((long)rowId);
        mgr.notify(id, note);
        SynchronizedWork.setNotified(context, id);

        Logger.log("A notification has just been thrown for a received alarm.");
    }
}
