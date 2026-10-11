package com.flipminios.launcher;

import android.Manifest;
import android.accessibilityservice.AccessibilityService;
import android.app.Activity;
import android.app.ActivityOptions;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.drawable.Drawable;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraManager;
import android.media.AudioManager;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Display;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.GridLayout;
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

/** Compact, rootless launcher with an editable quick panel and app drawer. */
public final class LauncherActivity extends Activity {
    private static final String[] ALL_TILES = {"Internet", "Bluetooth", "Torch", "Rotation", "Sound", "Settings"};
    private final List<AppRepository.Entry> installed = new ArrayList<>();
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
        installed.addAll(AppRepository.load(this));
        page = getIntent().getStringExtra("page") == null ? "home" : getIntent().getStringExtra("page");
        buildShell();
        showPage(page);
    }
    @Override protected void onResume() {
        super.onResume();
        if (content != null) {
            installed.clear();
            installed.addAll(AppRepository.load(this));
            showPage(page);
        }
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
        outer.addView(navigation(), Ui.lp(this, -1, 56));
        setContentView(outer);
    }
    private LinearLayout navigation() {
        LinearLayout line = Ui.horizontal(this);
        line.setPadding(Ui.dp(this, 6), Ui.dp(this, 4), Ui.dp(this, 6), Ui.dp(this, 6));
        String[][] tabs = {{"home", "⌂", "Home"}, {"apps", "▦", "Apps"},
                {"controls", "☷", "Panel"}, {"settings", "⚙", "Edit"}};
        for (String[] tab : tabs) {
            TextView item = Ui.text(this, tab[1] + "\n" + tab[2], 12, true, Ui.TEXT);
            item.setGravity(Gravity.CENTER);
            item.setBackground(Ui.rounded(Ui.CARD, Ui.dp(this, 11)));
            LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(0, -1, 1);
            p.setMargins(Ui.dp(this, 3), 0, Ui.dp(this, 3), 0);
            line.addView(item, p);
            item.setOnClickListener(v -> showPage(tab[0]));
        }
        return line;
    }
    private void showPage(String requested) {
        page = requested;
        if (content == null) return;
        content.removeAllViews();
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
    private LinearLayout pageColumn() {
        LinearLayout column = Ui.vertical(this);
        column.setPadding(Ui.dp(this, 12), Ui.dp(this, 8), Ui.dp(this, 12), Ui.dp(this, 14));
        return column;
    }
    private TextView heading(String text) {
        TextView title = Ui.text(this, text, 19, true, Ui.TEXT);
        title.setPadding(Ui.dp(this, 3), Ui.dp(this, 8), Ui.dp(this, 3), Ui.dp(this, 12));
        return title;
    }
    private void showHome() {
        LinearLayout column = pageColumn();
        scrolling(column);
        LinearLayout header = Ui.horizontal(this);
        // Keep the camera cut-outs clear: status text is deliberately placed on the right.
        TextView cameraSpace = Ui.text(this, "", 10, false, Ui.TEXT);
        header.addView(cameraSpace, new LinearLayout.LayoutParams(0, Ui.dp(this, 83), 1));
        LinearLayout summary = Ui.vertical(this);
        summary.setGravity(Gravity.CENTER_VERTICAL | Gravity.RIGHT);
        String time = new SimpleDateFormat("h:mm", Locale.getDefault()).format(new Date());
        String date = new SimpleDateFormat("EEE, MMM d", Locale.getDefault()).format(new Date());
        TextView clock = Ui.text(this, time, 32, true, Ui.TEXT);
        clock.setGravity(Gravity.RIGHT);
        summary.addView(clock);
        TextView dateLabel = Ui.text(this, date, 12, false, Ui.MUTED);
        dateLabel.setGravity(Gravity.RIGHT);
        summary.addView(dateLabel);
        header.addView(summary, new LinearLayout.LayoutParams(0, -1, 1));
        column.addView(header);
        column.addView(heading("Flip Mini OS"));
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
        addWeighted(shortRow, Ui.makeTile(this, "Back", "‹", () -> global(AccessibilityService.GLOBAL_ACTION_BACK)));
        column.addView(shortRow, Ui.lp(this, -1, 78));
    }
    private void showApps() {
        LinearLayout column = pageColumn();
        content.addView(column, new FrameLayout.LayoutParams(-1, -1));
        column.addView(heading("All apps"));
        EditText search = new EditText(this);
        search.setSingleLine(true);
        search.setTextSize(14);
        search.setTextColor(Ui.TEXT);
        search.setHintTextColor(Ui.MUTED);
        search.setHint("Search installed apps");
        search.setPadding(Ui.dp(this, 13), Ui.dp(this, 7), Ui.dp(this, 13), Ui.dp(this, 7));
        search.setBackground(Ui.rounded(Ui.CARD, Ui.dp(this, 15)));
        column.addView(search, Ui.lp(this, -1, 45));
        addSpace(column, 10);
        FrameLayout listRegion = new FrameLayout(this);
        column.addView(listRegion, new LinearLayout.LayoutParams(-1, 0, 1));
        Runnable update = () -> {
            listRegion.removeAllViews();
            ScrollView scroll = new ScrollView(this);
            LinearLayout list = Ui.vertical(this);
            List<AppRepository.Entry> filtered = new ArrayList<>();
            String needle = search.getText().toString().toLowerCase(Locale.ROOT);
            for (AppRepository.Entry e : installed) if (e.name.toLowerCase(Locale.ROOT).contains(needle)) filtered.add(e);
            makeAppGrid(list, filtered);
            scroll.addView(list);
            listRegion.addView(scroll);
        };
        search.addTextChangedListener(new TextWatcher() {
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            public void onTextChanged(CharSequence s, int start, int before, int count) { update.run(); }
            public void afterTextChanged(Editable e) {}
        });
        update.run();
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
            image.setImageDrawable(app.icon);
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
        for (int i = 0; i < tiles.size(); i += 2) {
            LinearLayout row = Ui.horizontal(this);
            addWeighted(row, quickTile(tiles.get(i)));
            if (i + 1 < tiles.size()) addWeighted(row, quickTile(tiles.get(i + 1)));
            else row.addView(new View(this), new LinearLayout.LayoutParams(0, -1, 1));
            LinearLayout.LayoutParams p = Ui.lp(this, -1, 79);
            p.bottomMargin = Ui.dp(this, 8);
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
                "Brightness affects Flip Mini OS only. Rotation changes Android's system-wide auto-rotate setting if you grant permission.",
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
            case "Internet": return Ui.makeTile(this, "Internet", "◉", () -> safeSettings(new Intent(Settings.Panel.ACTION_INTERNET_CONNECTIVITY)));
            case "Bluetooth": return Ui.makeTile(this, "Bluetooth", "ᛒ", () -> safeSettings(new Intent(Settings.ACTION_BLUETOOTH_SETTINGS)));
            case "Torch": return Ui.makeTile(this, "Torch", "ϟ", this::toggleTorch);
            case "Rotation": return Ui.makeTile(this, "Rotate", "⟳", this::toggleRotation);
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
    private void toggleRotation() {
        if (!Settings.System.canWrite(this)) {
            Intent intent = new Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS, Uri.parse("package:" + getPackageName()));
            safeSettings(intent);
            toast("Allow Modify system settings, then tap Rotate again");
            return;
        }
        try {
            boolean enabled = Settings.System.getInt(getContentResolver(), Settings.System.ACCELEROMETER_ROTATION, 0) != 0;
            Settings.System.putInt(getContentResolver(), Settings.System.ACCELEROMETER_ROTATION, enabled ? 0 : 1);
            toast(enabled ? "System auto-rotate off" : "System auto-rotate on");
        } catch (SecurityException ex) { toast("Rotation change blocked by system"); }
    }
    private void global(int action) {
        if (!CoverAccessibilityService.action(action)) {
            toast("Enable optional Accessibility navigation in Edit first");
        }
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
        addSpace(column, 14);
        column.addView(heading("Navigation and Reels"));
        TextView access = Ui.pill(this, "Enable Accessibility navigation", () -> safeSettings(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)));
        column.addView(access, Ui.lp(this, -1, 45));
        addSpace(column, 7);
        Switch navDock = new Switch(this);
        navDock.setText("Floating Back / Home / Recents on the cover");
        navDock.setTextSize(12);
        navDock.setTextColor(Ui.TEXT);
        navDock.setChecked(Prefs.navigationDockEnabled(this));
        navDock.setOnCheckedChangeListener((button, enabled) -> {
            Prefs.setNavigationDock(this, enabled);
            toast("Accessibility service needed. The dock appears in other cover-screen apps.");
        });
        column.addView(navDock);
        addSpace(column, 7);
        Switch video = new Switch(this);
        video.setText("TikTok / Instagram mini toolbar");
        video.setTextSize(12);
        video.setTextColor(Ui.TEXT);
        video.setChecked(Prefs.overlayEnabled(this));
        video.setOnCheckedChangeListener((button, enabled) -> {
            Prefs.setOverlay(this, enabled);
            if (enabled) toast("Enable Accessibility service and relaunch TikTok on the cover");
        });
        column.addView(video);
        addSpace(column, 7);
        LinearLayout sideRow = Ui.horizontal(this);
        TextView leftSide = Ui.pill(this, "Toolbar left", () -> { Prefs.setOverlayOnRight(this, false); toast("Left side selected. Relaunch video app."); });
        TextView rightSide = Ui.pill(this, "Toolbar right", () -> { Prefs.setOverlayOnRight(this, true); toast("Right side selected. Relaunch video app."); });
        addWeighted(sideRow, leftSide);
        addWeighted(sideRow, rightSide);
        column.addView(sideRow, Ui.lp(this, -1, 43));
        addSpace(column, 7);
        column.addView(Ui.text(this,
                "Optional: a camera-side overlay appears only when TikTok/Instagram runs on the secondary display. Swipe works through Android gestures; Like, Comment, Share need accessible app controls. Touch controls may be blocked by One UI or app updates.",
                12, false, Ui.MUTED));
        addSpace(column, 12);
        column.addView(Ui.pill(this, "Close video toolbar", () -> {
            Prefs.setOverlay(this, false);
            toast("Toolbar disabled. Reopen app to dismiss it.");
            showPage("settings");
        }), Ui.lp(this, -1, 43));
        addSpace(column, 14);
        column.addView(heading("Navigation"));
        LinearLayout nav = Ui.horizontal(this);
        addWeighted(nav, Ui.makeTile(this, "Back", "‹", () -> global(AccessibilityService.GLOBAL_ACTION_BACK)));
        addWeighted(nav, Ui.makeTile(this, "Home", "⌂", () -> global(AccessibilityService.GLOBAL_ACTION_HOME)));
        addWeighted(nav, Ui.makeTile(this, "Recents", "▣", () -> global(AccessibilityService.GLOBAL_ACTION_RECENTS)));
        column.addView(nav, Ui.lp(this, -1, 70));
        addSpace(column, 15);
        column.addView(Ui.text(this, "For video apps: original accounts and apps stay intact. 9:16 forcing is not available without Samsung window-management support.", 11, false, Ui.MUTED));
        addSpace(column, 8);
        column.addView(Ui.text(this, "Root-free • No analytics • No network permission • Source available", 11, false, Ui.MUTED));
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
