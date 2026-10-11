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
assert 'navigationDockEnabled' in code
assert 'showNavOverlay' in code
assert 'GLOBAL_ACTION_RECENTS' in code
assert 'setLaunchDisplayId' in code
assert 'setDisplayId(overlayDisplayId)' in code
assert 'getWindowsOnAllDisplays' in code
assert 'createWindowContext' in code
manifest = (root/'app/src/main/AndroidManifest.xml').read_text()
for forbidden in ('QUERY_ALL_PACKAGES','android.permission.INTERNET','BIND_DEVICE_ADMIN','BIND_VPN_SERVICE'):
    assert forbidden not in manifest
assert 'android.permission.WRITE_SETTINGS' in manifest
assert '<queries>' in manifest
print('Build workflow, features and security checks: passed')
