#!/usr/bin/env bash
# Local-only Android build. Does not install, publish, or upload anything.
set -euo pipefail
ROOT="$(cd "$(dirname "$0")" && pwd)"
cd "$ROOT"
SDK="${ANDROID_SDK_ROOT:-${ANDROID_HOME:-}}"
if [[ -z "$SDK" && -d "$HOME/Library/Android/sdk" ]]; then SDK="$HOME/Library/Android/sdk"; fi
if [[ -z "$SDK" && -d "$HOME/Android/Sdk" ]]; then SDK="$HOME/Android/Sdk"; fi
if [[ -z "$SDK" ]]; then echo 'Set ANDROID_SDK_ROOT to your Android SDK directory.' >&2; exit 1; fi
BT="$SDK/build-tools/35.0.0"
ANDROID="$SDK/platforms/android-35/android.jar"
for executable in aapt2 d8 zipalign apksigner; do
  [[ -x "$BT/$executable" ]] || { echo "Missing Android build-tools/35.0.0: $executable" >&2; exit 1; }
done
[[ -f "$ANDROID" ]] || { echo 'Missing Android SDK platform android-35.' >&2; exit 1; }
for executable in java javac jar keytool; do command -v "$executable" >/dev/null || { echo "Missing JDK tool: $executable" >&2; exit 1; }; done
BUILD="$ROOT/build"
mkdir -p "$BUILD" "$ROOT/dist"
# Each invocation owns its temp directory; concurrent builds cannot share dex/classes.
STAGING="$(mktemp -d "$BUILD/compile.XXXXXX")"
trap 'rm -rf "$STAGING"' EXIT
mkdir -p "$STAGING/classes" "$STAGING/dex"
"$BT/aapt2" compile --dir res -o "$STAGING/resources.zip"
"$BT/aapt2" link -o "$STAGING/base.apk" -I "$ANDROID" --manifest AndroidManifest.xml "$STAGING/resources.zip"
javac -encoding UTF-8 --release 8 -classpath "$ANDROID" -d "$STAGING/classes" src/nyc/arash/ridehome/*.java
# A jar avoids shell argument-length limits and works with the macOS Bash 3.2 shell.
jar cf "$STAGING/classes.jar" -C "$STAGING/classes" .
"$BT/d8" --lib "$ANDROID" --min-api 23 --output "$STAGING/dex" "$STAGING/classes.jar"
(cd "$STAGING/dex" && jar uf "$STAGING/base.apk" classes.dex)
"$BT/zipalign" -f 4 "$STAGING/base.apk" "$STAGING/aligned.apk"
# This is a local developer key, not a distribution/release signing mechanism.
# Existing installations can only update when built with the same private key.
KEYSTORE="${RIDEHOME_DEBUG_KEYSTORE:-$BUILD/debug.p12}"
if [[ -n "${RIDEHOME_DEBUG_KEYSTORE:-}" && ! -f "$KEYSTORE" ]]; then
  echo "The supplied signing key does not exist; refusing to create a replacement." >&2; exit 1
fi
if [[ ! -f "$KEYSTORE" ]]; then
  (umask 077; keytool -genkeypair -keystore "$KEYSTORE" -storetype PKCS12 \
    -storepass android -keypass android -alias ridehome -dname 'CN=Ride Home Developer' \
    -keyalg RSA -keysize 2048 -validity 10000 >/dev/null 2>&1)
fi
APK="$STAGING/RideHome-debug.apk"
"$BT/apksigner" sign --ks "$KEYSTORE" --ks-key-alias ridehome --ks-pass pass:android --key-pass pass:android --out "$APK" "$STAGING/aligned.apk"
"$BT/apksigner" verify "$APK"
cp "$APK" "$ROOT/dist/RideHome-debug.apk"
echo "Built $ROOT/dist/RideHome-debug.apk (local developer signature)"
