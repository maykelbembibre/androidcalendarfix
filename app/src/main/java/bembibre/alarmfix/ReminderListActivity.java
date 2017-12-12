package bembibre.alarmfix;

import android.app.ListActivity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.graphics.PorterDuff;
import android.os.Bundle;
import android.view.ContextMenu;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
import android.widget.Spinner;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import bembibre.alarmfix.alarms.AlarmException;
import bembibre.alarmfix.logging.Logger;
import bembibre.alarmfix.logic.SynchronizedWork;
import bembibre.alarmfix.database.RemindersDbAdapter;
import bembibre.alarmfix.models.DateTime;
import bembibre.alarmfix.userinterface.NotificationManager;
import bembibre.alarmfix.userinterface.ReminderListCursorAdapter;
import bembibre.alarmfix.userinterface.UserInterfaceUtils;

/**
 * The main activity.
 */
public class ReminderListActivity extends ListActivity {

    private static final int ACTIVITY_CREATE = 0;

    private RemindersDbAdapter mDbHelper;
    private Cursor remindersCursor;

    Spinner yearSpinner;
    Spinner monthSpinner;
    private List<String> yearSpinnerArray;
    private List<String> monthSpinnerArray;
    private int spinnersEnabled;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.reminder_list);
        mDbHelper = RemindersDbAdapter.getInstance(this);

        this.mDbHelper.open();

        // Fill spinners and data.
        createSpinnersAndFillData();

        // Lack of options button in the device.
        this.findViewById(R.id.options).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ReminderListActivity.this.openOptionsMenu();
            }
        });

        registerForContextMenu(getListView());
    }

    private void setSpinnerLookAndFeel(Spinner spinner, List<String> items) {
        // Sets the layout of items and dropdown items.
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this, R.layout.spinner_item, items);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

        // Sets the color of the small triangle.
        spinner.getBackground().setColorFilter(getResources().getColor(R.color.actionBarForeground), PorterDuff.Mode.SRC_ATOP);

        spinner.setAdapter(adapter);
    }

    private void createYearSpinner() {
        // you need to have a list of data that you want the spinner to display
        this.yearSpinnerArray =  new ArrayList<>();
        Integer selectedPosition;

        Cursor cursor = this.mDbHelper.fetchAllRemindersByYear();
        if (cursor != null) {
            cursor.moveToFirst();
            while (!cursor.isAfterLast()) {
                int year = cursor.getInt(cursor.getColumnIndexOrThrow(RemindersDbAdapter.KEY_YEAR));
                yearSpinnerArray.add(Integer.valueOf(year).toString());
                cursor.moveToNext();
            }
            cursor.close();
        }

        String currentYear = Integer.valueOf(Calendar.getInstance().get(Calendar.YEAR)).toString();

        // If no reminders, no years, and spinner is empty. Put current year for aesthetics.
        if (yearSpinnerArray.isEmpty()) {
            yearSpinnerArray.add(currentYear);
        }

        int index = yearSpinnerArray.indexOf(currentYear);
        if (index == -1) {
            selectedPosition = null;
        } else {
            selectedPosition = index;
        }

        this.yearSpinner = (Spinner) findViewById(R.id.year_spinner);
        this.yearSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parentView, View selectedItemView, int position, long id) {
                if (spinnersEnabled == 2) {
                    ReminderListActivity.this.fillData();
                } else {
                    spinnersEnabled++;
                    Logger.log("Select buttons enabled: " + spinnersEnabled);
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parentView) {
                // your code here
            }
        });
        this.setSpinnerLookAndFeel(this.yearSpinner, yearSpinnerArray);
        if (selectedPosition != null) {
            this.yearSpinner.setSelection(selectedPosition);
        }
    }

    private void createMonthSpinner() {
        // you need to have a list of data that you want the spinner to display
        this.monthSpinnerArray =  new ArrayList<>();
        int currentMonth = Calendar.getInstance().get(Calendar.MONTH);
        for (int i = 1;i <= 12;i++) {
            this.monthSpinnerArray.add(Integer.valueOf(i).toString());
        }

        this.monthSpinner = (Spinner) findViewById(R.id.month_spinner);
        this.monthSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parentView, View selectedItemView, int position, long id) {
                if (spinnersEnabled == 2) {
                    ReminderListActivity.this.fillData();
                } else {
                    spinnersEnabled++;
                    Logger.log("Select buttons enabled: " + spinnersEnabled);
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parentView) {
                // your code here
            }
        });
        this.setSpinnerLookAndFeel(this.monthSpinner, this.monthSpinnerArray);
        this.monthSpinner.setSelection(currentMonth);
    }

    /**
     * Loads the reminder list from the database and makes it to be shown in the screen.
     */
    public void fillData() {
        // It is necessary to close gracefully previous data if it exists.
        if (this.remindersCursor != null) {
            stopManagingCursor(this.remindersCursor);
        }

        int year = Integer.valueOf(this.yearSpinnerArray.get(this.yearSpinner.getSelectedItemPosition()));
        int month = Integer.valueOf(this.monthSpinnerArray.get(this.monthSpinner.getSelectedItemPosition()));
        Logger.log("Data for listing reminders is going to be retrieved from database for year " + year + ", month " + month + ".");
        this.remindersCursor = mDbHelper.fetchAllReminders(year, month - 1);
        startManagingCursor(remindersCursor);
        // Create an array to specify the fields we want (only the TITLE)
        String[] from = new String[]{RemindersDbAdapter.KEY_TITLE, RemindersDbAdapter.KEY_DATE_TIME, RemindersDbAdapter.KEY_NOTIFIED};
        // and an array of the fields we want to bind in the view
        int[] to = new int[]{R.id.text1, R.id.text2, R.id.text3};
        // Now create a simple cursor adapter and set it to display
        SimpleCursorAdapter reminders =
                new ReminderListCursorAdapter(this, R.layout.reminder_row,
                        remindersCursor, from, to);
        setListAdapter(reminders);

        int count = this.mDbHelper.countAllReminders();
        ((TextView)this.findViewById(R.id.status_bar)).setText(this.getResources().getString(R.string.total_reminders) + ": " + Integer.valueOf(count).toString());
    }

    public synchronized void createSpinnersAndFillData() {
        this.spinnersEnabled = 0;

        // Spinners don't work for calling fillData()
        this.createYearSpinner();
        this.createMonthSpinner();

        // From here, spinners do work.

        fillData();
    }

    @Override
    protected void onListItemClick(ListView l, View v, int position, long id) {
        super.onListItemClick(l, v, position, id);
        Intent i = new Intent(this, ReminderEditActivity.class);
        i.putExtra(RemindersDbAdapter.KEY_ROWID, id);
        startActivity(i);
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo
            menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);
        MenuInflater mi = getMenuInflater();
        mi.inflate(R.menu.list_menu_item_longpress, menu);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        MenuInflater mi = getMenuInflater();
        mi.inflate(R.menu.list_menu, menu);
        UserInterfaceUtils.setMenuDesign(this, menu);

        return true;
    }

    @Override
    public boolean onMenuItemSelected(int featureId, MenuItem item) {
        switch(item.getItemId()) {
            case R.id.menu_insert:
                createReminder();
                return true;
        }
        return super.onMenuItemSelected(featureId, item);
    }

    private void createReminder() {
        Intent i = new Intent(this, ReminderEditActivity.class);
        startActivityForResult(i, ACTIVITY_CREATE);
    }

    /**
     * Called when the editing activity comes back to this, the list activity.
     *
     * @param requestCode
     * @param resultCode
     * @param intent
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent intent)
    {
        /*
         * Reload the year filter, just in case a reminder for a new year has been created and the
         * user may want to filter the reminders by that year.
         */
        this.createSpinnersAndFillData();

        super.onActivityResult(requestCode, resultCode, intent);
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        switch(item.getItemId()) {
            case R.id.menu_delete:
                // Delete the task
                AdapterView.AdapterContextMenuInfo info =
                        (AdapterView.AdapterContextMenuInfo) item.getMenuInfo();

                /*
                 * info.id is the database identifier of the reminder to be deleted.
                 * It's magic.
                 */
                SynchronizedWork.reminderDeleted(this, this.mDbHelper, info.id);
                return true;
        }
        return super.onContextItemSelected(item);
    }

    /*
     * This broadcast receiver listens to changes done from outside this activity, and makes all of
     * the reminders to be reloaded from scratch.
     *
     * This happens when a notification comes and its associated reminder is marked as notified in
     * the database. Then, the list of reminders of this activity gets updated for reflecting that
     * change.
     */
    private BroadcastReceiver broadcastBufferReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent bufferIntent) {
            ReminderListActivity.this.fillData();
        }
    };

    @Override
    public void onResume() {
        super.onResume();
        registerReceiver(broadcastBufferReceiver, new IntentFilter(SynchronizedWork.BROADCAST_BUFFER_SEND_CODE));
    }

    @Override
    public void onPause() {
        super.onPause();
        this.unregisterReceiver(broadcastBufferReceiver);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        this.mDbHelper.close();
    }
}
