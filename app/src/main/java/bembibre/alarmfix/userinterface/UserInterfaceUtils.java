package bembibre.alarmfix.userinterface;

/**
 * Created by Max Power on 06/12/2017.
 */

import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Color;
import android.support.v7.app.AlertDialog;
import android.text.SpannableString;
import android.text.style.ForegroundColorSpan;
import android.view.Menu;
import android.view.MenuItem;

import bembibre.alarmfix.R;
import bembibre.alarmfix.ReminderEditActivity;

/**
 * Utilities class for user interface details that are too complex to design other way.
 */
public class UserInterfaceUtils {

    /**
     * Adjusts the colors and design of the given menu. This method should be called every time
     * a menu is rendered, for having consistent look and feel.
     *
     * @param context
     * @param menu
     */
    public static void setMenuDesign(Context context, Menu menu) {
        int menuTextColor = context.getResources().getColor(R.color.menuTextColor);
        for(int i = 0; i < menu.size(); i++) {
            MenuItem item = menu.getItem(i);
            SpannableString spanString = new SpannableString(menu.getItem(i).getTitle().toString());
            spanString.setSpan(new ForegroundColorSpan(menuTextColor), 0, spanString.length(), 0);
            item.setTitle(spanString);
        }
    }

    /**
     * Shows a simple alert dialog that makes an action when the user clicks the OK button.
     * @param context the application context.
     * @param messageResourceId ID of the string to show inside of the dialog.
     * @param listener the action that will be performed when the user clicks the OK button.
     */
    public static void showDialog(Context context, int messageResourceId, DialogInterface.OnClickListener listener) {
        AlertDialog alertDialog = new AlertDialog.Builder(context).create();
        alertDialog.setTitle(context.getResources().getString(R.string.alert_dialog_title));
        alertDialog.setMessage(context.getResources().getString(messageResourceId));
        alertDialog.setButton(AlertDialog.BUTTON_NEUTRAL, "OK", listener);

        alertDialog.show();
    }

    /**
     * Shows a dialog that does nothing when the user clicks the OK button.
     * @param context the application context.
     * @param messageResourceId ID of the string to show inside of the dialog.
     */
    public static void showSimpleDialog(Context context, int messageResourceId) {
        DialogInterface.OnClickListener listener = new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        };
        UserInterfaceUtils.showDialog(context, R.string.reminder_does_not_exist, listener);
    }
}
