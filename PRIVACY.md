# Privacy

Ride Home processes information locally on the device:

- Installed launchable app names, package identifiers and icons, to show the app list.
- The favorite app order and the Home tab's position, stored in private preferences.
- Current media volume, to display and adjust the volume slider.

It has no network permission, accounts, ads, analytics, crash-reporting service or
remote update check. It does not send this information to the project maintainer.
The Android backup mechanism is disabled for this app.

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
