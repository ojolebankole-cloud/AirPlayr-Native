# AirPlayr Native — Focused Migration Audit (Home Header + Search)

Source packages inspected:
- First ZIP: current Android-native AirPlayr (`AirPlayr-migration.zip`)
- Second ZIP: previous Expo AirPlayr (`AirPlayr-user-visible-fixed-2026-09-14.zip`)

Scope enforced:
- Home header / "Search your music" card behavior
- Search screen UI + navigation + search behavior + results + initial-state/empty handling
- Search three-dots action sheet and connected actions
- Global snackbar feedback infrastructure
- Media-scan counting used by the Home header

Expo reference behavior inspected:
- HomeHeader.js: 10-second AIR PLAYR -> Search your music transition; startup scan progress text/count; menu/cast controls.
- Home tab index.js: startup scan state and progress flow.
- search.js: 180ms debounce; seven filters; recent artists + recent media on open; category aggregation; token/accent-normalized matching; row overflow menu; connected actions.
- history.js: MediaActionSheet structure/actions.
- QueueFeedback.js: global snackbar behavior and 4-second timeout.
- Supporting media scan/history/storage services.

Native changes:
1. Home header now has the Expo-style rounded surface, menu/cast controls, delayed branding/search prompt, and scan progress/count messaging.
2. Native initial full scan now exposes batch progress to the UI; AppContainer publishes progress and clears the completed state after the same short hold.
3. Search was rebuilt around the Expo behavior: debounce, filters, recent artists/media on open, category aggregation, normalized token matching, result count, no premature empty-state on opening, and working navigation.
4. Search result rows now expose the three-dots action sheet for songs/videos.
5. The action sheet is connected to real native queue, favorites, playlists, album/artist/folder navigation, details, sharing, deletion confirmation, metadata editing, and lyrics editing/fetching.
6. Added app-wide SnackbarHost/AppFeedback so transient confirmations/notifications can be emitted from any native screen.
7. No unrelated player/library/settings/features screens were modified.

Validation:
- `git diff --check`: passed.
- Modified-file scope check: only the two requested UI areas plus the minimum shared infrastructure/data plumbing required for scan progress and global snackbar.
- Kotlin parser/static compilation pass was attempted against the changed files. No syntax/parse/type errors were reported by the filtered check; full Android compilation was not possible because the Gradle wrapper distribution was not locally cached and this environment has no network access to services.gradle.org.
- Physical-device/pixel screenshot verification was NOT possible in this environment. Therefore this report does not falsely claim device-level pixel-perfect parity.

Known limitation:
- The native implementation is now behaviorally mapped to the inspected Expo reference at source level, but final pixel-perfect confirmation still requires building the native APK and comparing it on-device against the Expo reference. That is a verification limitation, not a claim that an untested APK is already perfect.
