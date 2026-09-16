# AirPlayr Native — Phase 1 Asset & Design Foundation

## Scope
Expo AirPlayr is the visual/source-of-truth reference. This phase modifies only the Android native project.

## Asset parity
All reference assets were compared byte-for-byte before this pass:
- `assets/icon.png`
- `assets/favicon.png`
- `assets/splash-icon.png`
- `assets/adaptive-icon.png`
- all 7 feature-card SVGs
- all 8 font files

All 19 reference assets already existed in the native project with identical SHA-256 content.

## Improvements made in this phase
1. Promoted all 8 exact Expo fonts into `app/src/main/res/font/` using Android-safe resource names so Compose can load the fonts directly.
2. Added `AirPlayrFonts` exposing every reference family: Open Sans, Smooch Sans, Oswald, Dancing Script, Shadows Into Light, Caacupe One.
3. Replaced the placeholder `Typography()` with the Expo typography contract mapped to Material 3 roles.
4. Added centralized spacing/radius/elevation tokens matching the Expo theme.
5. Expanded native fallback Material 3 color schemes to match the Expo light/dark semantic palette instead of only the primary/background subset.
6. Added exact custom vector geometry for the four Expo bottom-dock icons as reusable Compose `ImageVector`s. The dock layout itself is intentionally left for the dedicated navigation/UI parity phase.
7. Kept all existing Android branding, feature artwork SVG sources, generated PNG artwork, and splash/launcher resources unchanged.

## Verification
- 19/19 Expo asset files: present and SHA-256 identical in native.
- 8/8 font resources copied into `res/font`.
- Native XML/resources remain structurally valid.
- No Expo/React/Node runtime files were introduced.
- No files outside the Android native project were modified.

## Explicit boundary
This is the design-foundation phase, not the full screen-by-screen UI migration. The next phase should consume these exact tokens/fonts/icons while rebuilding the Expo Home/Menu/navigation/player/settings/statistics/equalizer and remaining screens.
