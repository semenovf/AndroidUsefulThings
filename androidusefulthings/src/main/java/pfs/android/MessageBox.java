////////////////////////////////////////////////////////////////////////////////
// Copyright (c) 2023 Vladislav Trifochkin
//
// This file is part of `Android support library`.
//
// Changelog:
//      2023.08.25 Initial version.
////////////////////////////////////////////////////////////////////////////////
package pfs.android;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.FragmentManager;
import android.content.DialogInterface;
import android.os.Bundle;


// 1. [Dialogs]
//    (https://developer.android.com/develop/ui/views/components/dialogs)
public class MessageBox extends DialogFragment
{
    private static final String TITLE = "title";
    private static final String TEXT = "text";
    private static final String ICON = "icon";
    //private static final String ICON_NAME = "icon_name";

    public static final int ALERT_ICON = android.R.drawable.ic_dialog_alert;
    public static final int INFO_ICON  = android.R.drawable.ic_dialog_info;

    @Override
    public Dialog onCreateDialog (Bundle savedInstanceState)
    {
        Bundle args = this.getArguments();
        String title = args.getString(TITLE, "No title");
        String text = args.getString(TEXT, "No message");
        int icon = args.getInt(ICON, ALERT_ICON);

        // Use the Builder class for convenient dialog construction
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle(title)
            .setMessage(text)
            .setIcon(icon)
            .setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int id) {}
            });

        return builder.create();
    }

    private static void staticShow (FragmentManager fm, String tag, int icon, String title, String text)
    {
        Bundle args = new Bundle();
        args.putString(TITLE, title);
        args.putString(TEXT, text);
        args.putInt(ICON, icon);
        DialogFragment dialogFragment = new MessageBox();
        dialogFragment.setArguments(args);
        dialogFragment.show(fm, tag);
    }

    public static void showAlert (FragmentManager fm, String tag, String title, String text)
    {
        staticShow(fm, tag, ALERT_ICON, title, text);
    }

    public static void showInfo (FragmentManager fm, String tag, String title, String text)
    {
        staticShow(fm, tag, INFO_ICON, title, text);
    }
}
