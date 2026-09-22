#!/usr/bin/env bash
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
fail=0
check(){ if eval "$1" >/dev/null 2>&1; then echo "PASS: $2"; else echo "FAIL: $2"; fail=1; fi; }
check "grep -q 'featureSeed = remember' '$ROOT/app/src/main/java/com/airplayr/app/ui/screens/HomeScreen.kt'" 'Home feature shuffle is stable per mount'
check "grep -q 'Recently played albums' '$ROOT/app/src/main/java/com/airplayr/app/ui/screens/HomeScreen.kt'" 'Home recent album rail present'
check "grep -q 'Recent Artists' '$ROOT/app/src/main/java/com/airplayr/app/ui/screens/HomeScreen.kt'" 'Home recent artist rail present'
check "grep -q 'Currently watching' '$ROOT/app/src/main/java/com/airplayr/app/ui/screens/HomeScreen.kt'" 'Home currently-watching rail present'
check "grep -q 'StatsCardPreview' '$ROOT/app/src/main/java/com/airplayr/app/ui/screens/HomeScreen.kt'" 'Home stats card present'
for f in SearchScreen.kt HistoryScreen.kt FavoritesScreen.kt PlaylistsScreen.kt CollectionsScreen.kt ParityScreens.kt; do
  check "grep -q 'AirPlayrScreenHeader' '$ROOT/app/src/main/java/com/airplayr/app/ui/screens/$f'" "secondary screen header styling: $f"
done
check "grep -q 'AirPlayrMediaRow' '$ROOT/app/src/main/java/com/airplayr/app/ui/screens/SearchScreen.kt'" 'Search uses AirPlayr media rows'
check "grep -q 'AirPlayrMediaRow' '$ROOT/app/src/main/java/com/airplayr/app/ui/screens/HistoryScreen.kt'" 'History uses AirPlayr media rows'
check "grep -q 'AirPlayrMediaRow' '$ROOT/app/src/main/java/com/airplayr/app/ui/screens/FavoritesScreen.kt'" 'Favorites uses AirPlayr media rows'
check "grep -q 'AirPlayrCard' '$ROOT/app/src/main/java/com/airplayr/app/ui/screens/PlaylistsScreen.kt'" 'Playlists uses AirPlayr cards'
check "grep -q 'AirPlayrCard' '$ROOT/app/src/main/java/com/airplayr/app/ui/screens/CollectionsScreen.kt'" 'Collections uses AirPlayr cards'
check "grep -q 'lyricsState.*idle' '$ROOT/app/src/main/java/com/airplayr/app/ui/components/AirPlayrSheets.kt'" 'Lyrics has idle/reference state'
check "grep -q 'getCached(state.currentId.orEmpty())' '$ROOT/app/src/main/java/com/airplayr/app/ui/components/AirPlayrSheets.kt'" 'Lyrics loads saved lyrics before network fetch'
check "grep -q 'Fetch Lyrics' '$ROOT/app/src/main/java/com/airplayr/app/ui/components/AirPlayrSheets.kt'" 'Lyrics fetch action card present'
check "grep -q 'Saved Lyrics' '$ROOT/app/src/main/java/com/airplayr/app/ui/components/AirPlayrSheets.kt'" 'Lyrics saved action present'
check "grep -q 'activeIndex' '$ROOT/app/src/main/java/com/airplayr/app/ui/components/AirPlayrSheets.kt'" 'Lyrics active line tracking present'
check "grep -q 'onSeek(cue.start)' '$ROOT/app/src/main/java/com/airplayr/app/ui/components/AirPlayrSheets.kt'" 'Lyrics line seek preserved'
check "grep -q 'Video' '$ROOT/app/src/main/java/com/airplayr/app/ui/screens/HomeScreen.kt'" 'Home video rails retained without video-player migration'
if [[ $fail -ne 0 ]]; then echo 'RESULT: FAIL'; exit 1; fi
echo 'RESULT: PASS'
