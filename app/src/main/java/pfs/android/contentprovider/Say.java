////////////////////////////////////////////////////////////////////////////////
// Copyright (c) 2023 Vladislav Trifochkin
//
// This file is part of `Android support library`.
//
// Changelog:
//      2023.07.31 Initial version.
////////////////////////////////////////////////////////////////////////////////
package pfs.android.contentprovider;

import android.util.Log;
import android.widget.Toast;

public final class Say
{
    private static String _pattern = "%s";
    private static android.content.Context _androidContext = null;

    public static void setContext (@NonNull android.content.Context ctx)
    {
        _androidContext = ctx;
    }

    public static void setPattern (@androidx.annotation.NonNull String pattern)
    {
        _pattern = pattern;
    }

    public static void resetPattern ()
    {
        _pattern = "%s";
    }

    public static void d (String text)
    {
        Log.d(LogTag.TAG, String.format(_pattern, text));
    }

    public static void d (int resId)
    {
        if (_androidContext != null)
            d(_androidContext.getString(resId));
        else
            w(String.format("Unable to obtain resource by identifier: %d: need Android context", resId));
    }

    public static void dtoast (String text)
    {
        String msg = String.format(_pattern, text);
        Log.d(LogTag.TAG, msg);

        if (_androidContext != null) {
            Toast toast = Toast.makeText(_androidContext, msg, Toast.LENGTH_LONG);
            toast.show();
        } else {
            w(String.format("Unable to toast specified text: %s: need Android context", text));
        }
    }

    public static void dtoast (int resId)
    {
        if (_androidContext != null) {
            dtoast(_androidContext.getString(resId));
        } else {
            w(String.format("Unable to obtain resource by identifier: %d: need Android context", resId));
        }
    }

    public static void e (String text)
    {
        Log.e(LogTag.TAG, String.format(_pattern, text));
    }

    public static void w (String text)
    {
        Log.w(LogTag.TAG, String.format(_pattern, text));
    }
}
