package bembibre.alarmfix.logic;

/**
 * Created by Max Power on 14/12/2017.
 */

import android.database.Cursor;

import bembibre.alarmfix.database.RemindersDbAdapter;
import bembibre.alarmfix.models.YearsMonthsAndReminders;

/**
 * Simplified operations for accessing data.
 */
public class DataAccessHelper {

    private RemindersDbAdapter dbAdapter;

    public DataAccessHelper(RemindersDbAdapter dbAdapter) {
        this.dbAdapter = dbAdapter;
    }

    public YearsMonthsAndReminders getAllReminderYears() {
        YearsMonthsAndReminders years = new YearsMonthsAndReminders();
        Cursor cursor = this.dbAdapter.fetchAllRemindersByYear();
        if (cursor != null) {
            cursor.moveToFirst();
            while (!cursor.isAfterLast()) {
                int count = cursor.getInt(0);
                int year = cursor.getInt(cursor.getColumnIndexOrThrow(RemindersDbAdapter.REMINDERS_COLUMN_YEAR));
                int month = cursor.getInt(cursor.getColumnIndexOrThrow(RemindersDbAdapter.REMINDERS_COLUMN_MONTH));
                System.out.println(year + ", " + month + ": " + count);
                years.add(year, month, count);
                cursor.moveToNext();
            }
            cursor.close();
        }
        return years;
    }
}
