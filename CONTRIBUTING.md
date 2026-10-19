# Contributing

Keep the interface touch-first and the home screen static. Preserve a clear route
home and make every additional permission or integration an explicit design decision.

## Development

1. Branch from the current remote default branch.
2. Use JDK 17, Android platform 35 and build tools 35.0.0.
3. Run `./scripts/test.sh` and `./build.sh` before opening a focused pull request.
4. For navigation, window, lifecycle, timing or persistence changes, perform the
   relevant [device checks](docs/testing.md). If hardware is unavailable, say so.

The tests use production preference/geometry helpers and check the manifest's privacy
contract. They do not replace physical overlay, drag, full-screen and reboot checks.
PR CI does not use credentials, install on a device, publish an APK or change hosting.

Format Java with google-java-format 1.24.0 (development tool only, not a runtime or
build dependency). Avoid new libraries unless they solve a concrete problem.

## Public-source boundary

Never commit signing material, APKs, app data, recovery snapshots, private screenshots,
local SDK paths or device serials. The `.gitignore` and source checks provide a guard,
not a substitute for reviewing the complete staged diff and new images.

Keep `nyc.arash.ridehome` stable for compatible updates. Preserve saved favorite order
and tab position. Fresh installations must not expose the maintainer's own apps.
No root commands, app removal, accessibility-service-list replacement or automatic
system setting changes belong in the build.

## Releases

There is no automatic publishing workflow. A maintainer release needs reviewed source,
passing CI, relevant physical-device checks, a stable private distribution signing key,
versioned APK plus checksum, and clear compatibility/recovery notes. Never publish the
private key. Local developer builds are not official releases. Do not claim device
coverage or streaming playback tests that have not been performed.
