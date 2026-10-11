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
assert 'android.permission.WRITE_SETTINGS' in manifest
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

# v0.5 app controls and original-app portrait window experiment
ui = (root / 'app/src/main/java/com/flipminios/launcher/Ui.java').read_text()
assert 'SpannableString' in ui and 'setMaxLines(2)' in ui
assert 'showSystemRotationOptions' in launcher and 'Settings.System.canWrite' in launcher
assert 'showCompactVideoProfile' in launcher
assert 'CoverShizukuControl' in launcher
assert 'Ui.lp(this, -1, 72)' in launcher
print('v0.5 compact tiles, system rotation, native-app experimental window checks passed')

# v0.6 Shizuku safety invariants
service = (root / 'app/src/main/java/com/flipminios/launcher/CoverShellService.java').read_text()
bridge = (root / 'app/src/main/java/com/flipminios/launcher/CoverShizukuControl.java').read_text()
build = (root / 'app/build.gradle').read_text()
assert 'dev.rikka.shizuku:api:13.1.5' in build
assert 'dev.rikka.shizuku:provider:13.1.5' in build
assert 'versionName \'0.6.1\'' in build
assert 'Physical size: 748x720' in service
assert 'Physical density: 340' in service
assert 'COMPACT_DPI = 280' in service
assert 'SECONDS = 90' in service and 'restoreTimer.schedule' in service
assert 'wm density -d 1 reset' in service
assert 'requestPermission' in bridge and 'bindUserService' in bridge
assert 'testVideoWindow(' not in launcher  # broken floating overlay removed
print('v0.6 Shizuku safety and native-app fallback structural checks passed')

# v0.6.1: Shizuku must actually receive its Binder through a protected provider.
assert 'rikka.shizuku.ShizukuProvider' in manifest
assert '${applicationId}.shizuku' in manifest
assert 'android.permission.INTERACT_ACROSS_USERS_FULL' in manifest
assert 'moe.shizuku.manager.permission.API_V23' in manifest
assert 'android:multiprocess="false"' in manifest
assert 'moe.shizuku.client.V3_SUPPORT' in manifest
print('Shizuku Binder discovery provider present and access protected')
