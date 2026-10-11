package com.flipminios.launcher;

import android.app.Activity;
import android.content.ComponentName;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.os.IBinder;
import android.os.Parcel;
import android.os.Looper;
import android.os.Handler;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;
import rikka.shizuku.Shizuku;

/** Opt-in, no-root Shizuku bridge for guarded cover-display diagnostics. */
final class CoverShizukuControl {
    private static final int REQUEST = 8501;
    private final Activity activity;
    private final Handler main = new Handler(Looper.getMainLooper());
    private final ExecutorService background = Executors.newSingleThreadExecutor();
    private final Shizuku.UserServiceArgs args;
    private IBinder remote;
    private boolean binding;
    private int pendingCode;
    private Consumer<String> pending;
    private boolean closed;
    private boolean listenerRegistered;

    private final ServiceConnection connection = new ServiceConnection() {
        @Override public void onServiceConnected(ComponentName name, IBinder service) {
            binding = false;
            remote = service;
            flush();
        }
        @Override public void onServiceDisconnected(ComponentName name) {
            binding = false;
            remote = null;
            if (pending != null) deliver("Shizuku service disconnected");
        }
    };

    private final Shizuku.OnRequestPermissionResultListener permissionListener =
            (requestCode, result) -> {
                if (requestCode != REQUEST) return;
                if (result == PackageManager.PERMISSION_GRANTED) {
                    main.post(this::bind);
                } else {
                    main.post(() -> deliver("Shizuku permission was not granted"));
                }
            };

    CoverShizukuControl(Activity activity) {
        this.activity = activity;
        args = new Shizuku.UserServiceArgs(
                new ComponentName(activity.getPackageName(), CoverShellService.class.getName()))
                .processNameSuffix("cover_shell").daemon(true).version(1);
        // The normal launcher must work even when the Shizuku app is absent.
        try {
            Shizuku.addRequestPermissionResultListener(permissionListener);
            listenerRegistered = true;
        } catch (Throwable ignored) {
            listenerRegistered = false;
        }
    }

    void execute(int code, Consumer<String> result) {
        if (closed) return;
        if (pending != null) {
            result.accept("Wait for the previous Shizuku operation to finish.");
            return;
        }
        pendingCode = code;
        pending = result;
        try {
            if (!Shizuku.pingBinder()) {
                deliver("Shizuku is not running. Start it using Wireless debugging.");
            } else if (Shizuku.checkSelfPermission() != PackageManager.PERMISSION_GRANTED) {
                Shizuku.requestPermission(REQUEST);
            } else {
                bind();
            }
        } catch (RuntimeException ex) {
            deliver("Shizuku is unavailable: " + ex.getClass().getSimpleName());
        }
    }

    private void bind() {
        if (closed) return;
        if (remote != null && remote.isBinderAlive()) {
            flush();
        } else if (!binding) {
            binding = true;
            try {
                Shizuku.bindUserService(args, connection);
            } catch (RuntimeException ex) {
                binding = false;
                deliver("Cannot bind Shizuku: " + ex.getClass().getSimpleName());
            }
        }
    }

    private void flush() {
        if (pending == null || remote == null || !remote.isBinderAlive() || closed) return;
        final int code = pendingCode;
        final Consumer<String> callback = pending;
        pending = null;
        background.execute(() -> {
            String answer;
            Parcel data = Parcel.obtain();
            Parcel reply = Parcel.obtain();
            try {
                if (!remote.transact(code, data, reply, 0))
                    answer = "Shizuku UserService rejected the request";
                else {
                    reply.readException();
                    answer = reply.readString();
                }
            } catch (Exception ex) {
                answer = "Shizuku call failed: " + ex.getClass().getSimpleName();
            } finally {
                data.recycle();
                reply.recycle();
            }
            final String result = answer;
            main.post(() -> { if (!closed) callback.accept(result); });
        });
    }

    private void deliver(String result) {
        Consumer<String> callback = pending;
        pending = null;
        if (callback != null && !closed) callback.accept(result);
    }

    void close() {
        closed = true;
        if (listenerRegistered) {
            try { Shizuku.removeRequestPermissionResultListener(permissionListener); }
            catch (Throwable ignored) {}
        }
        // A daemon user service remains alive for the safety timer; no destructive unbind.
        background.shutdown();
        remote = null;
    }
}