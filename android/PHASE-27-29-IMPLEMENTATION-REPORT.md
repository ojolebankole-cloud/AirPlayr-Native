# AirPlayr Android Native — Phases 27–29 Implementation Report

## Scope
Reference project: Expo AirPlayr `user-visible-fixed-2026-09-14`.
Target project: Android Native after Phases 23–26.

Video-player UI and video-player behavior remain intentionally outside parity scope.

## Phase 27 — Home fidelity
- Kept the existing Home data/repository/navigation implementation.
- Matched the Expo Home header proportions more closely: 54dp pill, 27dp radius, 24dp horizontal breathing room, AirPlayr display typography.
- Preserved the existing cast action and search/menu behavior.
- Refined Quick Actions to Expo-style 46dp circular action surfaces, 64dp action width, 11sp Smooch Sans labels and reference spacing.
- Refined the Home stats preview card typography and spacing to the Expo StatsCard contract.
- Refined Home rail spacing and card typography for songs, albums, artists and video content cards.
- Fixed feature-card ordering stability: one shuffle seed is created per Home mount and reused, so Compose recomposition/time-card updates do not reorder cards unexpectedly.
- Kept all existing Home rails: recently added songs, recently played albums, recent artists, currently watching, completed watching, recently added videos and recently played videos.
- Did not copy Expo's video player UI.

## Phase 28 — Secondary-screen visual parity
- Added reusable `AirPlayrSecondaryComponents` for shared screen headers, media rows, section empty states and consistent AirPlayr typography.
- Updated Search to AirPlayr-style search shell, filter chips, result headings, empty states and media rows while retaining the existing native search repository behavior.
- Updated History to use AirPlayr typography, artwork-led rows, album rail presentation and the existing playback/data source.
- Updated Favorites to use AirPlayr cards, artwork rows and existing favorites/playback sources.
- Updated Playlists to use AirPlayr cards, typography and artwork-style icon containers while retaining create/delete/open behavior.
- Updated Smart Collections to use the same AirPlayr card/header/artwork language while retaining the existing collection loaders/routes.
- Updated the shared `ParityScreens` presentation used by recent, browse, folder, playlist and collection detail/index screens so they inherit the new AirPlayr header/row/empty-state system.
- No repository/data-layer rewrite was introduced.

## Phase 29 — Lyrics UI parity
- Changed LyricsSheet startup behavior to load saved lyrics first, matching the Expo state model (`idle` until cached/available lyrics exist) instead of automatically forcing a network fetch when the sheet opens.
- Preserved explicit Fetch Lyrics, Edit Lyrics and Saved Lyrics actions.
- Preserved delete/reset behavior.
- Preserved synced-line seeking.
- Added AirPlayr-style action cards, icon containers, typography and state presentation.
- Preserved loading/error/empty/plain/synced lyric states.
- Kept the existing lyrics service, provider logic, parser, cache and editor implementation untouched.

## Regression protection
Only these application files differ from the Phase 23–26 baseline:
- `HomeScreen.kt`
- `SearchScreen.kt`
- `HistoryScreen.kt`
- `FavoritesScreen.kt`
- `PlaylistsScreen.kt`
- `CollectionsScreen.kt`
- `ParityScreens.kt`
- `AirPlayrSheets.kt`
- added `AirPlayrSecondaryComponents.kt`

No `res/` files changed.
No playback/audio-engine source changed.
No startup source changed.
No statistics/metadata source changed.
No video-player source was changed.

## Verification
- Phase 0–2 verifier: PASS
- Phase 3 static verifier: PASS
- Phase 3–5 verifier: PASS
- Phase 6–8 verifier: PASS
- Phase 9–11 verifier: PASS
- Phase 12–15 verifier: PASS
- Native-only verifier: PASS
- Asset/feature parity verifier: PASS
- Phase 23–26 verifier: PASS
- New Phase 27–29 focused verifier: PASS
- ZIP integrity: PASS

## Honest limitation
The environment cannot download Gradle 9.5.0 from `services.gradle.org`, so an actual Gradle compile/release APK build could not be completed here. Therefore this phase is source/static verified, not physical-device certified.
