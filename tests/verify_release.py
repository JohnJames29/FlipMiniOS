"""Offline release security checks. Android build and hardware testing are separate."""
from pathlib import Path

root = Path(__file__).resolve().parents[1]
wf = (root / '.github/workflows/android-apk.yml').read_text()
manifest = (root / 'app/src/main/AndroidManifest.xml').read_text()
code = '\n'.join(f.read_text() for f in (root/'app/src/main/java').rglob('*.java'))
privacy_config = (root/'app/src/main/res/xml/rotation_accessibility_config.xml').read_text()
assert 'gradle --no-daemon :app:assembleDebug' in wf
assert 'app-debug.apk' in wf
assert 'Verify embedded cover rotation components' in wf
assert 'CoverRotationEngineActivity' in code
assert 'CoverRotationAccessibilityService' in code
assert 'setLaunchDisplayId' in code
assert 'SCREEN_ORIENTATION_FULL_SENSOR' in code
assert 'FLAG_NOT_TOUCHABLE' in code
assert 'android.permission.SYSTEM_ALERT_WINDOW' in manifest
assert 'BIND_ACCESSIBILITY_SERVICE' in manifest
assert 'canRetrieveWindowContent="false"' in privacy_config
assert 'canPerformGestures="false"' in privacy_config
assert 'Shizuku' not in manifest and 'shizuku' not in manifest
assert 'android.permission.INTERNET' not in manifest
assert 'android.permission.QUERY_ALL_PACKAGES' not in manifest
assert 'hasSystemFeature' in code
assert 'showRotationOptions' in code
assert 'setCoverRotationEnabled' in code
assert 'CoverSpin-LICENSE' in str(root / 'third_party/CoverSpin-LICENSE.txt')
print("PASS: v0.7 source and permission checks. Device-specific rotation is not certified.")
