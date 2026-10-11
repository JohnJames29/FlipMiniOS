# Flip Mini OS 0.3 - Cover Fix (Lite)

Target: Galaxy Z Flip6 SM-F741B, Android 16 / One UI 8.5. **Rootless**.

## One-pass improvements

- Glass-look cover widget: translucent gradient, subtle outline, new typography and button styling. **True live background blur is not possible to guarantee in a normal Samsung RemoteViews widget** because Samsung controls the host surface; if the host draws an opaque backing, wallpaper translucency may not show.
- Safe camera corner on **all** launcher pages (not just Home), with camera side and 68-140dp clearance in Edit. Default: cameras left, 94dp. Android display cutouts are considered where provided.
- Optimized app drawer: app discovery runs off the UI thread; native GridView recycles visible app cells; icon loading is asynchronous with an LRU cache; app listings refresh at most every 3 minutes on resume, with manual Refresh in Edit.
- Refined dark theme, prominent camera-safe clock, active navigation highlights, compact Home and quick settings tiles.
- Lite build does **not** register the Accessibility Service. This avoids the sensitive permission that triggered the prior installation block. Thus no floating navigation dock or TikTok/Instagram interaction toolbar in this build. Native TikTok/Reels resizing is not implemented; the original apps stay intact.
- No root, network permission, `QUERY_ALL_PACKAGES`, telemetry or bootloader changes.

## Build

Upload project contents including hidden `.github` folder to the repository root and run **Actions > Build Flip Mini OS APK**. Download the debug APK from the Artifacts section of the successful run. This source checkpoint has passed XML, local Java syntax and structural checks but still needs GitHub Actions compilation and Z Flip6 tests.

## One-by-one test sequence after the single v0.3 build

1. **Widget appearance**: check the glass style; blur may be unavailable depending on Samsung's widget host.
2. **Cutout**: open Apps, Home, Controls and Edit; ensure no title/controls touch the camera corner. Adjust Edit > Cover camera clearance and camera side.
3. **Performance**: open and scroll All apps multiple times, then return to the widget, comparing responsiveness to the previous version.
4. **Navigation**: switch Home/Apps/Panel/Edit and open native apps from the cover.
5. **Controls**: Internet, Bluetooth, flashlight (camera permission optional), volume, brightness, auto-rotate (Modify system settings permission optional).
6. **App compatibility**: launch TikTok and Instagram; note they still use their own native layout.

## Permissions

CAMERA is requested only if Torch is used. WRITE_SETTINGS permission is requested only if the user toggles system auto-rotate. There is no registered Accessibility Service in v0.3 Lite.

Known constraints: The cover screen is a Samsung-managed secondary display, and a third-party app cannot become its system launcher or force TikTok to reflow. A widget may be styled translucent, but cannot reliably force live blur of wallpaper underneath it.
