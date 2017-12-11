package bembibre.alarmfix.database;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import bembibre.alarmfix.logging.Logger;
import bembibre.alarmfix.models.DateTime;

/**
 * Created by Max Power on 12/08/2017.
 */

public class RemindersDbAdapter {
    private static final int DATABASE_VERSION = 1;
    private static final String DATABASE_NAME = "data";

    /*
     * Reminders table.
     */

    private static final String DATABASE_TABLE = "reminders";

    /**
     * Reminder title.
     */
    public static final String KEY_TITLE = "title";

    /**
     * Reminder text.
     */
    public static final String KEY_BODY = "body";

    /**
     * Flag that indicates if the reminder has already been notified to the user by the application.
     * Already notified reminders are not processed while the phone is turned on, but the not
     * notified ones get notified to the user or set as alarms if they are in the future.
     */
    public static final String KEY_NOTIFIED = "notified";

    /**
     * Integer number that is 0 at the beginning but is increased with a unit each time an alarm is
     * set in the system for the reminder, in order to keep track of the alarms that are set for
     * each single reminder. A reminder can be set more than a single alarm for example if the user
     * updates the reminder later than he creates it.
     */
    public static final String KEY_ALARM_ID = "alarm_id";

    /**
     * Date and time of the reminder stored as milliseconds since the Epoch.
     */
    public static final String KEY_DATE_TIME = "reminder_date_time";

    /**
     * Autoincrement primary key of each row.
     */
    public static final String KEY_ROWID = "_id";

    // Other fields.

    public static final long FIRST_ALARM_ID = 1;

    private DatabaseHelper mDbHelper;
    private SQLiteDatabase mDb;
    private static final String DATABASE_CREATE =
            "create table " + DATABASE_TABLE + " ("
                    + KEY_ROWID + " integer primary key autoincrement, "
                    + KEY_TITLE + " text not null, "
                    + KEY_BODY + " text not null, "
                    + KEY_NOTIFIED + " integer, "
                    + KEY_ALARM_ID + " integer not null, "
                    + KEY_DATE_TIME + " integer not null);";
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
            db.execSQL(DATABASE_CREATE);
        }
        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion,
                              int newVersion) {
            // Not used, but you could upgrade the database with ALTER
            // Scripts. Use only ALTER statements that don't delete the current user data!
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
            reminderDateTime) {
        ContentValues initialValues = new ContentValues();
        initialValues.put(KEY_TITLE, title);
        initialValues.put(KEY_BODY, body);
        initialValues.put(KEY_NOTIFIED, false);
        initialValues.put(KEY_ALARM_ID, RemindersDbAdapter.FIRST_ALARM_ID);
        initialValues.put(KEY_DATE_TIME, reminderDateTime);
        return mDb.insert(DATABASE_TABLE, null, initialValues);
    }
    public boolean deleteReminder(long rowId) {
        return
                mDb.delete(DATABASE_TABLE, KEY_ROWID + "=" + rowId, null) > 0;
    }

    public Cursor fetchAllReminders() {
        return mDb.query(DATABASE_TABLE, new String[] {KEY_ROWID, KEY_TITLE,
                KEY_BODY, KEY_NOTIFIED, KEY_ALARM_ID, KEY_DATE_TIME}, null, null, null, null, KEY_DATE_TIME);
    }

    /**
     * Returns all reminders that haven't been notified to the user, yet.
     * @return all reminders that haven't been notified to the user, yet.
     */
    public Cursor fetchAllNotNotifiedReminders() {
        return mDb.query(DATABASE_TABLE, new String[] {KEY_ROWID, KEY_TITLE,
                KEY_BODY, KEY_NOTIFIED, KEY_ALARM_ID, KEY_DATE_TIME}, KEY_NOTIFIED + "= 0", null, null, null, null);
    }

    public Cursor fetchReminder(long rowId) throws SQLException {
        Cursor mCursor =
                mDb.query(true, DATABASE_TABLE, new String[] {KEY_ROWID,
                        KEY_TITLE, KEY_BODY, KEY_NOTIFIED, KEY_ALARM_ID, KEY_DATE_TIME}, KEY_ROWID + "=" +
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
        args.put(KEY_TITLE, title);
        args.put(KEY_BODY, body);
        args.put(KEY_NOTIFIED, 0);
        args.put(KEY_ALARM_ID, alarmId);
        args.put(KEY_DATE_TIME, reminderDateTime);
        return
                mDb.update(DATABASE_TABLE, args, KEY_ROWID + "=" + rowId, null) > 0;
    }

    public boolean updateReminder(long rowId, boolean notified) {
        ContentValues args = new ContentValues();
        int notifiedAsInt;
        if (notified) {
            notifiedAsInt = 1;
        } else {
            notifiedAsInt = 0;
        }
        args.put(KEY_NOTIFIED, notifiedAsInt);
        return
                mDb.update(DATABASE_TABLE, args, KEY_ROWID + "=" + rowId, null) > 0;
    }
}
