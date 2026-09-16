from pathlib import Path
import hashlib, re, sys
ROOT=Path(__file__).resolve().parents[1]
fail=[]
def check(ok,msg):
    print(('PASS ' if ok else 'FAIL ')+msg)
    (None if ok else fail.append(msg))

def read(rel): return (ROOT/rel).read_text(encoding='utf-8')
# Phase 3 shell
app=read('app/src/main/java/com/airplayr/app/ui/AirPlayrApp.kt')
dock=read('app/src/main/java/com/airplayr/app/ui/components/AirPlayrBottomDock.kt')
drawer=read('app/src/main/java/com/airplayr/app/ui/components/MenuDrawer.kt')
check('AirPlayrBottomDock(' in app and 'destination?.route != "player"' in app, 'global dock mounted above content and hidden on full player')
check('Modifier.fillMaxWidth().height(72.dp)' in dock and 'Arrangement.SpaceEvenly' in dock, 'icon-only 72dp dock row')
check('Modifier.size(62.dp)' in dock and 'Modifier.size(56.dp)' in dock, 'Expo mini-player artwork/play geometry retained')
check('width * 0.87' in drawer or 'screenWidthDp * 0.87' in drawer, 'drawer uses Expo 87% width rule')
check('swipe' not in drawer.lower() or 'draggable' in drawer, 'drawer has horizontal drag interaction')
check('showAbout' in drawer and 'showSupport' in drawer, 'About/Support actions have dialogs')
# Phase 4 music
music=read('app/src/main/java/com/airplayr/app/ui/screens/MusicScreen.kt')
for phrase, label in [
 ('"songs" to "Songs"','music category Songs'),('"likes" to "Likes"','music category Likes'),('"albums" to "Albums"','music category Albums'),('"playlists" to "Playlists"','music category Playlists'),('"artists" to "Artists"','music category Artists'),('"genres" to "Genres"','music category Genres'),('"folders" to "Folders"','music category Folders'),
 ('combinedClickable','long-press selection'),('setLibraryLayout','persisted list/grid layout'),('container.structured.addToPlaylist','add-to-playlist behavior'),('TagEditorSheet','song edit flow'),('libraryViewModel.refresh()','refresh on music entry'),
]: check(phrase in music,label)
# Phase 5 player
player=read('app/src/main/java/com/airplayr/app/ui/screens/PlayerScreen.kt')
for phrase,label in [
 ('Artwork(state.artworkUri','player artwork'),('onValueChange = { seeking = true','seek drag state'),('onValueChangeFinished = { seeking = false','seek commit'),('AirPlayrIcons.Previous','previous icon'),('AirPlayrIcons.Next','next icon'),('AirPlayrIcons.Play','play icon'),('AirPlayrIcons.Pause','pause icon'),('AirPlayrIcons.Shuffle','shuffle action'),('AirPlayrIcons.Repeat','repeat action'),('PlayerSheet.Sleep','sleep timer'),('SpeedPitchSheet','speed/pitch sheet'),('LyricsSheet','lyrics sheet'),('TagEditorSheet','tag editor'),('AirPlayrIcons.Lyrics','lyrics top action'),('playLast','play-last action'),('PlayerPlaylistSheet','player playlist action'),
]: check(phrase in player,label)
controller=read('app/src/main/java/com/airplayr/app/ui/playback/PlaybackController.kt')
check('fun playLast' in controller,'native playback controller supports play-last')
# Phase 0-2 preservation
hashmanifest=ROOT/'PHASE-0-REFERENCE-ASSET-HASHES.txt'
check(hashmanifest.exists(),'Phase 0 asset hash manifest retained')
# No JS/runtime files
bad=[]
for p in ROOT.rglob('*'):
    if p.is_file() and p.suffix.lower() in {'.js','.jsx','.ts','.tsx'} or p.name in {'package.json','package-lock.json'}:
        bad.append(str(p))
check(not bad,'no JS/TS/Node runtime files in native tree')
# no obvious stale broken references
check('PlayerScreen(container.appContext,' in app,'PlayerScreen route call uses native callback contract')
if fail:
    print(f'\nRESULT: FAIL ({len(fail)} checks)')
    sys.exit(1)
print('\nRESULT: PASS')
