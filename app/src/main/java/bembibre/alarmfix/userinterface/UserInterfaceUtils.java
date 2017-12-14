package bembibre.alarmfix.userinterface;

/**
 * Created by Max Power on 06/12/2017.
 */

import android.content.Context;
import android.content.DialogInterface;
import android.graphics.PorterDuff;
import android.support.v7.app.AlertDialog;
import android.text.SpannableString;
import android.text.style.ForegroundColorSpan;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ArrayAdapter;
import android.widget.Spinner;

import java.util.List;

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
        for (int i = 0; i < menu.size(); i++) {
            MenuItem item = menu.getItem(i);
            SpannableString spanString = new SpannableString(menu.getItem(i).getTitle().toString());
            spanString.setSpan(new ForegroundColorSpan(menuTextColor), 0, spanString.length(), 0);
            item.setTitle(spanString);
        }
    }

    /**
     * Shows a simple information dialog that makes an action when the user clicks the OK button.
     *
     * @param context         the application context.
     * @param messageResource string to show inside of the dialog.
     * @param listener        the action that will be performed when the user clicks the OK button.
     */
    public static void showInformationDialog(Context context, String messageResource, DialogInterface.OnClickListener listener) {
        UserInterfaceUtils.showWarningDialog(context, context.getResources().getString(R.string.info_dialog_title), messageResource, listener);
    }

    /**
     * Shows a simple warning dialog that makes an action when the user clicks the OK button.
     *
     * @param context         the application context.
     * @param messageResource string to show inside of the dialog.
     * @param listener        the action that will be performed when the user clicks the OK button.
     */
    public static void showWarningDialog(Context context, String messageResource, DialogInterface.OnClickListener listener) {
        UserInterfaceUtils.showWarningDialog(context, context.getResources().getString(R.string.alert_dialog_title), messageResource, listener);
    }

    /**
     * Shows an information dialog that does nothing when the user clicks the OK button.
     *
     * @param context         the application context.
     * @param messageResource string to show inside of the dialog.
     */
    public static void showSimpleInformationDialog(Context context, String messageResource) {
        DialogInterface.OnClickListener listener = new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        };
        UserInterfaceUtils.showInformationDialog(context, messageResource, listener);
    }

    /**
     * Shows a warning dialog that does nothing when the user clicks the OK button.
     *
     * @param context         the application context.
     * @param messageResource string to show inside of the dialog.
     */
    public static void showSimpleWarningDialog(Context context, String messageResource) {
        DialogInterface.OnClickListener listener = new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        };
        UserInterfaceUtils.showWarningDialog(context, messageResource, listener);
    }

    /**
     * Creates an spinner and sets the look and feel.
     *
     * @param context
     * @param spinner
     * @param items
     */
    public static void setSpinnerLookAndFeel(Context context, Spinner spinner, List<String> items) {
        // Sets the layout of items and dropdown items.
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                context, R.layout.spinner_item, items);
        adapter.setDropDownViewResource(R.layout.spinner_dropdown_item);

        // Sets the color of the small triangle.
        spinner.getBackground().setColorFilter(context.getResources().getColor(R.color.actionBarForeground), PorterDuff.Mode.SRC_ATOP);

        spinner.setAdapter(adapter);
    }

    /**
     * Shows a confirmation dialog.
     *
     * @param context       application context.
     * @param messageId     resource identifier of the message to show.
     * @param whatToDoIfYes action that will be done only if the user selects Yes.
     */
    public static void showConfirmationDialog(Context context, int messageId, DialogInterface.OnClickListener whatToDoIfYes) {
        new AlertDialog.Builder(context)
                .setIcon(android.R.drawable.ic_dialog_alert)
                .setTitle(context.getResources().getString(R.string.alert_dialog_title))
                .setMessage(context.getResources().getString(messageId))
                .setPositiveButton(context.getResources().getString(R.string.yes), whatToDoIfYes)
                .setNegativeButton(context.getResources().getString(R.string.no), null)
                .show();
    }

    /**
     * Shows a simple alert dialog that makes an action when the user clicks the OK button.
     *
     * @param context         the application context.
     * @param titleResource   string to show as the dialog's title.
     * @param messageResource string to show inside of the dialog.
     * @param listener        the action that will be performed when the user clicks the OK button.
     */
    private static void showWarningDialog(Context context, String titleResource, String messageResource, DialogInterface.OnClickListener listener) {
        AlertDialog alertDialog = new AlertDialog.Builder(context).create();
        alertDialog.setTitle(titleResource);
        alertDialog.setMessage(messageResource);
        alertDialog.setButton(AlertDialog.BUTTON_NEUTRAL, "OK", listener);

        alertDialog.show();
    }
}
