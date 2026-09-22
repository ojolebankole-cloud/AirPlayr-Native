#!/usr/bin/env bash
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
BASE="/mnt/data/phase30work/baseline/native"
PASS=0
FAIL=0
check(){ if eval "$1" >/dev/null 2>&1; then echo "PASS: $2"; PASS=$((PASS+1)); else echo "FAIL: $2"; FAIL=$((FAIL+1)); fi; }

# Phase 30 — Statistics UI/data binding checks
check "grep -q 'Listening & watching stats' '$ROOT/app/src/main/java/com/airplayr/app/ui/screens/StatsScreen.kt'" 'Statistics screen title present'
check "grep -q 'TOTAL MEDIA TIME' '$ROOT/app/src/main/java/com/airplayr/app/ui/screens/StatsScreen.kt'" 'Statistics hero card present'
check "grep -q 'Your listening story starts here' '$ROOT/app/src/main/java/com/airplayr/app/ui/screens/StatsScreen.kt'" 'Statistics empty-story state present'
check "grep -q 'Sessions' '$ROOT/app/src/main/java/com/airplayr/app/ui/screens/StatsScreen.kt'" 'Sessions card present'
check "grep -q 'When you listen' '$ROOT/app/src/main/java/com/airplayr/app/ui/screens/StatsScreen.kt'" 'Listening activity card present'
check "grep -q 'When you watch' '$ROOT/app/src/main/java/com/airplayr/app/ui/screens/StatsScreen.kt'" 'Watching activity mode present'
check "grep -q 'Genres' '$ROOT/app/src/main/java/com/airplayr/app/ui/screens/StatsScreen.kt'" 'Genres card present'
check "grep -q 'Top songs' '$ROOT/app/src/main/java/com/airplayr/app/ui/screens/StatsScreen.kt'" 'Top songs card present'
check "grep -q 'Top videos' '$ROOT/app/src/main/java/com/airplayr/app/ui/screens/StatsScreen.kt'" 'Top videos card present'
check "grep -q 's.listeningSeconds' '$ROOT/app/src/main/java/com/airplayr/app/ui/screens/StatsScreen.kt'" 'Statistics uses actual listening seconds'
check "grep -q 's.watchingSeconds' '$ROOT/app/src/main/java/com/airplayr/app/ui/screens/StatsScreen.kt'" 'Statistics uses actual watching seconds'
check "grep -q 'onOpenMedia' '$ROOT/app/src/main/java/com/airplayr/app/ui/screens/StatsScreen.kt'" 'Top media actions wired to native navigation callback'

# Phase 31 — Secondary screens
for f in LibraryScreen.kt ParityScreens.kt SearchScreen.kt HistoryScreen.kt FavoritesScreen.kt PlaylistsScreen.kt CollectionsScreen.kt; do
  check "grep -q 'AirPlayrScreenHeader' '$ROOT/app/src/main/java/com/airplayr/app/ui/screens/$f'" "AirPlayr header: $f"
done
check "grep -q 'AirPlayrMediaRow' '$ROOT/app/src/main/java/com/airplayr/app/ui/screens/LibraryScreen.kt'" 'Library uses AirPlayr media rows'
check "grep -q 'AirPlayrCard' '$ROOT/app/src/main/java/com/airplayr/app/ui/screens/ParityScreens.kt'" 'Parity screens use shared AirPlayr cards'
check "grep -q 'primaryContainer' '$ROOT/app/src/main/java/com/airplayr/app/ui/screens/ParityScreens.kt'" 'Secondary screens use AirPlayr surface hierarchy'

# Phase 32 — Non-video route inventory
NAV="$ROOT/app/src/main/java/com/airplayr/app/ui/AirPlayrApp.kt"
for route in home music videos settings player equalizer search history favorites playlists collections metadata diagnostics queue listening-watching-stats folders my-playlists music-playlist most-played for-you rediscover tuned-in rock-your-world smart-collections album artist genre music-folder folder open; do
  check "grep -q 'composable(\"$route' '$NAV'" "native route exists: $route"
done
check "grep -q 'composable(\"video/{id}\")' '$NAV'" 'video route remains native-only and separate'
check "! grep -Rqs 'Expo\|react-native' '$ROOT/app/src/main/java/com/airplayr/app/ui/screens/VideoScreen.kt'" 'native video screen has no Expo UI dependency'

