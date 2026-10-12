package com.flipminios.launcher;

import android.app.Activity;
import android.app.ActivityOptions;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.graphics.drawable.GradientDrawable;
import android.hardware.display.DisplayManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.view.Display;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import android.util.DisplayMetrics;
import android.widget.TextView;

/**
 * Cover-only rotation overlay, adapted from CoverSpin's MIT-licensed approach.
 *
 * Source and license: https://github.com/crispim1411/CoverSpin
 *
 * The 0x0, non-touchable window makes an orientation request to Android's
 * secondary-display window manager. It does not change display size/density,
 * run shell commands, access third-party app data, or use Shizuku.
 */
public final class CoverRotationEngineActivity extends Activity {
    private static final Handler MAIN = new Handler(Looper.getMainLooper());
    private static View rotationAnchor;
    private static TextView refreshButton;
    private static WindowManager overlayManager;
    private static Context coverContext;
    private static int installedDisplayId = Display.INVALID_DISPLAY;
    private static boolean attached;
    private static boolean refreshing;
    private static final int REFRESH_WAIT_MS = 850;

    public static void startOnCover(Context context) {
        if (!Prefs.coverRotationEnabled(context)) return;
        if (!Settings.canDrawOverlays(context)) return;
        DisplayManager dm = (DisplayManager) context.getSystemService(Context.DISPLAY_SERVICE);
        if (dm == null) return;
        Display cover = dm.getDisplay(1);
        if (cover == null || cover.getState() != Display.STATE_ON) return;
        if (attached && installedDisplayId == cover.getDisplayId()) {
            applyPreferences(context);
            return;
        }
        // Avoid a transient cover activity, which can reveal a blank system-bar
        // region. We already have a legitimate cover activity in the foreground.
        if (context instanceof Activity) {
            Display current = ((Activity) context).getDisplay();
            if (current != null && current.getDisplayId() == cover.getDisplayId()) {
                installOverlay(context, cover);
                return;
            }
        }
        Intent intent = new Intent(context, CoverRotationEngineActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK |
                Intent.FLAG_ACTIVITY_NO_ANIMATION | Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS);
        ActivityOptions options = ActivityOptions.makeBasic();
        options.setLaunchDisplayId(cover.getDisplayId());
        try {
            context.startActivity(intent, options.toBundle());
        } catch (RuntimeException ex) {
            // The user may still launch the activity directly from the cover widget.
            android.util.Log.w("FlipCoverRotation", "Cover activity not permitted", ex);
        }
    }

    /**
     * Foreground service safe path: attaches an external-display overlay directly.
     * Does not attempt to start an activity from the background on Android 15+.
     */
    public static void ensureFromWake(Context context) {
        if (!Prefs.coverRotationEnabled(context) || !Settings.canDrawOverlays(context)) return;
        DisplayManager manager = (DisplayManager) context.getSystemService(Context.DISPLAY_SERVICE);
        if (manager == null) return;
        Display display = manager.getDisplay(1);
        if (display == null || display.getState() != Display.STATE_ON) return;
        if (isActive() && installedDisplayId == display.getDisplayId()) {
            applyPreferences(context);
            return;
        }
        installOverlay(context, display);
    }

    /** Disables cover rotation overlay without affecting display configuration. */
    public static void stop() {
        removeButton();
        if (rotationAnchor != null && overlayManager != null) {
            try { overlayManager.removeView(rotationAnchor); }
            catch (RuntimeException ignored) { }
        }
        rotationAnchor = null;
        overlayManager = null;
        coverContext = null;
        installedDisplayId = Display.INVALID_DISPLAY;
        attached = false;
        refreshing = false;
    }

    public static boolean isActive() {
        return attached && rotationAnchor != null && rotationAnchor.getParent() != null;
    }

    public static void applyPreferences(Context context) {
        if (!isActive() || overlayManager == null) return;
        try {
            WindowManager.LayoutParams p =
                    (WindowManager.LayoutParams) rotationAnchor.getLayoutParams();
            int newMode = Prefs.coverRotationAuto(context) ?
                    ActivityInfo.SCREEN_ORIENTATION_FULL_SENSOR :
                    ActivityInfo.SCREEN_ORIENTATION_LOCKED;
            if (p.screenOrientation != newMode) {
                p.screenOrientation = newMode;
                overlayManager.updateViewLayout(rotationAnchor, p);
            }
            showButtonIfEnabled(context);
        } catch (RuntimeException ex) {
            android.util.Log.w("FlipCoverRotation", "Unable to update cover rotation", ex);
        }
    }

