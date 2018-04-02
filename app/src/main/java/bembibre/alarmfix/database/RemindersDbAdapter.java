package bembibre.alarmfix.database;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import bembibre.alarmfix.logging.Logger;
import bembibre.alarmfix.models.Alarm;

/**
 * Created by Max Power on 12/08/2017.
 */

public class RemindersDbAdapter {

    /**
     * Versions.
     * 1: first version.
     * 2: added table "alarms". Some users with version 1 could already have that table because a
     * logistic error.
     */
    private static final int DATABASE_VERSION = 2;
    private static final String DATABASE_NAME = "data";

    /*
     * Reminders table.
     */
    private static final String DATABASE_TABLE_REMINDERS = "reminders";

    /**
     * Autoincrement primary key of each row.
     */
    public static final String REMINDERS_COLUMN_ROWID = "_id";

    /**
     * Reminder title.
     */
    public static final String REMINDERS_COLUMN_TITLE = "title";

    /**
     * Reminder text.
     */
    public static final String REMINDERS_COLUMN_BODY = "body";

    /**
     * Flag that indicates if the reminder has already been notified to the user by the application.
     * Already notified reminders are not processed while the phone is turned on, but the not
     * notified ones get notified to the user or set as alarms if they are in the future.
     */
    public static final String REMINDERS_COLUMN_NOTIFIED = "notified";

    /**
     * Integer number that is 0 at the beginning but is increased with a unit each time an alarm is
     * set in the system for the reminder, in order to keep track of the alarms that are set for
     * each single reminder. A reminder can be set more than a single alarm for example if the user
     * updates the reminder later than he creates it.
     */
    public static final String REMINDERS_COLUMN_ALARM_ID = "alarm_id";

    public static final String REMINDERS_COLUMN_YEAR = "year";

    /**
     * The month starts from 0 respecting the Java format.
     */
    public static final String REMINDERS_COLUMN_MONTH = "month";

    /**
     * Date and time of the reminder stored as milliseconds since the Epoch.
     */
    public static final String REMINDERS_COLUMN_DATE_TIME = "reminder_date_time";

    /*
     * Alarms table. It keeps track of the alarms that are currently set and pending. Every alarm
     * of any type set across the application which is still pending to go off, is inside of this
     * table. When the alarms goes off or gets erased, it is taken out of the table.
     *
     * The purpose of this table is to be able to know at any time if there is any alarm that is
     * earlier than the current date and time and has failed to go off so that the application can
     * recover from that failure.
     */
    private static final String DATABASE_TABLE_ALARMS = "alarms";

    /**
     * Autoincrement primary key of each row.
     */
    public static final String ALARMS_COLUMN_ROWID = "_id";

    /**
     * Date and time of the alarm stored as milliseconds since the Epoch.
     */
    public static final String ALARMS_COLUMN_DATE_TIME = "alarm_date_time";

    /**
     * Identifier of the reminder for which this alarm was set.
     */
    public static final String ALARMS_COLUMN_REMINDER_ID = "reminder_id";

    // Other fields.

    public static final long FIRST_ALARM_ID = 1;

    /**
     * For pagination.
     */
    public static final int PAGE_SIZE = 10;

    private DatabaseHelper mDbHelper;
    private SQLiteDatabase mDb;
    private static final String TABLE_REMINDERS_CREATE =
            "create table " + DATABASE_TABLE_REMINDERS + " ("
                    + REMINDERS_COLUMN_ROWID + " integer primary key autoincrement, "
                    + REMINDERS_COLUMN_TITLE + " text not null, "
                    + REMINDERS_COLUMN_BODY + " text not null, "
                    + REMINDERS_COLUMN_NOTIFIED + " integer, "
                    + REMINDERS_COLUMN_ALARM_ID + " integer not null, "
                    + REMINDERS_COLUMN_YEAR + " integer not null, "
                    + REMINDERS_COLUMN_MONTH + " integer not null, "
                    + REMINDERS_COLUMN_DATE_TIME + " integer not null);";

    private static final String TABLE_ALARMS_CREATE =
            "create table if not exists " + DATABASE_TABLE_ALARMS + " ("
                    + ALARMS_COLUMN_ROWID + " integer primary key autoincrement, "
                    + ALARMS_COLUMN_REMINDER_ID + " integer, "
                    + ALARMS_COLUMN_DATE_TIME + " integer not null);";

    private final Context mCtx;

    private static RemindersDbAdapter instance;
    private int openConnectionsCount = 0;

    // https://stackoverflow.com/questions/2493331/what-are-the-best-practices-for-sqlite-on-android

