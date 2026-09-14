#!/usr/bin/env bash
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
JAVA="$ROOT/app/src/main/java"
RES="$ROOT/app/src/main/res"
ASSETS="$ROOT/app/src/main/assets"

test -f "$ASSETS/branding/icon.png"
test -f "$ASSETS/branding/adaptive-icon.png"
test -f "$ASSETS/branding/splash-icon.png"

for f in \
  "Media player-bro.svg" "Headphone-amico.svg" "Recording-pana.svg" \
  "Playing Music-bro.svg" "Music-cuate.svg" "Playlist-pana.svg" "DJ boy-cuate.svg"; do
  test -f "$ASSETS/feature-cards/$f"
done

for f in \
  OpenSans-Regular.ttf OpenSans-Bold.ttf OpenSans-Medium.ttf CaacupeOne-Regular.ttf \
  Oswald-Medium.ttf SmoochSans-SemiBold.ttf DancingScript-Regular.ttf ShadowsIntoLight-Regular.ttf; do
  test -f "$ASSETS/fonts/$f"
done

grep -q 'class MainActivity' "$JAVA/com/airplayr/app/MainActivity.kt"
grep -q 'fun HomeScreen' "$JAVA/com/airplayr/app/ui/screens/HomeScreen.kt"
grep -q 'Theme.App.SplashScreen' "$ROOT/app/src/main/AndroidManifest.xml"
grep -q 'core-splashscreen' "$ROOT/app/build.gradle.kts"

if grep -RIlE '(^|[^A-Za-z])(expo|react-native|expo-router|@expo|package\.json|app\.json|node_modules)([^A-Za-z]|$)' \
  "$ROOT/app/src/main/java" "$ROOT/app/src/main/res" "$ROOT/app/src/main/assets" >/tmp/airplayr_bad_refs 2>/dev/null; then
  echo "FAIL: legacy runtime references found:"
  cat /tmp/airplayr_bad_refs
  exit 1
fi

echo "PASS: AirPlayr native asset/feature parity gates"
