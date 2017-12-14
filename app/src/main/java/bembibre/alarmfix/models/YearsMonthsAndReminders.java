package bembibre.alarmfix.models;

/**
 * Created by Max Power on 14/12/2017.
 */

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Represents a year and the number of reminders stored currently for that year.
 */
public class YearsMonthsAndReminders {

    /**
     * Ordered list of years.
     */
    private List<Integer> years;

    /**
     * Map year - months and reminders.
     */
    private Map<Integer, Map<Integer, Long>> monthsAndReminders;

    /**
     * Map year - reminders.
     */
    private Map<Integer, Long> yearAndReminders;

    public YearsMonthsAndReminders() {
        years = new ArrayList<>();
        monthsAndReminders = new HashMap<>();
        yearAndReminders = new HashMap<>();
    }

    public void add(int year, int month, long count) {
        Map<Integer, Long> months = this.monthsAndReminders.get(year);
        if (months == null) {
            months = new HashMap<>();
            this.years.add(year);
            this.monthsAndReminders.put(year, months);
        }
        months.put(month, count);

        Long remindersByYear = this.yearAndReminders.get(year);
        if (remindersByYear == null) {
            this.yearAndReminders.put(year, count);
        } else {
            this.yearAndReminders.put(year, remindersByYear + count);
        }
    }

    public List<Integer> getOrderedYears() {
        return this.years;
    }

    public long getRemindersByYear(int year) {
        Long result = this.yearAndReminders.get(year);
        if (result == null) {
            result = 0L;
        }
        return result;
    }

    public long getRemindersByYearAndMonth(int year, int month) {
        Long result;
        Map<Integer, Long> remindersByMonth = this.monthsAndReminders.get(year);
        if (remindersByMonth == null) {
            result = null;
        } else {
            result = remindersByMonth.get(month);
        }
        if (result == null) {
            result = 0L;
        }
        return result;
    }
}
