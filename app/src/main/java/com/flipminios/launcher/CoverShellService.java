package com.flipminios.launcher;

import android.os.Binder;
import android.os.IBinder;
import android.os.Parcel;
import android.os.RemoteException;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * Shizuku UserService, running as Android shell rather than root.
 * Only handles three fixed operations against the Z Flip6 external display:
 * inspect, temporary compact density, and restore default.
 * No arbitrary shell commands accepted from the application process.
 */
public final class CoverShellService extends Binder {
    public static final int INSPECT = IBinder.FIRST_CALL_TRANSACTION + 30;
    public static final int COMPACT = IBinder.FIRST_CALL_TRANSACTION + 31;
    public static final int RESTORE = IBinder.FIRST_CALL_TRANSACTION + 32;
    private final ScheduledExecutorService restoreTimer = Executors.newSingleThreadScheduledExecutor();
    private ScheduledFuture<?> pendingRestore;
    private boolean modified = false;
    private static final int COVER_DISPLAY = 1;
    private static final int COMPACT_DPI = 280;
    private static final int SECONDS = 90;

    @Override protected synchronized boolean onTransact(int code, Parcel data, Parcel reply, int flags)
            throws RemoteException {
        if (code != INSPECT && code != COMPACT && code != RESTORE) {
            return super.onTransact(code, data, reply, flags);
        }
        String result;
        try {
            if (code == INSPECT) {
                result = readDisplayState();
            } else if (code == COMPACT) {
                result = compact();
            } else {
                result = restore();
            }
        } catch (Exception ex) {
            result = "Error: " + ex.getClass().getSimpleName();
        }
        reply.writeNoException();
        reply.writeString(result);
        return true;
    }

    private String readDisplayState() throws Exception {
        return run("wm size -d " + COVER_DISPLAY) + "\n" + run("wm density -d " + COVER_DISPLAY);
    }

    private boolean expectedHardware(String state) {
        return state.contains("Physical size: 748x720") &&
               state.contains("Physical density: 340");
    }

    private synchronized String compact() throws Exception {
        String original = readDisplayState();
        if (!expectedHardware(original)) {
            return "REFUSED: cover display hardware/configuration was not recognized.\n" + original;
        }
        if (original.contains("Override density:") && !modified) {
            return "REFUSED: another density override is already active. No changes made.";
        }
        String result = run("wm density -d " + COVER_DISPLAY + " " + COMPACT_DPI);
        if (result.toLowerCase().contains("error") || result.toLowerCase().contains("exception")) {
            return "Failed to change cover density: " + result;
        }
        modified = true;
        if (pendingRestore != null) pendingRestore.cancel(false);
        pendingRestore = restoreTimer.schedule(() -> {
            try { restore(); } catch (Exception ignored) {}
        }, SECONDS, TimeUnit.SECONDS);
        // Independent shell watchdog as fallback if the Shizuku service is interrupted.
        // This is best-effort, not an Android-guaranteed persistence mechanism.
        try { run("(sleep 90; wm density -d 1 reset) >/dev/null 2>&1 </dev/null &"); }
        catch (Exception ignored) {}
        return "Compact cover density requested: 280 dpi. Auto-restore in 90 seconds.\n" + readDisplayState();
    }

    private synchronized String restore() throws Exception {
        if (pendingRestore != null) {
            pendingRestore.cancel(false);
            pendingRestore = null;
        }
        String output = run("wm density -d " + COVER_DISPLAY + " reset");
        modified = false;
        return "Cover density reset requested.\n" + readDisplayState() +
                (output.isEmpty() ? "" : "\n" + output);
    }

    private static String run(String command) throws Exception {
        Process proc = new ProcessBuilder("/system/bin/sh", "-c", command)
                .redirectErrorStream(true).start();
        String result;
        try (InputStream in = proc.getInputStream();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[1024];
            int n;
            while ((n = in.read(buffer)) != -1 && out.size() < 8192) {
                out.write(buffer, 0, n);
            }
            result = out.toString(StandardCharsets.UTF_8.name()).trim();
        }
        if (!proc.waitFor(8, TimeUnit.SECONDS)) {
            proc.destroyForcibly();
            throw new IllegalStateException("Window manager timed out");
        }
        if (proc.exitValue() != 0) {
            throw new IllegalStateException("Command failed: " + result);
        }
        return result;
    }
}