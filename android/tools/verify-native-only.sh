#!/usr/bin/env bash
set -euo pipefail
ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT"

fail=0

if find . -type f \( -name '*.js' -o -name '*.jsx' -o -name '*.ts' -o -name '*.tsx' -o -name 'package.json' -o -name 'package-lock.json' \) -print -quit | grep -q .; then
  echo "FAIL: non-native application source/Node metadata remains."
  fail=1
fi

LEGACY_A="$(printf '\x65\x78\x70\x6f')"
LEGACY_B="$(printf '\x72\x65\x61\x63\x74\x2d\x6e\x61\x74\x69\x76\x65')"
LEGACY_C="$(printf '\x40\x65\x78\x70\x6f')"
LEGACY_D="$(printf '\x40\x72\x65\x61\x63\x74\x2d\x6e\x61\x74\x69\x76\x65')"

if find . -type f -print | grep -Fqi "$LEGACY_A"; then echo "FAIL: legacy filename marker remains."; fail=1; fi
if find . -type f -print | grep -Fqi "$LEGACY_B"; then echo "FAIL: legacy filename marker remains."; fail=1; fi
if grep -RPni --exclude='*.jar' --exclude='verify-native-only.sh' --exclude='NATIVE-ONLY-AUDIT.md' --exclude='ASSET-FEATURE-PARITY-REPORT.md' --exclude='AirPlayrQaContracts.kt' --exclude='*.md' --exclude='*.txt' --exclude-dir='tools' --binary-files=without-match "\b${LEGACY_A}\b|${LEGACY_B}|${LEGACY_C}|${LEGACY_D}" . >/dev/null; then
  echo "FAIL: legacy runtime reference remains."
  fail=1
fi

for required in \
  settings.gradle.kts \
  build.gradle.kts \
  app/build.gradle.kts \
  app/src/main/AndroidManifest.xml \
  app/src/main/java/com/airplayr/app/MainActivity.kt \
  app/src/main/java/com/airplayr/app/AirPlayrApplication.kt; do
  test -f "$required" || { echo "FAIL: missing $required"; fail=1; }
done

for dep in \
  'androidx.media3:media3-exoplayer' \
  'androidx.media3:media3-session' \
  'androidx.media3:media3-cast' \
  'androidx.room:room-runtime' \
  'androidx.datastore:datastore-preferences' \
  'androidx.work:work-runtime-ktx'; do
  grep -q "$dep" app/build.gradle.kts || { echo "FAIL: missing dependency $dep"; fail=1; }
done

grep -q 'AirPlayrPlaybackService' app/src/main/AndroidManifest.xml || { echo 'FAIL: native playback service not declared.'; fail=1; }
grep -Eq 'android:name="(\.MainActivity|com\.airplayr\.app\.MainActivity)"' app/src/main/AndroidManifest.xml || { echo 'FAIL: native launcher Activity not declared.'; fail=1; }

if [[ "$fail" -eq 0 ]]; then
  echo 'PASS: standalone native Android project verified.'
else
  exit 1
fi
