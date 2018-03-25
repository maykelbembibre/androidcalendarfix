package bembibre.alarmfix.alarms.intentservices;

import android.content.Intent;

import java.util.Calendar;

import bembibre.alarmfix.alarms.ReminderManager;
import bembibre.alarmfix.alarms.WakeReminderIntentService;
import bembibre.alarmfix.core.SynchronizedWork;
import bembibre.alarmfix.database.RemindersDbAdapter;
import bembibre.alarmfix.logging.Logger;
import bembibre.alarmfix.utils.GeneralUtils;

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
    protected void doReminderWork(Intent intent) {
        SynchronizedWork.reminderAlarmReceived(this, intent);

        // Code for deleting the alarm reference from the database because it has already gone off.
        RemindersDbAdapter dbAdapter = RemindersDbAdapter.getInstance(this);
        dbAdapter.open();
        try {
            if (intent.hasExtra(ReminderManager.EXTRA_SET_ALARM_ID)) {
                /*
                 * It is needed to delete the reference to the alarm that has gone off and was
                 * stored into the database.
                 */
                long setAlarmId = intent.getExtras().getLong(ReminderManager.EXTRA_SET_ALARM_ID);
                dbAdapter.deleteAlarm(setAlarmId);

                Logger.log("An alarm reference has been deleted from the database at " + GeneralUtils.format(Calendar.getInstance()) + ". Alarm reference id: " + setAlarmId);
            } else {
                Logger.log("CRITICAL ERROR: an alarm has been triggered which hasn't got any reference at the database. The application must work in such way that every alarm has a reference into the database so as to keep track of the alarms.");
            }
        } catch (Throwable t) {
            Logger.log("Error while deleting gone off alarm reference at the database.", t);
        } finally {
            dbAdapter.close();
        }
    }
}