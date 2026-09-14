# AirPlayr Native Build + Player Readiness Audit

Date: 2026-09-14

## Scope

Full static audit of the supplied native Android ZIP, including build configuration, Kotlin sources, Android resources, Media3 playback/session plumbing, music/video data paths, persistence, SAF scanning, duplicate files, and release/build automation.

## Build readiness

The supplied project used Compose BOM `2026.08.00` while still targeting compile SDK 36 with AGP 8.13.2/Gradle 8.13. Compose 1.12 requires compileSdk 37 and minimum AGP 9.1.2. The project is therefore upgraded to AGP 9.3.2, Gradle 9.5.0, compileSdk 37 and targetSdk 37. The current Android documentation lists AGP 9.3.2 as a stable release and requires Gradle 9.5 for AGP 9.3. citeturn406930search0turn995386search6turn995386search12

A GitHub Actions workflow was added at `.github/workflows/android.yml`. It uses JDK 17, installs Android API 37/build-tools, compiles Kotlin/tests, assembles the debug APK, and uploads the APK as a workflow artifact.

A local Gradle compile could not be completed because this environment cannot resolve `services.gradle.org`. Therefore **no APK build is claimed as locally verified**. The repository is prepared for cloud build; GitHub Actions is the intended build verification path.

## Static defects repaired

- Diagnostics repository referenced a non-existent `countByType()` DAO method; corrected to the actual `count()` DAO API.
- Video deep links no longer search only the first 250 videos; the media DAO/repository now has direct ID lookup.
- Checked-in `android/app/debug.keystore` was removed.
- Queue restore now prefers the saved stable media ID before falling back to the numeric index.
- SAF folder scans now reconcile previous rows for that selected root, preventing stale deleted/moved files from remaining indefinitely.
- SAF folder paths now preserve the nested relative path instead of flattening everything to the immediate folder name.
- Persisted SAF tree roots are remembered and rescanned at application startup when possible.
- Media3 metadata now explicitly marks playable/non-browsable items and carries known duration information.
- Gapless playback preference is now wired to Media3 `setPauseAtEndOfMediaItems`, so disabling gapless causes an intentional pause at each media item boundary. Media3 documents this API as the control for pausing at the end of each media item. citeturn406930search3turn406930search8
- The playback service was upgraded from `MediaSessionService` to `MediaLibraryService` with `MediaLibrarySession` browsing/search support for Music and Videos, and the manifest now advertises the MediaLibraryService interface. Android's current media guidance recommends `MediaLibraryService` for an app that exposes a browsable media library and explicitly calls it the service required for Android Auto media-library support. citeturn995386search2turn995386search4
- Video screen now initializes favorite state from persistent storage and exposes persistent bookmark chips that seek/remove bookmarks.
- Video PiP can be entered from the video screen on API 26+.

## Duplicate/missing-file audit

No suspicious duplicate Kotlin source files were found. The same-byte files are expected resource variants/copies: debug vs debugOptimized manifests, adaptive/round vector icon variants, and intentional branding artwork copies. No duplicate application classes with the same fully-qualified name were found.

The native tree contains the expected launcher, splash, Compose/Kotlin source, resources, fonts, feature-card SVG sources, rendered feature-card PNGs, database schemas/migrations, and Gradle wrapper. No JavaScript/TypeScript/package-manager project files remain.

## Music player coverage

Implemented/present in the inspected project:

- MediaStore local music discovery.
- SAF additional-folder discovery.
- Room cached library.
- Background library refresh via WorkManager.
- Media3 ExoPlayer playback.
- Queue persistence.
- Play/pause/seek/next/previous.
- Repeat/shuffle.
- Playback speed 0.25x–4x.
- Pitch/preserve-pitch controls.
- MediaSession/MediaLibrarySession.
- Background service architecture.
- System media controls through MediaSession.
- Audio focus and noisy-audio handling through Media3 player setup.
- Android output/cast plumbing through Media3 CastPlayer.
- Equalizer/bass/loudness/virtualizer effect plumbing, subject to device support.
- Favorites/history/playlists.
- Artwork/metadata cache paths.
- Search.
- Diagnostics/crash reporting.

## Video player coverage

Implemented/present:

- Local video library.
- MediaStore + SAF sources.
- Media3 video playback.
- Resume position persistence.
- Playback speed 0.25x–4x.
- A/B repeat during the visible player screen.
- External VTT/SRT subtitle loading.
- Favorite state persistence.
- Video bookmarks with seek/remove UI.
- Picture-in-picture entry and aspect-ratio configuration.
- Shared MediaSession/MediaLibrarySession playback plumbing.

## Features that are NOT falsely marked complete

The following remain known product-level gaps and were intentionally not faked with placeholder behavior:

- Production-grade crossfade. A stored crossfade preference existed, but the original implementation did not actually perform audio crossfades. A correct implementation requires coordinated multi-player/audio-volume transition behavior and runtime testing, especially with Cast output. The current package still exposes the old preference field; it should be removed or disabled before presenting crossfade as a supported feature.
- Full ReplayGain application.
- Full embedded-tag writing for every supported format.
- Advanced video gesture/zoom/frame-step/screenshot tooling.
- A complete audio-track/subtitle track selection UX beyond file-based subtitle injection and the Media3 player's available controls.
- Full Android Auto category tree beyond the new Music/Videos library roots and paginated children.
- A complete sleep timer with fade-out behavior.

## Runtime verification limits

No physical Android device execution occurred in this environment. No claim of verified playback of every codec/container is made. Media3 1.11.0 is the current stable Media3 release and provides the ExoPlayer/MediaSession foundation, but optional formats/protocols can require additional Media3 modules. citeturn893412search1turn893412search2

## Bottom line

**Native project structure:** READY

**Static source/resources audit:** PASS with fixes applied

**Cloud build automation:** READY

**Local APK compile:** NOT VERIFIED due to environment network restriction

**Music core:** strong/native and substantially complete

**Video core:** strong, with the documented advanced gaps above

**Crossfade:** NOT COMPLETE and should not be represented as complete

**Physical-device validation:** still required before release
