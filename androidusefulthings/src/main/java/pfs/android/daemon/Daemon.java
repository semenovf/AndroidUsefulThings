////////////////////////////////////////////////////////////////////////////////
// Copyright (c) 2024 Vladislav Trifochkin
//
// This file is part of `Android support library`.
//
// Changelog:
//      2024.06.05 Initial version.
////////////////////////////////////////////////////////////////////////////////
package pfs.android.daemon;

import android.app.Notification;
import android.app.Service;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ServiceInfo;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;

import java.util.concurrent.atomic.AtomicInteger;
import pfs.android.IntentDumper;

// References:
// 1. https://developer.android.com/develop/background-work/services
// 2. https://developer.android.com/develop/background-work/services/foreground-services

public class Daemon extends Service
{
    private final static String TAG = "~Daemon~";

    public static final int START_SERVICE_ID = 1;

    private static final String KEY_COMMAND = "cmd";
    private static final int COMMAND_INVALID = 0;
    private static final int COMMAND_START = 1;
    private static final int COMMAND_STOP = 2;

    private static final int STATE_INITIAL = 0;
    private static final int STATE_STARTING = 1;
    private static final int STATE_RUNNING = 2;
    private static final int STATE_FINISHING = 3;
    private static final int STATE_FINISHED = 4;

    public static final int NO_ERROR = 0;
    public static final int ERROR_DAEMON_NOT_RUNNING = NO_ERROR + 1;
    public static final int ERROR_NOTIFICATIONS_DISABLED = NO_ERROR + 2;

    // FIXME Need to initialize
    static private Class<?> _daemonActivatorClass;

    AtomicInteger _state = new AtomicInteger(STATE_INITIAL);

    private static native String [] nativeSupportLibraries ();

    // Used to load the 'daemon' library on application startup.
    static
    {
        Log.d(TAG, "Load native library: daemon");
        System.loadLibrary("daemon");

        String [] nativeLibs = nativeSupportLibraries();

        for (int i = 0; i < nativeLibs.length; i++) {
            Log.d(TAG, "Load native library for daemon: " + nativeLibs[i]);
//            //System.loadLibrary(nativeLibs[i]);
        }
    }

    public Daemon ()
    {
        Log.d(TAG, "Daemon constructed");
    }

    public static String errorString (int rc)
    {
        switch (rc) {
            case NO_ERROR: return "No error";
            case ERROR_DAEMON_NOT_RUNNING: return "Daemon not running";
            case ERROR_NOTIFICATIONS_DISABLED: return "Notifications are disabled";
            default:
                throw new RuntimeException("Add error message for code: " + rc);
        }
    }

    private void startJob (int startId)
    {
        int state = _state.get();

        if (state == STATE_STARTING) {
            new Thread(new Runnable() {
                int counter = 0;

                public void run ()
                {
                    _state.set(STATE_RUNNING);
                    Log.d(TAG, "Daemon running");

                    while (_state.get() == STATE_RUNNING) {
                        try {
                            Thread.sleep(2000);
                            Log.d(TAG, startId + ": COUNTER: " + ++counter);
                        } catch (InterruptedException e) {
                            Log.d(TAG, "Daemon finishing");
                            _state.set(STATE_FINISHING);
                        }
                    }

                    _state.set(STATE_FINISHED);
                    Log.d(TAG, "Daemon finished");
                }
            }).start();
        } else {
            Log.e(TAG, "Daemon expected in STARTING state");
        }
    }

    private void stopJob ()
    {
        _state.set(STATE_FINISHING);
        Log.d(TAG, "Daemon finishing");
    }

    public static Intent startDaemonIntent (Context context)
    {
        Intent intent = new Intent(context, Daemon.class);
        intent.putExtra(KEY_COMMAND, COMMAND_START);
        return intent;
    }

