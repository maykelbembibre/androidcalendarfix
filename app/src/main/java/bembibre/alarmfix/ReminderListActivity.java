package bembibre.alarmfix;

import android.app.ListActivity;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Color;
import android.os.Bundle;
import android.text.SpannableString;
import android.text.style.ForegroundColorSpan;
import android.view.ContextMenu;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;

import bembibre.alarmfix.database.RemindersDbAdapter;
import bembibre.alarmfix.userinterface.UserInterfaceUtils;

/**
 * The main activity.
 */
public class ReminderListActivity extends ListActivity {

    private static final int ACTIVITY_CREATE = 0;

    private RemindersDbAdapter mDbHelper;
    private Cursor remindersCursor;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.reminder_list);
        mDbHelper = RemindersDbAdapter.getInstance(this);
        this.mDbHelper.open();
        fillData();

        // Lack of options button in the device.
        this.findViewById(R.id.options).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ReminderListActivity.this.openOptionsMenu();
            }
        });

        registerForContextMenu(getListView());
    }

    /**
     * Loads the reminder list from the database and makes it to be shown in the screen.
     */
    private void fillData() {
        // It is necessary to close gracefully previous data if it exists.
        if (this.remindersCursor != null) {
            stopManagingCursor(this.remindersCursor);
        }

        this.remindersCursor = mDbHelper.fetchAllReminders();
        startManagingCursor(remindersCursor);
        // Create an array to specify the fields we want (only the TITLE)
        String[] from = new String[]{RemindersDbAdapter.KEY_TITLE};
        // and an array of the fields we want to bind in the view
        int[] to = new int[]{R.id.text1};
        // Now create a simple cursor adapter and set it to display
        SimpleCursorAdapter reminders =
                new SimpleCursorAdapter(this, R.layout.reminder_row,
                        remindersCursor, from, to);
        setListAdapter(reminders);
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

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent intent)
    {
        super.onActivityResult(requestCode, resultCode, intent);
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        switch(item.getItemId()) {
            case R.id.menu_delete:
                // Delete the task
                AdapterView.AdapterContextMenuInfo info =
                        (AdapterView.AdapterContextMenuInfo) item.getMenuInfo();
                mDbHelper.deleteReminder(info.id);
                fillData();
                return true;
        }
        return super.onContextItemSelected(item);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        this.mDbHelper.close();
    }
}
