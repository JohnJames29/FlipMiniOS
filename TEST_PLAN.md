# Flip Mini OS 0.3 • on-device test sequence

After a single GitHub Actions build succeeds, test these in order and report Pass / Fail for each:

1. **Widget glass look**: Add the widget on cover; see translucent frosted styling. Real live blur is host-dependent; opaque backing is possible.
2. **Camera clearance**: From widget open Apps, Home, Controls, Edit. Check no header or buttons overlap the upper-left cameras. If needed go Edit > Cover camera clearance; increase clearance or swap camera side.
3. **Lag**: Open Apps and scroll fast; filter/search. Navigate away and return 3 times. Compare with old build; icons should populate as they load.
4. **App launching**: Open 2-3 regular apps and come back to the widget; confirm none are routed to the broken inner panel.
5. **Navigation**: Check Home, Apps, Panel, Edit tabs highlight and respond.
6. **Controls**: Internet, Bluetooth, Torch, Rotate, media volume, activity brightness. Grant only prompted permissions.
7. **Video apps**: Instagram and TikTok remain native, with their existing square-layout limitations. 9:16 forced reflow is not part of v0.3 Lite.

### Installing over v0.2

The GitHub workflow uses a temporary debug signing key. If Android reports 'App not installed' or a signature conflict during upgrade, uninstall the old Flip Mini OS APK before installing the new one. Uninstalling resets Flip Mini OS settings and favorites, so make note of your chosen settings first. Do NOT uninstall TikTok/Instagram or change Google Play Protect settings.

### GitHub upload

Upload the *contents* of this extracted folder to your GitHub repository root, preserving `.github/workflows/android-apk.yml`. You can overwrite files with matching names. Commit the changes; open Actions and download `FlipMiniOS-debug-APK` from the new successful run. Avoid uploading the ZIP itself as a file; GitHub Actions needs the source directories.
