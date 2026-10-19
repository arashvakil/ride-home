# Testing and compatibility

## Automated checks

`./scripts/test.sh` tests the production favorite-order parser and tab geometry:
empty first run, legacy preferences, saved ordering, duplicate removal, edge snapping,
drag boundaries, resized screens and invalid persisted offsets. It also checks the
permission/accessibility contract, update policy and public-source boundary.

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

## Update regression checklist

- Default developer builds make no public-channel network requests; manual check
  explains that they use a separate signing identity.
- Official release starts a check on returning Home no more than once per six hours.
  Manual check runs immediately; automatic checks can be disabled in Settings.
- Test offline/missing release assets, no newer version and a newer verified release.
  Home and navigation must stay usable while requests run or fail.
- Reject wrong SHA-256, package, version code and signer without changing the
  installed app or its preferences. Reject non-HTTPS and unapproved redirect hosts.
- Download the release into private app storage; offer Install only after verification.
  Restart before installing and verify that the available update is restored.
- On Android 9, deny then grant installation permission, return and tap Install.
  Confirm Android shows its own installer; cancel once, then retry successfully.
- Verify the updated version, favorites, tab position and Accessibility service after
  installation. Never uninstall to bypass a mismatched signing key.
- Verify the published APK and metadata match `SHA256SUMS` downloaded from GitHub.

## Evidence and limits

The 2.0 prototype was physically tested on a SoulCycle SH-1 / Android 9 / 1920 × 1080.
Favorites editing, label launch, Home/Back/Recent Apps, volume, full-screen access,
tray dismissal, saved position and reboot startup were exercised. That is one device,
not general Android compatibility. Version 2.1.0 prepares the source for publication:
portable build, generic first-run favorites, no accessibility event subscription,
and extracted geometry/preference helpers with regression tests.

No Play Store certification, all-vendor coverage, protected streaming-window guarantee,
remote-control UX is claimed. Version 2.2.0 adds signed GitHub release updates;
use the checklist above to record physical update-flow verification. The target SDK remains 28
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

## Release-update smoke check (2.2.0)

An isolated test package built from the updater sources was exercised on the
SoulCycle SH-1 / Android 9. A verified candidate APK was seeded in its private cache
with automatic checks disabled. The update button survived process restart; the
permission explanation opened Android's install-source settings. Android's own
installer was shown, cancellation left the older version installed, and retry
updated version code 5 to 6. Favorite order and tab-position preferences were
unchanged. The original launcher was not replaced by this isolated test.

This firmware required both `GET_SIGNATURES` and `GET_SIGNING_CERTIFICATES` to
collect an APK archive's certificates. The updater requests both and still compares
current signer sets from `SigningInfo` on API 28+, rejecting empty sets. The test
caught and verified this compatibility fix. Pure policy tests separately cover
wrong hash format, identity/version/signer mismatches and transport restrictions.

The cache-seeded smoke test does not prove a live GitHub download or other Android
versions. Release publication includes separate downloaded-asset verification.
