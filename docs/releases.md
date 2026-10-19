# Maintainer release procedure

The default `./build.sh` creates a developer APK and never publishes. Release mode
uses an existing, persistent private key outside this checkout. Never commit that
key, its password, a machine-specific path or generated APKs. Losing the key means
existing installations cannot accept future in-place updates; keep a secure backup.
Official releases retain the original prototype signing key for update continuity.

## Prepare locally

1. Start from reviewed source on the current default branch. Increase both
   `versionCode` and `versionName` in `AndroidManifest.xml` for every release. Use
   a three-part numeric version name, such as `2.2.0`; never reuse a published tag.
2. Run `./scripts/test.sh` and `./build.sh`, then complete the relevant checks in
   [testing](testing.md). Review the final diff and pass the required PR check.
3. Set signing variables in your local environment (or obtain them from a password
   manager). Supply an existing key outside the repository:

   ```sh
   export RIDEHOME_RELEASE_KEYSTORE=/secure/location/ridehome-release.p12
   export RIDEHOME_RELEASE_KEY_ALIAS=ridehome
   # Set RIDEHOME_KEYSTORE_PASSWORD securely in this shell.
   # RIDEHOME_KEY_PASSWORD is optional; defaults to the keystore password.
   ./build.sh --release
   ```

   The build deliberately refuses to generate a replacement release key. Passwords
   are passed to Android's signing tool through environment-variable references,
   not embedded in command-line arguments. Clear them from your shell afterward.
4. Release mode verifies the APK signature and prepares
   `dist/release/v<VERSION>/RideHome-<VERSION>.apk`, `update.json` and `SHA256SUMS`.
   The preparer checks the APK package and version against the manifest, rejects
   debuggable APKs and requires the maintained distribution certificate:
   `2fdd061e19d8daad8460f5123352061a7eda9f177d7b5cb18a27ae958331d36c`.
   Fork maintainers can explicitly set `RIDEHOME_EXPECTED_SIGNER_SHA256` to their own
   certificate digest; this does not make their APK compatible with official installs.
   Compare the printed fingerprint before publishing. Optionally
   regenerate metadata with short plain-text notes:

   ```sh
   python3 scripts/prepare-release.py dist/RideHome-release.apk \
     --notes 'What changed in this release.'
   ```

   Set `ANDROID_SDK_ROOT`, or supply `--build-tools` with your SDK's
   `build-tools/35.0.0` directory. Never prepare the developer APK for distribution.
5. Install the signed release with `adb -s SERIAL install -r` on the intended test
   tablet. Check Home, favorites, overlay behavior and the installer flow before
   publishing. Never uninstall or clear app data to resolve signature errors.

## Publish the reviewed release

Merge the reviewed PR through the protected default branch first. Tag that exact
commit `v<VERSION>`, then create a GitHub release in `arashvakil/ride-home` with
**all three prepared files** attached. Review version, checksums, certificate and
release notes together. Use a draft while uploading, then publish once all assets
are present. Do not mark an experimental release as the latest stable release.

The updater reads `releases/latest/download/update.json`. Its `apkUrl` points to the
versioned release tag so the selected APK stays fixed even if a newer release is
published during the download. The SHA-256 in that manifest must match the attached
APK exactly. `SHA256SUMS` supports manual verification of the APK and metadata.

Release metadata contains `versionCode`, `versionName`, `packageName`, `apkUrl`,
`sha256` and optional `notes`. The updater also independently verifies the APK's
package, newer version code and signing certificate against the installed app.
Metadata alone cannot authorize another signing key. Installation requires the
user's explicit Android installer confirmation.

After publishing, download the assets from GitHub, verify checksums/signature and
confirm the release is offered to an older official build. Record what was actually
tested; do not claim physical-device checks that were unavailable. Keep signing
material local and private; ordinary PR CI needs no signing or publication secrets.
