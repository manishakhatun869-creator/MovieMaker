#!/usr/bin/env python3
"""Apply resource-level branding to an apktool-decoded Flutter APK."""
from pathlib import Path
import re
import shutil
import sys
import xml.etree.ElementTree as ET

root = Path(sys.argv[1])
brand = Path(__file__).resolve().parent.parent / 'branding'
name = 'Towfik Serial Platform'
manifest = root / 'AndroidManifest.xml'
ET.register_namespace('android', 'http://schemas.android.com/apk/res/android')
tree = ET.parse(manifest)
manifest_root = tree.getroot()
# The compiled app checks the server's app_version_min setting on launch.
# Advertise a version newer than the original package so ordinary minimum-version
# comparisons do not immediately redirect users to the original Play Store entry.
manifest_root.set('{http://schemas.android.com/apk/res/android}versionCode', '9990000')
manifest_root.set('{http://schemas.android.com/apk/res/android}versionName', '999.0.0')
application = manifest_root.find('application')
if application is None:
    raise RuntimeError('No application in decoded manifest')
application.set('{http://schemas.android.com/apk/res/android}label', name)
tree.write(manifest, encoding='utf-8', xml_declaration=True)

# Update any Android-side visible names without touching resource IDs or API URLs.
for path in (root / 'res').glob('values*/strings.xml'):
    text = path.read_text()
    text = re.sub(r'(<string name="app_name"[^>]*>).*?(</string>)',
                  lambda m: m[1] + name + m[2], text, flags=re.DOTALL)
    path.write_text(text)

icons = list((root / 'res').glob('mipmap-*/ic_launcher.png'))
if not icons:
    raise RuntimeError('Launcher icon resource not found')
for icon in icons:
    shutil.copyfile(brand / 'icon.png', icon)

assets = root / 'assets/flutter_assets/assets/images'
for filename in ('logo.png', 'logo_transparent.png'):
    target = assets / filename
    if target.exists():
        shutil.copyfile(brand / filename, target)
print(f'Branded {len(icons)} launcher icons and Flutter logo assets')
