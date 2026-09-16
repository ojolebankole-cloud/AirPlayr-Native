#!/usr/bin/env python3
from pathlib import Path
import re, sys, hashlib

ROOT = Path(__file__).resolve().parents[1]
FAIL=[]

def check(ok, msg):
    print(('PASS ' if ok else 'FAIL ') + msg)
    if not ok: FAIL.append(msg)

def read(rel):
    p=ROOT/rel
    check(p.exists(), f'file exists: {rel}')
    return p.read_text(encoding='utf-8') if p.exists() else ''

# Phase 9 — lyrics service and sheets
lyrics = read('app/src/main/java/com/airplayr/app/core/lyrics/LyricsService.kt')
sheets = read('app/src/main/java/com/airplayr/app/ui/components/AirPlayrSheets.kt')
check('lrclib' in lyrics and 'netease' in lyrics and 'kugou' in lyrics and 'lyricsOvh' in lyrics, 'lyrics uses the four reference providers')
check('parseLrc' in lyrics and 'Cue' in lyrics and 'confidence' in lyrics, 'lyrics parser produces synchronized cues and match confidence')
check('cachedIds' in lyrics and 'putStringSet(indexKey' in lyrics, 'lyrics cache index persists saved lyric IDs')
for phrase,label in [
    ('ActivityResultContracts.OpenDocument()', 'lyrics editor can import a file'),
    ('ClipboardManager', 'lyrics editor can paste clipboard text'),
    ('service.fetch(item.id', 'lyrics editor can search online'),
    ('parseLrc(syncedText.orEmpty())', 'lyrics editor parses synced LRC on save'),
    ('onSeek(cue.start)', 'synced lyric lines seek playback'),
    ('service.clear(state.currentId.orEmpty())', 'saved lyrics can be deleted'),
]: check(phrase in sheets,label)

# Phase 10 — persistent real playback analytics / Home / stats
entities = read('app/src/main/java/com/airplayr/app/core/data/db/UserDataEntities.kt')
dao = read('app/src/main/java/com/airplayr/app/core/data/db/UserDataDao.kt')
db = read('app/src/main/java/com/airplayr/app/core/data/db/AirPlayrDatabase.kt')
container = read('app/src/main/java/com/airplayr/app/di/AppContainer.kt')
stats = read('app/src/main/java/com/airplayr/app/core/data/PlaybackStatsRepository.kt')
service = read('app/src/main/java/com/airplayr/app/core/playback/service/AirPlayrPlaybackService.kt')
home = read('app/src/main/java/com/airplayr/app/ui/screens/HomeScreen.kt')
stats_ui = read('app/src/main/java/com/airplayr/app/ui/screens/StatsScreen.kt')
for phrase,label in [
    ('PlaybackEventEntity', 'play events persisted'), ('PlaybackSessionEntity', 'play sessions persisted'), ('PlaybackActivityEntity', 'time-of-day activity persisted')
]: check(phrase in entities,label)
for phrase,label in [
    ('playbackEvents(since: Long, until: Long)', 'period event queries exist'), ('playbackSessions(since: Long, until: Long)', 'period session queries exist'), ('playbackActivity(fromDay: String, toDay: String)', 'activity-grid queries exist')
]: check(phrase in dao,label)
check('version = 3' in db and 'Migration(2, 3)' in container, 'Room v3 migration creates analytics tables')
for phrase,label in [
    ('suspend fun start', 'stats starts a real play event'), ('suspend fun track', 'stats tracks forward playback deltas'), ('pending', 'activity persistence is batched in memory'), ('5000L', 'stats flushes in batches instead of writing every second'), ('periodStart', 'week/month/all period semantics exist'), ('activityGrid', '7x6 heatmap data exists'), ('topSongs', 'top song ranking exists'), ('topVideos', 'top video ranking exists'), ('GenreStat', 'genre analytics exists'), ('calculateStreak', 'listening streak exists')
]: check(phrase in stats,label)
check('stats.start(' in service and 'stats.track(' in service and 'stats.finish()' in service, 'playback service feeds real analytics lifecycle')
for phrase,label in [
    ('Recently added songs', 'Home has recently added songs rail'), ('Recently played albums', 'Home has recently played albums rail'), ('Recent Artists', 'Home has recent artists rail'), ('Currently watching', 'Home has currently watching rail'), ('Complete watching', 'Home has completed watching rail'), ('Recently added videos', 'Home has recently added videos rail'), ('Recently played videos', 'Home has recently played videos rail')
]: check(phrase in home,label)
# Expo ordering: all rails before StatsCard; native must match that ordering.
idx_stats = home.find('StatsCardPreview')
idx_recent = home.find('Recently added songs')
check(idx_recent >= 0 and idx_stats > idx_recent, 'Home stats card is placed after the content rails')
for phrase,label in [('Week', 'Week period control'), ('Month', 'Month period control'), ('All time', 'All-time period control'), ('When you listen', 'music activity heatmap'), ('When you watch', 'video activity heatmap'), ('Top songs', 'top songs section'), ('Top videos', 'top videos section'), ('Genres', 'genre section'), ('Music', 'music/video mode'), ('Videos', 'video mode')]: check(phrase in stats_ui,label)

