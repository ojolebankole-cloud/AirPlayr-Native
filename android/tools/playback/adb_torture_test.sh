#!/usr/bin/env bash
set -euo pipefail
# Phase 18 device harness. It stresses lifecycle/session behavior; actual media
# playback controls should be driven from the UI or a known local media URI.
ROOT="$(cd "$(dirname "$0")/../.." && pwd)"
OUT="${1:-$ROOT/qa/playback/$(date +%Y%m%d-%H%M%S)}"
mkdir -p "$OUT"
PKG="com.airplayr.app"

adb wait-for-device >/dev/null
adb logcat -c
adb shell am force-stop "$PKG"
adb shell monkey -p "$PKG" 1 >/dev/null 2>&1 || true
sleep 3

for i in $(seq 1 5); do
  adb shell input keyevent 3
  sleep 1
  adb shell monkey -p "$PKG" 1 >/dev/null 2>&1 || true
  sleep 1
done

adb shell dumpsys media_session > "$OUT/media_session.txt"
adb shell dumpsys activity services "$PKG" > "$OUT/services.txt" || true
adb logcat -d -v time > "$OUT/logcat.txt"

grep -E "FATAL EXCEPTION|ANR in|AirPlayrPlayback|ExoPlayer|MediaSession" "$OUT/logcat.txt" > "$OUT/relevant-log.txt" || true

echo "Collected playback lifecycle diagnostics: $OUT"
