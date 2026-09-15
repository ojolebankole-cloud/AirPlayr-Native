# AirPlayr Android Native — Phase 3 Implementation

Reference: original Expo AirPlayr archive supplied by the user.
Target: Android Native project only.

## Implemented

### Presentation foundation used by Phase 3
- Expo-matched AirPlayr font families remain wired into Compose typography.
- Central spacing, radius and elevation tokens remain in the native design system.
- Exact Expo bottom-dock icon vector geometry is available as native ImageVectors.

### Home
- Rebuilt the Home composition around the Expo reference hierarchy.
- AirPlayr rounded header with menu, search, queue and statistics actions.
- Quick-action row for History, Favorites, Most Played and Shuffle.
- Compact feature-card rail using the existing AirPlayr illustration assets and Expo-inspired gradients.
- Listening/watching statistics entry card.
- Recently-added music/video rails.
- Artwork-aware rows and a richer now-playing card.
- Native slide-in menu drawer with Music, Videos, Favorites, History, Queue, Statistics, Equalizer, Playlists, Folders and Settings destinations.

### Navigation / mini-player
- Replaced the stock Material NavigationBar UI with an AirPlayr-style rounded dock.
- Uses the exact Phase-1 custom AirPlayr navigation icon geometry.
- Adds the current-song mini-player above the dock whenever a track is active.

### Music
- Search field.
- Category chip navigation for Songs, Likes, Albums, Playlists, Artists, Genres and Folders.
- Sort toggle.
- Shuffle-all.
- Artwork-aware music rows.
- Queue insertion from each song.
- Integrated mini-player.

### Music playback screen
- Large artwork presentation.
- Current track / artist / album metadata.
- Favorite action backed by the native structured repository.
- Seek/progress controls.
- Previous/next.
- Play/pause.
- +/-10 second seek.
- Shuffle/repeat.
- Queue position.
- Lyrics sheet entry.
- Sleep timer sheet.
- Speed/pitch sheet.
- Track-details sheet.
- Playback-more sheet.
- Equalizer entry.

### Equalizer
- Master enable switch.
- Preset chips: Flat, Bass, Vocal, Treble.
- Ten-band equalizer controls.
- Bass, loudness and virtualizer controls.
- Persistent DSP state through the existing native DspRepository.
- Reset-to-flat action.

### Lyrics
- Native `LyricsService` with sequential provider fallback modeled on the Expo service.
- LRCLIB provider.
- NetEase provider.
- Kugou provider.
- lyrics.ovh provider.
- LRC parsing with timestamp sorting and duplicate timestamp merging.
- Confidence matching using title, artist, album, duration and synchronized-lyrics evidence.
- Device-local cached lyrics.
- Active synchronized lyric line highlighting.
- Tap a lyric line to seek playback.
- Refresh action.
- Plain-lyrics fallback.

### Listening / watching statistics
- Week / Month / All time period selector.
- Music/video mode.
- Summary cards for music plays, video plays, listening time, completed videos and favorites.
- Activity heatmap representation.
- Top-artist insight.
- Top song/video ranking list with artwork.

## Validation

Static source sanity checks were run on the edited project.

A full Gradle build could not be executed in this workspace because the Gradle wrapper needed to download Gradle 9.5.0 from services.gradle.org and outbound access is unavailable here.

Therefore this package is **source-level Phase 3 implementation complete for the edited native files, with GitHub Actions/native build verification still required**.

## Known remaining parity limits

This phase substantially advances the requested Home, navigation, Music, playback, Equalizer, statistics and Lyrics surfaces, but a true pixel/device acceptance pass is still required. The existing native backend also does not yet expose every Expo statistics activity metric (for example, exact tracked-session seconds) in a dedicated native statistics store, so the stats screen uses the currently available native playback-history/progress data rather than inventing unavailable values.
