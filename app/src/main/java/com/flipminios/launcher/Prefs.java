package com.flipminios.launcher;

import android.content.Context;
import android.content.SharedPreferences;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

final class Prefs {
    static final String QUICK_DEFAULT = "Internet,Bluetooth,Torch,Rotation,Sound,Settings";
    private static SharedPreferences store(Context c) { return c.getSharedPreferences("flip_mini", Context.MODE_PRIVATE); }
    static boolean overlayEnabled(Context c) { return store(c).getBoolean("overlay", false); }
    static void setOverlay(Context c, boolean yes) { store(c).edit().putBoolean("overlay", yes).apply(); }
    static boolean overlayOnRight(Context c) { return store(c).getBoolean("overlayOnRight", false); }
    static void setOverlayOnRight(Context c, boolean yes) { store(c).edit().putBoolean("overlayOnRight", yes).apply(); }
    static boolean navigationDockEnabled(Context c) { return store(c).getBoolean("navigation_dock", false); }
    static void setNavigationDock(Context c, boolean enabled) { store(c).edit().putBoolean("navigation_dock", enabled).apply(); }
    static int columns(Context c) { return store(c).getInt("columns", 3); }
    static void setColumns(Context c, int n) { store(c).edit().putInt("columns", Math.max(2, Math.min(4, n))).apply(); }
    static List<String> tiles(Context c) {
        String raw = store(c).getString("tiles", QUICK_DEFAULT);
        if (raw == null || raw.isEmpty()) return new ArrayList<>();
        return new ArrayList<>(Arrays.asList(raw.split(",")));
    }
    static void saveTiles(Context c, List<String> entries) { store(c).edit().putString("tiles", String.join(",", entries)).apply(); }
    static boolean isFavorite(Context c, String component) {
        return store(c).getStringSet("favorites", java.util.Collections.emptySet()).contains(component);
    }
    static void toggleFavorite(Context c, String component) {
        java.util.Set<String> current = new java.util.HashSet<>(store(c).getStringSet("favorites", java.util.Collections.emptySet()));
        if (!current.remove(component)) current.add(component);
        store(c).edit().putStringSet("favorites", current).apply();
    }
}
