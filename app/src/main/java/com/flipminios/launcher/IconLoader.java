package com.flipminios.launcher;

import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.os.Looper;
import android.util.LruCache;
import android.widget.ImageView;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** Never decode installed application icons during app-grid layout. */
final class IconLoader {
    private final PackageManager pm;
    private final ExecutorService executor = Executors.newFixedThreadPool(2);
    private final Handler main = new Handler(Looper.getMainLooper());
    private final LruCache<String, Drawable.ConstantState> cache = new LruCache<>(96);

    IconLoader(Context context) { pm = context.getApplicationContext().getPackageManager(); }
    void bind(ImageView image, AppRepository.Entry entry) {
        String key = entry.id();
        image.setTag(key);
        Drawable.ConstantState state = cache.get(key);
        if (state != null) {
            image.setImageDrawable(state.newDrawable(image.getResources()));
            return;
        }
        image.setImageDrawable(pm.getDefaultActivityIcon());
        executor.execute(() -> {
            Drawable icon;
            try { icon = pm.getActivityIcon(entry.component); }
            catch (Exception ex) { icon = pm.getDefaultActivityIcon(); }
            final Drawable.ConstantState loadedState = icon.getConstantState();
            main.post(() -> {
                if (loadedState != null) cache.put(key, loadedState);
                if (key.equals(image.getTag())) {
                    image.setImageDrawable(loadedState == null ? pm.getDefaultActivityIcon() : loadedState.newDrawable(image.getResources()));
                }
            });
        });
    }
    void shutdown() { executor.shutdownNow(); }
}
