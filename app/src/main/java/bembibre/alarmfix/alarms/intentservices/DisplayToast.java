package bembibre.alarmfix.alarms.intentservices;

import android.content.Context;
import android.widget.Toast;

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
        Toast.makeText(mContext, mText, Toast.LENGTH_SHORT).show();
    }
}