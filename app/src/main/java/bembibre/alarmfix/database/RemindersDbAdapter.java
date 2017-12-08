package bembibre.alarmfix.database;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import bembibre.alarmfix.logging.Logger;

/**
 * Created by Max Power on 12/08/2017.
 */

public class RemindersDbAdapter {
    private static final String DATABASE_NAME = "data";
    private static final String DATABASE_TABLE = "reminders";
    private static final int DATABASE_VERSION = 1;
    public static final String KEY_TITLE = "title";
    public static final String KEY_BODY = "body";
    public static final String KEY_NOTIFIED = "notified";
    public static final String KEY_DATE_TIME = "reminder_date_time";
    public static final String KEY_ROWID = "_id";
    private DatabaseHelper mDbHelper;
    private SQLiteDatabase mDb;
    private static final String DATABASE_CREATE =
            "create table " + DATABASE_TABLE + " ("
                    + KEY_ROWID + " integer primary key autoincrement, "
                    + KEY_TITLE + " text not null, "
                    + KEY_BODY + " text not null, "
                    + KEY_NOTIFIED + " integer, "
                    + KEY_DATE_TIME + " text not null);";
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

    public long createReminder(String title, String body, String
            reminderDateTime) {
        ContentValues initialValues = new ContentValues();
        initialValues.put(KEY_TITLE, title);
        initialValues.put(KEY_BODY, body);
        initialValues.put(KEY_NOTIFIED, false);
        initialValues.put(KEY_DATE_TIME, reminderDateTime);
        return mDb.insert(DATABASE_TABLE, null, initialValues);
    }
    public boolean deleteReminder(long rowId) {
        return
                mDb.delete(DATABASE_TABLE, KEY_ROWID + "=" + rowId, null) > 0;
    }

    public Cursor fetchAllReminders() {
        return mDb.query(DATABASE_TABLE, new String[] {KEY_ROWID, KEY_TITLE,
                KEY_BODY, KEY_NOTIFIED, KEY_DATE_TIME}, null, null, null, null, null);
    }

    public Cursor fetchReminder(long rowId) throws SQLException {
        Cursor mCursor =
                mDb.query(true, DATABASE_TABLE, new String[] {KEY_ROWID,
                        KEY_TITLE, KEY_BODY, KEY_DATE_TIME}, KEY_ROWID + "=" +
                rowId, null,
                null, null, null, null);
        if (mCursor != null) {
            mCursor.moveToFirst();
        }
        return mCursor;
    }

    public boolean updateReminder(long rowId, String title, String body, String
            reminderDateTime) {
        ContentValues args = new ContentValues();
        args.put(KEY_TITLE, title);
        args.put(KEY_BODY, body);
        args.put(KEY_NOTIFIED, 0);
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
