#!/usr/bin/env bash
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
SRC="$ROOT/app/src/main/java"

echo "[phase3] Kotlin files: $(find "$SRC" -name '*.kt' | wc -l)"
echo "[phase3] checking required AirPlayr assets"
for f in app/src/main/res/font/open_sans_regular.ttf app/src/main/res/font/open_sans_medium.ttf app/src/main/res/font/open_sans_bold.ttf app/src/main/res/font/smooch_sans_semibold.ttf app/src/main/res/font/oswald_medium.ttf app/src/main/res/font/dancing_script_regular.ttf app/src/main/res/font/shadows_into_light_regular.ttf app/src/main/res/font/caacupe_one_regular.ttf; do
  test -f "$ROOT/$f"
done
for f in app/src/main/res/drawable-nodpi/airplayr_logo.png app/src/main/res/drawable-nodpi/playing_music_bro.png app/src/main/res/drawable-nodpi/recording_pana.png app/src/main/res/drawable-nodpi/playlist_pana.png app/src/main/res/drawable-nodpi/media_player_bro.png app/src/main/res/drawable-nodpi/headphone_amico.png app/src/main/res/drawable-nodpi/music_cuate.png app/src/main/res/drawable-nodpi/dj_boy_cuate.png; do
  test -f "$ROOT/$f"
done

echo "[phase3] checking the dock no longer uses stock Material NavigationBarItem"
! grep -q 'NavigationBarItem(' "$SRC/com/airplayr/app/ui/AirPlayrApp.kt"

echo "[phase3] static checks passed"
