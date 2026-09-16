# AirPlayr Android Native — Phase 9–11 Implementation Report

## Reference and target

- **Reference:** `AirPlayr-user-visible-fixed-2026-09-14.zip` (Expo project)
- **Target:** `AirPlayr-Android-Native-Phase6-8-Completed.zip` (native baseline)
- Both archives were unpacked and inventoried before implementation.
- Expo remains the behavioral/UI reference for Music and non-video screens. The video player itself was intentionally left native, per the project decision.

## Phase 9 — Lyrics

Implemented a native lyrics pipeline matching the reference workflow:

- Four online providers: LRCLIB, NetEase, Kugou and lyrics.ovh.
- Conservative title/artist/album/duration matching with a confidence score.
- LRC parsing into timed cues.
- Synchronized lyric highlighting based on the current playback position.
- Tapping a lyric cue seeks the music player.
- Cached lyrics stored locally per media id.
- Saved-lyrics index maintenance on save/delete.
- Lyrics sheet with fetch/refresh, edit and delete actions.
- Lyrics editor with:
  - online search
  - plain text entry
  - synced LRC entry
  - Android file import
  - clipboard paste
  - clear/save
- Plain lyrics remain scrollable while synced lyrics use tappable, highlighted rows.

### Known Phase 9 limitation

The native editor currently stores AirPlayr's lyric cache but does **not** embed edited lyrics back into the audio file's ID3/metadata. The existing native `MetadataRepository` does not expose a lyrics metadata field, so this is a deliberate implementation boundary rather than a claim of full embedded-metadata parity.

## Phase 10 — Home + statistics

Implemented persistent, real playback analytics using Room v3:

- `playback_events` for individual play events.
- `playback_sessions` for session duration/history.
- `playback_activity` for day/time-of-day buckets.
- Room migration 2 → 3 creates all analytics tables and indexes.
- Playback service sends play-start, forward-progress and session-finish signals.
- Statistics persist independently of the UI lifecycle.
- Forward playback is batched in memory and flushed approximately every five seconds to reduce database churn and ANR risk.
- Week / Month / All-time aggregation.
- Music / Videos mode.
- Six daily time buckets: Night, Early, Morn, Midday, Eve, Late.
- Seven-day activity grid for listening/watching heatmaps.
- Top artist.
- Top songs.
- Top videos.
- Genre totals and music/video splits.
- Listening streak.
- Average and longest tracked session.
- Actual listening time from forward playback deltas rather than a synthetic `playCount × fixed minutes` formula.
- Home Stats Card now uses the real tracked counters and is placed **after** the Home content rails, matching the Expo render order.

Home now exposes the reference sequence of content sections:

1. Header / quick actions / feature rail
2. Recently added songs
3. Recently played albums
4. Recent artists
5. Currently watching
6. Complete watching
7. Recently added videos
8. Recently played videos
9. Listening & watching statistics card
10. Current playback content when applicable

The time-of-day feature card now uses the reference phase titles and copy, and the feature rail is shuffled once per Home mount.

### Important Phase 10 limitation

Precise historical play-event reconstruction is impossible for plays that existed only in the pre-v3 native database. Those older records have `playCount`/`playedAt` but no per-play event timeline. New plays are tracked precisely going forward; old history is not falsely backfilled as exact session data.

## Phase 11 — Library secondary screens

Implemented/expanded:

- Favorites screen with artwork, saved ordering, play-all and individual playback.
- History screen with recent play counts and direct music/video actions.
- Playlists screen with create/open/delete flows.
- Playlist detail playback support through the existing native queue.
- Recent typed views for:
  - songs
  - albums
  - artists
  - videos added
  - videos played
- Music-first Smart Collections:
  - For you now
  - Recently played
  - Recently added
  - Most played
  - Favorites
  - Rediscover
  - Rock your world
- Collection rows now show real artwork/title/artist cards rather than only a count/list summary.
- Collection `Rock your world` no longer incorrectly uses the video-unwatched set; it is built from music listening signals.
- Existing browse screens remain available for albums/artists/genres/folders.
- Existing feature routes remain wired for For You, Most Played, Rediscover, Tuned In, Rock Your World, Smart Collections, Recent, Favorites, History and Playlists.

## Verification performed

### Full cross-phase static gates

- Phase 0–2 verifier: **PASS**
- Phase 3–5 verifier: **PASS**
- Phase 6–8 verifier: **PASS**
- Native-only verifier: **PASS**
- Native asset/feature parity verifier: **PASS**
- Phase 9–11 verifier: **PASS**

### Critical asset preservation

The previously established Phase 0 asset hash contract remains intact. The post-implementation check confirms the critical original feature-card/font assets remain exact SHA-256 matches.

### Compile-level syntax audit

A Kotlin compiler pass was run over the native source tree without the Android/Compose dependency classpath. No new syntax/parse/conflicting-overload errors were reported in the Phase 9–11 files. The command cannot serve as an Android build because the framework/dependency classpath is intentionally absent.

### Real Gradle build

Attempted:

```text
./gradlew --offline --no-daemon :app:compileDebugKotlin
```

Result: **not build-verified in this environment**. The Gradle wrapper attempted to fetch Gradle 9.5.0 from `services.gradle.org`, but the sandbox has no DNS/network access.

## Honest certification status

**Phase 9 source implementation:** ✅

**Phase 10 source implementation:** ✅

**Phase 11 source implementation:** ✅

**Phase 0–11 static verification:** ✅

**Original critical asset preservation:** ✅

**Android APK/AAB compilation:** ❌ not certified in this environment

**Physical-device regression:** ❌ not performed in this environment

**Pixel-level screenshot comparison:** ❌ not certified in this environment

**Video player migration:** intentionally excluded ✅

This project should therefore be described as **implemented and statically verified through Phase 11, awaiting a real Android build/device validation gate** rather than as a production-certified release.
