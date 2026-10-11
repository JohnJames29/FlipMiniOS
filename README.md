# Flip Mini OS v0.5 · Control tiles + Rotation + Native-App Fit Experiment

**Samsung Galaxy Z Flip6 (SM-F741B), Android 16 / One UI 8.5. No root.**

## What changed
- The six Panel controls now use a single two-line icon+label view per tile and 72dp height so labels no longer get clipped. The screen is scrollable to reach brightness and volume.
- Rotate now has **two separate kinds of actions**: Auto/Portrait/Landscape for Flip Mini OS itself, and an optional Android **System auto-rotate** toggle for other apps. Android requires user approval for "Modify system settings" to use the latter. It still cannot force another app or Samsung's cover display to honor orientation.
- **Experimental native 9:16 launch:** Edit → Test TikTok 9:16 / Test Instagram 9:16. Opens each existing native app while requesting a right-aligned portrait window via `ActivityOptions.setLaunchBounds()`. This is explicitly experimental; Samsung's Flip6 cover typically does not support freeform window management. Android may ignore the requested size entirely, meaning TikTok/Reels remain cropped.
- Original apps, profiles, feeds and permissions remain unchanged. Experimental window bounds are attempted only when user explicitly taps "Try once".
- Kept rootless Lite APK with no Accessibility Service. It does not bypass Play Protect or modify the system.

## On-device test sequence
1. Panel: icons and full labels for Internet/Bluetooth/Torch/Rotate/Sound/Settings visible.
2. Panel: scroll and confirm both brightness and volume sliders can be reached.
3. Rotate → "System auto-rotate (other apps)" displays Android status and asks permission if needed.
4. Test a third-party app that already supports rotation. Samsung's cover policy can still block rotation.
5. Edit → Test TikTok 9:16, then Instagram 9:16. Verify whether the original app actually opens in a right-aligned narrow window. If it stays square, Android rejected/ignored the bounds.
6. Check normal app drawer launching still works.

**Limitations:** Samsung officially lists native cover-screen aspect-ratio controls as Flip7-exclusive in MultiStar. Android's launch bounds are ignored on displays without freeform/PiP support. Without root/privileged display management or changing the apps, a third-party launcher cannot promise to rearrange Reels/TikTok UI. We do not promise a native crop fix in v0.5.

GitHub Actions workflow: [Build Flip Mini OS APK](.github/workflows/android-apk.yml)
