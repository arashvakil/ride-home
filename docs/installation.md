# Installation and recovery

## SoulCycle access and APK sideloading

Ride Home assumes your SoulCycle tablet is already **“jailbroken” or unlocked for
Android settings and APK sideloading**. This describes getting out of the restricted
SoulCycle interface; Ride Home itself does not require root or a bootloader unlock.
A tablet that already boots directly into Android with Nova is a suitable starting
point. Keep that working launcher installed while testing Ride Home.

### Community-reported SoulCycle settings access

Older community instructions describe this sequence from the SoulCycle interface:

1. Press and hold the area immediately below the camera for about **10 seconds**
   to bring up a code prompt.
2. Depending on the software version, community reports mention **`983845623`** or
   **`548278537`** as access codes.
3. Once Android settings are accessible, enable developer/USB debugging access as
   needed for ADB and authorize your computer on the tablet.

These are historical community reports, not a SoulCycle unlock procedure independently
verified by this project. The gesture, prompt and codes may differ or be unavailable
on your firmware. This project's device testing started with Android/ADB access
already enabled; it did not reproduce the initial unlock.

### APK tutorial and USB-C cable

[Watch the linked APK-sideloading tutorial](https://www.youtube.com/watch?v=O8Vq-JK1um8).
The community discussion identifies this as a **Peloton** video: use it for the general
APK/ADB workflow **after** reaching Android settings, not as a SoulCycle-specific
settings-unlock tutorial. The video itself has not been independently reviewed here.

Use a **data-capable USB-C cable** between the computer and tablet. A charge-only cable
will not provide the USB data connection needed to push/install APKs. Check
`adb devices -l` and accept the tablet's USB-debugging authorization prompt before
following the install commands below. An `unauthorized` entry needs authorization on
the tablet; no entry can indicate a cable, port or debugging-setup problem.

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
