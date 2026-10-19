#!/usr/bin/env python3
"""Check the public source boundary and the app's permission contract."""
from pathlib import Path
import re
import subprocess
import xml.etree.ElementTree as ET

ROOT = Path(__file__).resolve().parents[1]
A = "{http://schemas.android.com/apk/res/android}"
manifest = ET.parse(ROOT / "AndroidManifest.xml").getroot()
assert manifest.attrib["package"] == "nyc.arash.ridehome"
assert {p.attrib[A + "name"] for p in manifest.findall("uses-permission")} == {
    "android.permission.INTERNET", "android.permission.REQUEST_INSTALL_PACKAGES"
}, "Only release-update network and user-confirmed installation permissions are allowed"
receiver = manifest.find("application/receiver")
assert receiver is not None and receiver.attrib[A + "exported"] == "false"
service = manifest.find("application/service")
assert service is not None and service.attrib[A + "permission"] == "android.permission.BIND_ACCESSIBILITY_SERVICE"
config = ET.parse(ROOT / "res/xml/navigation.xml").getroot()
assert config.attrib.get(A + "canRetrieveWindowContent") == "false"
assert A + "accessibilityEventTypes" not in config.attrib, "Do not subscribe to screen events"
assert config.attrib.get(A + "canRequestFilterKeyEvents", "false") == "false"
assert config.attrib.get(A + "canPerformGestures", "false") == "false"
for path in (ROOT / "src").rglob("*.java"):
    source = path.read_text()
    assert not re.search(r"\b(?:getRootInActiveWindow|getWindows|onKeyEvent|dispatchGesture)\s*\(", source), path

# Scan only candidate public inputs, never ignored build output or private keys.
paths = set(subprocess.check_output(["git", "ls-files", "-z"], cwd=ROOT).decode().split("\0"))
paths |= set(subprocess.check_output(["git", "ls-files", "--others", "--exclude-standard", "-z"], cwd=ROOT).decode().split("\0"))
for relative in filter(None, paths):
    path = ROOT / relative
    assert path.suffix.lower() not in {".apk", ".aab", ".p12", ".jks", ".keystore", ".pem", ".key"}, relative
    if path.suffix.lower() in {".png", ".jpg", ".webp"}:
        continue
    data = path.read_text()
    assert not re.search(r"/(?:Users|home)/[A-Za-z0-9_.-]+/", data), relative
    assert not re.search(r"-----BEGIN (?:RSA |EC |OPENSSH )?PRIVATE KEY-----", data), relative
    assert not re.search(r"\b(?:gh[pousr]_[A-Za-z0-9]{30,}|AKIA[A-Z0-9]{16})\b", data), relative
print("Public source boundary and accessibility/privacy contract passed.")
