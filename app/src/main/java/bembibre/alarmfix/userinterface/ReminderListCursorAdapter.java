package bembibre.alarmfix.userinterface;

import android.content.Context;
import android.database.Cursor;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

import bembibre.alarmfix.R;
import bembibre.alarmfix.database.RemindersDbAdapter;
import bembibre.alarmfix.utils.GeneralUtils;

/**
 * Created by Max Power on 11/12/2017.
 */

/**
 * Class responsible for painting in the screen the elements of the list that shows the reminders.
 */
public class ReminderListCursorAdapter extends SimpleCursorAdapter {

    public ReminderListCursorAdapter(Context context, int layout, Cursor c, String[] from, int[] to) {
        super(context, layout, c, from, to);
    }

    @Override
    public void setViewText(TextView v, String text) {
        if (v.getId() == R.id.text2) { // Make sure it matches your time field
            DateFormat dateFormat = new SimpleDateFormat(GeneralUtils.DATE_TIME_FORMAT);
            // You may want to try/catch with NumberFormatException in case `text` is not a numeric value
            text = dateFormat.format(new Date(Long.parseLong(text)));
        }
        v.setText(text);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        final View row = super.getView(position, convertView, parent);

        Cursor c = getCursor();
        c.moveToPosition(position);
        int notified = c.getInt(c.getColumnIndex(RemindersDbAdapter.KEY_NOTIFIED));

        if (notified == 1)
            row.setBackgroundColor(row.getResources().getColor(R.color.notifiedReminder));
        return row;

    }
}