    public static Intent stopDaemonIntent (Context context)
    {
        Intent intent = new Intent(context, Daemon.class);
        intent.putExtra(KEY_COMMAND, COMMAND_STOP);
        return intent;
    }

    private void commandStart (int startId)
    {
        if (_state.get() != STATE_INITIAL) {
            Log.e(TAG, "Daemon already running");
            return;
        }

        _state.set(STATE_STARTING);
        Log.d(TAG, "Daemon starting");

        try {
            // FIXME---------------------------------------------------------------------------------v
            Notification notification = DaemonNotificator.createNotification(this, _daemonActivatorClass);

            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
                startForeground(START_SERVICE_ID, notification);
            } else {
                // https://medium.com/@domen.lanisnik/guide-to-foreground-services-on-android-9d0127dc8f9a
                startForeground(START_SERVICE_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_REMOTE_MESSAGING);
            }

            startJob(startId);
        } catch (Exception e) {
            Log.e(TAG, "Starting daemon failure: " + e.toString());
            _state.set(STATE_FINISHED);
            stopSelf(startId);
        }
    }

    private void commandStop (int startId)
    {
        if (_state.get() == STATE_INITIAL) {
            Log.e(TAG, "Daemon not running");
            return;
        }

        try {
            stopJob();
            stopForeground(true);
            stopSelf(startId);
        } catch (Exception e) {
            Log.e(TAG, "Stopping daemon failure: " + e.toString());
            _state.set(STATE_FINISHED);
            stopSelf(startId);
        }
    }

    private boolean route (int startId, Intent intent)
    {
        if (intent == null)
            return false;

        if (!intent.getExtras().containsKey(KEY_COMMAND))
            return false;

        int command = intent.getExtras().getInt(KEY_COMMAND, COMMAND_INVALID);

        if (command == COMMAND_INVALID)
            return false;

        switch (command) {
            case COMMAND_START:
                commandStart(startId);
                break;
            case COMMAND_STOP:
                commandStop(startId);
                break;
            default:
                return false;
        }

        return true;
    }

    private static int startStop (Context context, int command)
    {
        if (command == COMMAND_START) {
            Log.d(TAG, "Starting daemon...");
            context.startForegroundService(startDaemonIntent(context));
        } else {
            Log.d(TAG, "Stopping daemon...");

            //boolean success = context.stopService(stopDaemonIntent(context));
            ComponentName componentName = context.startForegroundService(stopDaemonIntent(context));

            if (componentName == null) {
               Log.w(TAG, "Daemon not running or not found for stopping");
               return ERROR_DAEMON_NOT_RUNNING;
            }
        }

        return NO_ERROR;
    }

    public static int start (Context context, Class<?> daemonActivatorClass)
    {
        if (!DaemonNotificator.areNotificationsEnabled(context))
            return ERROR_NOTIFICATIONS_DISABLED;

        _daemonActivatorClass = daemonActivatorClass;
        return startStop(context, COMMAND_START);
    }

    public static int stop (Context context)
    {
        return startStop(context, COMMAND_STOP);
    }

   @Override
    public void onCreate ()
    {
        super.onCreate();
        _state.set(STATE_INITIAL);
        Log.d(TAG, "Daemon created");
    }

    @Override
    public int onStartCommand (Intent intent, int flags, int startId)
    {
        Log.d(TAG, String.format("Daemon's onStartCommand: flags=%d; startId=%d; intent=%s"
            , flags, startId, IntentDumper.toString(intent)));
        route(startId, intent);
        return START_NOT_STICKY;
    }

    @Override
    public IBinder onBind (Intent intent)
    {
        // Binding is not provided by this service
        return null;
    }

    @Override
    public void onDestroy ()
    {
        Log.d(TAG, "Daemon destroying");

        int state = _state.get();

        // Force daemon stopping when activating by stopService() call
        if (state == STATE_STARTING || state == STATE_RUNNING)
            commandStop(1);

        super.onDestroy();
    }
}