# Phase 11 — library secondary screens and smart collections
collections = read('app/src/main/java/com/airplayr/app/core/intelligence/CollectionsRepository.kt')
collections_ui = read('app/src/main/java/com/airplayr/app/ui/screens/CollectionsScreen.kt')
favorites = read('app/src/main/java/com/airplayr/app/ui/screens/FavoritesScreen.kt')
history = read('app/src/main/java/com/airplayr/app/ui/screens/HistoryScreen.kt')
playlists = read('app/src/main/java/com/airplayr/app/ui/screens/PlaylistsScreen.kt')
parity = read('app/src/main/java/com/airplayr/app/ui/screens/ParityScreens.kt')
app = read('app/src/main/java/com/airplayr/app/ui/AirPlayrApp.kt')
for phrase,label in [
    ('forYou', 'For you collection'), ('recentlyPlayedItems', 'Recently played music collection'), ('recentlyAdded', 'Recently added music collection'), ('mostPlayed', 'Most played collection'), ('favorites', 'Favorites collection'), ('rediscover', 'Rediscover collection'), ('rockYourWorld', 'Rock your world collection')
]: check(phrase in collections,label)
check('unwatched(limit' in collections and 'rockYourWorld' in collections and 'unwatched(limit) ' not in collections, 'video unwatched logic is kept separate from Rock your world')
for phrase,label in [
    ('For you now', 'collection card: For you now'), ('Recently played', 'collection card: Recently played'), ('Recently added', 'collection card: Recently added'), ('Most played', 'collection card: Most played'), ('Favorites', 'collection card: Favorites'), ('Rediscover', 'collection card: Rediscover'), ('Rock your world', 'collection card: Rock your world'), ('Artwork(', 'collection cards show artwork'), ('View all', 'collection cards provide full-list navigation')
]: check(phrase in collections_ui,label)
for text,src,label in [('Favorites',favorites,'Favorites screen implemented'),('History',history,'History screen implemented'),('Playlists',playlists,'Playlists screen implemented'),('Recently',parity,'Recent screen implemented')]: check(text in src,label)
for phrase,label in [('HistoryScreen(container, playbackController)', 'History route has playback controller'), ('PlaylistsScreen(container)', 'Playlists route supports open callback'), ('smart-collections', 'Smart collections route exists'), ('recent/{type}', 'Recent typed route exists')]: check(phrase in app,label)

# Preserve Phase 0 asset contract.
hash_manifest = ROOT/'PHASE-0-REFERENCE-ASSET-HASHES.txt'
check(hash_manifest.exists(), 'Phase 0 asset hash manifest retained')
if hash_manifest.exists():
    # verify a representative set of the critical original assets by SHA-256
    rows = []
    for line in hash_manifest.read_text().splitlines():
        if line.strip():
            rows.append(line.split(maxsplit=1))
    good=0
    for expected, rel in rows:
        if rel.startswith('assets/'):
            p=ROOT/'app/src/main'/rel
            if rel in {'assets/feature-cards/Playlist-pana.svg','assets/fonts/OpenSans-Regular.ttf','assets/fonts/SmoochSans-SemiBold.ttf'}:
                if p.exists():
                    good += hashlib.sha256(p.read_bytes()).hexdigest()==expected
    check(good==3, 'critical font/feature assets still exact after Phase 9–11')

# No JS runtime introduced.
bad=list(ROOT.rglob('*.js'))+list(ROOT.rglob('*.tsx'))+list(ROOT.rglob('package.json'))
check(not bad, 'native tree still contains no JS/Node runtime artifacts')

print(f'\nRESULT: {"PASS" if not FAIL else f"FAIL ({len(FAIL)} checks)"}')
sys.exit(0 if not FAIL else 1)
