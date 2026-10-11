package com.flipminios.launcher;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.drawable.Drawable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/** Discovers launchable installed applications through Android's package-visibility rules. */
final class AppRepository {
    static final class Entry {
        final String name;
        final ComponentName component;
        final Drawable icon;
        Entry(String name, ComponentName component, Drawable icon) {
            this.name = name;
            this.component = component;
            this.icon = icon;
        }
        String id() { return component.flattenToString(); }
    }
    static List<Entry> load(Context context) {
        PackageManager pm = context.getPackageManager();
        Intent query = new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER);
        List<ResolveInfo> infos = pm.queryIntentActivities(query, 0);
        List<Entry> entries = new ArrayList<>();
        java.util.HashSet<String> seen = new java.util.HashSet<>();
        for (ResolveInfo info : infos) {
            if (info.activityInfo == null || info.activityInfo.packageName.equals(context.getPackageName())) continue;
            ComponentName cn = new ComponentName(info.activityInfo.packageName, info.activityInfo.name);
            if (!seen.add(cn.flattenToString())) continue;
            try { entries.add(new Entry(String.valueOf(info.loadLabel(pm)), cn, info.loadIcon(pm))); }
            catch (RuntimeException ignored) { /* unavailable apps are skipped */ }
        }
        entries.sort((a, b) -> a.name.toLowerCase(Locale.ROOT).compareTo(b.name.toLowerCase(Locale.ROOT)));
        return entries;
    }
}
