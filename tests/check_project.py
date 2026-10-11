from pathlib import Path
import re
import xml.etree.ElementTree as ET
root = Path(__file__).resolve().parents[1]
files = list((root / 'app/src/main/res').rglob('*.xml')) + [root / 'app/src/main/AndroidManifest.xml']
for file in files:
    ET.parse(file)
print(f'XML: {len(files)} valid files')
manifest = (root / 'app/src/main/AndroidManifest.xml').read_text()
assert 'android.permission.INTERNET' not in manifest
assert 'android.permission.QUERY_ALL_PACKAGES' not in manifest
assert 'android.permission.BIND_ACCESSIBILITY_SERVICE' not in manifest
assert 'android.permission.ACCESS_NETWORK_STATE' in manifest
assert 'android.permission.WRITE_SETTINGS' not in manifest
assert 'android:exported="true"' in manifest
assert 'samsung_widget_info' in manifest
print('Rootless Lite manifest and cover widget metadata: OK')
values = {p.stem for p in (root / 'app/src/main/res/xml').glob('*.xml')}
for name in ('cover_widget_info','samsung_widget_info','accessibility_config'):
    assert name in values, name
widgets = (root / 'app/src/main/res/layout/cover_widget.xml').read_text()
for id in ('open_launcher','open_panel'):
    assert f'@+id/{id}' in widgets
src = '\n'.join(p.read_text() for p in (root / 'app/src/main/java').rglob('*.java'))
assert 'setLaunchDisplayId' in src
assert 'Display.DEFAULT_DISPLAY' in src
assert 'class AppGridAdapter' in src
assert 'class IconLoader' in src
assert 'safeCameraHeight' in src
assert 'refreshAppsIfNeeded' in src
print('Source key features / safety guard markers: OK')
print('All offline structural tests passed (does not validate Android compilation/runtime).')

# v0.3 must use translucent widget tint and no AccessibilityService registration.
assert '#B1344961' in (root / 'app/src/main/res/drawable/widget_bg.xml').read_text()
assert 'CoverAccessibilityService' not in manifest
assert 'extends AccessibilityService' not in src

# v0.4 compact cover corrections and working in-app settings affordances.
launcher = (root / 'app/src/main/java/com/flipminios/launcher/LauncherActivity.java').read_text()
prefs = (root / 'app/src/main/java/com/flipminios/launcher/Prefs.java').read_text()
assert 'showInternetOptions' in launcher
assert 'showRotationOptions' in launcher
assert 'Settings.ACTION_WIFI_SETTINGS' in launcher
assert 'SCREEN_ORIENTATION_FULL_SENSOR' in launcher
assert 'private void addCoverShelf' in launcher
assert 'i += 3' in launcher  # compact control tiles
assert 'camera_height_v4' in prefs
assert 'camera_height", 94' not in prefs
print('v0.4 compact layout and control-dialog checks passed')
