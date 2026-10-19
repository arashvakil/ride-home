# Testing and compatibility

## Automated checks

`./scripts/test.sh` tests the production favorite-order parser and tab geometry:
empty first run, legacy preferences, saved ordering, duplicate removal, edge snapping,
drag boundaries, resized screens and invalid persisted offsets. It also checks the
permission/accessibility contract and public-source boundary.

`./build.sh` compiles and signs a local APK and verifies its signature. PR CI runs both
commands on Linux with JDK 17 and pinned Android SDK packages. It never connects to a
real device or publishes an artifact. Pure tests and a successful build do not prove
accessibility-window behavior on an OEM tablet.

## Manual regression checklist

Use a consented test device with a known working alternative launcher. Keep reports
free of private data and do not clear data or uninstall an existing app to bypass signing.

- Fresh launch: empty favorites has a working Add a favorite path.
- Add, remove, replace and move favorites. Restart and verify the saved order.
- Migrate an existing saved order without resetting it.
- Launch from artwork and app labels; return with Home to Favorites, including an
  app launched from All Apps. Press feedback should not change titles/background.
- Enable navigation through Accessibility settings. The Home tab should be visible
  inside another app, including immersive full screen, and hidden on Ride Home.
- Tap Home; drag it to both edges; verify clamping and position after reboot.
- Hold Home; test Back, Recent Apps, Home, Settings and media volume.
- Dismiss the tray by Close, outside tap, navigation action and inactivity. Dragging
  the volume slider must not trigger inactivity dismissal during the drag.
- Open Controls on the home screen and close it without leaving an overlay behind.
- Disable the service, then reopen the launcher. The setup path remains available.
- Restore the previous launcher through Android Settings.

## Evidence and limits

The 2.0 prototype was physically tested on a SoulCycle SH-1 / Android 9 / 1920 × 1080.
Favorites editing, label launch, Home/Back/Recent Apps, volume, full-screen access,
tray dismissal, saved position and reboot startup were exercised. That is one device,
not general Android compatibility. Version 2.1.0 prepares the source for publication:
portable build, generic first-run favorites, no accessibility event subscription,
and extracted geometry/preference helpers with regression tests.

No Play Store certification, all-vendor coverage, protected streaming-window guarantee,
remote-control UX or automatic update mechanism is claimed. The target SDK remains 28
for this Android 9 appliance. Newer Android versions need explicit device validation.

## Publication-preparation smoke check (2.1.0)

An isolated package build of the same Java sources was installed on the SH-1 so the
existing launcher's preferences remained separate. Fresh empty favorites, adding
apps through the picker, launch from a favorite, hidden-on-home behavior, the tray's
long press/outside dismissal, drag-to-edge position, service reconnect with the saved
position, and Home access through an immersive activity were checked. The original
accessibility-service list and launcher were restored and both test packages removed.
The example images were captured from that clean test setup, not a personal profile.

This OEM's UIAutomator dump could not consistently retrieve an active root with the
event-free service enabled. The checks therefore used direct ADB gestures, window
state and visually reviewed screenshots. Do not add screen/event access merely to
make that automation tool work. A full reboot of 2.1.0 and newer Android hardware
remain unverified; 2.0's prior reboot evidence is listed separately above.
