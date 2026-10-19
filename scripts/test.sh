#!/usr/bin/env bash
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT"
TEMP="$(mktemp -d)"
trap 'rm -rf "$TEMP"' EXIT
javac -encoding UTF-8 --release 8 -d "$TEMP" src/nyc/arash/ridehome/NavGeometry.java src/nyc/arash/ridehome/FavoriteOrder.java tests/CoreTests.java
java -cp "$TEMP" nyc.arash.ridehome.CoreTests
python3 scripts/check-public-source.py
