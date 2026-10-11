# Flip Mini OS v0.6 · Rootless Cover Screen Build

**Target device:** Samsung Galaxy Z Flip6 SM-F741B, Android 16 / One UI 8.5.
**Status:** Android source build checked through GitHub Actions; Samsung hardware verification requires the user's physical device.

## What works and what remains limited
- Rootless launcher, Samsung Flex Window widget, favorites, app drawer, search, camera-safe header, a customizable compact control panel, quick settings with readable labels, normal app launching through MultiStar, Wi-Fi status, flashlight, brightness for the app, volume, and local launcher orientation.
- Android-wide auto-rotation toggles the system preference when permitted. It **cannot** force the Z Flip6 secondary display to rotate: on the actual phone, `wm user-rotation -d 1 free` and `lock 1` were observed to have no visual effect.
- **TikTok and Instagram remain the genuine installed apps with their existing account, feed, likes, and messages.** Samsung's Flip6 cover does not provide third-party apps a reliable mechanism to reposition native buttons or force a 9:16 layout. The v0.5 right-hand floating 9:16 popup was visibly clipped and has been removed.

## New optional Shizuku video-scale experiment
The advanced settings contain a rootless Shizuku-only compatibility option. It requests a **temporary display-1 density of 280 dpi** (instead of the verified physical 340 dpi) to give native apps more logical space. The app is then launched normally, with no floating window.

**Not a confirmed TikTok fix.** Whether Samsung or the apps respect the new density is device-specific. The option is off until the user taps it and approves Shizuku permission. It will refuse to change an unrecognized display, and it refuses to overwrite a density override that was already active before the feature. A Shizuku UserService attempts to reset the density to normal after 90 seconds; a separate shell watchdog also tries to reset it. Automatic restoration cannot be guaranteed if Shizuku/the device terminates these processes. Manual reset uses:
```sh
wm density -d 1 reset
```
The Edit screen also has **Restore normal 340 dpi** and **Display status (read only)**.

If the cover screen becomes misaligned, use aShell You with Shizuku to run the reset command. Avoid changing your main display.

## Security
- No rooting or bootloader unlocking; Shizuku is optional.
- No INERNET permission or telemetry in the application.
- No AccessibilityService registered.
- Shizuku UserService only accepts three coded actions: inspect the specified cover display, request one allowlisted density profile, and restore density. Arbitrary shell commands are not accepted.
- Modify-system-settings permission is optional for the normal Android rotation preference and is not needed to launch the app.

## Build
GitHub Actions → **Build Flip Mini OS APK**. Workflow installs JDK 17, Android SDK 35, Gradle 8.13, checks source and Java syntax, builds `app-debug.apk`, and uploads the artifact.

## Consolidated acceptance checklist
1. The Samsung cover widget launches the app on display 1.
2. Apps, Home, Edit and Panel remain in bounds; panel tile icons and labels are fully visible, with reachable brightness and sound sliders.
3. Normal installed apps launch via MultiStar with the same account.
4. Shizuku status displays the original `748x720`, physical `340 dpi` values before any experiment.
5. Optional compact TikTok/Instagram mode only if the user elects to try it. Display is manually restorable and timed for 90-second rollback. Native cropping may remain.
6. UI remains usable when Shizuku is stopped, with only the advanced density controls unavailable.

Even with a successful CI build, a user-space APK cannot be honestly certified to make every native TikTok/Reels screen fit perfectly on a Galaxy Z Flip6 with One UI 8.5.
