////////////////////////////////////////////////////////////////////////////////
// Copyright (c) 2024 Vladislav Trifochkin
//
// This file is part of `Android support library`.
//
// Changelog:
//      2024.06.05 Initial version.
////////////////////////////////////////////////////////////////////////////////
package pfs.android.daemon;

import static android.app.PendingIntent.FLAG_IMMUTABLE;
import static android.content.Context.NOTIFICATION_SERVICE;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.Icon;

// Android Notification Service
// 1. https://medium.com/exploring-android/exploring-android-o-notification-channels-94cd274f604c
// 2. https://developer.android.com/develop/ui/views/notifications/channels
// 3. https://developer.android.com/develop/ui/views/notifications/build-notification

public class DaemonNotificator
{
    private final static String CHANNEL_ID = "DAEMON_NOTIFICATION_CHANNEL_ID";
    private final static CharSequence CHANNEL_NAME = "Daemon Notificator";
    private final static int NOTIFICATION_ICON = pfs.android.R.drawable.ic_stat_daemon;
    private final static int STOP_ACTION_ICON = android.R.drawable.ic_menu_close_clear_cancel;

    private final static int REQUEST_CODE_BASE = 100;

    private static String createChannel (Service context)
    {
        // POST_NOTIFICATIONS permission required
        NotificationManager notificationManager = (NotificationManager)context.getSystemService(NOTIFICATION_SERVICE);

        int importance = NotificationManager.IMPORTANCE_DEFAULT;
        NotificationChannel notificationChannel = new NotificationChannel(CHANNEL_ID, CHANNEL_NAME, importance);
        notificationChannel.enableLights(true);
        notificationChannel.setLightColor(Color.RED);
        notificationChannel.enableVibration(false);
        notificationChannel.setVibrationPattern(new long[]{100, 200, 300, 400, 500, 400, 300, 200, 400});
        notificationManager.createNotificationChannel(notificationChannel);
        return (String) CHANNEL_ID;
    }

    private static Notification buildNotification (Service context, String channelId, Class<?> daemonActivatorClass)
    {
        PendingIntent stopServiceIntent = PendingIntent.getService(context
            , REQUEST_CODE_BASE + 1, Daemon.stopDaemonIntent(context), FLAG_IMMUTABLE);

        PendingIntent launchMainActivityIntent = PendingIntent.getActivity(context, REQUEST_CODE_BASE + 2
            , new Intent(context, daemonActivatorClass/*DaemonActivator.class*/).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            , FLAG_IMMUTABLE);

        Notification.Action launchMainActivityAction = new Notification.Action.Builder(
            Icon.createWithResource(context, NOTIFICATION_ICON)
            , "Launch activity", launchMainActivityIntent).build();

        // Action to stop the service.
        Notification.Action stopAction = new Notification.Action.Builder(
              Icon.createWithResource(context, STOP_ACTION_ICON)
            , "Stop daemon", stopServiceIntent).build();

        // Create a notification.
        return new Notification.Builder(context, channelId)
            .setContentTitle("FIXME: set content title")
            .setContentText("FIXME: set notification content")
            .setSmallIcon(NOTIFICATION_ICON)
            .setContentIntent(launchMainActivityIntent)
            .addAction(launchMainActivityAction)
            .addAction(stopAction)
            .setStyle(new Notification.BigTextStyle())
            //.setStyle(new Notification.InboxStyle())
            //.setStyle(new Notification.MediaStyle())
            //.setStyle(new Notification.MediaStyle())
            //.setStyle(new Notification.MessagingStyle("User Name"))
            //.setStyle(new Notification.CallStyle())
            //.setStyle(new Notification.BigPictureStyle())
            .build();
    }

    public static Notification createNotification (Service context, Class<?> daemonActivatorClass)
    {
        String channelId = createChannel(context);
        Notification notification = buildNotification(context, channelId, daemonActivatorClass);
        return notification;
    }

    public static boolean areNotificationsEnabled (Context context)
    {
        NotificationManager notificationManager = (NotificationManager)context.getSystemService(NOTIFICATION_SERVICE);
        return notificationManager.areNotificationsEnabled();
    }
}
