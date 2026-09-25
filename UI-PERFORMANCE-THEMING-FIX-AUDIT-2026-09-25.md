# AirPlayr UI / Performance / Dynamic Theme Fix Audit — 2026-09-25

## Scope

Investigated and fixed:
- For You Now / Rock Your World filter loading flash.
- Illustrated wave playback visibility.
- Library/navigation warm-state restoration.
- App-wide destination fade transitions.
- Most Played / Rediscover illustration-vs-hero colour separation.
- Dynamic Material You light/dark background and surface palette.

## Implementation

### 1. Music mix filter loading
`MusicMixScreen.kt`
- The current mix is no longer cleared/recreated when a genre filter changes.
- The previous mix remains visible while the new filtered mix is calculated on `Dispatchers.Default`.
- A small progress indicator communicates the update without replacing the content with an empty state.
- The empty state is now shown only after the library snapshot is ready and the mix generation has completed with no results.
- This applies to the For You, Time of Day and Rock Your World mix screen implementation.

### 2. Wave animation
`AirPlayrWaveAnimation.kt`
- Added an explicit `isPlaying` input.
- Lottie playback pauses when the song is not playing.
- The wave fades out when playback stops and fades back in when playback resumes.
- The existing animation asset and line artwork remain unchanged.

### 3. Warm library state
`LibraryViewModel.kt`, `AppContainer.kt`, `AirPlayrApp.kt`
- Added an application-scoped warm cache shared across Activity/ViewModel recreation.
- The cache keeps references to the already-built immutable Room-backed lists instead of copying the library.
- Soft references are used so Android can reclaim the cached lists under memory pressure.
- A coherent cached snapshot is used as the initial StateFlow value when available, preventing the former empty-first frame during navigation/recreation.
- Feature collection cache is also retained across ViewModel recreation.
- Room remains the source of truth and live Room emissions replace the warm state.

### 4. Navigation transitions
`AirPlayrApp.kt`
- Normal destination changes now use consistent fade-in/fade-out transitions across the NavHost.
- The old directional slide component was removed to avoid competing motion and make navigation feel lighter.
- Reduced-motion startup behavior remains respected.

### 5. Illustration theming
`AirPlayrLottieTheme.kt`, `MostPlayedScreen.kt`, `ParityScreens.kt`
- Lottie illustration colours now use a dedicated hue-shifted illustration palette derived from the dynamic/app accent.
- Most Played and Rediscover hero backgrounds no longer use the same primary/secondary/tertiary colours as the illustration.
- Hero surfaces use a separate Material surface gradient while the illustration uses its own accent family.
- The supplied Lottie geometry/assets remain unchanged.

### 6. Dynamic Material You palette
`AirPlayrTheme.kt`
- Dynamic mode now uses Android's wallpaper-derived primary/secondary/tertiary colours as inputs, but AirPlayr generates its own light and dark canvas/surface hierarchy.
- Dynamic light mode no longer defaults to a plain white canvas.
- Dynamic dark mode no longer defaults to a near-black canvas.
- Light mode uses a softly tinted AirPlayr surface hierarchy.
- Dark mode uses a charcoal/tinted hierarchy with distinct containers and elevation levels.
- Fixed/non-dynamic accent modes retain their existing AirPlayr palette path.

## Verification

- Compared the modified tree against the supplied current app ZIP: 9 source files changed plus this audit report; no existing project files were intentionally removed.
- Kotlin delimiter/balance checks passed for all modified Kotlin files.
- A local Gradle compile was attempted with `./gradlew --offline :app:compileDebugKotlin`.
- The compile could not start because the wrapper attempted to download Gradle 9.5.0 and this environment cannot reach `services.gradle.org` (`UnknownHostException`).
- Therefore the changes are source-inspected and structurally checked, but the final Android compile remains unverified until GitHub Actions/another networked Gradle environment runs it.
