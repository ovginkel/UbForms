package org.iphukan.ubforms.common;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.support.annotation.StringRes;

import org.iphukan.ubforms.R;

public class DialogUtils {

    public static void displayChooseOkNoDialog(final Activity activity, @StringRes int title,
                                               @StringRes int message,
                                               final IDialogClickListener callBack) {
        if (activity == null) return;
        displayChooseOkNoDialog(activity, activity.getString(title), activity.getString(message), callBack);
    }

    public static void displayChooseOkNoDialog(final Activity activity, String title, String message,
                                               final IDialogClickListener callBack) {
        if (activity == null) return;
        new AlertDialog.Builder(activity)
                .setTitle(title)
                .setMessage(message)
                .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        callBack.onOK();
                    }
                })
                .setNegativeButton(R.string.no, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {

                    }
                }).create().show();
    }

}
