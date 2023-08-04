package pfs.android.contentprovider;

import android.util.Log;
import android.widget.Toast;

public final class Say
{
    private String _pattern = "%s";
    private android.content.Context _androidContext = null;

    public Say (android.content.Context ctx)
    {
        _androidContext = ctx;
    }

    public Say (String pattern, android.content.Context ctx)
    {
        _pattern = pattern;
        _androidContext = ctx;
    }

    public void d (String text)
    {
        Log.d(LogTag.TAG, String.format(_pattern, text));
    }

    public void d (int resId)
    {
        d(_androidContext.getString(resId));
    }

    public void dt (String text)
    {
        String msg = String.format(_pattern, text);
        Log.d(LogTag.TAG, msg);
        Toast toast = Toast.makeText(_androidContext, msg, Toast.LENGTH_LONG);
        toast.show();
    }

    public void dt (int resId)
    {
        dt(_androidContext.getString(resId));
    }

    public void e (String text)
    {
        Log.e(LogTag.TAG, String.format(_pattern, text));
    }

    public void e (int resId)
    {
        e(_androidContext.getString(resId));
    }

    public void w (String text)
    {
        Log.w(LogTag.TAG, String.format(_pattern, text));
    }

    public void w (int resId)
    {
        w(_androidContext.getString(resId));
    }
}
