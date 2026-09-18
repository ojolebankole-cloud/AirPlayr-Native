# AirPlayr UI Fixes — Honest Source Audit

Date: 2026-09-18
Source ZIP audited: `AirPlayr-ui-real-patch.zip`
Baseline ZIP member count: 291
Android Kotlin source files: 82
Android resource files: 43

## Audit method

The uploaded ZIP was extracted and the Android source/resource tree was inspected directly. Prior Markdown audit reports were treated as historical documentation, not proof. The implementation decisions below are based on the actual Kotlin source present in the submitted ZIP.

The pre-patch ZIP had a few important mismatches with its previous audit claims. In particular, `AirPlayrBottomDock.kt` still owned a full-screen `Box(fillMaxSize())`, `AirPlayrApp.kt` still painted an extra full-screen background around the navigation shell, `HomeScreen.kt` used `Arrangement.spacedBy(0.dp)`, Recently Added Songs were rendered as two horizontal rails (including `drop(1)` duplication), and the Home stats preview was a single hand-shadowed combined card rather than two Material-elevated cards.

## Requested fixes and verified implementation

### 1. Floating MiniPlayer

**Status: IMPLEMENTED**

- `AirPlayrApp.kt` now gives the dock a bottom-aligned modifier instead of relying on the dock to own the screen.
- `AirPlayrBottomDock.kt` no longer uses `fillMaxSize()` at its root.
- The MiniPlayer is its own Material `Surface` with rounded corners, tonal elevation, and shared AirPlayr elevation.
- The MiniPlayer-only route also uses the same independent surface treatment.

Source: `android/app/src/main/java/com/airplayr/app/ui/AirPlayrApp.kt`
Source: `android/app/src/main/java/com/airplayr/app/ui/components/AirPlayrBottomDock.kt`

### 2. Floating Bottom Dock

**Status: IMPLEMENTED**

- The dock is a bottom-aligned, wrap-content column.
- The navigation row is a separate Material `Surface` with rounded corners and elevation.
- MiniPlayer and dock are separated by an explicit 8dp gap.
- Existing navigation routes and callbacks were preserved.
- Bottom reservation constants were raised from 184/112dp to approximate the actual floating stack (210/128dp) so content is less likely to be obscured.

### 3. Home section spacing

**Status: IMPLEMENTED**

- Home `LazyColumn` now uses `Arrangement.spacedBy(24.dp)`.
- The top cluster keeps tighter internal rhythm (12dp) while the feature row receives the requested 24dp separation before the first content section.
- Content sections are grouped into reusable 8dp header-to-content blocks.
- Bottom content padding is now 24dp instead of a large fixed 110dp slab.

Source: `HomeScreen.kt`

### 4. Feature card elevation

**Status: IMPLEMENTED**

- Feature cards now use the shared feature-card shadow token at 8dp.
- The previous 10dp custom shadow call was replaced with the shared token value.

Source: `FeatureCard.kt`, `FeatureCardTokens.kt`

### 5. Recently Added Songs spacing/layout

**Status: IMPLEMENTED**

- The duplicated two-rail implementation was removed.
- Recently Added Songs are now one vertical list of song cards.
- Cards are separated by 8dp.
- Song cards remain clickable and use the shared AirPlayr card primitive.

### 6. Album shadow removal

**Status: IMPLEMENTED**

- The Home album rail no longer wraps each album in a transparent `Surface` with shadow configuration.
- Album items are plain clickable columns around their artwork/text, so there is no extra card shadow layer.

### 7. Artist shadow removal

**Status: IMPLEMENTED**

- The Home artist rail received the same cleanup: no transparent `Surface` wrapper and no extra shadow layer.

### 8. Listening / Watching stats

**Status: IMPLEMENTED**

- The combined Home stats preview was split into two real Material cards:
  - `Listening stats`
  - `Watching stats`
- Each card uses Material3 `CardDefaults.cardElevation` with the shared 8dp AirPlayr medium elevation.
- The cards have a 12dp vertical gap.
- Existing stats data and navigation target are preserved.

### 9. Playback elevation system

**Status: IMPLEMENTED**

