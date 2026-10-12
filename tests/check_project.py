from pathlib import Path
import xml.etree.ElementTree as ET

root = Path(__file__).resolve().parents[1]
manifest_path = root / "app/src/main/AndroidManifest.xml"
manifest = manifest_path.read_text()
build = (root / "app/build.gradle").read_text()
java = root / "app/src/main/java/com/flipminios/launcher"

for file in list((root / "app/src/main/res").rglob("*.xml")) + [manifest_path]:
    ET.parse(file)

src = "\n".join(p.read_text() for p in java.glob("*.java"))
launcher = (java / "LauncherActivity.java").read_text()
rotation = (java / "CoverRotationEngineActivity.java").read_text()
service = (java / "CoverRotationAccessibilityService.java").read_text()
prefs = (java / "Prefs.java").read_text()
config = (root / "app/src/main/res/xml/rotation_accessibility_config.xml").read_text()

assert "versionName '0.7.0'" in build and "versionCode 8" in build
assert "dev.rikka.shizuku" not in build
assert "moe.shizuku.manager.permission.API_V23" not in manifest
assert "rikka.shizuku.ShizukuProvider" not in manifest
assert "android.permission.INTERNET" not in manifest
assert "QUERY_ALL_PACKAGES" not in manifest
assert "SYSTEM_ALERT_WINDOW" in manifest
assert 'android:name=".CoverRotationEngineActivity"' in manifest
assert 'android:name=".CoverRotationAccessibilityService"' in manifest
assert 'android:permission="android.permission.BIND_ACCESSIBILITY_SERVICE"' in manifest
assert 'android:exported="false"' in manifest  # cover overlay installer activity
assert "rotation_accessibility_config" in manifest
assert "android:canRetrieveWindowContent=\"false\"" in config
assert "android:canPerformGestures=\"false\"" in config
assert "flagRetrieveInteractiveWindows" not in config
assert "onAccessibilityEvent(AccessibilityEvent event)" in service
assert "Deliberately ignore" in service
assert "ACTION_SCREEN_ON" in service
assert "TYPE_APPLICATION_OVERLAY" in rotation
assert "SCREEN_ORIENTATION_FULL_SENSOR" in rotation
assert "SCREEN_ORIENTATION_LOCKED" in rotation
assert "FLAG_NOT_TOUCHABLE" in rotation
assert "new WindowManager.LayoutParams(\n                0, 0" in rotation
assert "Display.DEFAULT_DISPLAY" in rotation
assert "setLaunchDisplayId" in rotation
assert "Settings.canDrawOverlays" in launcher
assert "coverRotationEnabled" in prefs
assert "showCoverRotationOptions" in launcher
assert "stopCoverRotation" in launcher
assert "showCompactVideoProfile" not in launcher
assert "shizukuControl" not in launcher
assert "setLaunchDisplayId" in launcher  # Existing native app launches kept
assert "class AppGridAdapter" in src
assert "class IconLoader" in src
assert (root / "third_party/CoverSpin-LICENSE.txt").exists()
assert (root / "app/src/main/assets/third_party/CoverSpin-LICENSE.txt").exists()
assert (root / "app/src/main/res/xml/cover_widget_info.xml").exists()
assert (root / "app/src/main/res/xml/samsung_widget_info.xml").exists()
print("PASS: v0.7 manifest, rootless overlay, optional privacy-restricted accessibility, original app launcher, license")
