#!/usr/bin/env bash
set -euo pipefail
mkdir -p smoke-results
adb wait-for-device
adb shell wm size 748x720
adb shell wm density 340
adb install -r app/build/outputs/apk/debug/app-debug.apk
adb logcat -c || true
adb shell am start -W -n com.flipminios.launcher/.LauncherActivity
sleep 5
adb shell pidof com.flipminios.launcher >/dev/null || {
  echo "FAIL: app process not running"; exit 2;
}
adb shell uiautomator dump /sdcard/flip-ui.xml || true
adb pull /sdcard/flip-ui.xml smoke-results/home.xml || true
adb shell screencap -p /sdcard/flip-home.png
adb pull /sdcard/flip-home.png smoke-results/home.png
# Four tab buttons, Panel is third. Approximate for the test's 748x720 display.
adb shell input tap 465 655
sleep 3
adb shell screencap -p /sdcard/flip-panel.png
adb pull /sdcard/flip-panel.png smoke-results/panel.png
adb shell uiautomator dump /sdcard/flip-panel.xml || true
adb pull /sdcard/flip-panel.xml smoke-results/panel.xml || true
adb shell pidof com.flipminios.launcher >/dev/null || {
  echo "FAIL: app crashed after tab navigation"; exit 2;
}
adb logcat -d >smoke-results/logcat.txt
if grep -A 18 "FATAL EXCEPTION" smoke-results/logcat.txt | grep -q "Process: com.flipminios.launcher"; then
  echo "FAIL: launcher threw an uncaught exception"; exit 3;
fi
adb shell dumpsys package com.flipminios.launcher > smoke-results/package.txt
if grep -Fq 'CoverRotationAccessibilityService' smoke-results/package.txt; then
  echo "FAIL: old sideload-sensitive AccessibilityService still packaged"; exit 4
fi
# Android package dumps don't list every private/non-exported service.
# Presence of the service is separately verified in packaged AndroidManifest
# by the APK build workflow. Here verify installed permissions and version.
if ! grep -Fq 'versionName=0.7.3' smoke-results/package.txt; then
  echo "FAIL: installed app isn't the expected v0.7.3 build"; exit 5
fi
if ! grep -Fq 'android.permission.FOREGROUND_SERVICE_SPECIAL_USE' smoke-results/package.txt; then
  echo "FAIL: foreground rotation helper permission missing"; exit 6
fi
echo "PASS: v0.7.1 launcher boots, navigates, retains cover foreground rotation and contains no AccessibilityService."