`PlayerScreen.kt` was normalized away from the previous large ad-hoc shadows:

- Root background is now the theme background color rather than a hardcoded vertical gradient.
- Back control uses a Material `Surface` with soft shared elevation.
- Lyrics control uses Material card elevation instead of an 18dp manual shadow.
- Artwork shadow reduced to shared medium elevation.
- Playback action strip is a Material `Surface` with medium shared elevation.
- Previous/Next controls use Material surfaces with soft shared elevation.
- Main Play/Pause control uses Material surface elevation.
- More-group and details cards use `CardDefaults.cardElevation` instead of raw shadow modifiers.

A small 3dp slider-thumb shadow remains intentionally, because it is a tiny control affordance rather than a large decorative surface shadow.

### 10. Music / Repeat icons

**Status: IMPLEMENTED**

- `AirPlayrIcons.Music` is now a native custom vector instead of `Icons.Default.LibraryMusic`.
- Repeat and Repeat-One vectors were refined to the app’s custom 28dp vector family with consistent stroke weights.
- Remaining active `LibraryMusic` usages were switched to `AirPlayrIcons.Music` in the Music empty state, parity fallback, menu drawer, and What’s New sheet.


## Exact active Kotlin files changed

The repair pass changed these 12 active Kotlin files only:

- `android/app/src/main/java/com/airplayr/app/ui/AirPlayrApp.kt`
- `android/app/src/main/java/com/airplayr/app/ui/components/AirPlayrBottomDock.kt`
- `android/app/src/main/java/com/airplayr/app/ui/components/AirPlayrSheets.kt`
- `android/app/src/main/java/com/airplayr/app/ui/components/FeatureCard.kt`
- `android/app/src/main/java/com/airplayr/app/ui/components/MenuDrawer.kt`
- `android/app/src/main/java/com/airplayr/app/ui/components/MusicScreen.kt`
- `android/app/src/main/java/com/airplayr/app/ui/components/WhatsNewSheet.kt`
- `android/app/src/main/java/com/airplayr/app/ui/screens/HomeScreen.kt`
- `android/app/src/main/java/com/airplayr/app/ui/screens/ParityScreens.kt`
- `android/app/src/main/java/com/airplayr/app/ui/screens/PlayerScreen.kt`
- `android/app/src/main/java/com/airplayr/app/ui/theme/AirPlayrIcons.kt`
- `android/app/src/main/java/com/airplayr/app/ui/theme/FeatureCardTokens.kt`

Only the `LibraryMusic` import was removed from `AirPlayrSheets.kt`; no sheet behavior was otherwise changed.

## Architecture scope

No navigation architecture, playback state model, repository layer, media scanning behavior, or screen routing was changed. The repair is limited to UI composition/elevation/icon presentation and the existing Home shell layout.

## Source verification performed

- The 12 modified Kotlin files were checked for balanced `()`, `{}`, and `[]` structures after editing.
- A fresh filesystem count of the submitted ZIP contains 84 Kotlin source files and 43 Android resource files before patching.
- The patched tree still contains 84 Kotlin source files and 43 resource files. (The earlier report's count of 82 Kotlin files was inaccurate.)
- No Android resources were added or removed by the UI repair.
- Search verification confirms the patched `AirPlayrBottomDock.kt` no longer has a root `fillMaxSize()` container.
- Search verification confirms the patched `PlayerScreen.kt` no longer contains the previous 18dp/24dp decorative shadows or the root `Brush.verticalGradient`.
- Search verification confirms no `LibraryMusic` reference remains in active Android `*.kt` UI source files.

## Build verification limitation

The project contains its Gradle wrapper (`android/gradlew`) and requests Gradle 9.5.0, but this environment cannot resolve `services.gradle.org`. The attempted wrapper invocation failed with `java.net.UnknownHostException: services.gradle.org` before Gradle could start.

Therefore **no APK build/install claim is made for this repair pass**. The source was statically audited and structurally checked, but an Android compile/runtime test could not be completed in this environment.

## Final truth

The requested UI fixes are implemented in source. The one thing not honestly verifiable here is device/runtime rendering, because the Android build could not start in the current network-restricted environment.
