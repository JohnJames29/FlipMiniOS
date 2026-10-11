# Flip Mini OS v0.4 - Compact Cover Fix (Lite)

For **Galaxy Z Flip6 SM-F741B**, Android 16 / One UI 8.5. Rootless; no unlock or Accessibility Service.

## What changed from v0.3

- Reduced the cover-screen camera shelf to a compact configurable 52dp default. The former 94dp spacer on dense cover firmware pushed pages a long way down.
- Camera cutout protection applies to the header corner. Edit > Camera clearance offers 32–84dp; some Samsung-reported system cutouts are capped at 68dp. You can change camera side.
- Shortened the bottom navigation and search bar. The controls now use three smaller tiles per row so volume/brightness are reachable.
- **Internet** opens a real in-app status/dialog even if Samsung blocks its external settings page. Wi-Fi and Connections buttons may still be blocked or appear on the inner display by the system. Direct Wi-Fi toggles are restricted by modern Android.
- **Rotate** offers Auto, Portrait, and Landscape for **Flip Mini OS itself**, without requiring "Modify system settings". Samsung may ignore orientation requests on its cover. It does not force rotation of TikTok/Instagram.
- Keeps the v0.3 glass-style widget, recycled app grid and lazy icons.

## Important limitations

This is not a replacement One UI cover home screen. Use **Settings > Cover screen > Widgets > Flip Mini OS** to open from Samsung's widget, or Good Lock MultiStar.

TikTok and Instagram are the original, unmodified apps. Their **native 9:16 resizing, camera cutout handling and button relocation are not fixed** by this build. A stock third-party launcher cannot reliably change those apps' window sizes or internal controls. The previous experimental Accessibility Service remains **unregistered**, to avoid Play Protect's sensitive-permission block.

## Build and test

GitHub Actions -> "Build Flip Mini OS APK" builds the debug artifact. Test on the cover:
1. Check that the app title appears high enough without crossing the cameras. Adjust **Edit > Cover camera clearance** if needed.
2. Open Apps and scroll, verify navigation and search are visible.
3. Open Panel: all six controls should fit in two rows.
4. Tap Internet: the in-app dialog must appear; test the Wi-Fi settings link separately.
5. Tap Rotate and choose Portrait/Landscape/Auto: the dialog should appear; Samsung may lock the cover screen.
6. Test native TikTok/Reels separately and record which elements are blocked by the camera cutouts.

APK compilation is checked by GitHub Actions; real cover-screen operation requires physical testing.

No internet permission or telemetry, no root and no firmware modification.
