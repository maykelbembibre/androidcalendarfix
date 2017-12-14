package bembibre.alarmfix.alarms;

/**
 * Created by Max Power on 08/12/2017.
 */

/**
 * The meaning of this exception is that, for an unknown reason, the system no more lets us to set
 * alarms.
 */
public class AlarmException extends Exception {

    public AlarmException(Throwable cause) {
        super("The Android operating system doesn't let us to set alarms any longer.", cause);
    }
}
