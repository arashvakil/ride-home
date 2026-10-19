# Privacy

Ride Home processes information locally on the device:

- Installed launchable app names, package identifiers and icons, to show the app list.
- The favorite app order and the Home tab's position, stored in private preferences.
- Current media volume, to display and adjust the volume slider.

It has no accounts, ads, analytics or crash-reporting service. It does not send
favorites, installed-app details or accessibility information to the project
maintainer. The Android backup mechanism is disabled for this app.

## Updates

Official release builds check GitHub's latest-release metadata when you return Home,
at most once every six hours. You can also check from Settings. Developer builds
skip automatic checks. Update requests disclose ordinary connection information
(such as your IP address) to GitHub and its download infrastructure, subject to
[GitHub's privacy statement](https://docs.github.com/en/site-policy/privacy-policies/github-general-privacy-statement).
No device identifier, app inventory, favorites or ride information is uploaded.

When a newer release is available, its APK downloads into private app storage.
You can disable automatic checks in Ride Home's update settings.
Ride Home verifies its SHA-256 hash, package, newer version and installed signing
certificate before opening Android's installer. Installation always requires your
confirmation. Android may also ask you to allow Ride Home to install unknown apps.
This permission is used only for the update you select; Ride Home cannot silently
install updates.

## Accessibility access

The optional **Ride Home navigation** service draws a movable accessibility overlay
and uses Android's global Back and Recent Apps actions. Home opens Ride Home's own
activity. The service does not subscribe to accessibility events, read screen content,
retrieve windows, capture keystrokes or request gesture injection. Visibility of the
Home screen is determined by Ride Home's own activity lifecycle.

Android requires explicit user activation of accessibility services. Its standard
permission warning may describe broader capabilities than this service configures.
You can disable the service at any time in Android Accessibility settings; the
launcher still works, but the floating Home tab and global actions are unavailable.

## Removal and other apps

Removing Ride Home deletes its private preferences and service. Select a replacement
Home app before removal. Apps opened from Ride Home follow their own privacy policies
and may communicate over the network independently.

Issue reports and contributions sent to GitHub are handled by GitHub. Do not include
private device data, credentials, ride history, installed-app inventories or unredacted
screenshots in public reports.
