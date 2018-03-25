package bembibre.alarmfix;

import android.app.Activity;
import android.app.DatePickerDialog;
import android.app.Dialog;
import android.app.TimePickerDialog;
import android.content.DialogInterface;
import android.database.Cursor;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.TimePicker;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

import bembibre.alarmfix.database.RemindersDbAdapter;
import bembibre.alarmfix.logging.Logger;
import bembibre.alarmfix.core.SynchronizedWork;
import bembibre.alarmfix.userinterface.UserInterfaceUtils;
import bembibre.alarmfix.utils.GeneralUtils;

public class ReminderEditActivity extends Activity {

    private static final String DATE_FORMAT = "yyyy-MM-dd";
    private static final String TIME_FORMAT = "kk:mm";

    private static final int DATE_PICKER_DIALOG = 0;
    private static final int TIME_PICKER_DIALOG = 1;

    public RemindersDbAdapter mDbHelper;

    /**
     * The button of the user interface that the user uses for setting the date.
     */
    private Button mDateButton;

    /**
     * The button of the user interface that the user uses for setting the time.
     */
    private Button mTimeButton;

    /**
     * This object holds the date and time currently selected by the user for the reminder that is
     * being created or modified.
     */
    public Calendar mCalendar;

    private EditText mTitleText;
    private Button mConfirmButton;
    private EditText mBodyText;
    private Long alarmId;

    public Long mRowId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mDbHelper = RemindersDbAdapter.getInstance(this);

        setContentView(R.layout.reminder_edit);

        mDateButton = (Button) findViewById(R.id.reminder_date);
        mTimeButton = (Button) findViewById(R.id.reminder_time);
        mCalendar = Calendar.getInstance();
        mCalendar.set(Calendar.SECOND, 0);
        mConfirmButton = (Button) findViewById(R.id.confirm);
        mTitleText = (EditText) findViewById(R.id.title);
        mBodyText = (EditText) findViewById(R.id.body);

        mRowId = savedInstanceState != null
                ? savedInstanceState.getLong(RemindersDbAdapter.REMINDERS_COLUMN_ROWID)
                : null;

