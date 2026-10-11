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
assert 'android:exported="true"' in manifest
assert 'samsung_widget_info' in manifest
print('Manifest permissions and Flex Window widget metadata: OK')
values = {p.stem for p in (root / 'app/src/main/res/xml').glob('*.xml')}
for name in ('cover_widget_info','samsung_widget_info','accessibility_config'):
    assert name in values, name
widgets = (root / 'app/src/main/res/layout/cover_widget.xml').read_text()
for id in ('open_launcher','open_panel'):
    assert f'@+id/{id}' in widgets
src = '\n'.join(p.read_text() for p in (root / 'app/src/main/java').rglob('*.java'))
assert 'setLaunchDisplayId' in src
assert 'android.view.accessibility.AccessibilityEvent' in src
assert 'GestureDescription' in src
assert 'Display.DEFAULT_DISPLAY' in src
assert 'getWindowsOnAllDisplays' in src
print('Source key features / safety guard markers: OK')
print('All offline structural tests passed (does not validate Android compilation/runtime).')