    /**
     * Unlike toggling LOCKED to FULL_SENSOR, explicitly request an orientation
     * perpendicular to the current display and then restore sensor rotation.
     * This can produce a real configuration change, but third-party app reflow
     * is controlled by Instagram/One UI and remains unverified on the Flip6.
     */
    public static void requestRefresh() {
        if (!isActive() || coverContext == null || overlayManager == null ||
                refreshing) return;
        if (!Prefs.coverRotationAuto(coverContext)) {
            android.widget.Toast.makeText(coverContext,
                    "Enable Auto rotation before refreshing", android.widget.Toast.LENGTH_SHORT).show();
            return;
        }
        DisplayManager manager = (DisplayManager) coverContext.getSystemService(Context.DISPLAY_SERVICE);
        Display display = manager == null ? null : manager.getDisplay(installedDisplayId);
        if (display == null) return;
        refreshing = true;
        try {
            DisplayMetrics metrics = new DisplayMetrics();
            display.getRealMetrics(metrics);
            // Force the opposite layout class, not a lock on the same angle.
            int forced = metrics.widthPixels >= metrics.heightPixels ?
                    ActivityInfo.SCREEN_ORIENTATION_PORTRAIT :
                    ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE;
            WindowManager.LayoutParams p =
                    (WindowManager.LayoutParams) rotationAnchor.getLayoutParams();
            p.screenOrientation = forced;
            overlayManager.updateViewLayout(rotationAnchor, p);
            setButtonFeedback(true);
            MAIN.postDelayed(() -> {
                if (isActive() && overlayManager != null && coverContext != null) {
                    try {
                        WindowManager.LayoutParams restore =
                                (WindowManager.LayoutParams) rotationAnchor.getLayoutParams();
                        restore.screenOrientation = ActivityInfo.SCREEN_ORIENTATION_FULL_SENSOR;
                        overlayManager.updateViewLayout(rotationAnchor, restore);
                    } catch (RuntimeException ex) {
                        android.util.Log.w("FlipCoverRotation", "Cannot restore sensor orientation", ex);
                    }
                }
                refreshing = false;
                setButtonFeedback(false);
            }, REFRESH_WAIT_MS);
        } catch (RuntimeException ex) {
            refreshing = false;
            setButtonFeedback(false);
            android.util.Log.w("FlipCoverRotation", "Refresh rotation cycle failed", ex);
        }
    }

    /** Shows that the tap was received without claiming Instagram reflowed. */
    private static void setButtonFeedback(boolean pending) {
        if (refreshButton == null) return;
        refreshButton.setText(pending ? "···" : "⟳");
        refreshButton.setAlpha(pending ? 0.60f : 1.0f);
    }

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        Display display = getDisplay();
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_FULLSCREEN | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION |
                        View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY |
                        View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN |
                        View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION |
                        View.SYSTEM_UI_FLAG_LAYOUT_STABLE);
        if (display == null || display.getDisplayId() == Display.DEFAULT_DISPLAY ||
                display.getDisplayId() != 1 ||
                !Prefs.coverRotationEnabled(this) || !Settings.canDrawOverlays(this)) {
            finish();
            return;
        }
        installOverlay(this, display);
        finish();
        overridePendingTransition(0, 0);
    }

    private static void installOverlay(Context context, Display display) {
        stop();
        coverContext = context.getApplicationContext().createDisplayContext(display);
        overlayManager = (WindowManager) coverContext.getSystemService(WINDOW_SERVICE);
        if (overlayManager == null) return;
        View anchor = new View(coverContext);
        WindowManager.LayoutParams p = new WindowManager.LayoutParams(
                0, 0, WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE |
                        WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE |
                        WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                PixelFormat.TRANSLUCENT);
        p.gravity = Gravity.TOP | Gravity.START;
        p.screenOrientation = Prefs.coverRotationAuto(context) ?
                ActivityInfo.SCREEN_ORIENTATION_FULL_SENSOR :
                ActivityInfo.SCREEN_ORIENTATION_LOCKED;
        try {
            overlayManager.addView(anchor, p);
            rotationAnchor = anchor;
            installedDisplayId = display.getDisplayId();
            attached = true;
            showButtonIfEnabled(context);
        } catch (RuntimeException ex) {
            stop();
            android.util.Log.w("FlipCoverRotation", "Cover overlay rejected", ex);
        }
    }

    private static void showButtonIfEnabled(Context context) {
        removeButton();
        if (!Prefs.coverRotationButton(context) || overlayManager == null ||
                coverContext == null || !isActive()) return;
        int size = Math.round(34 * coverContext.getResources().getDisplayMetrics().density);
        TextView button = new TextView(coverContext);
        button.setText("⟳");
        button.setTextSize(21);
        button.setTextColor(Color.WHITE);
        button.setGravity(Gravity.CENTER);
        GradientDrawable bg = new GradientDrawable();
        bg.setShape(GradientDrawable.OVAL);
        bg.setColor(0xBB1D2533);
        button.setBackground(bg);
        button.setContentDescription("Refresh cover rotation request");
        button.setOnClickListener(v -> requestRefresh());
        WindowManager.LayoutParams p = new WindowManager.LayoutParams(
                size, size, WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE |
                        WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN |
                        WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                PixelFormat.TRANSLUCENT);
        p.layoutInDisplayCutoutMode =
                WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS;
        p.screenOrientation = Prefs.coverRotationAuto(context) ?
                ActivityInfo.SCREEN_ORIENTATION_FULL_SENSOR :
                ActivityInfo.SCREEN_ORIENTATION_LOCKED;
        // Bottom-left avoids the right-hand engagement buttons on Reels,
        // and avoids the cameras located at the top of the cover.
        p.gravity = Gravity.BOTTOM | Gravity.START;
        p.x = Math.round(14 * coverContext.getResources().getDisplayMetrics().density);
        p.y = Math.round(56 * coverContext.getResources().getDisplayMetrics().density);
        try {
            overlayManager.addView(button, p);
            refreshButton = button;
        } catch (RuntimeException ex) {
            android.util.Log.w("FlipCoverRotation", "Refresh button rejected", ex);
        }
    }

    private static void removeButton() {
        if (refreshButton != null && overlayManager != null) {
            try { overlayManager.removeView(refreshButton); }
            catch (RuntimeException ignored) { }
        }
        refreshButton = null;
    }
}