        registerButtonListenersAndSetDefaultText();
    }

    private void setRowIdFromIntent() {
        if (mRowId == null) {
            Bundle extras = getIntent().getExtras();
            mRowId = extras != null
                    ? extras.getLong(RemindersDbAdapter.REMINDERS_COLUMN_ROWID)
                    : null;
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        mDbHelper.close();
    }

    @Override
    protected void onResume() {
        super.onResume();
        mDbHelper.open();
        setRowIdFromIntent();
        populateFields();
    }

    // Date picker, button click events, and buttonText updating, createDialog
    // left out for brevity
    // they normally go here ...
    private void populateFields() {
        if (mRowId != null) {
            Cursor reminder = mDbHelper.fetchReminder(mRowId);
            if (reminder.getCount() < 1) {
                DialogInterface.OnClickListener listener = new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                        ReminderEditActivity.this.finish();
                    }
                };
                Logger.log("Attempt to open the edition activity with a reminder that does not exist, its identifier is: " + this.mRowId);
                UserInterfaceUtils.showWarningDialog(this, this.getResources().getString(R.string.reminder_does_not_exist), listener);

            } else {
                startManagingCursor(reminder);
                mTitleText.setText(reminder.getString(
                        reminder.getColumnIndexOrThrow(RemindersDbAdapter.REMINDERS_COLUMN_TITLE)));
                mBodyText.setText(reminder.getString(
                        reminder.getColumnIndexOrThrow(RemindersDbAdapter.REMINDERS_COLUMN_BODY)));
                alarmId = reminder.getLong(reminder.getColumnIndexOrThrow(RemindersDbAdapter.REMINDERS_COLUMN_ALARM_ID));
                SimpleDateFormat dateTimeFormat = new SimpleDateFormat(GeneralUtils.DATE_TIME_FORMAT);
                long reminderDateTime = reminder.getLong(
                        reminder.getColumnIndexOrThrow(
                                RemindersDbAdapter.REMINDERS_COLUMN_DATE_TIME));
                mCalendar.setTime(new Date(reminderDateTime));
            }
        }
        updateDateButtonText();
        updateTimeButtonText();
    }

    /**
     * This gets called whenever this activity is killed for any reason. It saves all that will be
     * needed for restoring the previous state of the activity.
     * @param outState where the necessary information is saved for the future.
     */
    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putLong(RemindersDbAdapter.REMINDERS_COLUMN_ROWID, mRowId);
    }

    private void saveState() {
        String title = mTitleText.getText().toString();
        String body = mBodyText.getText().toString();
        if ((title == null) || (title.isEmpty())) {
            title = this.getResources().getString(R.string.default_reminder_title);
        }
        SynchronizedWork.reminderCreatedOrUpdated(this, title, body, mCalendar, this.alarmId);
    }

    private void registerButtonListenersAndSetDefaultText(){
        mDateButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showDialog(DATE_PICKER_DIALOG);
            }
        });
        mTimeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showDialog(TIME_PICKER_DIALOG);
            }
        });
        mConfirmButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                setResult(RESULT_OK);

                // This method finally finishes this activity.
                saveState();
            }
        });
        updateDateButtonText();
        updateTimeButtonText();
    }

    @Override
    protected Dialog onCreateDialog(int id) {
        switch(id) {
            case DATE_PICKER_DIALOG:
                return showDatePicker();
            case TIME_PICKER_DIALOG:
                return showTimePicker();
        }
        return super.onCreateDialog(id);
    }

    /**
     * Creates a small window in the user interface that allows to select a date.
     *
     * @return the small window.
     */
    private DatePickerDialog showDatePicker() {
        DatePickerDialog datePicker = new DatePickerDialog(ReminderEditActivity.this,
                new DatePickerDialog.OnDateSetListener() {
                    @Override
                    public void onDateSet(DatePicker view, int year, int monthOfYear,
                                          int dayOfMonth) {
                        mCalendar.set(Calendar.YEAR, year);
                        mCalendar.set(Calendar.MONTH, monthOfYear);
                        mCalendar.set(Calendar.DAY_OF_MONTH, dayOfMonth);
                        updateDateButtonText();
                    }
                }, mCalendar.get(Calendar.YEAR), mCalendar.get(Calendar.MONTH),
                mCalendar.get(Calendar.DAY_OF_MONTH));
        return datePicker;
    }

    /**
     * Creates a small window in the user interface that allows to select a time.
     *
     * @return the small window.
     */
    private TimePickerDialog showTimePicker() {
        TimePickerDialog timePicker = new TimePickerDialog(this, new
                TimePickerDialog.OnTimeSetListener() {
                    @Override
                    public void onTimeSet(TimePicker view, int hourOfDay, int minute){
                        mCalendar.set(Calendar.HOUR_OF_DAY, hourOfDay);
                        mCalendar.set(Calendar.MINUTE, minute);
                        mCalendar.set(Calendar.SECOND, 0);
                        updateTimeButtonText();
                    }
                }, mCalendar.get(Calendar.HOUR_OF_DAY),
                mCalendar.get(Calendar.MINUTE), true);
        return timePicker;
    }

    /**
     * Makes the date button to show the date that has been previously selected by the user.
     */
    private void updateDateButtonText() {
        SimpleDateFormat dateFormat = new SimpleDateFormat(DATE_FORMAT);
        String dateForButton = dateFormat.format(mCalendar.getTime());
        mDateButton.setText(dateForButton);
    }

    /**
     * Makes the time button to show the time that has been previously selected by the user.
     */
    private void updateTimeButtonText() {
        SimpleDateFormat timeFormat = new SimpleDateFormat(TIME_FORMAT);
        String timeForButton = timeFormat.format(mCalendar.getTime());
        mTimeButton.setText(timeForButton);
    }
}
