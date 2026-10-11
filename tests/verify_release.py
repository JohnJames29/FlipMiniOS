"""Offline source-quality guard; cannot replace Android SDK build or hardware tests."""
from pathlib import Path
import hashlib
import sys
import zipfile
root = Path(__file__).resolve().parents[1]
wf = (root/'.github/workflows/android-apk.yml').read_text()
assert 'gradle --no-daemon :app:assembleDebug' in wf
assert 'app/build/outputs/apk/debug/app-debug.apk' in wf
assert "gradle-version: '8.13'" in wf
code = '\n'.join(f.read_text() for f in (root/'app/src/main/java').rglob('*.java'))
assert 'class AppGridAdapter' in code
assert 'class IconLoader' in code
assert 'safeCameraHeight' in code
assert 'setLaunchDisplayId' in code
manifest = (root/'app/src/main/AndroidManifest.xml').read_text()
for forbidden in ('QUERY_ALL_PACKAGES','android.permission.INTERNET','BIND_DEVICE_ADMIN','BIND_VPN_SERVICE'):
    assert forbidden not in manifest
assert 'android.permission.WRITE_SETTINGS' not in manifest
assert 'android.permission.ACCESS_NETWORK_STATE' in manifest
assert 'showInternetOptions' in code
assert 'showRotationOptions' in code
assert 'BIND_ACCESSIBILITY_SERVICE' not in manifest
assert '<queries>' in manifest
print('Build workflow, features and security checks: passed')
