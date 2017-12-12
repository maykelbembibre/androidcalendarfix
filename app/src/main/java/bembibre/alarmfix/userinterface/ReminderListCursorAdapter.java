package bembibre.alarmfix.userinterface;

import android.content.Context;
import android.database.Cursor;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
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
            // Special date formatting for the field that shows the reminder date.

            DateFormat dateFormat = new SimpleDateFormat(GeneralUtils.DATE_TIME_FORMAT);
            // You may want to try/catch with NumberFormatException in case `text` is not a numeric value
            text = dateFormat.format(new Date(Long.parseLong(text)));
        } else if (v.getId() == R.id.text3) {
            /*
             * Special format for the field that shows if the reminder has already been notified.
             * For sake of coolness, the whole list item background gets changed.
             */

            boolean notified;
            if ("0".equals(text)) {
                notified = false;
            } else if ("1".equals(text)) {
                notified = true;
            } else {
                throw new RuntimeException("Application wrong programmed. The notified field of a reminder must not have got any value other than 0 or 1.");
            }

            /*
             * Some trickery.
             * FrameLayout extends Viewgroup extends View
             * LinearLayout extends Viewgroup extends View
             * ViewGroup implements ViewParent
             *
             * In my case my ViewParent objects are FrameLayout or LinearLayout. They also can be
             * seen as View objects for simplicity sake.
             */
            ViewParent parent = v.getParent();
            View notifiedTextViewContainer = ((View)parent);
            View topContainer = (View)notifiedTextViewContainer.getParent();

            if (notified) {
                text = v.getResources().getString(R.string.notified);
                topContainer.setBackgroundColor(v.getResources().getColor(R.color.notifiedReminder));
            } else {
                text = v.getResources().getString(R.string.not_notified);
                topContainer.setBackgroundColor(v.getResources().getColor(R.color.notNotifiedReminder));
            }
        }
        v.setText(text);
    }
}
