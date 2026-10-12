# Flip Mini OS v0.7: Built-in cover rotation

**For Galaxy Z Flip6 cover display.** This version preserves the existing launcher, widgets, and installed original apps. It replaces the unsuccessful Shizuku experiment with an adaptation of CoverSpin's MIT-licensed Android secondary-display rotation overlay.

## What is implemented

- **Auto-rotate cover**: an invisible, 0x0 `TYPE_APPLICATION_OVERLAY` on the cover display requests `SCREEN_ORIENTATION_FULL_SENSOR`.
- **Lock cover orientation**: switches the cover overlay to `SCREEN_ORIENTATION_LOCKED`.
- **Stop cover rotation**: removes the overlay and optional refresh button.
- **Optional rotation refresh bubble**: a small button at bottom-left toggles the overlay orientation request to locked briefly, then restores the selected rotation mode. **It has NOT been verified to repair Instagram Reels layout after a swipe.** The original CoverSpin did not claim this feature; it is our experiment.
- **Optional accessibility wake keepalive**: reactivates the cover overlay on screen wake. Accessibility XML explicitly forbids reading window content and performing gestures. The service ignores all accessibility events.
- **No Shizuku dependency, ADB, shell commands, root, network access, or display density overrides.**
- Existing TikTok, Instagram and Reddit packages remain untouched.

## Enable

1. Install the APK over the earlier Flip Mini OS (same package name; a different signing key may require uninstall first).
2. Add **Flip Mini OS** to **Good Lock → MultiStar → Launcher Widget / cover apps** if necessary, since Samsung gates external activities.
3. From Flip Mini OS **Edit → Cover rotation → Auto-rotate cover**, or the **Panel → Rotate** tile, enable the cover rotation. Grant **Display over other apps** only if you are comfortable. The Android permission screen may launch on the damaged inner panel; do not force access.
4. **Optional:** `Permissions & optional auto-start → Optional auto-start`, then enable **Flip Mini OS cover rotation** in Android Accessibility. This does not scan app screens or text, but Android sideload restrictions may require explicitly allowing restricted settings. Without the optional service, rotation can start when the launcher is opened and may stop after system process death.
5. Disable CoverSpin's active overlay if both apps compete for rotation.

## Evidence and limitations

We inspected CoverSpin's open-source implementation at https://github.com/crispim1411/CoverSpin. Its core uses an invisible overlay on the **secondary display**, not `wm user-rotation`. This same mechanism is now integrated into the app. Attribution and MIT license are included in `third_party/CoverSpin-LICENSE.txt` and the APK assets.

**Compilation and generic Android emulator smoke tests cannot certify behavior on One UI 8.5 Flip6 cover hardware.** The overlay may still be blocked unless enabled through MultiStar; it may not preserve Instagram's Reel layout when swiping, and it cannot independently move controls hidden behind the physical cover cameras. Original Reddit, Instagram and TikTok remain untouched. No permanent OS changes are made.

## Reverting

In Flip Mini OS, choose **Stop cover rotation**; if using auto-start, also turn OFF the optional accessibility service. The overlay is removed; Samsung's OS settings remain unchanged.

## Build

GitHub Actions builds `app-debug.apk` and checks source tests, app permissions, the rotation manifest components and MIT license.
