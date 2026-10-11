# Flip Mini OS • v0.2 rootless build candidate

**Device target:** Samsung Galaxy Z Flip6 (SM-F741 variants), cover screen / Flex Window.  
**Architecture:** regular Android application and Samsung-style cover widget. **NO ROOT.**  
**Status:** Android Studio project with a GitHub Actions build workflow. **APK compilation and Z Flip6 device operation are not verified in this environment.**

A rootless mini launcher for using a Z Flip6 when the inner screen is damaged. This does **not** replace Samsung One UI, modify system apps, patch TikTok/Instagram, or resize arbitrary apps to 9:16. It provides a practical cover home, app drawer, quick panel, and an optional accessibility-powered toolbar for video apps.

## Features implemented in source

- Samsung Flex Window-compatible AppWidgetProvider, with buttons opening **Apps** and **Controls** on display `1` via launch options.
- Cover-friendly mini launcher and searchable app drawer, app icon/label listing, long-press-to-pin favorites, 2/3/4-column options.
- Editable quick control tiles with add/remove/reorder: Internet settings panel, Bluetooth settings, rear flashlight (camera permission), Android auto-rotate setting (user-granted `WRITE_SETTINGS`), sound settings, standard settings.
- Brightness slider **only for the Flip Mini OS activity**, plus device media volume slider.
- Optional user-enabled accessibility navigation: Back, Home, Recents, plus a small floating dock in other cover-screen apps.
- Experimental video accessibility overlay **only on non-primary displays** when Instagram/TikTok is foregrounded. Buttons: next/previous swipes, Like/Comment/Share via accessible nodes when exposed, dismiss, and configurable left/right side. **OFF by default.**
- No root, privileged system APK, external SDK, app data harvesting, telemetry, or `INTERNET` permission.

## Known limits / please read

1. **Build is unverified here.** This environment lacks Android SDK/platform jars, Gradle and network access to download them. GitHub Actions workflow can compile the APK where the SDK is available, but its success has not yet been observed.
2. `ActivityOptions.setLaunchDisplayId(1)` follows Samsung's developer sample. Samsung's private-display restrictions, Good Lock, device firmware, and actual cover display ID may affect behavior. An attempted launch that throws on the cover is **not retried on the damaged main screen**.
3. **TikTok/Instagram original native layouts remain unchanged.** The separate overlay is experimental. The native apps can ignore touch forwarding, expose no accessible Like button, or display wrongly on a cover screen.
4. The service never obtains or forwards app account credentials. It only inspects the active native app's accessible UI **when a user taps Like/Comment/Share**. It does not capture screenshots or send data over the network.
5. On some firmware, a cover-specific accessibility overlay may be blocked. It fails silently rather than requesting root or modifying Android display settings.
6. Android auto-rotation control changes a **system-wide** setting after optional special-permission approval; it cannot force a third-party app into the correct orientation.
7. Samsung's actual cover screen camera cutouts/usable area and keyboard require physical testing. Interface uses a header that reserves a left-hand region for cameras.
8. Service is manually enabled from Android accessibility settings and can be turned off at any time. Do not grant permissions until you trust your own compiled APK.

## Get an APK using the included automatic build workflow

This repository now includes `.github/workflows/android-apk.yml`. It installs a supported Android SDK and Gradle in GitHub Actions, compiles the debug APK, and saves it as a downloadable artifact. This is preferable to accepting an unaudited APK from a stranger. **No claim is made that the APK has already been compiled here.**

1. Create a new GitHub repository.
2. Upload **the contents of the FlipMiniOS directory** to the repository root, preserving the `.github` hidden directory.
3. Open the repository's **Actions** tab, select **Build Flip Mini OS APK**, then **Run workflow** (or push to `main`).
4. Once the run succeeds, download **FlipMiniOS-debug-APK** from its **Artifacts** section.
5. Extract `app-debug.apk` and install it on the Z Flip6. Keep MultiStar enabled for cover-screen apps.

The debug APK is signed by Android's debug key, suitable for personal testing, not app-store distribution. Different build environments produce different debug keys and upgrades may need uninstall/reinstall; that clears local launcher preferences.

## Build

1. Install Android Studio with **Android SDK Platform 35**, JDK 17+, and Gradle 8.13 (Gradle may be configured by Android Studio). AGP `8.13.2`.
2. Open **the `FlipMiniOS` directory** as a project.
3. Let Gradle sync, ensure SDK Platform 35 is present.
4. Build `:app:assembleDebug`. With Gradle installed, from the project root:
   ```bash
   gradle :app:assembleDebug
   ```
