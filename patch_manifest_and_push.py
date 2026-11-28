import os
import re
import subprocess

manifest_path = "app/src/main/AndroidManifest.xml"
if os.path.exists(manifest_path):
    xml = open(manifest_path, "r", encoding="utf-8").read()

    # Ensure tools namespace exists
    if 'xmlns:tools=' not in xml:
        xml = xml.replace('<manifest ', '<manifest xmlns:tools="http://schemas.android.com/tools" ', 1)

    perms_to_add = [
        '<uses-permission android:name="android.permission.ACCESS_NOTIFICATION_POLICY" />',
        '<uses-permission android:name="android.permission.WRITE_SETTINGS" tools:ignore="ProtectedPermissions" />',
        '<uses-permission android:name="android.permission.REQUEST_IGNORE_BATTERY_OPTIMIZATIONS" />',
        '<uses-permission android:name="android.permission.SYSTEM_ALERT_WINDOW" />',
        '<uses-permission android:name="android.permission.SCHEDULE_EXACT_ALARM" />',
        '<uses-permission android:name="android.permission.CAMERA" />',
        '<uses-permission android:name="android.permission.ACCESS_FINE_LOCATION" />',
        '<uses-permission android:name="android.permission.BLUETOOTH_CONNECT" />',
        '<uses-permission android:name="android.permission.ANSWER_PHONE_CALLS" />',
    ]

    for perm in perms_to_add:
        perm_name = re.search(r'android:name="([^"]+)"', perm).group(1)
        if perm_name not in xml:
            xml = xml.replace('<application', f'    {perm}\n\n    <application', 1)

    acc_service_block = '''
        <service
            android:name=".service.SuzyAccessibilityService"
            android:exported="true"
            android:label="Suzy AI Accessibility Commander"
            android:permission="android.permission.BIND_ACCESSIBILITY_SERVICE">
            <intent-filter>
                <action android:name="android.accessibilityservice.AccessibilityService" />
            </intent-filter>
            <meta-data
                android:name="android.accessibilityservice"
                android:resource="@xml/suzy_accessibility_config" />
        </service>
'''
    if "SuzyAccessibilityService" not in xml:
        xml = xml.replace('</application>', f'{acc_service_block}    </application>', 1)

    open(manifest_path, "w", encoding="utf-8").write(xml)
    print("AndroidManifest.xml patched with Accessibility Service & Special Permissions!")

# Remove any stale legacy duplicate files if present
for stale in [
    "app/src/main/java/com/suzy/ai/data/SuzyPersonas.kt",
    "app/src/main/java/com/suzy/ai/ui/screens/SettingsScreen.kt",
]:
    if os.path.exists(stale):
        os.remove(stale)
        print("Removed stale file:", stale)
