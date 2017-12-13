package bembibre.alarmfix.models;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

import bembibre.alarmfix.utils.GeneralUtils;

/**
 * Created by Max Power on 11/12/2017.
 */

public class DateTime {

    private SimpleDateFormat dateTimeFormat = new SimpleDateFormat(GeneralUtils.DATE_TIME_FORMAT);
    private long millisecondsSinceTheEpoch;

    public DateTime(long millisecondsSinceTheEpoch) {
        this.millisecondsSinceTheEpoch = millisecondsSinceTheEpoch;
    }

    public DateTime(Calendar when) {
        this.millisecondsSinceTheEpoch = when.getTime().getTime();
    }

    public String toString() {
        return this.dateTimeFormat.format(new Date(millisecondsSinceTheEpoch));
    }

    public long toMillisecondsSinceTheEpoch() {
        return this.millisecondsSinceTheEpoch;
    }
}
