"""Security and project checks; do not substitute for device testing."""
from pathlib import Path
root = Path(__file__).resolve().parents[1]
workflow = (root / ".github/workflows/android-apk.yml").read_text()
manifest = (root / "app/src/main/AndroidManifest.xml").read_text()
source = "\n".join(p.read_text() for p in (root/"app/src/main/java").rglob("*.java"))
assert "gradle --no-daemon :app:assembleDebug" in workflow
assert "Verify embedded cover rotation components" in workflow
assert "CoverRotationForegroundService" in workflow
assert "BIND_ACCESSIBILITY_SERVICE" in workflow  # must check its absence in packaged manifest
assert "android.permission.BIND_ACCESSIBILITY_SERVICE" not in manifest
assert "<service android:name=\".CoverRotationAccessibilityService\"" not in manifest
assert "rikka.shizuku.ShizukuProvider" not in manifest
assert "android.permission.INTERNET" not in manifest
assert "android.permission.QUERY_ALL_PACKAGES" not in manifest
assert "android.permission.SYSTEM_ALERT_WINDOW" in manifest
assert "android.permission.FOREGROUND_SERVICE_SPECIAL_USE" in manifest
assert 'android:foregroundServiceType="specialUse"' in manifest
assert "CoverRotationEngineActivity" in source
assert "CoverRotationForegroundService" in source
assert "SCREEN_ORIENTATION_FULL_SENSOR" in source
assert "startForeground" in source
assert "ACTION_SCREEN_ON" in source
assert "showRotationOptions" in source
assert "setCoverRotationEnabled" in source
assert "setLaunchDisplayId" in source
assert (root/"app/src/main/assets/third_party/CoverSpin-LICENSE.txt").exists()
print("PASS: Play Protect-relevant sensitive permissions removed; cover rotation retained.")

assert "REFRESH_WAIT_MS" in source
assert "setButtonFeedback(true)" in source
assert "SCREEN_ORIENTATION_PORTRAIT" in source
assert "SCREEN_ORIENTATION_LANDSCAPE" in source
assert "applyCoverImmersive()" in source
assert "FLAG_LAYOUT_IN_SCREEN" in source
print("PASS v0.7.2 refresh and cover-system-bar invariants")
