package com.flipminios.launcher;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.GestureDescription;
import android.content.Context;
import android.graphics.Path;
import android.graphics.PixelFormat;
import android.graphics.Point;
import android.hardware.display.DisplayManager;
import android.os.Build;
import android.util.SparseArray;
import android.view.Display;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import android.view.accessibility.AccessibilityWindowInfo;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import java.util.List;
import java.util.Locale;

/**
 * Opt-in service for user-requested navigation and video controls. No content collection,
 * analytics, network access or background gestures. Works only on a non-default display.
 */
public final class CoverAccessibilityService extends AccessibilityService {
    private static volatile CoverAccessibilityService live;
    private WindowManager wm;
    private LinearLayout overlay;
    private WindowManager navManager;
    private LinearLayout navOverlay;
    private int navDisplayId = Display.INVALID_DISPLAY;
    private Context overlayContext;
    private int overlayDisplayId = Display.INVALID_DISPLAY;
    private String currentVideoPackage = "";

    public static boolean isAvailable() { return live != null; }
    public static boolean action(int action) {
        CoverAccessibilityService service = live;
        return service != null && service.performGlobalAction(action);
    }
    @Override protected void onServiceConnected() {
        super.onServiceConnected();
        live = this;
    }
    @Override public void onAccessibilityEvent(AccessibilityEvent event) {
        if (event == null) return;
        int type = event.getEventType();
        if (type != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED &&
                type != AccessibilityEvent.TYPE_WINDOWS_CHANGED) return;
        int id = event.getDisplayId();
        if (id <= Display.DEFAULT_DISPLAY) {
            // Never put overlays over the primary display. Cover-screen events are handled separately.
            return;
        }
        String pkg = event.getPackageName() == null ? "" : event.getPackageName().toString();
        // Android may emit events from System UI or our own floating controls while video stays visible.
        // Ignore those instead of repeatedly tearing down and recreating the windows.
        if (pkg.isEmpty() || pkg.equals("com.android.systemui")) return;
        if (Prefs.navigationDockEnabled(this) && !pkg.equals(getPackageName())) showNavOverlay(id);
        else removeNavOverlay();
        if (!Prefs.overlayEnabled(this)) { removeOverlay(); return; }
        if (isVideoApp(pkg)) {
            currentVideoPackage = pkg;
            showOverlay(id);
        } else if (!getPackageName().equals(pkg)) {
            currentVideoPackage = "";
            removeOverlay();
        }
    }
    /** Small opt-in cover-only dock. Global actions remain system-managed; no root required. */
    private void showNavOverlay(int displayId) {
        if (navOverlay != null && navDisplayId == displayId) return;
        removeNavOverlay();
        Display display = getSystemService(DisplayManager.class).getDisplay(displayId);
        if (display == null) return;
        try {
            Context ctx = createDisplayContext(display).createWindowContext(
                    WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY, null);
            navManager = (WindowManager) ctx.getSystemService(Context.WINDOW_SERVICE);
            navOverlay = Ui.horizontal(ctx);
            navOverlay.setPadding(Ui.dp(ctx, 4), Ui.dp(ctx, 2), Ui.dp(ctx, 4), Ui.dp(ctx, 2));
            navOverlay.setBackground(Ui.rounded(0xe9151b29, Ui.dp(ctx, 15)));
            navButton(ctx, "‹", "Back", GLOBAL_ACTION_BACK);
            navButton(ctx, "⌂", "Home", GLOBAL_ACTION_HOME);
            navButton(ctx, "▣", "Recents", GLOBAL_ACTION_RECENTS);
            WindowManager.LayoutParams params = new WindowManager.LayoutParams(
                    WindowManager.LayoutParams.WRAP_CONTENT, WindowManager.LayoutParams.WRAP_CONTENT,
                    WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
                    WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE |
                            WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
                    PixelFormat.TRANSLUCENT);
            params.gravity = Gravity.END | Gravity.BOTTOM;
            params.x = Ui.dp(ctx, 8);
            params.y = Ui.dp(ctx, 8);
            navManager.addView(navOverlay, params);
            navDisplayId = displayId;
        } catch (RuntimeException ex) { removeNavOverlay(); }
    }
    private void navButton(Context ctx, String symbol, String description, int action) {
        TextView button = Ui.text(ctx, symbol, 21, true, Ui.TEXT);
        button.setContentDescription(description);
        button.setGravity(Gravity.CENTER);
        button.setMinWidth(Ui.dp(ctx, 43));
        button.setMinHeight(Ui.dp(ctx, 39));
        button.setOnClickListener(v -> performGlobalAction(action));
        navOverlay.addView(button);
    }
    private void removeNavOverlay() {
        if (navOverlay != null && navManager != null) {
            try { navManager.removeViewImmediate(navOverlay); } catch (RuntimeException ignored) { }
        }
        navOverlay = null;
        navManager = null;
        navDisplayId = Display.INVALID_DISPLAY;
    }
    private boolean isVideoApp(String name) {
        return "com.instagram.android".equals(name) || "com.zhiliaoapp.musically".equals(name)
                || "com.zhiliaoapp.musically.go".equals(name);
    }
    private void showOverlay(int displayId) {
        if (overlay != null && overlayDisplayId == displayId) return;
        removeOverlay();
        Display display = getSystemService(DisplayManager.class).getDisplay(displayId);
        if (display == null) return;
        try {
            overlayContext = createDisplayContext(display).createWindowContext(WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY, null);
            wm = (WindowManager) overlayContext.getSystemService(Context.WINDOW_SERVICE);
            overlay = Ui.vertical(overlayContext);
            overlay.setPadding(Ui.dp(overlayContext, 4), Ui.dp(overlayContext, 6), Ui.dp(overlayContext, 4), Ui.dp(overlayContext, 6));
            overlay.setBackground(Ui.rounded(0xe5121827, Ui.dp(overlayContext, 13)));
            addButton("↑", () -> swipe(true));
            addButton("♡", () -> pressAccessible("Like", "Unlike"));
            addButton("☰", () -> pressAccessible("Comment", "Comments"));
            addButton("↗", () -> pressAccessible("Share", "Send to"));
            addButton("↓", () -> swipe(false));
            addButton("×", () -> { Prefs.setOverlay(this, false); removeOverlay(); });
            WindowManager.LayoutParams params = new WindowManager.LayoutParams(
                    Ui.dp(overlayContext, 58), WindowManager.LayoutParams.WRAP_CONTENT,
                    WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
                    WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE |
                            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
                    PixelFormat.TRANSLUCENT);
            params.gravity = (Prefs.overlayOnRight(this) ? Gravity.END : Gravity.START) | Gravity.CENTER_VERTICAL;
            wm.addView(overlay, params);
            overlayDisplayId = displayId;
        } catch (RuntimeException ex) {
            removeOverlay(); // Samsung may disallow overlays on the private cover display.
        }
    }
    private void addButton(String title, Runnable cb) {
        TextView item = Ui.text(overlayContext, title, 19, true, Ui.TEXT);
        item.setGravity(Gravity.CENTER);
        item.setHeight(Ui.dp(overlayContext, 43));
        item.setContentDescription("Video " + title);
        item.setOnClickListener(v -> cb.run());
        overlay.addView(item);
    }
    private void swipe(boolean next) {
        Display d = getSystemService(DisplayManager.class).getDisplay(overlayDisplayId);
        if (d == null) return;
        Point size = new Point();
        d.getRealSize(size);
        float x = size.x * .74f;
        float from = next ? size.y * .74f : size.y * .30f;
        float to = next ? size.y * .30f : size.y * .74f;
        Path path = new Path();
        path.moveTo(x, from);
        path.lineTo(x, to);
        GestureDescription gesture = new GestureDescription.Builder()
                .setDisplayId(overlayDisplayId)
                .addStroke(new GestureDescription.StrokeDescription(path, 0, 290))
                .build();
        dispatchGesture(gesture, null, null);
    }
    private void pressAccessible(String... names) {
        // Only called by a real user tap; do not silently interact with other apps.
        AccessibilityNodeInfo root = targetRoot();
        if (root == null) {
            Toast.makeText(this, "Controls unavailable in this view", Toast.LENGTH_SHORT).show();
            return;
        }
        if (!clickNamed(root, names, 0))
            Toast.makeText(this, "Original app did not expose that button", Toast.LENGTH_SHORT).show();
    }
    private AccessibilityNodeInfo targetRoot() {
        SparseArray<List<AccessibilityWindowInfo>> displays = getWindowsOnAllDisplays();
        List<AccessibilityWindowInfo> windows = displays.get(overlayDisplayId);
        if (windows == null) return null;
        for (AccessibilityWindowInfo win : windows) {
            if (win.getType() != AccessibilityWindowInfo.TYPE_APPLICATION) continue;
            AccessibilityNodeInfo root = win.getRoot();
            if (root != null && root.getPackageName() != null && currentVideoPackage.contentEquals(root.getPackageName())) return root;
        }
        return null;
    }
    private boolean clickNamed(AccessibilityNodeInfo root, String[] names, int depth) {
        if (root == null || depth > 35) return false;
        String description = root.getContentDescription() == null ? "" : root.getContentDescription().toString();
        String text = root.getText() == null ? "" : root.getText().toString();
        for (String name : names) {
            String needle = name.toLowerCase(Locale.ROOT);
            if (description.toLowerCase(Locale.ROOT).contains(needle) || text.equalsIgnoreCase(name)) {
                AccessibilityNodeInfo node = root;
                for (int i = 0; i < 4 && node != null; i++, node = node.getParent()) {
                    if (node.isClickable() && node.performAction(AccessibilityNodeInfo.ACTION_CLICK)) return true;
                }
            }
        }
        for (int i = 0; i < root.getChildCount(); i++) {
            if (clickNamed(root.getChild(i), names, depth + 1)) return true;
        }
        return false;
    }
    private void removeOverlay() {
        if (wm != null && overlay != null) {
            try { wm.removeViewImmediate(overlay); } catch (RuntimeException ignored) { }
        }
        overlay = null;
        wm = null;
        overlayContext = null;
        overlayDisplayId = Display.INVALID_DISPLAY;
    }
    @Override public void onInterrupt() { removeOverlay(); removeNavOverlay(); }
    @Override public void onDestroy() {
        removeOverlay();
        removeNavOverlay();
        if (live == this) live = null;
        super.onDestroy();
    }
}
