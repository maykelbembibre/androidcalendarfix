package bembibre.alarmfix.logic;

import android.app.ProgressDialog;
import android.content.Context;
import android.net.Uri;
import android.os.AsyncTask;
import android.support.annotation.NonNull;

import bembibre.alarmfix.R;
import bembibre.alarmfix.userinterface.UserInterfaceUtils;

/**
 * Created by Max Power on 13/12/2017.
 */

/**
 * This class can delete all reminders at once.
 */
public class DeleteAllReminders extends AsyncTask<Void, Float, Boolean> {

    private Context context;

    /**
     * This informs the user of the progress.
     */
    private ProgressDialog dialog;

    public DeleteAllReminders(Context context) {
        this.context = context;
        dialog = new ProgressDialog(context);
        dialog.setMessage(context.getResources().getString(R.string.deleting_message));
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
    void publishProgressFromOutside(float progress) {
        this.publishProgress(progress);
    }

    protected void onPreExecute() {
        dialog.setProgress(0);
        dialog.setMax(100);

        // Show the dialog when the task begins.
        dialog.show();
    }

    protected Boolean doInBackground(Void... params) {
        return SynchronizedWork.deleteAllData(context, this);
    }

    protected void onProgressUpdate(Float... valores) {
        int p = Math.round(100 * valores[0]);
        dialog.setProgress(p);
    }

    protected void onPostExecute(@NonNull Boolean success) {
        dialog.dismiss();
        String message;
        if (success) {
            message = this.context.getResources().getString(R.string.delete_success);
        } else {
            message = this.context.getResources().getString(R.string.delete_failure);
        }
        UserInterfaceUtils.showSimpleInformationDialog(this.context, message);
    }
}
