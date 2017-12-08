package bembibre.alarmfix.utils;

/**
 * Created by Max Power on 08/12/2017.
 */

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;

/**
 * Class with general utilities used throughout all the application.
 */
public class GeneralUtils {
    /**
     * Date format used for storing dates in the database and for logging.
     */
    public static final String DATE_TIME_FORMAT = "yyyy-MM-dd kk:mm:ss";

    /**
     * Returns the date of the calendar as a readable string.
     * @param calendar
     * @return
     */
    public static String format(Calendar calendar) {
        DateFormat dateFormat = new SimpleDateFormat(GeneralUtils.DATE_TIME_FORMAT);
        return dateFormat.format(calendar.getTime());
    }
}