# Card-by-card visual primitive gate
check "grep -q 'AirPlayrCard' '$ROOT/app/src/main/java/com/airplayr/app/ui/screens/HomeScreen.kt'" 'Home uses shared AirPlayr cards'
check "grep -q 'AirPlayrCard' '$ROOT/app/src/main/java/com/airplayr/app/ui/screens/StatsScreen.kt'" 'Stats uses shared AirPlayr cards'
check "grep -q 'AirPlayrCard' '$ROOT/app/src/main/java/com/airplayr/app/ui/screens/FavoritesScreen.kt'" 'Favorites uses shared AirPlayr cards'
check "grep -q 'AirPlayrCard' '$ROOT/app/src/main/java/com/airplayr/app/ui/screens/PlaylistsScreen.kt'" 'Playlists uses shared AirPlayr cards'
check "grep -q 'AirPlayrCard' '$ROOT/app/src/main/java/com/airplayr/app/ui/screens/CollectionsScreen.kt'" 'Collections uses shared AirPlayr cards'
check "grep -q 'AirPlayrFeatureCardTokens.radiusDp' '$ROOT/app/src/main/java/com/airplayr/app/ui/components/FeatureCard.kt'" 'FeatureCard uses AirPlayr radius token'
check "grep -q 'SmoochSans' '$ROOT/app/src/main/java/com/airplayr/app/ui/components/FeatureCard.kt'" 'FeatureCard uses AirPlayr typography'

# Freeze earlier green areas against Phase 27–29 baseline.
for rel in \
  app/src/main/java/com/airplayr/app/core/playback \
  app/src/main/java/com/airplayr/app/core/data/PlaybackStatsRepository.kt \
  app/src/main/java/com/airplayr/app/core/lyrics \
  app/src/main/java/com/airplayr/app/ui/screens/PlayerScreen.kt \
  app/src/main/java/com/airplayr/app/ui/screens/EqualizerScreen.kt \
  app/src/main/java/com/airplayr/app/ui/components/FeatureCard.kt \
  app/src/main/java/com/airplayr/app/ui/components/AirPlayrCard.kt \
  app/src/main/java/com/airplayr/app/ui/components/AirPlayrBottomDock.kt \
  app/src/main/java/com/airplayr/app/ui/components/MenuDrawer.kt \
  app/src/main/java/com/airplayr/app/ui/components/LiquidGlass.kt \
  app/src/main/java/com/airplayr/app/ui/theme \
  app/src/main/java/com/airplayr/app/MainActivity.kt; do
  python3 - "$BASE/$rel" "$ROOT/$rel" <<'PY'
from pathlib import Path
import hashlib,sys
a,b=Path(sys.argv[1]),Path(sys.argv[2])
def files(p):
    return {str(x.relative_to(p)): hashlib.sha256(x.read_bytes()).hexdigest() for x in p.rglob('*') if x.is_file()} if p.exists() else {}
if files(a) != files(b):
    raise SystemExit(1)
PY
  if [[ $? -eq 0 ]]; then echo "PASS: frozen green subsystem unchanged: $rel"; PASS=$((PASS+1)); else echo "FAIL: frozen green subsystem changed: $rel"; FAIL=$((FAIL+1)); fi
done

# Syntax balance check for changed Kotlin files.
for f in \
  app/src/main/java/com/airplayr/app/ui/AirPlayrApp.kt \
  app/src/main/java/com/airplayr/app/ui/screens/LibraryScreen.kt \
  app/src/main/java/com/airplayr/app/ui/screens/ParityScreens.kt \
  app/src/main/java/com/airplayr/app/ui/screens/StatsScreen.kt; do
  python3 - "$ROOT/$f" <<'PY' || exit 1
from pathlib import Path
import sys
s=Path(sys.argv[1]).read_text()
assert s.count('{')==s.count('}'), 'brace imbalance'
assert s.count('(')==s.count(')'), 'paren imbalance'
PY
  echo "PASS: Kotlin structural balance: $f"; PASS=$((PASS+1))
done

echo "SUMMARY: $PASS passed, $FAIL failed"
if [[ $FAIL -ne 0 ]]; then exit 1; fi
echo 'RESULT: PASS'
