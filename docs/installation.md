# Installation and recovery

## Before you change the default launcher

Build the developer APK using the README. Retain another working launcher, record
which one is your current default, and ensure Android Settings remains accessible.
ADB debugging must already be enabled and authorized if you plan to use ADB recovery.
Ride Home cannot unlock developer settings or repair inaccessible firmware.

Use `adb devices -l`, then supply `-s SERIAL` on every command. The placeholder is not
a command to run literally. Wireless ADB is optional; USB is sufficient.

```sh
adb -s SERIAL shell cmd package resolve-activity --brief \
  -a android.intent.action.MAIN -c android.intent.category.HOME
adb -s SERIAL install -r dist/RideHome-debug.apk
adb -s SERIAL shell am start -n nyc.arash.ridehome/.Home
```

Enable Ride Home navigation through Android Accessibility settings. Do not overwrite
`enabled_accessibility_services`: other services may be important to the device.

After testing the Home tab inside another app, use Ride Home → Settings → Default
home app to select it. On devices supporting the command, the ADB alternative is:

```sh
adb -s SERIAL shell cmd package set-home-activity nyc.arash.ridehome/.Home
```

## Returning to another launcher

Open Ride Home → Settings → Default home app, or open Android Home settings via ADB:

```sh
adb -s SERIAL shell am start -a android.settings.HOME_SETTINGS
```

Choose the launcher you recorded before installation. Then disable Ride Home navigation
in Accessibility settings to remove its tab. Android settings menu paths vary by vendor.
If you know the earlier launcher's component, `cmd package set-home-activity` can select
it directly; use its actual installed component rather than copying a vendor example.

Uninstall only after selecting a replacement Home app, and only if you intend to delete
Ride Home's local favorites and position settings. Never uninstall merely to get past
a signature mismatch. An update requires the same signing key as the installed APK.
The build script does not distribute a shared signing key.

## Known boundaries

- Physically tested: SoulCycle SH-1, Android 9, 1920 × 1080 landscape.
- Android 6+ is a compilation floor, not a claim that every version/device is supported.
- Sideloaded accessibility access can require an additional restricted-settings approval
  on newer Android versions. OEM policies or protected windows may suppress overlays.
- The small Home tab intentionally remains visible in other apps. Drag it if it covers
  an app's controls. The temporary tray blocks touches until dismissed.
- The launcher does not install streaming apps, change subscriptions or read bike sensors.
- No OS replacement, root, bootloader modification, telemetry-blocking or debloating is
  performed. Android's original navigation bars are not modified.