    private static class DatabaseHelper extends SQLiteOpenHelper {
        DatabaseHelper(Context context) {
            super(context, DATABASE_NAME, null, DATABASE_VERSION);
        }
        @Override
        public void onCreate(SQLiteDatabase db) {
            db.execSQL(TABLE_REMINDERS_CREATE);
            db.execSQL(TABLE_ALARMS_CREATE);
        }
        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion,
                              int newVersion) {
            // Not used, but you could upgrade the database with ALTER
            // Scripts. Use only ALTER statements that don't delete the current user data!

            // When going from version 1 to 2, the alarms table could need to be created.
            // Some users with version 1 could already have it due to a logistic error.
            if ((oldVersion == 1) && (newVersion == DATABASE_VERSION)) {
                db.execSQL(TABLE_ALARMS_CREATE);
            }
        }
    }

    private RemindersDbAdapter(Context ctx) {
        this.mCtx = ctx;
    }

    public synchronized static RemindersDbAdapter getInstance(Context ctx) {
        if (instance == null) {
            instance = new RemindersDbAdapter(ctx);
            instance.mDbHelper = new DatabaseHelper(ctx);
        }
        return instance;
    }

    public synchronized void open() throws android.database.SQLException {
        if (openConnectionsCount < 1) {
            mDb = mDbHelper.getWritableDatabase();
            Logger.log("Database opened");
        } else {
            Logger.log("Database needed. Connection open from " + (openConnectionsCount + 1) + " places.");
        }
        openConnectionsCount++;
    }

    public synchronized void close() {
        openConnectionsCount--;
        if (openConnectionsCount < 1) {
            mDbHelper.close();
            Logger.log("Database closed");
        } else {
            Logger.log("Database not needed any longer. Connection remains open from " + openConnectionsCount + " places.");
        }
    }

    /**
     * Begins a database transaction: remember to finish it at the finally part of a try/catch/finally block.
     */
    public void beginTransaction() {
        this.mDb.beginTransaction();
    }

    /**
     * Commits the current transaction.
     */
    public void setTransactionSuccessful() {
        this.mDb.setTransactionSuccessful();
    }

    /**
     * Ends the current transaction.
     */
    public void endTransaction() {
        this.mDb.endTransaction();
    }

    public long createReminder(String title, String body, long
            reminderDateTime, boolean notified) {
        ContentValues initialValues = new ContentValues();
        initialValues.put(REMINDERS_COLUMN_TITLE, title);
        initialValues.put(REMINDERS_COLUMN_BODY, body);
        initialValues.put(REMINDERS_COLUMN_NOTIFIED, notified);
        initialValues.put(REMINDERS_COLUMN_ALARM_ID, RemindersDbAdapter.FIRST_ALARM_ID);
        initialValues.put(REMINDERS_COLUMN_DATE_TIME, reminderDateTime);

        // Redundant information, for efficient filter by year and month.
        int[] yearAndMonth = this.getYearAndMonth(reminderDateTime);
        initialValues.put(REMINDERS_COLUMN_YEAR, yearAndMonth[0]);
        initialValues.put(REMINDERS_COLUMN_MONTH, yearAndMonth[1]);

        return mDb.insert(DATABASE_TABLE_REMINDERS, null, initialValues);
    }

    public boolean deleteReminder(long rowId) {
        return
                mDb.delete(DATABASE_TABLE_REMINDERS, REMINDERS_COLUMN_ROWID + "=" + rowId, null) > 0;
    }

    /**
     * Deletes ALL the database rows. Use it carefully ;)
     *
     * @return true if there was at least 1 row erased.
     */
    public boolean deleteAllReminders() {
        return
                mDb.delete(DATABASE_TABLE_REMINDERS, null, null) > 0;
    }

    public Cursor fetchAllReminders(int filterYear, int filterMonth) {
        String filter = REMINDERS_COLUMN_YEAR + " = " + filterYear + " AND " + REMINDERS_COLUMN_MONTH + " = " + filterMonth;
        return mDb.query(DATABASE_TABLE_REMINDERS, new String[] {REMINDERS_COLUMN_ROWID, REMINDERS_COLUMN_TITLE,
                REMINDERS_COLUMN_BODY, REMINDERS_COLUMN_NOTIFIED, REMINDERS_COLUMN_ALARM_ID, REMINDERS_COLUMN_DATE_TIME, REMINDERS_COLUMN_YEAR, REMINDERS_COLUMN_MONTH}, filter, null, null, null, REMINDERS_COLUMN_DATE_TIME);
    }

    /**
     * Fetches reminders doing pagination.
     *
     * @param page the page, starting from 0.
     * @return the cursor.
     */
    public Cursor fetchAllReminders(int page) {
        return mDb.rawQuery(
            "select * from " + DATABASE_TABLE_REMINDERS + " order by " + REMINDERS_COLUMN_DATE_TIME +
            " limit " + RemindersDbAdapter.PAGE_SIZE +
            " offset " + (page * RemindersDbAdapter.PAGE_SIZE), null
        );
    }

    public int countAllReminders() {
        Cursor mCount= mDb.rawQuery("select count(*) from " + DATABASE_TABLE_REMINDERS, null);
        mCount.moveToFirst();
        int count = mCount.getInt(0);
        mCount.close();
        return count;
    }

    public Cursor fetchAllRemindersByYear() {
        // Select year, group by year (avoid repetition), order by year (ascending).
        return mDb.rawQuery("select count(*), " + REMINDERS_COLUMN_YEAR + ", " + REMINDERS_COLUMN_MONTH + " from " + DATABASE_TABLE_REMINDERS + " GROUP BY " + REMINDERS_COLUMN_YEAR + ", " + REMINDERS_COLUMN_MONTH + " ORDER BY " + REMINDERS_COLUMN_YEAR + ", " + REMINDERS_COLUMN_MONTH, null);
    }

    /**
     * Returns all reminders that haven't been notified to the user, yet.
     * @return all reminders that haven't been notified to the user, yet.
     */
    public Cursor fetchAllNotNotifiedReminders() {
        return mDb.query(DATABASE_TABLE_REMINDERS, new String[] {REMINDERS_COLUMN_ROWID, REMINDERS_COLUMN_TITLE,
                REMINDERS_COLUMN_BODY, REMINDERS_COLUMN_NOTIFIED, REMINDERS_COLUMN_ALARM_ID, REMINDERS_COLUMN_DATE_TIME}, REMINDERS_COLUMN_NOTIFIED + "= 0", null, null, null, REMINDERS_COLUMN_DATE_TIME);
    }

    public Cursor fetchReminder(long rowId) throws SQLException {
        Cursor mCursor =
                mDb.query(true, DATABASE_TABLE_REMINDERS, new String[] {REMINDERS_COLUMN_ROWID,
                                REMINDERS_COLUMN_TITLE, REMINDERS_COLUMN_BODY, REMINDERS_COLUMN_NOTIFIED, REMINDERS_COLUMN_ALARM_ID, REMINDERS_COLUMN_DATE_TIME}, REMINDERS_COLUMN_ROWID + "=" +
                rowId, null,
                null, null, null, null);
        if (mCursor != null) {
            mCursor.moveToFirst();
        }
        return mCursor;
    }

    public boolean updateReminder(long rowId, String title, String body, long
            reminderDateTime, long alarmId) {
        ContentValues args = new ContentValues();
        args.put(REMINDERS_COLUMN_TITLE, title);
        args.put(REMINDERS_COLUMN_BODY, body);
        args.put(REMINDERS_COLUMN_NOTIFIED, 0);
        args.put(REMINDERS_COLUMN_ALARM_ID, alarmId);
        args.put(REMINDERS_COLUMN_DATE_TIME, reminderDateTime);

        // Redundant information, for efficient filter by year and month.
        int[] yearAndMonth = this.getYearAndMonth(reminderDateTime);
        args.put(REMINDERS_COLUMN_YEAR, yearAndMonth[0]);
        args.put(REMINDERS_COLUMN_MONTH, yearAndMonth[1]);

        return
                mDb.update(DATABASE_TABLE_REMINDERS, args, REMINDERS_COLUMN_ROWID + "=" + rowId, null) > 0;
    }

    public boolean updateReminder(long rowId, boolean notified) {
        ContentValues args = new ContentValues();
        int notifiedAsInt;
        if (notified) {
            notifiedAsInt = 1;
        } else {
            notifiedAsInt = 0;
        }
        args.put(REMINDERS_COLUMN_NOTIFIED, notifiedAsInt);
        return
                mDb.update(DATABASE_TABLE_REMINDERS, args, REMINDERS_COLUMN_ROWID + "=" + rowId, null) > 0;
    }

    /**
     * Creates an alarm reference at the database.
     *
     * @param alarmDateTime time of the alarm.
     * @param reminderId    database identifier of the associated reminder. It can be null if it is
     *                      not a reminder alarm but an alarm for checking reminders at a later time.
     * @return identifier of the created alarm reference.
     */
    public long createAlarm(long alarmDateTime, Long reminderId) {
        ContentValues initialValues = new ContentValues();
        initialValues.put(ALARMS_COLUMN_DATE_TIME, alarmDateTime);
        initialValues.put(ALARMS_COLUMN_REMINDER_ID, reminderId);

        return mDb.insert(DATABASE_TABLE_ALARMS, null, initialValues);
    }

    public boolean deleteAlarm(long rowId) {
        return
                mDb.delete(DATABASE_TABLE_ALARMS, ALARMS_COLUMN_ROWID + "=" + rowId, null) > 0;
    }

    /**
     * Deletes ALL the database rows in the alarms table. Use it carefully ;)
     *
     * @return true if there was at least 1 row erased.
     */
    public boolean deleteAllAlarms() {
        return
                mDb.delete(DATABASE_TABLE_ALARMS, null, null) > 0;
    }

    public Alarm fetchEarliestAlarm() {
        Alarm result;

        // Ascending order by date and time. I am interested in the first result, the earliest one.
        Cursor cursor = mDb.query(DATABASE_TABLE_ALARMS, new String[] {ALARMS_COLUMN_DATE_TIME}, null, null, null, null, ALARMS_COLUMN_DATE_TIME);

        if ((cursor != null) && (cursor.moveToFirst())) {
            Long dateTime = cursor.getLong(cursor.getColumnIndex(RemindersDbAdapter.ALARMS_COLUMN_DATE_TIME));
            result = new Alarm(new Date(dateTime));
        } else {
            result = null;
        }
        return result;
    }

    private int[] getYearAndMonth(long millis) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(new Date(millis));
        return new int[]{calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH)};
    }
}
