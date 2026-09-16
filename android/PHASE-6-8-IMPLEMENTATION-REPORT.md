# AirPlayr Android Native — Phase 6–8 Implementation Report

Date: 2026-09-15
Reference: Expo `userfix` project, Phase 6/7/8 requirements in `PHASE-1-5-6-IMPLEMENTATION.md` and `PHASE-7-9-VALIDATION.md`.

## Scope

Implemented Phase 6–8 on top of the previously verified Phase 0–5 native project.

The requested video-player exception is preserved: no attempt was made to copy the Expo video-player UI.

## Phase 6 — EQ lifecycle

Implemented and hardened:

- `AudioEffectsEngine` is the single native owner of Android `Equalizer`, `BassBoost`, `LoudnessEnhancer`, and `Virtualizer` instances.
- Effect attachment is keyed to the active Android audio-session ID.
- Re-applying DSP values to the same session is idempotent; it no longer releases and recreates every effect object on every state update.
- A genuinely new audio session releases the old effect instances and creates a new set.
- Saved EQ state is restored by the playback service without opening the EQ UI.
- The real local `ExoPlayer` audio-session listener owns session-change detection, including when playback is wrapped by `CastPlayer`.
- The DSP state collector applies speed/pitch parameters and EQ/effect state to the live player/session.
- Effect and listener cleanup is performed when the service is destroyed.
- Unsupported effect families are tolerated individually instead of crashing the playback service.

## Phase 7 — Android media system

Implemented with Media3's native architecture rather than duplicating a second legacy platform session:

- `AirPlayrPlaybackService` remains the sole `MediaLibraryService` / playback-session owner.
- Added official Media3 `MediaButtonReceiver` manifest registration.
- Added Media3 playback-resumption callback backed by the persisted `QueueStore`.
- Resumption restores the saved queue, current item, position, shuffle state, and repeat mode for actual playback resumption.
- Metadata-only resumption returns the current item instead of forcing the complete queue into the metadata request.
- Added a stable Media3 `DefaultMediaNotificationProvider` with an AirPlayr notification channel and stable notification ID.
- Session activity opens `MainActivity` from the media notification.
- Existing Media3 `MediaItem` metadata contains title, artist, album, artwork URI, media type, and duration, allowing the Media3 notification/session layer to present current-media metadata.
- Kept the foreground-service media-playback declaration and permissions in the manifest.
- No duplicate notification implementation or second competing playback engine was introduced.

This is intentionally different from the Expo Phase 7 implementation, because the native app already owns its playback through Media3/ExoPlayer. The correct native equivalent is one Media3 session/service, not a mirror session controlling another JS player.

## Phase 8 — Material You / dynamic theme

Implemented/hardened:

- Android 12+ dynamic light/dark Material 3 color schemes are used when dynamic color is enabled.
- Dynamic color use is guarded by API level S+.
- Dynamic palette is invalidated when the app resumes so system wallpaper/theme changes can propagate without an app restart.
- AirPlayr semantic colors are derived from Material 3 semantic roles instead of hardcoded foreground replacements.
- `textPrimary` follows `onSurface`.
- `textSecondary` follows `onSurfaceVariant`.
- `textMuted` follows `outline`.
- Primary, secondary, and tertiary containers preserve their `on*Container` roles.
- Surface/container roles remain semantic and are exposed through `LocalAirPlayrColors`.
- Existing AirPlayr fallback light/dark palettes remain available when dynamic color is disabled or unsupported.
- Artwork pixels and custom illustration colors remain data/asset colors rather than theme tokens.

## Verification performed

Dedicated Phase 6–8 static verifier: PASS (21/21 checks).

Phase 0–2 verifier: PASS.

Phase 3–5 verifier: PASS.

Native-only verifier: PASS.

Native asset/feature parity verifier: PASS.

Critical branded assets/fonts: 19/19 exact SHA-256 matches remain intact.

## Build/device boundary

A real Android Gradle compilation was attempted with the supplied wrapper. The sandbox cannot resolve `services.gradle.org` to download the required Gradle 9.5.0 distribution, so compilation could not proceed here.

Therefore the following are NOT claimed as physically certified in this workspace:

- APK installation on a real device
- lock-screen notification behavior on an OEM device
- Bluetooth/head-unit button acceptance
- audible EQ/effect behavior on specific Android hardware
- process-death playback resumption on a physical device
- dynamic wallpaper palette rendering on specific OEM skins

Those require a successful native build and real-device test.

## Honest status

Phase 6 source implementation: IMPLEMENTED + static verification PASS.

Phase 7 source implementation: IMPLEMENTED + static verification PASS.

Phase 8 source implementation: IMPLEMENTED + static verification PASS.

Build certification: NOT VERIFIED in this sandbox.

Physical-device certification: NOT PERFORMED in this sandbox.

Video player: intentionally not migrated, per project scope.
