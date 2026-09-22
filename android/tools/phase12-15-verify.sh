#!/usr/bin/env bash
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
FAIL=0
ok(){ echo "PASS: $1"; }
bad(){ echo "FAIL: $1"; FAIL=1; }
for f in \
  app/src/main/java/com/airplayr/app/ui/components/MenuDrawer.kt \
  app/src/main/java/com/airplayr/app/ui/components/AirPlayrSheets.kt \
  app/src/main/java/com/airplayr/app/ui/screens/SettingsScreen.kt \
  app/src/main/java/com/airplayr/app/ui/metadata/MetadataScreen.kt \
  app/src/main/java/com/airplayr/app/core/metadata/MetadataRepository.kt \
  app/src/main/java/com/airplayr/app/core/data/preferences/PreferencesRepository.kt \
  app/src/main/java/com/airplayr/app/ui/navigation/AirPlayrNavViewModel.kt \
  app/src/main/java/com/airplayr/app/ui/playback/PlaybackController.kt \
  app/src/main/java/com/airplayr/app/MainActivity.kt; do
  test -f "$ROOT/$f" && ok "$f exists" || bad "$f missing"
done
grep -q 'onAbout: () -> Unit' "$ROOT/app/src/main/java/com/airplayr/app/ui/components/MenuDrawer.kt" && grep -q 'onSupport: () -> Unit' "$ROOT/app/src/main/java/com/airplayr/app/ui/components/MenuDrawer.kt" && ok 'Drawer about/support callbacks are explicit' || bad 'Drawer callback contract missing'
grep -q 'onAbout()' "$ROOT/app/src/main/java/com/airplayr/app/ui/components/MenuDrawer.kt" && grep -q 'onSupport()' "$ROOT/app/src/main/java/com/airplayr/app/ui/components/MenuDrawer.kt" && ok 'Drawer about/support actions wired' || bad 'Drawer about/support actions not wired'
# settings contract
for k in resumeMode replayGainMode subtitleSize subtitleColor subtitleDelayMs gestureDoubleTapSeek gridColumns showNowPlayingNotification allowsPictureInPicture autoEnterPiPOnBackground savePlaybackHistory showContinueWatching showRecentlyPlayed; do grep -q "$k" "$ROOT/app/src/main/java/com/airplayr/app/core/data/preferences/PreferencesRepository.kt" && ok "Preference $k" || bad "Preference $k missing"; done
grep -q 'fun applyToMediaStore' "$ROOT/app/src/main/java/com/airplayr/app/core/metadata/MetadataRepository.kt" && grep -q 'saveOverride' "$ROOT/app/src/main/java/com/airplayr/app/core/metadata/MetadataRepository.kt" && ok 'Metadata write + persistent override' || bad 'Metadata persistence missing'
grep -q 'ACTION_SEND' "$ROOT/app/src/main/java/com/airplayr/app/ui/screens/PlayerScreen.kt" && ok 'Music share intent' || bad 'Music share missing'
grep -q 'createDeleteRequest' "$ROOT/app/src/main/java/com/airplayr/app/ui/screens/PlayerScreen.kt" && ok 'MediaStore delete request' || bad 'MediaStore delete missing'
grep -q 'android.intent.action.MEDIA_BUTTON' "$ROOT/app/src/main/AndroidManifest.xml" && grep -q 'mimeType="audio/\*"' "$ROOT/app/src/main/AndroidManifest.xml" && ok 'Android media intent integration' || bad 'Android media intent integration incomplete'
grep -q 'playExternal' "$ROOT/app/src/main/java/com/airplayr/app/ui/playback/PlaybackController.kt" && grep -q 'externalMedia' "$ROOT/app/src/main/java/com/airplayr/app/ui/navigation/AirPlayrNavViewModel.kt" && ok 'Open-with audio/video to native player' || bad 'Open-with flow incomplete'
# metadata fields exposed to MediaStore
grep -q 'ALBUM_ARTIST' "$ROOT/app/src/main/java/com/airplayr/app/core/metadata/MetadataRepository.kt" && grep -q 'TRACK' "$ROOT/app/src/main/java/com/airplayr/app/core/metadata/MetadataRepository.kt" && grep -q 'YEAR' "$ROOT/app/src/main/java/com/airplayr/app/core/metadata/MetadataRepository.kt" && ok 'Metadata fields include album artist/track/year' || bad 'Metadata fields incomplete'
# no obvious placeholder implementation in milestone files
if grep -RniE '^\s*(TODO|TODO:)|not implemented|placeholder implementation' "$ROOT/app/src/main/java/com/airplayr/app/ui/components/MenuDrawer.kt" "$ROOT/app/src/main/java/com/airplayr/app/ui/screens/SettingsScreen.kt" "$ROOT/app/src/main/java/com/airplayr/app/core/metadata/MetadataRepository.kt" >/tmp/m5_todo; then bad 'Milestone files contain TODO/placeholder markers'; else ok 'No TODO/placeholder markers in milestone files'; fi
if [ "$FAIL" -eq 0 ]; then echo 'RESULT: PASS'; else echo 'RESULT: FAIL'; exit 1; fi
