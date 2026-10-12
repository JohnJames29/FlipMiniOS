package com.flipminios.launcher;

import android.accessibilityservice.AccessibilityService;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Handler;
import android.os.Looper;
import android.view.accessibility.AccessibilityEvent;

/**
 * Opt-in keepalive equivalent of CoverSpin's unlock listener.
 * Does NOT read accessibility events, window content, text or gestures.
 * Only restarts the cover-only orientation anchor after screen wake.
 */
public final class CoverRotationAccessibilityService extends AccessibilityService {
    private BroadcastReceiver wakeReceiver;
    private final Handler main = new Handler(Looper.getMainLooper());

    @Override public void onServiceConnected() {
        super.onServiceConnected();
        if (Prefs.coverRotationEnabled(this)) {
            main.postDelayed(() -> CoverRotationEngineActivity.startOnCover(this), 900);
        }
        if (wakeReceiver != null) return;
        wakeReceiver = new BroadcastReceiver() {
            @Override public void onReceive(Context context, Intent intent) {
                if (!Prefs.coverRotationEnabled(context)) return;
                main.postDelayed(() -> CoverRotationEngineActivity.startOnCover(context), 900);
            }
        };
        IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_SCREEN_ON);
        filter.addAction(Intent.ACTION_USER_PRESENT);
        registerReceiver(wakeReceiver, filter);
    }

    @Override public void onAccessibilityEvent(AccessibilityEvent event) {
        // Deliberately ignore all accessibility events and app contents.
    }

    @Override public void onInterrupt() { }

    @Override public void onDestroy() {
        if (wakeReceiver != null) {
            try { unregisterReceiver(wakeReceiver); }
            catch (IllegalArgumentException ignored) { }
            wakeReceiver = null;
        }
        super.onDestroy();
    }
}
