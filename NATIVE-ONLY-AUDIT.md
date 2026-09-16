# AirPlayr Native-Only Audit

Date: 2026-09-14

## Scope

This package is intentionally restricted to the standalone Android application project.
The application runtime, launcher, UI, media library, playback service, persistence,
metadata, Cast integration, settings, accessibility, and diagnostics are implemented
under `android/`.

## Removal completed

The former JavaScript application tree and its Node package/build metadata were removed
from this release package. No JavaScript or TypeScript application source remains.

Legacy generated resource residue was removed where it was not required by the native
application. Native launcher resources, fonts, manifests, Gradle wrapper, Kotlin source,
and Android resources remain.

The native application has no bridge layer to another runtime. `MainActivity` is the
launcher entry point, `AirPlayrApplication` is the Application owner, and
`AirPlayrPlaybackService` is the background media owner.

## Dependency ownership

The app module contains Android/Jetpack/Google dependencies only:

- Android Gradle Plugin 9.3.2
- Kotlin 2.3.21
- Jetpack Compose / Material 3
- AndroidX Navigation
- AndroidX Room
- AndroidX DataStore
- AndroidX WorkManager
- AndroidX Media3 / ExoPlayer / MediaSession
- AndroidX Media3 Cast / MediaRouter
- AndroidX DocumentFile
- Kotlin Coroutines

The Gradle wrapper is pinned to Gradle 9.5.0, which matches the supported/default Gradle
line for AGP 8.13. Android Gradle Plugin 9.3.2 supports Kotlin 2.3 and API level 36-class
builds. Compose's current setup guidance uses Kotlin 2.3.21 and the 2026.08.00 BOM.

## Static verification

- Native-only verification script: PASS
- No JavaScript/TypeScript/package-manager files: PASS
- No legacy runtime/config filenames: PASS
- No legacy runtime references in project text: PASS
- Native launcher manifest declaration: PASS
- Native playback-service manifest declaration: PASS
- Media3 ExoPlayer dependency: PASS
- Media3 MediaSession dependency: PASS
- Media3 Cast dependency: PASS
- Room dependency: PASS
- DataStore dependency: PASS
- WorkManager dependency: PASS
- Kotlin source inventory: 53 files
- Main Android XML inventory: 8 files
- Gradle Kotlin build files: 3
- Release tree file count: 105 (including this audit)
- XML parsing: PASS
- Gradle wrapper shell syntax: PASS

## Build verification limitation

A real Gradle build was attempted in the available environment. It cannot complete here
because the environment cannot resolve `services.gradle.org` to download the pinned
Gradle distribution. This is an environment/network limitation, not a claim that the
application has been successfully compiled.

A physical Android device run has not been performed in this environment.
