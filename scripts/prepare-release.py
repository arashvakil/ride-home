#!/usr/bin/env python3
"""Prepare verified local release assets. Never signs, installs or publishes."""
import argparse
import hashlib
import json
import os
from pathlib import Path
import re
import shutil
import subprocess
import xml.etree.ElementTree as ET

ROOT = Path(__file__).resolve().parents[1]
A = "{http://schemas.android.com/apk/res/android}"
DISTRIBUTION_SIGNER = "2fdd061e19d8daad8460f5123352061a7eda9f177d7b5cb18a27ae958331d36c"


def output(command):
    return subprocess.check_output(command, text=True, stderr=subprocess.STDOUT)


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("apk", type=Path)
    parser.add_argument("--build-tools", type=Path, help="Android SDK build-tools/35.0.0 directory")
    parser.add_argument("--notes", default="", help="Optional plain-text release metadata notes")
    args = parser.parse_args()
    tools = args.build_tools
    if tools is None:
        sdk = os.environ.get("ANDROID_SDK_ROOT") or os.environ.get("ANDROID_HOME")
        if not sdk:
            parser.error("set ANDROID_SDK_ROOT or pass --build-tools")
        tools = Path(sdk) / "build-tools/35.0.0"
    apk = args.apk.resolve()
    if not apk.is_file():
        parser.error("signed APK does not exist")
    manifest = ET.parse(ROOT / "AndroidManifest.xml").getroot()
    package = manifest.attrib["package"]
    version = manifest.attrib[A + "versionName"]
    code = int(manifest.attrib[A + "versionCode"])
    if package != "nyc.arash.ridehome" or code < 1 or not re.fullmatch(r"[0-9]+\.[0-9]+\.[0-9]+", version):
        parser.error("unexpected release package/version")
    # apksigner exits nonzero for an invalid signature. Do not trust filenames.
    signature = output([str(tools / "apksigner"), "verify", "--verbose", "--print-certs", str(apk)])
    signers = re.findall(r"Signer #\d+ certificate SHA-256 digest: ([0-9a-fA-F]{64})", signature)
    if len(signers) != 1:
        parser.error("release requires exactly one verified APK signer")
    expected_signer = os.environ.get("RIDEHOME_EXPECTED_SIGNER_SHA256", DISTRIBUTION_SIGNER).lower()
    if not re.fullmatch(r"[0-9a-f]{64}", expected_signer):
        parser.error("RIDEHOME_EXPECTED_SIGNER_SHA256 must be a SHA-256 certificate digest")
    if signers[0].lower() != expected_signer:
        parser.error("APK signer differs from the maintained distribution certificate")
    badging = output([str(tools / "aapt2"), "dump", "badging", str(apk)])
    expected = f"package: name='{package}' versionCode='{code}' versionName='{version}'"
    if not any(line.startswith(expected + " ") or line == expected for line in badging.splitlines()):
        parser.error("APK package/version does not match source manifest")
    if "application-debuggable" in badging:
        parser.error("release APK must not be debuggable")
    name = f"RideHome-{version}.apk"
    destination = ROOT / "dist" / "release" / f"v{version}"
    destination.mkdir(parents=True, exist_ok=True)
    artifact = destination / name
    shutil.copyfile(apk, artifact)
    sha = hashlib.sha256(artifact.read_bytes()).hexdigest()
    update = {
        "versionCode": code,
        "versionName": version,
        "packageName": package,
        "apkUrl": f"https://github.com/arashvakil/ride-home/releases/download/v{version}/{name}",
        "sha256": sha,
    }
    if args.notes:
        update["notes"] = args.notes
    metadata = destination / "update.json"
    metadata.write_text(json.dumps(update, indent=2) + "\n")
    metadata_sha = hashlib.sha256(metadata.read_bytes()).hexdigest()
    (destination / "SHA256SUMS").write_text(f"{sha}  {name}\n{metadata_sha}  update.json\n")
    print(f"Prepared {destination}")
    print(f"Verified signer SHA-256: {signers[0].lower()}")
    print("No assets uploaded. Review and publish all three files together.")


if __name__ == "__main__":
    main()