5. The debug APK should appear at `app/build/outputs/apk/debug/app-debug.apk` **if compilation succeeds**.
6. Install the APK (Android Studio Run or `adb install -r ...`). Root is not required.

No Gradle wrapper binary is included. The included GitHub Actions workflow installs Gradle 8.13; for local builds with Gradle installed use `gradle :app:assembleDebug`.

## Configure on Galaxy Z Flip6

1. Back up the phone while the inner display is still usable. Install Good Lock and its **MultiStar** module from Galaxy Store if available in your region.
2. Under MultiStar → I ♡ Galaxy Foldable → Launcher Widget, add **Flip Mini OS** to apps allowed on the cover screen, or use Samsung Settings → Cover screen → Widgets to enable the **Flip Mini OS** widget. Options may vary by One UI version.
3. Open Flip Mini OS on the cover screen. Tap **Apps**, launch normal apps, and long-press favorite apps to pin them to Home.
4. **Edit → Control panel tiles** to add, remove or reorder your controls.
5. For Back/Home/Recents, optionally enable **Flip Mini OS navigation and video controls** in Android Accessibility settings. For TikTok/Instagram experimental overlay, additionally enable the toggle in **Edit** (off by default).
6. For rotation toggle, grant `Modify system settings` when prompted; for flashlight, grant Camera. Both are optional.

## Acceptance test plan (real phone needed)

| Test | Expected | Fallback |
|---|---|---|
| Add widget on cover screen | Buttons open Apps / Controls on the cover | Try MultiStar Launcher Widget |
| App launcher | List loads, installed apps launch and remain on cover | Enable each app through MultiStar |
| Launcher personalization | Long press app to pin; switch 2/3/4 columns | Reset app data if layout breaks |
| Quick panel | Tiles add/remove/reorder; volume/brightness adjust | Manual Samsung quick panel |
| Optional Accessibility | Back/Home/Recents may work on cover | Disable service if routing wrong |
| Optional Video overlay | Left or right toolbar appears only in TikTok/Instagram on secondary display | Toggle it off in Edit |
| Next/Previous | Vertical swipe navigates video feed | Use native gestures |
| Like, Comment, Share | Activate accessible app controls if offered | Use original app's UI |
| Rotation | Android auto-rotate setting toggles | Android Settings |
| 9:16 original-app rendering | **Not implemented in v0.1** | Native app on MultiStar |

## Source structure

- `LauncherActivity.java` main UI, app drawer, editable panel and safe cover-target app launching
- `AppRepository.java` installed launchable apps (limited `<queries>`, no `QUERY_ALL_PACKAGES`)
- `Prefs.java` local device settings
- `Ui.java` custom compact views
- `CoverWidgetProvider.java` Samsung Flex Window launcher widget
- `CoverAccessibilityService.java` user-enabled gestures and app controls
- `app/src/main/res/xml/samsung_widget_info.xml` Samsung Flex Window declaration
- `tests/check_project.py` basic offline structural validation

## Documentation used

- Samsung Developer: https://developer.samsung.com/galaxy-z/flex_window.html
- Samsung Good Lock widget guide: https://developer.samsung.com/sdp/blog/en/2024/01/09/best-practices-of-app-development-for-various-screen-sizespowered-by-good-lock
- Android ActivityOptions: https://developer.android.com/reference/android/app/ActivityOptions#setLaunchDisplayId(int)
- Android AccessibilityService: https://developer.android.com/reference/android/accessibilityservice/AccessibilityService
- Android Accessibility guide: https://developer.android.com/guide/topics/ui/accessibility/service

## Next development milestone

Build on Android Studio, run on real Z Flip6 cover screen, verify overlay window and display IDs. Then implement optional per-app profiles and window resizing experiments **only after confirming system behaviors**. Do not assume 9:16 resizing works on stock One UI, or ship shell resolution overrides without safe rollback. **No root is planned.**

## Important engineering boundaries

- This is a **regular Android app**, not a replacement system launcher for the Samsung Flex Window. Launcher entry is provided by a Samsung-style widget / MultiStar.
- The floating navigation dock and TikTok/Instagram assistive toolbar require the user to explicitly enable the Accessibility service. The toolbar is OFF by default.
- Rootless apps cannot guarantee changing TikTok or Instagram into a 9:16 window or relocating their own controls. The helper gestures depend on app accessibility labels; no unsupported aspect-ratio claims are made.
- No TikTok/Instagram private API use, credential access, password collection, network access, root or bootloader changes.
- Protect access to your damaged inner display and back up data before experimenting.
