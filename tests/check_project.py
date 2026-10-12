from pathlib import Path
import xml.etree.ElementTree as ET

root = Path(__file__).resolve().parents[1]
manifest = (root / "app/src/main/AndroidManifest.xml").read_text()
gradle = (root / "app/build.gradle").read_text()
java_root = root / "app/src/main/java/com/flipminios/launcher"
src = "\n".join(p.read_text() for p in java_root.glob("*.java"))
engine = (java_root / "CoverRotationEngineActivity.java").read_text()
service = (java_root / "CoverRotationForegroundService.java").read_text()
launcher = (java_root / "LauncherActivity.java").read_text()
prefs = (java_root / "Prefs.java").read_text()

for p in [root / "app/src/main/AndroidManifest.xml", *(root / "app/src/main/res").rglob("*.xml")]:
    ET.parse(p)

assert "versionName '0.7.1'" in gradle and "versionCode 9" in gradle
assert "dev.rikka.shizuku" not in gradle
assert "ShizukuProvider" not in manifest
assert "android.permission.INTERNET" not in manifest
assert "BIND_ACCESSIBILITY_SERVICE" not in manifest
assert "AccessibilityService" not in manifest
assert "android.permission.SYSTEM_ALERT_WINDOW" in manifest
assert "android.permission.FOREGROUND_SERVICE" in manifest
assert "android.permission.FOREGROUND_SERVICE_SPECIAL_USE" in manifest
assert 'android:foregroundServiceType="specialUse"' in manifest
assert "android.app.PROPERTY_SPECIAL_USE_FGS_SUBTYPE" in manifest
assert 'android:name=".CoverRotationEngineActivity"' in manifest
assert 'android:name=".CoverRotationForegroundService"' in manifest
assert 'android:exported="false"' in manifest
assert not (java_root / "CoverRotationAccessibilityService.java").exists()
assert not (root / "app/src/main/res/xml/rotation_accessibility_config.xml").exists()

assert "TYPE_APPLICATION_OVERLAY" in engine
assert "SCREEN_ORIENTATION_FULL_SENSOR" in engine
assert "SCREEN_ORIENTATION_LOCKED" in engine
assert "FLAG_NOT_TOUCHABLE" in engine
assert "new WindowManager.LayoutParams(\n                0, 0" in engine
assert "Display.DEFAULT_DISPLAY" in engine
assert "setLaunchDisplayId" in engine
assert "ensureFromWake" in engine
assert "CoverRotationEngineActivity.stop()" in launcher

assert "ACTION_SCREEN_ON" in service and "ACTION_USER_PRESENT" in service
assert "startForeground" in service
assert "START_STICKY" in service
assert "NotificationChannel" in service
assert "setOngoing(true)" in service
assert "Prefs.setCoverRotationEnabled(this, false)" in service
assert "coverRotationKeepAlive" in prefs
assert "toggleCoverWakeService" in launcher
assert "CoverRotationForegroundService.stop(this)" in launcher
assert "setLaunchDisplayId" in launcher  # original apps still launch through MultiStar
assert "class AppGridAdapter" in src and "class IconLoader" in src
assert (root / "third_party/CoverSpin-LICENSE.txt").exists()
assert (root / "app/src/main/assets/third_party/CoverSpin-LICENSE.txt").exists()
assert (root / "app/src/main/res/xml/cover_widget_info.xml").exists()
print("PASS v0.7.1: no Accessibility, rootless rotation overlay, notification-backed wake, original app launcher")
