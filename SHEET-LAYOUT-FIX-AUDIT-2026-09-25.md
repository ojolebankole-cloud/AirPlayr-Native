# AirPlayr — Bottom-sheet overflow fix audit

Date: 2026-09-25

## Problem investigated

Long action sheets were allowed to grow beyond the usable bottom-sheet viewport. On shorter displays, the last actions could be clipped instead of being reachable by scrolling. The most visible example was the Music Player's three-dot **Playback options** sheet.

## Implementation

The fix was applied at the shared sheet/action infrastructure level and to standalone sheets that do not use that infrastructure.

### Shared infrastructure

- `ui/components/AirPlayrSheets.kt`
  - `AirPlayrSheetScaffold` now owns a bounded vertical scroll container for its sheet body.
  - This fixes the Player **Playback options**, playlist, details, lyrics editor and tag-editor sheets that use the scaffold.
  - Existing fixed-height lyrics content remains independently scrollable where appropriate.

- `ui/components/AirPlayrSecondaryComponents.kt`
  - `AirPlayrPremiumMediaActionSheet` is now vertically scrollable.
  - `AirPlayrBulkSelectionSheet` is now vertically scrollable.
  - `ActionGroup` is now vertically scrollable for standalone overflow/action sheets, including playlist target selection.

### Standalone sheets

- `ui/components/HistoryFavoriteActionSheet.kt`
  - Add-to-playlist sheet is scrollable for arbitrarily long playlist lists.
  - Details, tag editor and lyrics sheets are scrollable.

- `ui/components/FeatureCollectionActionSheet.kt`
  - Feature collection action sheet is scrollable.

- `ui/screens/VideoScreen.kt`
  - Audio-track selection sheet is scrollable when a video exposes many audio streams.

- `ui/screens/ParityScreens.kt`
  - Folder sorting and video details sheets are scrollable.

## Sheets audited and intentionally left unchanged

Some sheets already use a `LazyColumn`, `LazyRow`, or an existing vertical scroll container and therefore did not need another scroll container. Examples include:

- Music queue
- Video queue
- Vault content
- What's New
- About content
- Cast picker where its device/content list is lazy
- Subtitle appearance controls (horizontal lazy rows)

Short selection sheets such as accent/sort sheets were also inspected; their current content fits the sheet and does not need a nested scroll container.

## Important nested-scroll check

The shared scaffold is the scroll owner for sheets built with `AirPlayrSheetScaffold`. A redundant nested vertical scroll was removed from the Tag Editor so it does not fight the parent scroll container. The Music genre sheet likewise relies on the shared `ActionGroup` scroll behavior rather than creating a nested vertical scroller.

## Verification performed

- Re-scanned all `ModalBottomSheet` call sites under `ui/`.
- Checked which sheets use `verticalScroll`, `LazyColumn`, or an existing scrollable container.
- Checked the Player three-dot sheet specifically: all five action groups are now inside the scrollable `AirPlayrSheetScaffold` body.
- Checked the long individual-media action sheet: its complete action list is inside a scrollable body.
- Checked arbitrary-length playlist sheets: playlist/action lists can now be reached by scrolling.
- No Android compilation result is claimed because this environment cannot download the project's Gradle 9.5.0 distribution from `services.gradle.org` (network/DNS unavailable).
