package bembibre.alarmfix.alarms.intentservices;

import android.content.Context;
import android.widget.Toast;

import bembibre.alarmfix.R;
import bembibre.alarmfix.logging.Logger;
import bembibre.alarmfix.userinterface.NotificationManager;

/**
 * Created by Max Power on 16/12/2017.
 */

public class DisplayToast implements Runnable {
    private final Context mContext;
    String mText;

    public DisplayToast(Context mContext, String text){
        this.mContext = mContext;
        mText = text;
    }

    public void run(){
        try {
            new NotificationManager(this.mContext).makeGeneralNotification(this.mContext.getResources().getString(R.string.info_dialog_title), mText);
        } catch (Exception e) {
            Toast.makeText(mContext, mText, Toast.LENGTH_SHORT).show();
            Logger.log("Unknown error accessing to notifications service.", e);
        }
    }
}