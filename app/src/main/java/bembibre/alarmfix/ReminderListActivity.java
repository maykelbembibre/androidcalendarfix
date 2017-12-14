package bembibre.alarmfix;

import android.app.ListActivity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.view.ContextMenu;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
import android.widget.Spinner;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Map;

import bembibre.alarmfix.logging.Logger;
import bembibre.alarmfix.logic.DataAccessHelper;
import bembibre.alarmfix.logic.DeleteAllReminders;
import bembibre.alarmfix.logic.exportimport.DataExport;
import bembibre.alarmfix.logic.exportimport.DataImport;
import bembibre.alarmfix.logic.SynchronizedWork;
import bembibre.alarmfix.database.RemindersDbAdapter;
import bembibre.alarmfix.models.YearsMonthsAndReminders;
import bembibre.alarmfix.userinterface.ListActivitySpinnerListener;
import bembibre.alarmfix.userinterface.ReminderListCursorAdapter;
import bembibre.alarmfix.userinterface.UserInterfaceUtils;

/**
 * The main activity.
 */
public class ReminderListActivity extends ListActivity {

    // Start activity for result
    private static final int ACTIVITY_CREATE = 0;
    private static final int PICKFILE_REQUEST_CODE = 1;

    private RemindersDbAdapter mDbHelper;
    private Cursor remindersCursor;

    Spinner yearSpinner;
    Spinner monthSpinner;
    private List<Integer> yearSpinnerValues;
    private List<Integer> monthSpinnerValues;

    /**
     * Reminder count by year and month for the spinners.
     */
    private YearsMonthsAndReminders yearsMonthsAndReminders;

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

    private void createYearSpinner() {
        Integer previousValue;
        Integer selectedPosition = null;

        // Save previous selection.
        if (this.yearSpinner == null) {
            previousValue = null;
        } else {
            previousValue = this.yearSpinnerValues.get(this.yearSpinner.getSelectedItemPosition());
        }

        // Get spinner values and texts.
        this.yearsMonthsAndReminders = new DataAccessHelper(this.mDbHelper).getAllReminderYears();
        this.yearSpinnerValues = new ArrayList<>();
        List<String> yearSpinnerTexts = new ArrayList<>();
        for (Integer year : this.yearsMonthsAndReminders.getOrderedYears()) {
            this.yearSpinnerValues.add(year);
            yearSpinnerTexts.add(year + " (" + this.yearsMonthsAndReminders.getRemindersByYear(year) + ")");
        }

        int currentYear = Calendar.getInstance().get(Calendar.YEAR);

        // If no reminders, no years, and spinner is empty. Put current year for aesthetics.
        if (yearSpinnerValues.isEmpty()) {
            yearSpinnerValues.add(currentYear);
            yearSpinnerTexts.add(currentYear + " (" + this.yearsMonthsAndReminders.getRemindersByYear(currentYear) + ")");
        }

        // Leave selected the previous value, if possible.
        if (previousValue != null) {
            int index = 0;
            for (Integer value : this.yearSpinnerValues) {
                if (value.equals(previousValue)) {
                    selectedPosition = index;
                    break;
                }
                index++;
            }
        }

        // If no value is selected, select the current year, if it is present.
        int index = yearSpinnerValues.indexOf(currentYear);
        if ((selectedPosition == null) && (index != -1)) {
            selectedPosition = index;
        }

        // Create the spinner.
        this.yearSpinner = (Spinner) findViewById(R.id.year_spinner);
        this.yearSpinner.setOnItemSelectedListener(new ListActivitySpinnerListener(this, "year", true));
        UserInterfaceUtils.setSpinnerLookAndFeel(this, this.yearSpinner, yearSpinnerTexts);
        if (selectedPosition != null) {
            this.yearSpinner.setSelection(selectedPosition);
        }
    }

    private List<String> getMonthSpinnerTexts() {
        int currentlySelectedYear = this.yearSpinnerValues.get(this.yearSpinner.getSelectedItemPosition());
        List<String> monthSpinnerArray = new ArrayList<>();
        for (int item : monthSpinnerValues) {
            monthSpinnerArray.add((item + 1) + " (" + this.yearsMonthsAndReminders.getRemindersByYearAndMonth(currentlySelectedYear, item) + ")");
        }
        return monthSpinnerArray;
    }

    public void createMonthSpinner() {
        Integer previousPosition;
        // Save previous selection.
        if (this.monthSpinner == null) {
            previousPosition = null;
        } else {
            previousPosition = this.monthSpinner.getSelectedItemPosition();
        }

        // you need to have a list of data that you want the spinner to display
        this.monthSpinnerValues =  new ArrayList<>();
        for (int i = 0;i <= 11;i++) {
            this.monthSpinnerValues.add(i);
        }

        this.monthSpinner = (Spinner) findViewById(R.id.month_spinner);
        this.monthSpinner.setOnItemSelectedListener(new ListActivitySpinnerListener(this, "month", false));
        UserInterfaceUtils.setSpinnerLookAndFeel(this, this.monthSpinner, this.getMonthSpinnerTexts());

        if (previousPosition == null) {
            // Month starts from 0. Position is the same.
            int currentMonth = Calendar.getInstance().get(Calendar.MONTH);
            this.monthSpinner.setSelection(currentMonth);
        } else {
            this.monthSpinner.setSelection(previousPosition);
        }
    }

    /**
     * Loads the reminder list from the database and makes it to be shown in the screen.
     */
    public void fillData() {
        // It is necessary to close gracefully previous data if it exists.
        if (this.remindersCursor != null) {
            stopManagingCursor(this.remindersCursor);
        }

        int year = this.yearSpinnerValues.get(this.yearSpinner.getSelectedItemPosition());
        int month = this.monthSpinnerValues.get(this.monthSpinner.getSelectedItemPosition());
        Logger.log("Data for listing reminders is going to be retrieved from database for year " + year + ", month " + month + ".");
        this.remindersCursor = mDbHelper.fetchAllReminders(year, month);
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
            case R.id.menu_export:
                new DataExport(this).execute();
                return true;
            case R.id.menu_import:
                UserInterfaceUtils.showConfirmationDialog(this, R.string.import_confirmation, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        ReminderListActivity.this.importFile();
                    }
                });
            case R.id.menu_delete_all:
                UserInterfaceUtils.showConfirmationDialog(this, R.string.deleting_confirmation, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        new DeleteAllReminders(ReminderListActivity.this).execute();
                    }
                });
                return true;
        }
        return super.onMenuItemSelected(featureId, item);
    }

    /**
     * Imports reminder data from a file.
     */
    private void importFile() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("*/*");
        startActivityForResult(intent, PICKFILE_REQUEST_CODE);
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
    protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
        switch (requestCode) {
            case ACTIVITY_CREATE:
                /*
                 * Reload the year filter, just in case a reminder for a new year has been created and the
                 * user may want to filter the reminders by that year.
                 */
                this.createSpinnersAndFillData();
                break;
            case PICKFILE_REQUEST_CODE:
                Uri uri = intent.getData();
                Logger.log("Importing files from URI " + uri.getPath());
                new DataImport(this).execute(uri);
                break;
        }
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
            ReminderListActivity.this.createSpinnersAndFillData();
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
