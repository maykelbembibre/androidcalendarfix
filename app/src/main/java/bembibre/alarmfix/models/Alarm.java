package bembibre.alarmfix.models;

import java.util.Date;

/**
 * Created by Max Power on 18/03/2018.
 */

/**
 * This represents a reference that is stored in the database to an alarm that has been set in
 * the operating system for a specific date and time.
 */
public class Alarm {

    private Date dateAndTime;

    public Alarm(Date dateAndTime) {
        if (dateAndTime == null) {
            throw new IllegalArgumentException("dateAndTime cannot be null");
        }
        this.dateAndTime = dateAndTime;
    }

    public long getTime() {
        return this.dateAndTime.getTime();
    }
}
