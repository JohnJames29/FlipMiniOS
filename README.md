# Flip Mini OS v0.7.3 · Safer CoverSpin-inspired rotation

Target: Samsung Galaxy Z Flip6 cover display, Android 16 / One UI 8.5.

## Installation-blocking fix

v0.7.0 declared a BIND_ACCESSIBILITY_SERVICE-protected service to restart rotation after sleep. Google's Play Protect guidance explains that newly sideloaded apps using Accessibility are commonly blocked with the message "App blocked to protect your device." **v0.7.1+ removes the Accessibility service entirely**, including the manifest registration and permission. No need to turn off Play Protect or Samsung Auto Blocker to try this build.

It is still possible for Play Protect to block a new, unsigned-reputation APK for another reason. This is not a guarantee of Play Protect approval.

## Functionality retained

- Original launcher, widget, app grid, and untouched TikTok, Instagram, and Reddit.
- Cover-only 0x0 transparent overlay requesting sensor-based orientation, based on CoverSpin's MIT-licensed source.
- Explicit Auto/Lock/Stop controls.
- Optional floating diagnostic refresh bubble: synchronizes both orientation overlays and reports whether the actual cover display rotates. It has not been proven to solve Instagram's next-Reel reset.
- **Optional wake restoration via a visible foreground service**, not Accessibility. Activate from Edit > Wake helper. While Android permits the service to run, it listens for screen on/unlock and ensures the cover overlay is restored. A visible system notification contains a Stop action. This behavior is best-effort under Samsung battery/background activity restrictions.
- Stop cover rotation disables the helper and removes the overlay.
- No Shizuku, root, Internet permission, Accessibility, ADB or display density changes.
- On newer Android versions a specialUse foreground service type is required for an ongoing user-initiated cover rotation helper. This type and its purpose are declared in AndroidManifest.xml.

## Enable

1. Install v0.7.3. Do not disable Play Protect to install it.
2. Ensure Flip Mini OS is allowed in Good Lock / MultiStar cover apps.
3. Edit > Auto-rotate cover. Grant Display over other apps permission only if comfortable. It may open on the damaged inner display, so don't risk that panel.
4. Optionally select **Wake helper: OFF** to enable persistent screen-wake restoration. A notification will show while active. Turning it OFF doesn't disable core rotation.
5. Disable the separate CoverSpin rotation service to avoid two apps fighting over orientation.

## Limitations

**v0.7.3 refresh diagnosis and correction:** v0.7.2 accidentally left the interactive refresh bubble requesting FULL_SENSOR while the invisible 0×0 anchor requested PORTRAIT or LANDSCAPE. The conflicting overlay window requests could prevent the requested rotation. v0.7.3 updates **both overlay windows together**, just as CoverSpin's source does, waits 1.7 seconds, then restores sensor rotation. An Android `DisplayManager.DisplayListener` also observes the *actual* display rotation. The button displays `✓` only if the display orientation really changed, and `!` plus a message if the system did not rotate. Unlike v0.7.2, the animation alone never indicates success. It does **not** inspect Instagram content or guarantee that swiping Reels stays corrected. The refresh is only available in Auto mode. The cover launcher retains immersive system-bar handling after rotation/focus; other apps' bars cannot be hidden by our launcher.

The cover rotation feature's behavior on this exact Samsung firmware is not certified by generic emulator tests. Android foreground services may be interrupted or restricted. The original apps determine their own layouts: this update does not guarantee TikTok/Reels camera-safe controls or prevent Instagram from resetting layouts on a swipe.

## Attribution

Core orientation overlay approach adapted from [CoverSpin](https://github.com/crispim1411/CoverSpin) under the MIT License (Copyright © 2024 Crispim). See third_party/CoverSpin-LICENSE.txt and bundled APK asset.

## Build checks

GitHub Actions runs source tests, Java parsing, full Android compile, packaged manifest verification for the foreground service and absence of Accessibility, and produces app-debug.apk.
