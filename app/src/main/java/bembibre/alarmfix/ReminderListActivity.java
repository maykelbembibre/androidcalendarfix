package bembibre.alarmfix;

import android.app.ListActivity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.os.Bundle;
import android.view.ContextMenu;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;

import bembibre.alarmfix.logic.SynchronizedWork;
import bembibre.alarmfix.database.RemindersDbAdapter;
import bembibre.alarmfix.userinterface.ReminderListCursorAdapter;
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
    public void fillData() {
        // It is necessary to close gracefully previous data if it exists.
        if (this.remindersCursor != null) {
            stopManagingCursor(this.remindersCursor);
        }

        this.remindersCursor = mDbHelper.fetchAllReminders();
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
