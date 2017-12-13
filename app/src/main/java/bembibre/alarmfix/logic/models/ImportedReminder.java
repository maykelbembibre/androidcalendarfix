package bembibre.alarmfix.logic.models;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Calendar;
import java.util.Date;

/**
 * Created by Max Power on 13/12/2017.
 */

public class ImportedReminder {
    private String title;
    private String body;
    private Calendar calendar;

    public ImportedReminder(JSONObject jsonReminder) throws JSONException {
        title = jsonReminder.getString("title");
        body = jsonReminder.getString("body");
        calendar = Calendar.getInstance();
        calendar.setTime(new Date(jsonReminder.getLong("date_time")));
    }

    public String getTitle() {
        return title;
    }

    public String getBody() {
        return body;
    }

    public Calendar getCalendar() {
        return calendar;
    }
}
