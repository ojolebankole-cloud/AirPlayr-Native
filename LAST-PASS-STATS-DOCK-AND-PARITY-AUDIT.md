> **Historical document notice (2026-09-18):** This document predates the current UI repair pass. Its earlier PASS statements were not treated as source-of-truth. The Kotlin/resource tree in the submitted ZIP was re-audited directly; see `UI-FIXES-HONEST-AUDIT-2026-09-18.md` for the verified current state.

# AirPlayr — Final Stats/Dock Pass + Full Parity Cross-Check

Date: 2026-09-17

Reference project: previous Expo AirPlayr (`AirPlayr-user-visible-fixed-2026-09-14.zip`)
Working project: current Android-native AirPlayr (`AirPlayr-Native-feature-screens-fix.zip`)

## Scope of this pass

Only these new targets were modified:

1. Home — Listening & watching stats card.
2. Bottom dock — icon appearance and icon press behavior only.

The rest of the dock was intentionally left untouched.

## Listening & watching stats card

The Home card was brought to the Expo component contract:

- `YOUR LIBRARY` label with 12sp / 16sp medium-weight system typography and 1.6sp letter spacing.
- `Listening & watching stats` uses Smooch Sans at 27sp / 33sp with 6dp top spacing.
- 2 x 2 stat grid rather than a single four-column row.
- 27sp / 32sp bold numeric values and 11sp / 14sp medium labels.
- 24dp card content padding.
- 18dp horizontal card margin.
- 24dp top card spacing.
- 32dp corner radius.
- Material/Android shadow treatment corresponding to the Expo 18dp blur + 8dp Y offset + elevation 6 treatment.
- Press opacity 0.94 and button semantics.
- `LISTENING TIME*` uses all-time forward-playback tracking rather than a generic monthly summary.
- Music/video play counts come from the Expo-equivalent 100-entry playback-history window.
- Top track and top video are derived from that same history window.
- `VIDEOS COMPLETED` uses the native completion-event support already present in this project so repeat completions can be counted rather than only counting the current `completed` flag.
- Expo explanatory note is reproduced.
- Divider and footer match the Expo spacing hierarchy.
- `TOP TRACK` and `TOP VIDEO` values use system medium/semibold typography matching the Expo typography tokens.
- Card tap navigates to `listening-watching-stats`.
- Pressed state is handled without a native ripple, matching the Expo Pressable behavior.

## Bottom dock icons

Only the icon layer in `AirPlayrBottomDock.kt` was changed.

- Native AirPlayr SVG-equivalent vector geometry is used rather than Material icon approximations.
- 30dp icon size and 28 x 28 viewport match the Expo `AirIcon` components.
- Home, Music, Video and Settings icons are always tinted with the AirPlayr/Material-You `primary` color, whether selected or unselected.
- Icon press feedback is opacity 0.68, matching Expo.
- Existing dock dimensions, mini-player, navigation arrangement and surface were not changed in this pass.
- Accessibility role remains tab with selected state.

## Time-of-day refresh correction

During the final cross-check the Expo Home source was re-read. It explicitly updates the time card every 30 seconds (`setInterval(updateTimeCard, 30000)`). The native Home implementation therefore keeps a 30-second lifecycle-scoped refresh as well. This is intentional parity, not an unrelated timer.

## Earlier authorized areas rechecked

### Search your music / Home header
Source-level checks confirm:

- 10-second AIR PLAYR -> `Search your music` transition.
- Startup media-scan progress is connected to the native scan state.
- Search is blocked during the visible startup scan state, matching the reference behavior.
- Menu and Cast remain wired.

### Search screen
Source-level checks confirm the native screen has the intended filters, debounce, recent-content behavior, result interactions, item overflow sheet, navigation and app-wide snackbar channel.

### History / Favorites
The requested UI/actions and real Room-backed favorite state remain in the current native project.

However, the final cross-check found one known behavior mismatch in the current native implementations: video items in the History/Favorites overflow sheets report that video queue actions are unavailable, while the Expo app passes video items through its shared QueueController. These earlier screens were intentionally not modified in this final stats/dock-only pass.

### Feature-card home rail / destination screens
The Home feature rail is wired to native routes and uses the Material-You dynamic palette. The final cross-check found that the native destination implementation is still a shared/generic `FeatureCollectionScreen`, while the Expo reference uses dedicated implementations for Rock Your World, Rediscover, Tuned In, For You Now and My Playlists. Therefore those destination screens should NOT be described as pixel/behavior exact based on source inspection alone.

This is a material parity finding left unchanged because the current instruction restricted this pass to the stats card and bottom-dock icon.

## Verification performed

- Re-read the two ZIP source trees and the specific Expo reference components/data paths.
- Compared the Expo `StatsCard`, Home stats derivation, `ListeningStats`, `PlaybackStore` completion behavior and `AirPlayrBottomDock` against native equivalents.
- Checked that the stats card navigation route exists in the native NavHost.
- Checked that the bottom-dock vector definitions are present and referenced by the dock.
- Ran whitespace/diff hygiene checks.
- Checked ZIP integrity after packaging.

## Build / device verification limitation

A complete Android Gradle build was not possible in this environment because Gradle 9.5 is not cached and the wrapper distribution could not be retrieved from `services.gradle.org`. A physical-device/pixel-level visual comparison therefore has NOT been claimed.

This report deliberately distinguishes source-level parity from device-level visual verification.
