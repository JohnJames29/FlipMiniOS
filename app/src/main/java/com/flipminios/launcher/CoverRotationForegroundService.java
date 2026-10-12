package com.flipminios.launcher;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ServiceInfo;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.provider.Settings;
import android.util.Log;

/**
 * User-enabled visible foreground service for keeping the cover rotation
 * overlay active after waking the screen. No AccessibilityService is used.
 * No inspection of TikTok, Instagram, Reddit, screen contents, or gestures.
 */
public final class CoverRotationForegroundService extends Service {
    private static final String TAG = "FlipCoverRotation";
    private static final String CHANNEL = "cover_rotation";
    private static final int NOTIFICATION_ID = 1057;
    private static final String ACTION_STOP = "com.flipminios.launcher.STOP_ROTATION";
    private final Handler main = new Handler(Looper.getMainLooper());
    private BroadcastReceiver receiver;

    public static boolean start(Context context) {
        if (!Prefs.coverRotationEnabled(context) ||
                !Prefs.coverRotationKeepAlive(context) ||
                !Settings.canDrawOverlays(context)) return false;
        try {
            Intent intent = new Intent(context, CoverRotationForegroundService.class);
            context.startForegroundService(intent);
            return true;
        } catch (RuntimeException exception) {
            Log.w(TAG, "Foreground keepalive cannot start", exception);
            return false;
        }
    }

    public static void stop(Context context) {
        context.stopService(new Intent(context, CoverRotationForegroundService.class));
    }

    @Override public void onCreate() {
        super.onCreate();
        NotificationManager manager = getSystemService(NotificationManager.class);
        if (manager != null) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL, "Flip Mini cover rotation", NotificationManager.IMPORTANCE_LOW);
            channel.setDescription("Visible while the optional cover rotation wake helper is active");
            manager.createNotificationChannel(channel);
        }
    }

    @Override public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null && ACTION_STOP.equals(intent.getAction())) {
            Prefs.setCoverRotationEnabled(this, false);
            Prefs.setCoverRotationKeepAlive(this, false);
            CoverRotationEngineActivity.stop();
            stopSelf();
            return START_NOT_STICKY;
        }
        if (!Prefs.coverRotationEnabled(this) ||
                !Prefs.coverRotationKeepAlive(this) ||
                !Settings.canDrawOverlays(this)) {
            stopSelf();
            return START_NOT_STICKY;
        }
        try {
            Notification notification = createNotification();
            if (Build.VERSION.SDK_INT >= 34) {
                startForeground(NOTIFICATION_ID, notification,
                        ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE);
            } else {
                startForeground(NOTIFICATION_ID, notification);
            }
        } catch (RuntimeException ex) {
            Log.w(TAG, "Cannot start visible cover rotation foreground service", ex);
            stopSelf();
            return START_NOT_STICKY;
        }
        if (receiver == null) {
            receiver = new BroadcastReceiver() {
                @Override public void onReceive(Context context, Intent received) {
                    if (!Prefs.coverRotationEnabled(context)) return;
                    // The service already runs in the foreground; create the
                    // cover overlay directly, with no background Activity launch.
                    main.postDelayed(() ->
                            CoverRotationEngineActivity.ensureFromWake(context), 700);
                }
            };
            IntentFilter filter = new IntentFilter();
            filter.addAction(Intent.ACTION_SCREEN_ON);
            filter.addAction(Intent.ACTION_USER_PRESENT);
            registerReceiver(receiver, filter);
        }
        CoverRotationEngineActivity.ensureFromWake(this);
        return START_STICKY;
    }

    private Notification createNotification() {
        Intent stop = new Intent(this, CoverRotationForegroundService.class);
        stop.setAction(ACTION_STOP);
        PendingIntent stopAction = PendingIntent.getService(this, 7, stop,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        Intent open = new Intent(this, LauncherActivity.class);
        PendingIntent openAction = PendingIntent.getActivity(this, 8, open,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        return new Notification.Builder(this, CHANNEL)
                .setContentTitle("Flip Mini cover rotation")
                .setContentText("Cover rotation wake-up helper is active")
                .setSmallIcon(android.R.drawable.ic_popup_sync)
                .setContentIntent(openAction)
                .setOngoing(true)
                .addAction(new Notification.Action.Builder(
                        android.R.drawable.ic_menu_close_clear_cancel,
                        "Stop rotation", stopAction).build())
                .build();
    }

    @Override public IBinder onBind(Intent intent) { return null; }

    @Override public void onDestroy() {
        if (receiver != null) {
            try { unregisterReceiver(receiver); }
            catch (IllegalArgumentException ignored) { }
            receiver = null;
        }
        main.removeCallbacksAndMessages(null);
        super.onDestroy();
    }
}
