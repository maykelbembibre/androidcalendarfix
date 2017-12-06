package bembibre.alarmfix.alarms.reboot;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.util.Log;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;

import bembibre.alarmfix.ReminderEditActivity;
import bembibre.alarmfix.alarms.ReminderManager;
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
        dbHelper.open();
        try {
            Cursor cursor = dbHelper.fetchAllReminders();
            if (cursor != null) {
                cursor.moveToFirst();
                int rowIdColumnIndex = cursor.getColumnIndex(RemindersDbAdapter.KEY_ROWID);
                int dateTimeColumnIndex = cursor.getColumnIndex(RemindersDbAdapter.KEY_DATE_TIME);
                while (!cursor.isAfterLast()) {
                    Long rowId = cursor.getLong(rowIdColumnIndex);
                    String dateTime = cursor.getString(dateTimeColumnIndex);
                    Calendar cal = Calendar.getInstance();
                    SimpleDateFormat format = new SimpleDateFormat(ReminderEditActivity.DATE_TIME_FORMAT);
                    try {
                        java.util.Date date = format.parse(dateTime);
                        cal.setTime(date);
                        reminderMgr.setReminder(rowId, cal);
                    } catch (ParseException e) {
                        Log.e("OnBootReceiver", e.getMessage(), e);
                    }
                    cursor.moveToNext();
                }
                cursor.close();
            }
        } finally {
            dbHelper.close();
        }
    }
}