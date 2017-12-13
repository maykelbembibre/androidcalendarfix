package bembibre.alarmfix.userinterface;

import android.view.View;
import android.widget.AdapterView;

import bembibre.alarmfix.ReminderListActivity;
import bembibre.alarmfix.logging.Logger;

/**
 * Created by Max Power on 12/12/2017.
 */

public class ListActivitySpinnerListener implements AdapterView.OnItemSelectedListener {

    private ReminderListActivity listActivity;

    /**
     * This value is false the first time, when the spinner is created, for preventing it to do
     * excessive calls to the database. Then it comes true and the spinner works normally.
     */
    private boolean spinnerEnabled = false;

    /**
     * The name of the spinner associated to this listener, only for debugging purpose.
     */
    private String name;

    /**
     * Creates a listener for a spinner (a dropdown select field of the given activity.
     *
     * @param listActivity the activity.
     * @param name name of the spinner for debugging purposes.
     */
    public ListActivitySpinnerListener(ReminderListActivity listActivity, String name) {
        this.listActivity = listActivity;
        this.name = name;
    }

    @Override
    public void onItemSelected(AdapterView<?> parentView, View selectedItemView, int position, long id) {
        if (spinnerEnabled) {
            listActivity.fillData();
        } else {
            spinnerEnabled = true;
            Logger.log("Select button enabled: " + this.name);
        }
    }

    @Override
    public void onNothingSelected(AdapterView<?> parentView) {
    }
}