package bembibre.alarmfix.logic.exportimport;

import android.app.ProgressDialog;
import android.content.Context;
import android.database.Cursor;
import android.os.AsyncTask;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.util.Calendar;

import bembibre.alarmfix.R;
import bembibre.alarmfix.database.RemindersDbAdapter;
import bembibre.alarmfix.logging.Logger;
import bembibre.alarmfix.storage.Storage;
import bembibre.alarmfix.userinterface.UserInterfaceUtils;

/**
 * Created by Max Power on 13/12/2017.
 */

/**
 * This class can export the application data into a file.
 */
public class DataExport extends AsyncTask<Void, Float, Boolean> {

    private static final String EXPORT_DIRECTORY_NAME = "Alarm Fix Backups";
    private static final String EXPORT_FILE_NAME = "Reminders ";
    private static final String EXPORT_FILE_EXTENSION = ".json";
    private Context context;

    /**
     * This informs the user of the progress.
     */
    private ProgressDialog dialog;

    private static String getExportFileFullName() {
        Calendar now = Calendar.getInstance();
        String month = Integer.valueOf(now.get(Calendar.MONTH) + 1).toString();
        String formattedMonth;
        if (month.length() == 1) {
            formattedMonth = "0" + month;
        } else {
            formattedMonth = month;
        }
        String day = Integer.valueOf(now.get(Calendar.DAY_OF_MONTH)).toString();
        String formattedDay;
        if (month.length() == 1) {
            formattedDay = "0" + day;
        } else {
            formattedDay = day;
        }
        return EXPORT_FILE_NAME + now.get(Calendar.YEAR) + formattedMonth + formattedDay + EXPORT_FILE_EXTENSION;
    }

    private static boolean writeBackupFile(File where, String content) {
        return Storage.writeStringToFile(where, content, false);
    }

    public DataExport(Context context) {
        this.context = context;
        dialog = new ProgressDialog(context);
        dialog.setMessage(context.getResources().getString(R.string.exporting_message));
        dialog.setTitle(context.getResources().getString(R.string.progress));
        dialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
        dialog.setCancelable(false);
    }

    protected void onPreExecute() {
        dialog.setProgress(0);
        dialog.setMax(100);

        // Show the dialog when the task begins.
        dialog.show();
    }

    protected Boolean doInBackground(Void... params) {
        boolean result;
        RemindersDbAdapter dbHelper = RemindersDbAdapter.getInstance(this.context);
        dbHelper.open();
        int page = 0;
        JSONArray reminders = new JSONArray();
        JSONObject reminder;
        try {
            long totalReminders = dbHelper.countAllReminders();
            Cursor cursor = dbHelper.fetchAllReminders(page);
            long processedReminders = 0;
            while ((cursor != null) && (cursor.moveToFirst())) {
                do {
                    String title = cursor.getString(cursor.getColumnIndexOrThrow(RemindersDbAdapter.REMINDERS_COLUMN_TITLE));
                    String body = cursor.getString(cursor.getColumnIndexOrThrow(RemindersDbAdapter.REMINDERS_COLUMN_BODY));
                    Long dateTime = cursor.getLong(cursor.getColumnIndexOrThrow(RemindersDbAdapter.REMINDERS_COLUMN_DATE_TIME));
                    int notified = cursor.getInt(cursor.getColumnIndexOrThrow(RemindersDbAdapter.REMINDERS_COLUMN_NOTIFIED));
                    boolean notifiedBoolean;
                    notifiedBoolean = notified == 1;
                    reminder = new JSONObject();
                    reminder.put("title", title);
                    reminder.put("body", body);
                    reminder.put("date_time", dateTime);
                    reminder.put("notified", notifiedBoolean);
                    reminders.put(reminder);
                    processedReminders++;

                    publishProgress(((float) processedReminders) / totalReminders / 10 * 8);
                } while (cursor.moveToNext());
                cursor.close();
                page++;
                cursor = dbHelper.fetchAllReminders(page);
            }
        } catch (JSONException e) {
            Logger.log("JSON encoding wrong programmed", e);
            reminders = null;
        } finally {
            dbHelper.close();
        }

        File backupsDirectory = Storage.getApplicationDirectory(EXPORT_DIRECTORY_NAME);
        String jsonString;
        if ((backupsDirectory == null) || (reminders == null)) {
            jsonString = null;
        } else {
            try {
                jsonString = reminders.toString(4);
            } catch (JSONException e) {
                jsonString = null;
                Logger.log("JSON encoding wrong programmed", e);
            }
        }
        if (jsonString == null) {
            result = false;
        } else {
            File backupFile = new File(backupsDirectory, getExportFileFullName());
            result = writeBackupFile(backupFile, jsonString);
            publishProgress(1f);
        }

        return result;
    }

    protected void onProgressUpdate(Float... valores) {
        int p = Math.round(100 * valores[0]);
        dialog.setProgress(p);
    }

    protected void onPostExecute(Boolean success) {
        dialog.dismiss();
        int messageId;
        if (success) {
            messageId = R.string.export_success;
        } else {
            messageId = R.string.export_failure;
        }
        UserInterfaceUtils.showSimpleInformationDialog(this.context, this.context.getResources().getString(messageId));
    }
}
