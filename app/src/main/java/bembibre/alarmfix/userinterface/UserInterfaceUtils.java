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
     * Shows a simple alert dialog.
     * @param context
     * @param messageResourceId
     */
    public static void showDialog(Context context, int messageResourceId, DialogInterface.OnClickListener listener) {
        AlertDialog alertDialog = new AlertDialog.Builder(context).create();
        alertDialog.setTitle(context.getResources().getString(R.string.alert_dialog_title));
        alertDialog.setMessage(context.getResources().getString(messageResourceId));
        alertDialog.setButton(AlertDialog.BUTTON_NEUTRAL, "OK", listener);

        alertDialog.show();
    }
}
