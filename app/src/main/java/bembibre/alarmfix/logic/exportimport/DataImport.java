package bembibre.alarmfix.logic.exportimport;

import android.app.ProgressDialog;
import android.content.Context;
import android.net.Uri;
import android.os.AsyncTask;
import android.support.annotation.NonNull;

import java.io.File;

import bembibre.alarmfix.R;
import bembibre.alarmfix.logging.Logger;
import bembibre.alarmfix.logic.models.DataImportResultType;
import bembibre.alarmfix.storage.Storage;
import bembibre.alarmfix.logic.SynchronizedWork;
import bembibre.alarmfix.storage.UriProcessor;
import bembibre.alarmfix.logic.models.DataImportResult;
import bembibre.alarmfix.userinterface.UserInterfaceUtils;

/**
 * Created by Max Power on 13/12/2017.
 */

/**
 * This class can import reminders from a file.
 */
public class DataImport extends AsyncTask<Uri, Float, DataImportResult> {

    private Context context;

    /**
     * This informs the user of the progress.
     */
    private ProgressDialog dialog;

    public DataImport(Context context) {
        this.context = context;
        dialog = new ProgressDialog(context);
        dialog.setMessage(context.getResources().getString(R.string.importing_message));
        dialog.setTitle(context.getResources().getString(R.string.progress));
        dialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
        dialog.setCancelable(false);
    }

    /**
     * The method for publishing progress made publicly available for making it be able to be called
     * from our synchronized thread-safe context.
     *
     * @param progress
     */
    public void publishProgressFromOutside(float progress) {
        this.publishProgress(progress);
    }

    protected void onPreExecute() {
        dialog.setProgress(0);
        dialog.setMax(100);

        // Show the dialog when the task begins.
        dialog.show();
    }

    protected DataImportResult doInBackground(Uri... params) {
        DataImportResult result;
        if (params.length > 0) {
            String path = UriProcessor.getPath(this.context, params[0]);
            Logger.log("Importing files from path " + path);
            if (path == null) {
                result = new DataImportResult(DataImportResultType.PATH_ERROR, 0, 0);
            } else {
                File dataToImport = new File(path);
                String json = Storage.readStringFromFile(dataToImport);
                if (json == null) {
                    result = new DataImportResult(DataImportResultType.STORAGE_ERROR, 0, 0);;
                } else {
                    result = SynchronizedWork.importData(context, this, json);
                }
            }
        } else {
            result = new DataImportResult(DataImportResultType.UNKNOWN_ERROR, 0, 0);;
        }
        return result;
    }

    protected void onProgressUpdate(Float... valores) {
        int p = Math.round(100 * valores[0]);
        dialog.setProgress(p);
    }

    protected void onPostExecute(@NonNull DataImportResult success) {
        dialog.dismiss();
        String message;
        switch (success.getType()) {
            case UNKNOWN_ERROR:
                message = this.context.getResources().getString(R.string.import_failure);
                break;
            case FORMAT_ERROR:
                message = this.context.getResources().getString(R.string.import_format_error);
                break;
            case PATH_ERROR:
                message = this.context.getResources().getString(R.string.import_path_error);
                break;
            case STORAGE_ERROR:
                message = this.context.getResources().getString(R.string.import_storage_error);
                break;
            case OK:
            default:
                message = this.context.getResources().getString(R.string.import_success, success.getCorrectlyImportedElements(), success.getElementsWithError());
                break;
        }
        UserInterfaceUtils.showSimpleInformationDialog(this.context, message);
    }
}
