package bembibre.alarmfix.alarms.intentservices;

import android.content.Context;
import android.widget.Toast;

import bembibre.alarmfix.R;
import bembibre.alarmfix.logging.Logger;
import bembibre.alarmfix.userinterface.NotificationManager;

/**
 * Class needed to display in a different thread the notification of alarms configured.
 */
public class AlarmsConfiguredNotificationRunnable implements Runnable {
    private final Context mContext;
    String mText;

    public AlarmsConfiguredNotificationRunnable(Context mContext, String text){
        this.mContext = mContext;
        mText = text;
    }

    public void run(){
        try {
            new NotificationManager(this.mContext).makeAlarmsConfiguredNotification(this.mContext.getResources().getString(R.string.info_dialog_title), mText);
        } catch (Exception e) {
            Toast.makeText(mContext, mText, Toast.LENGTH_SHORT).show();
            Logger.log("Unknown error accessing to notifications service.", e);
        }
    }
}