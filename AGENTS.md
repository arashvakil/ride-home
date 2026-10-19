# Ride Home contributor guidance

Ride Home is a touch-first Android launcher. Preserve its package identity
`nyc.arash.ridehome` so signed updates can retain user data.

- Keep the home screen static: tapping an app opens it; no focus-driven app titles,
  content feeds, ads or remote-dependent navigation.
- The Home tab must remain discoverable inside other apps and hidden on Ride Home.
  Preserve drag, long-press controls, dismissal and saved-position behavior.
- Accessibility is for overlays and Back/Recent Apps only. Never enable screen-content
  retrieval, key capture or gesture interception. No network permission or telemetry.
- Keep signing keys, private device data, user screenshots and machine-specific
  paths out of Git. Never uninstall or clear app data to make an update succeed.
- Run `./scripts/test.sh` and `./build.sh` for source changes. Physically verify
  navigation/lifecycle changes on the supported Android 9 device when available;
  document limits rather than claiming unperformed tests.
- CI must remain credential-free and eligible on every pull request. A build is
  not authorization to install on a device, publish an APK or change repo visibility.
- Use a short-lived task branch from the freshly fetched default branch, a focused
  pull request and passing proven checks. Preserve stronger branch protections.
