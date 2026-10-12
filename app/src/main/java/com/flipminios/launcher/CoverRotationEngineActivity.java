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
    // Allow Android long enough for an actual display rotation to occur,
    // and avoid rapid changes that Android may classify as an orientation loop.
    private static final int REFRESH_WAIT_MS = 1700;
    private static final int RESULT_DURATION_MS = 2400;
    private static Runnable completion;
    private static Runnable resetFeedback;
    private static DisplayManager.DisplayListener displayObserver;

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
        cancelRefresh();
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
        if (!isActive() || overlayManager == null || refreshing) return;
        try {
            WindowManager.LayoutParams p =
                    (WindowManager.LayoutParams) rotationAnchor.getLayoutParams();
            int newMode = Prefs.coverRotationAuto(context) ?
                    ActivityInfo.SCREEN_ORIENTATION_FULL_SENSOR :
                    ActivityInfo.SCREEN_ORIENTATION_LOCKED;
            // Both overlay windows must request the SAME orientation. The
            // topmost floating button otherwise overrides the invisible anchor.
            applyOrientationToBoth(newMode);
            showButtonIfEnabled(context);
        } catch (RuntimeException ex) {
            android.util.Log.w("FlipCoverRotation", "Unable to update cover rotation", ex);
        }
    }

    /**
     * Synchronously update the two TYPE_APPLICATION_OVERLAY windows. CoverSpin
     * updates its gesture overlay together with the underlying 0x0 anchor.
     * Our v0.7.2 skipped this, leaving conflicting orientation requests.
     */
    private static void applyOrientationToBoth(int orientation) {
        if (overlayManager == null) return;
        if (refreshButton != null && refreshButton.getParent() != null) {
            WindowManager.LayoutParams buttonParams =
                    (WindowManager.LayoutParams) refreshButton.getLayoutParams();
            if (buttonParams.screenOrientation != orientation) {
                buttonParams.screenOrientation = orientation;
                overlayManager.updateViewLayout(refreshButton, buttonParams);
            }
        }
        if (rotationAnchor != null && rotationAnchor.getParent() != null) {
            WindowManager.LayoutParams anchorParams =
                    (WindowManager.LayoutParams) rotationAnchor.getLayoutParams();
            if (anchorParams.screenOrientation != orientation) {
                anchorParams.screenOrientation = orientation;
                overlayManager.updateViewLayout(rotationAnchor, anchorParams);
            }
        }
    }

    private static void cancelRefresh() {
        if (completion != null) MAIN.removeCallbacks(completion);
        if (resetFeedback != null) MAIN.removeCallbacks(resetFeedback);
        completion = null;
        resetFeedback = null;
        if (displayObserver != null && coverContext != null) {
            DisplayManager mgr = (DisplayManager) coverContext.getSystemService(Context.DISPLAY_SERVICE);
            if (mgr != null) mgr.unregisterDisplayListener(displayObserver);
        }
        displayObserver = null;
        refreshing = false;
    }

    /**
     * A real rotation trial: update BOTH overlays, observe whether Android
     * changed the actual display rotation, then restore the chosen sensor mode.
     * A request or a click animation is NOT evidence that rotation took place.
     */
    public static void requestRefresh() {
        if (!isActive() || coverContext == null || overlayManager == null ||
                refreshing) return;
        if (!Prefs.coverRotationAuto(coverContext)) {
            android.widget.Toast.makeText(coverContext,
                    "Switch to Auto rotation first", android.widget.Toast.LENGTH_SHORT).show();
            return;
        }
        DisplayManager manager =
                (DisplayManager) coverContext.getSystemService(Context.DISPLAY_SERVICE);
        final Display display = manager == null ? null : manager.getDisplay(installedDisplayId);
        if (display == null || display.getState() != Display.STATE_ON) {
            showRefreshResult(false);
            return;
        }

        if (resetFeedback != null) MAIN.removeCallbacks(resetFeedback);
        resetFeedback = null;
        final int initialRotation = display.getRotation();
        final boolean[] rotated = {false};
        displayObserver = new DisplayManager.DisplayListener() {
            @Override public void onDisplayAdded(int displayId) { }
            @Override public void onDisplayRemoved(int displayId) { }
            @Override public void onDisplayChanged(int displayId) {
                if (displayId != installedDisplayId) return;
                Display live = manager.getDisplay(displayId);
                if (live != null && live.getRotation() != initialRotation) {
                    rotated[0] = true;
                }
            }
        };
        manager.registerDisplayListener(displayObserver, MAIN);
        refreshing = true;
        setButtonPending();

        try {
            DisplayMetrics metrics = new DisplayMetrics();
            display.getRealMetrics(metrics);
            final int forced = metrics.widthPixels >= metrics.heightPixels ?
                    ActivityInfo.SCREEN_ORIENTATION_PORTRAIT :
                    ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE;
            // Unlike v0.7.2, the visible overlay no longer vetoes the anchor.
            applyOrientationToBoth(forced);
            completion = () -> {
                completion = null;
                if (manager.getDisplay(installedDisplayId) != null &&
                        manager.getDisplay(installedDisplayId).getRotation() != initialRotation) {
                    rotated[0] = true;
                }
                if (displayObserver != null) {
                    manager.unregisterDisplayListener(displayObserver);
                    displayObserver = null;
                }
                refreshing = false;
                if (isActive() && coverContext != null) {
                    try {
                        applyOrientationToBoth(Prefs.coverRotationAuto(coverContext) ?
                                ActivityInfo.SCREEN_ORIENTATION_FULL_SENSOR :
                                ActivityInfo.SCREEN_ORIENTATION_LOCKED);
                    } catch (RuntimeException ex) {
                        android.util.Log.w("FlipCoverRotation", "Restore orientation failed", ex);
                    }
                }
                showRefreshResult(rotated[0]);
                android.util.Log.i("FlipCoverRotation",
                        "Refresh trial: display rotation " +
                                (rotated[0] ? "changed" : "DID NOT CHANGE") +
                                " (Reels layout cannot be inspected by this app)");
            };
            MAIN.postDelayed(completion, REFRESH_WAIT_MS);
        } catch (RuntimeException ex) {
            cancelRefresh();
            if (isActive() && coverContext != null) {
                try { applyOrientationToBoth(ActivityInfo.SCREEN_ORIENTATION_FULL_SENSOR); }
                catch (RuntimeException ignored) { }
            }
            showRefreshResult(false);
            android.util.Log.w("FlipCoverRotation", "Rotation request rejected", ex);
        }
    }

    private static void setButtonPending() {
        if (refreshButton == null) return;
        refreshButton.setText("…");
        refreshButton.setTextColor(Color.WHITE);
        refreshButton.setAlpha(0.7f);
        refreshButton.setContentDescription("Waiting for Android rotation");
    }

    /** Reports an observed display rotation, not assumed Instagram success. */
    private static void showRefreshResult(boolean didRotate) {
        if (refreshButton == null) return;
        refreshButton.setText(didRotate ? "✓" : "!");
        refreshButton.setTextColor(didRotate ? 0xFF86EFAC : 0xFFFCA5A5);
        refreshButton.setAlpha(1.0f);
        refreshButton.setContentDescription(didRotate ?
                "Display rotated; Instagram's layout may still be unchanged" :
                "Android did not rotate the cover display");
        android.widget.Toast.makeText(coverContext, didRotate ?
                "Display rotated. Instagram layout is app-controlled." :
                "Samsung ignored the rotation request", android.widget.Toast.LENGTH_SHORT).show();
        resetFeedback = () -> {
            resetFeedback = null;
            if (refreshButton == null) return;
            refreshButton.setText("⟳");
            refreshButton.setTextColor(Color.WHITE);
            refreshButton.setContentDescription("Try cover rotation and verify the display changed");
        };
        MAIN.postDelayed(resetFeedback, RESULT_DURATION_MS);
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
