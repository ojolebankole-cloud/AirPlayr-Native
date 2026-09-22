#!/usr/bin/env bash
set -euo pipefail
# Phase 16 device harness. Requires adb and a connected Android device.
ROOT="$(cd "$(dirname "$0")/../.." && pwd)"
OUT="${1:-$ROOT/qa/goldens/$(date +%Y%m%d-%H%M%S)}"
mkdir -p "$OUT"
PKG="com.airplayr.app"

aadb wait-for-device >/dev/null
adb shell am force-stop "$PKG"
adb shell monkey -p "$PKG" 1 >/dev/null 2>&1 || true
sleep 2
adb exec-out screencap -p > "$OUT/01-home.png"
adb shell am start -a android.intent.action.MAIN -c android.intent.category.LAUNCHER -n "$PKG/.MainActivity" >/dev/null
sleep 1
adb exec-out screencap -p > "$OUT/02-home-relaunch.png"

echo "Captured: $OUT"
echo "Manual capture targets: Music, Player, Equalizer, Speed&Pitch, Sleep Timer, Lyrics, Settings, Drawer."
