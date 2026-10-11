package com.flipminios.launcher;

import android.Manifest;
import android.app.Activity;
import android.app.ActivityOptions;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.drawable.Drawable;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraManager;
import android.net.ConnectivityManager;
import android.net.NetworkCapabilities;
import android.media.AudioManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.provider.Settings;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Display;
import android.view.DisplayCutout;
import android.view.WindowInsets;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.GridLayout;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** Compact, rootless launcher with an editable quick panel and app drawer. */
public final class LauncherActivity extends Activity {
    private static final String[] ALL_TILES = {"Internet", "Bluetooth", "Torch", "Rotation", "Sound", "Settings"};
    private final List<AppRepository.Entry> installed = new ArrayList<>();
    private final ExecutorService appQuery = Executors.newSingleThreadExecutor();
    private final Handler mainThread = new Handler(Looper.getMainLooper());
    private IconLoader iconLoader;
    private boolean querying = false;
    private long lastAppLoad = 0;
    private LinearLayout navBar;
    private LinearLayout outer;
    private FrameLayout content;
    private String page = "home";
    private boolean torchEnabled = false;
    private String torchCamera = null;

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        getWindow().setStatusBarColor(Ui.BG);
        getWindow().setNavigationBarColor(Ui.BG);
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
        if (getDisplay() != null && getDisplay().getDisplayId() != Display.DEFAULT_DISPLAY) {
            getWindow().getDecorView().setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_FULLSCREEN | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION |
                    View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
        }
        iconLoader = new IconLoader(this);
        page = getIntent().getStringExtra("page") == null ? "home" : getIntent().getStringExtra("page");
        buildShell();
        showPage(page);
        refreshAppsIfNeeded(true);
        applySavedRotation();
    }
    private void applySavedRotation() {
        int mode = Prefs.rotationMode(this);
        if (mode == 1) setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        else if (mode == 2) setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        // Leave the firmware's default orientation unchanged until the user selects Auto.
    }
    @Override protected void onResume() {
        super.onResume();
        refreshAppsIfNeeded(false);
    }
    private void refreshAppsIfNeeded(boolean first) {
        if (querying || (!first && SystemClock.elapsedRealtime() - lastAppLoad < 180000L)) return;
        querying = true;
        appQuery.execute(() -> {
            List<AppRepository.Entry> result = AppRepository.load(getApplicationContext());
            mainThread.post(() -> {
                if (isFinishing() || isDestroyed()) return;
                installed.clear();
                installed.addAll(result);
                lastAppLoad = SystemClock.elapsedRealtime();
                querying = false;
                showPage(page);
            });
        });
    }
    @Override protected void onDestroy() {
        appQuery.shutdownNow();
        if (iconLoader != null) iconLoader.shutdown();
        super.onDestroy();
    }
    @Override public void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        showPage(intent.getStringExtra("page") == null ? "home" : intent.getStringExtra("page"));
    }
    @Override public void onConfigurationChanged(Configuration change) {
        super.onConfigurationChanged(change);
        buildShell();
        showPage(page);
    }
    private void buildShell() {
        outer = Ui.vertical(this);
        outer.setBackgroundColor(Ui.BG);
        content = new FrameLayout(this);
        outer.addView(content, new LinearLayout.LayoutParams(-1, 0, 1));
        navBar = navigation();
        outer.addView(navBar, Ui.lp(this, -1, 49));
        setContentView(outer);
    }
    private LinearLayout navigation() {
        LinearLayout line = Ui.horizontal(this);
        line.setPadding(Ui.dp(this, 5), Ui.dp(this, 2), Ui.dp(this, 5), Ui.dp(this, 4));
        return line;
    }
    private void updateNavigation() {
        if (navBar == null) return;
        navBar.removeAllViews();
        String[][] tabs = {{"home", "⌂", "Home"}, {"apps", "▦", "Apps"},
                {"controls", "☷", "Panel"}, {"settings", "⚙", "Edit"}};
        for (String[] tab : tabs) {
            boolean selected = page.equals(tab[0]);
            TextView item = Ui.text(this, tab[1] + "\n" + tab[2], 12, selected,
                    selected ? Ui.TEXT : Ui.MUTED);
            item.setGravity(Gravity.CENTER);
            item.setBackground(selected ? Ui.glass(this, 15) : Ui.rounded(0xff151c2c, Ui.dp(this, 15)));
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(0, -1, 1);
            params.setMargins(Ui.dp(this, 3), 0, Ui.dp(this, 3), 0);
            navBar.addView(item, params);
            item.setOnClickListener(v -> showPage(tab[0]));
        }
    }
    private void showPage(String requested) {
        page = requested;
        if (content == null) return;
        content.removeAllViews();
        updateNavigation();
        switch (page) {
            case "apps": showApps(); break;
            case "controls": showControls(); break;
            case "settings": showSettings(); break;
            default: page = "home"; showHome();
        }
    }
    private ScrollView scrolling(LinearLayout target) {
        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setVerticalScrollBarEnabled(false);
        scroll.addView(target);
        content.addView(scroll, new FrameLayout.LayoutParams(-1, -1));
        return scroll;
    }
    private boolean coverDisplay() {
        return getDisplay() != null && getDisplay().getDisplayId() != Display.DEFAULT_DISPLAY;
    }
    /**
     * Only the compact top shelf avoids the physical camera corner.
     * v0.3 reserved up to 140dp and pushed the entire UI down.
     * Some Samsung firmware reports oversized cutout bounds, so cap the inferred portion.
     */
    private int safeCameraHeight() {
        int height = Prefs.cameraHeight(this);
        WindowInsets insets = getWindow().getDecorView().getRootWindowInsets();
        if (coverDisplay() && insets != null && insets.getDisplayCutout() != null) {
            for (android.graphics.Rect region : insets.getDisplayCutout().getBoundingRects()) {
                int bottomDp = Math.round(region.bottom / getResources().getDisplayMetrics().density);
                height = Math.max(height, Math.min(68, bottomDp + 5));
            }
        }
        return height;
    }
    /** A short camera-safe header; the rest of each page uses the full display width. */
    private void addCoverShelf(LinearLayout column) {
        if (!coverDisplay()) return;
        LinearLayout shelf = Ui.horizontal(this);
        shelf.setGravity(Gravity.CENTER_VERTICAL);
        View cameraSpace = new View(this);
        LinearLayout status = Ui.vertical(this);
        status.setGravity(Gravity.CENTER_VERTICAL);
        boolean camerasRight = Prefs.camerasOnRight(this);
        TextView eyebrow = Ui.text(this, "FLIP / MINI", 9, true, Ui.PRIMARY);
        TextView clock = Ui.text(this, new SimpleDateFormat("h:mm", Locale.getDefault()).format(new Date()), 20, true, Ui.TEXT);
        int gravity = camerasRight ? Gravity.LEFT : Gravity.RIGHT;
        eyebrow.setGravity(gravity);
        clock.setGravity(gravity);
        status.addView(eyebrow);
        status.addView(clock);
        LinearLayout.LayoutParams cameraParams = new LinearLayout.LayoutParams(0, -1, 49);
        LinearLayout.LayoutParams statusParams = new LinearLayout.LayoutParams(0, -1, 51);
        if (camerasRight) {
            shelf.addView(status, statusParams);
            shelf.addView(cameraSpace, cameraParams);
        } else {
            shelf.addView(cameraSpace, cameraParams);
            shelf.addView(status, statusParams);
        }
        column.addView(shelf, Ui.lp(this, -1, safeCameraHeight()));
    }
    private LinearLayout pageColumn() {
        LinearLayout column = Ui.vertical(this);
        column.setPadding(Ui.dp(this, 13), Ui.dp(this, 5), Ui.dp(this, 13), Ui.dp(this, 16));
        addCoverShelf(column);
        return column;
    }
    private TextView heading(String text) {
        TextView title = Ui.text(this, text, 19, true, Ui.TEXT);
        title.setPadding(Ui.dp(this, 3), Ui.dp(this, 3), Ui.dp(this, 3), Ui.dp(this, 7));
        return title;
    }
    private void showHome() {
        LinearLayout column = pageColumn();
        scrolling(column);
        if (!coverDisplay()) {
            TextView welcome = Ui.text(this,
                    new SimpleDateFormat("EEE  •  h:mm a", Locale.getDefault()).format(new Date()),
                    12, true, Ui.PRIMARY);
            column.addView(welcome);
        }
        column.addView(heading("Your mini home"));
        TextView hint = Ui.text(this, "Long-press an app to pin it here.", 12, false, Ui.MUTED);
        column.addView(hint);
        addSpace(column, 12);
        List<AppRepository.Entry> favorites = new ArrayList<>();
        for (AppRepository.Entry e : installed) if (Prefs.isFavorite(this, e.id())) favorites.add(e);
        if (favorites.isEmpty()) {
            String[] defaults = {"com.zhiliaoapp.musically", "com.instagram.android", "com.android.chrome",
                    "com.whatsapp", "com.google.android.youtube", "com.google.android.apps.messaging"};
            for (String pkg : defaults) {
                for (AppRepository.Entry e : installed) {
                    if (pkg.equals(e.component.getPackageName()) && !favorites.contains(e)) { favorites.add(e); break; }
                }
            }
        }
        if (favorites.isEmpty()) favorites.addAll(installed.subList(0, Math.min(installed.size(), 9)));
        makeAppGrid(column, favorites);
        addSpace(column, 16);
        column.addView(heading("Quick actions"));
        LinearLayout shortRow = Ui.horizontal(this);
        addWeighted(shortRow, Ui.makeTile(this, "Controls", "☷", () -> showPage("controls")));
        addWeighted(shortRow, Ui.makeTile(this, "All apps", "▦", () -> showPage("apps")));
        addWeighted(shortRow, Ui.makeTile(this, "Customize", "⚙", () -> showPage("settings")));
        column.addView(shortRow, Ui.lp(this, -1, 78));
    }
    private void showApps() {
        LinearLayout column = pageColumn();
        content.addView(column, new FrameLayout.LayoutParams(-1, -1));
        column.addView(heading("All your apps"));
        EditText search = new EditText(this);
        search.setSingleLine(true);
        search.setTextSize(14);
        search.setTextColor(Ui.TEXT);
        search.setHintTextColor(Ui.MUTED);
        search.setHint("Search apps");
        search.setPadding(Ui.dp(this, 14), Ui.dp(this, 7), Ui.dp(this, 14), Ui.dp(this, 7));
        search.setBackground(Ui.glass(this, 16));
        column.addView(search, Ui.lp(this, -1, 40));
        addSpace(column, 10);
        AppGridAdapter adapter = new AppGridAdapter(this, installed, iconLoader);
        GridView grid = new GridView(this);
        grid.setNumColumns(Prefs.columns(this));
        grid.setVerticalSpacing(Ui.dp(this, 3));
        grid.setHorizontalSpacing(Ui.dp(this, 3));
        grid.setStretchMode(GridView.STRETCH_COLUMN_WIDTH);
        grid.setClipToPadding(false);
        grid.setPadding(0, 0, 0, Ui.dp(this, 16));
        grid.setSelector(android.R.color.transparent);
        grid.setAdapter(adapter);
        grid.setOnItemClickListener((parent, view, position, id) -> launch(adapter.item(position)));
        grid.setOnItemLongClickListener((parent, view, position, id) -> {
            AppRepository.Entry entry = adapter.item(position);
            Prefs.toggleFavorite(this, entry.id());
            toast(Prefs.isFavorite(this, entry.id()) ? "Pinned to home" : "Removed from home");
            return true;
        });
        column.addView(grid, new LinearLayout.LayoutParams(-1, 0, 1));
        if (installed.isEmpty()) {
            TextView wait = Ui.text(this, "Loading your apps…", 11, false, Ui.MUTED);
            column.addView(wait);
        }
        search.addTextChangedListener(new TextWatcher() {
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                adapter.filter(s.toString());
            }
            public void afterTextChanged(Editable e) {}
        });
    }
    private void makeAppGrid(LinearLayout into, List<AppRepository.Entry> apps) {
        int cols = Prefs.columns(this);
        GridLayout grid = new GridLayout(this);
        grid.setColumnCount(cols);
        int width = getResources().getDisplayMetrics().widthPixels;
        int itemWidth = Math.max(Ui.dp(this, 65), (width - Ui.dp(this, 32)) / cols);
        for (AppRepository.Entry app : apps) {
            LinearLayout item = Ui.vertical(this);
            item.setGravity(Gravity.CENTER);
            item.setPadding(Ui.dp(this, 3), Ui.dp(this, 10), Ui.dp(this, 3), Ui.dp(this, 6));
            ImageView image = new ImageView(this);
            iconLoader.bind(image, app);
            LinearLayout.LayoutParams img = Ui.lp(this, 40, 40);
            img.gravity = Gravity.CENTER;
            item.addView(image, img);
            TextView label = Ui.text(this, app.name, 11, false, Ui.TEXT);
            label.setGravity(Gravity.CENTER);
            label.setMaxLines(1);
            label.setEllipsize(android.text.TextUtils.TruncateAt.END);
            LinearLayout.LayoutParams lp = Ui.lp(this, -1, 27);
            lp.setMargins(Ui.dp(this, 2), Ui.dp(this, 4), Ui.dp(this, 2), 0);
            item.addView(label, lp);
            GridLayout.LayoutParams g = new GridLayout.LayoutParams();
            g.width = itemWidth;
            g.height = Ui.dp(this, 94);
            g.setMargins(0, 0, 0, Ui.dp(this, 2));
            grid.addView(item, g);
            item.setOnClickListener(v -> launch(app));
            item.setOnLongClickListener(v -> {
                Prefs.toggleFavorite(this, app.id());
                Toast.makeText(this, Prefs.isFavorite(this, app.id()) ? "Pinned to Home" : "Unpinned", Toast.LENGTH_SHORT).show();
                return true;
            });
        }
        into.addView(grid);
    }
    private void launch(AppRepository.Entry app) {
        Intent intent = Intent.makeMainActivity(app.component);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
        try {
            int displayId = getDisplay() == null ? Display.DEFAULT_DISPLAY : getDisplay().getDisplayId();
            ActivityOptions options = ActivityOptions.makeBasic();
            options.setLaunchDisplayId(displayId);
            startActivity(intent, options.toBundle());
        } catch (RuntimeException e) {
            // Failing closed prevents an inaccessible activity opening on the broken inner display.
            Toast.makeText(this, "Cover launch blocked. Enable this app in Good Lock → MultiStar.", Toast.LENGTH_LONG).show();
        }
    }
    private void showControls() {
        LinearLayout column = pageColumn();
        scrolling(column);
        column.addView(heading("Control center"));
        List<String> tiles = Prefs.tiles(this);
        if (tiles.isEmpty()) column.addView(Ui.text(this, "No quick tiles selected. Add them in Edit.", 13, false, Ui.MUTED));
        // Three compact columns leave room for the sliders on the cover screen.
        for (int i = 0; i < tiles.size(); i += 3) {
            LinearLayout row = Ui.horizontal(this);
            for (int j = 0; j < 3; j++) {
                if (i + j < tiles.size()) addWeighted(row, quickTile(tiles.get(i + j)));
                else row.addView(new View(this), new LinearLayout.LayoutParams(0, -1, 1));
            }
            LinearLayout.LayoutParams p = Ui.lp(this, -1, 60);
            p.bottomMargin = Ui.dp(this, 6);
            column.addView(row, p);
        }
        addSpace(column, 12);
        column.addView(Ui.text(this, "Brightness (this app)", 12, false, Ui.MUTED));
        SeekBar brightness = new SeekBar(this);
        brightness.setMax(100);
        float current = getWindow().getAttributes().screenBrightness;
        brightness.setProgress(current < 0 ? 70 : Math.round(current * 100));
        brightness.setOnSeekBarChangeListener(simpleSlider(value -> {
            WindowManager.LayoutParams params = getWindow().getAttributes();
            params.screenBrightness = Math.max(0.02f, value / 100f);
            getWindow().setAttributes(params);
        }));
        column.addView(brightness);
        column.addView(Ui.text(this, "Media volume", 12, false, Ui.MUTED));
        AudioManager audio = (AudioManager) getSystemService(AUDIO_SERVICE);
        SeekBar volume = new SeekBar(this);
        volume.setMax(audio.getStreamMaxVolume(AudioManager.STREAM_MUSIC));
        volume.setProgress(audio.getStreamVolume(AudioManager.STREAM_MUSIC));
        volume.setOnSeekBarChangeListener(simpleSlider(value -> audio.setStreamVolume(AudioManager.STREAM_MUSIC, value, 0)));
        column.addView(volume);
        addSpace(column, 12);
        TextView note = Ui.text(this,
                "Brightness affects Flip Mini OS only. Rotate controls only this launcher; Samsung may restrict rotation on its cover screen.",
                11, false, Ui.MUTED);
        column.addView(note);
    }
    private interface SliderChange { void changed(int progress); }
    private SeekBar.OnSeekBarChangeListener simpleSlider(SliderChange cb) {
        return new SeekBar.OnSeekBarChangeListener() {
            public void onProgressChanged(SeekBar bar, int value, boolean fromUser) { if (fromUser) cb.changed(value); }
            public void onStartTrackingTouch(SeekBar bar) {}
            public void onStopTrackingTouch(SeekBar bar) {}
        };
    }
    private LinearLayout quickTile(String name) {
        switch (name) {
            case "Internet": return Ui.makeTile(this, "Internet", "◉", this::showInternetOptions);
            case "Bluetooth": return Ui.makeTile(this, "Bluetooth", "ᛒ", () -> safeSettings(new Intent(Settings.ACTION_BLUETOOTH_SETTINGS)));
            case "Torch": return Ui.makeTile(this, "Torch", "ϟ", this::toggleTorch);
            case "Rotation": return Ui.makeTile(this, "Rotate", "⟳", this::showRotationOptions);
            case "Sound": return Ui.makeTile(this, "Sound", "♫", () -> safeSettings(new Intent(Settings.ACTION_SOUND_SETTINGS)));
            default: return Ui.makeTile(this, "Settings", "⚙", () -> safeSettings(new Intent(Settings.ACTION_SETTINGS)));
        }
    }
    private void safeSettings(Intent intent) {
        try {
            // Starting from the active cover activity avoids intentionally targeting the damaged panel.
            ActivityOptions options = ActivityOptions.makeBasic();
            options.setLaunchDisplayId(getDisplay() == null ? Display.DEFAULT_DISPLAY : getDisplay().getDisplayId());
            startActivity(intent, options.toBundle());
        } catch (RuntimeException ex) {
            Toast.makeText(this, "Samsung blocked this settings panel on the cover screen", Toast.LENGTH_LONG).show();
        }
    }
    private void toggleTorch() {
        if (checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.CAMERA}, 403);
            return;
        }
        try {
            CameraManager manager = (CameraManager) getSystemService(CAMERA_SERVICE);
            if (torchCamera == null) {
                for (String id : manager.getCameraIdList()) {
                    CameraCharacteristics c = manager.getCameraCharacteristics(id);
                    Boolean flash = c.get(CameraCharacteristics.FLASH_INFO_AVAILABLE);
                    Integer facing = c.get(CameraCharacteristics.LENS_FACING);
                    if (Boolean.TRUE.equals(flash) && facing != null && facing == CameraCharacteristics.LENS_FACING_BACK) {
                        torchCamera = id; break;
                    }
                }
            }
            if (torchCamera == null) { toast("No usable rear torch found"); return; }
            manager.setTorchMode(torchCamera, !torchEnabled);
            torchEnabled = !torchEnabled;
            toast(torchEnabled ? "Torch on" : "Torch off");
        } catch (Exception ex) { toast("Torch is unavailable while camera is busy"); }
    }
    @Override public void onRequestPermissionsResult(int code, String[] permissions, int[] grants) {
        super.onRequestPermissionsResult(code, permissions, grants);
        if (code == 403 && grants.length > 0 && grants[0] == PackageManager.PERMISSION_GRANTED) toggleTorch();
    }
    /** Always shows a response on the secondary display even if Samsung blocks Settings. */
    private void showInternetOptions() {
        String status = "Connection status unavailable";
        try {
            ConnectivityManager manager = (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);
            NetworkCapabilities caps = manager.getNetworkCapabilities(manager.getActiveNetwork());
            if (caps == null) status = "No active network";
            else if (caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) status = "Connected over Wi-Fi";
            else if (caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)) status = "Connected over mobile data";
            else status = "Network is connected";
        } catch (RuntimeException ignored) {}
        new AlertDialog.Builder(this)
                .setTitle("Internet")
                .setMessage(status + "\n\nAndroid requires a system settings screen to change Wi-Fi or mobile data. On the Flip6 cover, Samsung may block that screen.")
                .setPositiveButton("Wi-Fi settings", (dialog, which) -> safeSettings(new Intent(Settings.ACTION_WIFI_SETTINGS)))
                .setNeutralButton("Connections", (dialog, which) -> safeSettings(new Intent(Settings.ACTION_WIRELESS_SETTINGS)))
                .setNegativeButton("Close", null)
                .show();
    }
    /**
     * Change the launcher activity's orientation instead of silently changing
     * the global auto-rotate flag. Samsung may still lock secondary displays.
     */
    private void showRotationOptions() {
        String[] modes = {"Auto (Flip Mini OS)", "Portrait (Flip Mini OS)", "Landscape (Flip Mini OS)"};
        new AlertDialog.Builder(this)
                .setTitle("Rotation")
                .setSingleChoiceItems(modes, Prefs.rotationMode(this), (dialog, selected) -> {
                    Prefs.setRotationMode(this, selected);
                    int orientation = selected == 0 ? ActivityInfo.SCREEN_ORIENTATION_FULL_SENSOR :
                            selected == 1 ? ActivityInfo.SCREEN_ORIENTATION_PORTRAIT :
                            ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE;
                    try {
                        setRequestedOrientation(orientation);
                        toast("Requested " + modes[selected] + ". Samsung may lock cover rotation.");
                    } catch (RuntimeException ex) {
                        toast("Samsung blocked cover rotation");
                    }
                    dialog.dismiss();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }
    private void showSettings() {
        LinearLayout column = pageColumn();
        scrolling(column);
        column.addView(heading("Personalize"));
        column.addView(Ui.text(this, "App columns", 13, false, Ui.MUTED));
        LinearLayout columns = Ui.horizontal(this);
        for (int value : new int[]{2, 3, 4}) {
            int v = value;
            TextView btn = Ui.pill(this, value + "", () -> { Prefs.setColumns(this, v); showPage("settings"); });
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, Ui.dp(this, 40), 1);
            lp.setMargins(Ui.dp(this, 4), 0, Ui.dp(this, 4), 0);
            columns.addView(btn, lp);
        }
        column.addView(columns);
        addSpace(column, 16);
        column.addView(heading("Control panel tiles"));
        column.addView(Ui.text(this, "Add, remove, and reorder your control shortcuts.", 12, false, Ui.MUTED));
        List<String> selected = Prefs.tiles(this);
        for (String tile : new ArrayList<>(selected)) {
            LinearLayout row = Ui.horizontal(this);
            row.addView(Ui.text(this, tile, 14, true, Ui.TEXT), new LinearLayout.LayoutParams(0, Ui.dp(this, 41), 1));
            row.addView(Ui.pill(this, "↑", () -> moveTile(tile, -1)), Ui.lp(this, 42, 40));
            row.addView(Ui.pill(this, "↓", () -> moveTile(tile, 1)), Ui.lp(this, 42, 40));
            row.addView(Ui.pill(this, "×", () -> { List<String> next = Prefs.tiles(this); next.remove(tile); Prefs.saveTiles(this, next); showPage("settings"); }), Ui.lp(this, 42, 40));
            column.addView(row);
            addSpace(column, 4);
        }
        for (String tile : ALL_TILES) {
            if (!selected.contains(tile)) {
                TextView add = Ui.pill(this, "+ Add " + tile, () -> { List<String> next = Prefs.tiles(this); next.add(tile); Prefs.saveTiles(this, next); showPage("settings"); });
                LinearLayout.LayoutParams p = Ui.lp(this, -1, 40);
                p.bottomMargin = Ui.dp(this, 4);
                column.addView(add, p);
            }
        }
        addSpace(column, 15);
        column.addView(heading("Cover camera clearance"));
        column.addView(Ui.text(this,
                "Only the top header avoids the camera corner. Adjust it to keep content visible without wasting the screen.",
                12, false, Ui.MUTED));
        addSpace(column, 6);
        LinearLayout sides = Ui.horizontal(this);
        addWeighted(sides, Ui.pill(this, "Cameras left", () -> {
            Prefs.setCamerasOnRight(this, false); showPage("settings");
        }));
        addWeighted(sides, Ui.pill(this, "Cameras right", () -> {
            Prefs.setCamerasOnRight(this, true); showPage("settings");
        }));
        column.addView(sides, Ui.lp(this, -1, 44));
        addSpace(column, 8);
        TextView clearance = Ui.text(this, "Clearance: " + Prefs.cameraHeight(this) + " dp", 12, true, Ui.TEXT);
        column.addView(clearance);
        SeekBar height = new SeekBar(this);
        height.setMax(52);
        height.setProgress(Prefs.cameraHeight(this) - 32);
        height.setOnSeekBarChangeListener(simpleSlider(value -> {
            Prefs.setCameraHeight(this, value + 32);
            clearance.setText("Clearance: " + Prefs.cameraHeight(this) + " dp (switch tabs to apply)");
        }));
        column.addView(height);
        addSpace(column, 10);
        column.addView(Ui.pill(this, "Refresh app list", () -> {
            lastAppLoad = 0;
            refreshAppsIfNeeded(true);
            toast("Refreshing in background");
        }), Ui.lp(this, -1, 45));
        addSpace(column, 15);
        column.addView(heading("App compatibility"));
        column.addView(Ui.text(this,
                "TikTok and Instagram use their own layout. Android does not let a regular launcher relocate their buttons or force a 9:16 window. Keep the real apps through MultiStar; resizing remains experimental.",
                12, false, Ui.MUTED));
        addSpace(column, 10);
        column.addView(Ui.text(this, "V0.3 • Faster grid • Cover safe zone • Glass widget styling", 11, false, Ui.PRIMARY));
    }
    private void moveTile(String tile, int change) {
        List<String> next = Prefs.tiles(this);
        int index = next.indexOf(tile), dest = index + change;
        if (index < 0 || dest < 0 || dest >= next.size()) return;
        next.remove(index); next.add(dest, tile);
        Prefs.saveTiles(this, next);
        showPage("settings");
    }
    private void addWeighted(LinearLayout row, View view) {
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(0, -1, 1);
        p.setMargins(Ui.dp(this, 3), 0, Ui.dp(this, 3), 0);
        row.addView(view, p);
    }
    private void addSpace(LinearLayout into, int heightDp) { into.addView(new View(this), Ui.lp(this, 1, heightDp)); }
    private void toast(String msg) { Toast.makeText(this, msg, Toast.LENGTH_SHORT).show(); }
}
