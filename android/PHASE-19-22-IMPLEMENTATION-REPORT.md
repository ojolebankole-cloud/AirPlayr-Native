# AirPlayr Android Native — Phases 19–22 Implementation Report

## Scope

This pass starts from the verified Phase 0–18 native snapshot and changes only the previously non-green areas requested for Phases 19–22.

Expo remains the reference for good non-video music behavior and visual design. The Expo video-player UI/behavior was not migrated or used as the target.

## Phase 19 — Pitch and audio modifier correctness

Implemented:
- Native Media3 speed/pitch application now uses the real `COMMAND_SET_SPEED_AND_PITCH` path when available.
- Independent pitch uses the stored semitone factor while speed remains independently controlled.
- Lock Pitch to Speed follows the speed ratio, matching the reference controller semantics.
- Pitch/speed state continues to persist through the existing DSP repository.
- DSP is applied to both local playback slots so crossfade does not silently lose speed/pitch state.
- EQ effects are now owned independently per playback slot, preventing the secondary crossfade player from releasing the active player's effects.
- Added unit coverage for pitch-ratio and linked-pitch calculations.

## Phase 20 — Persistent sleep timer

Implemented:
- Sleep timer state moved out of `PlayerScreen` coroutine state and into persistent DataStore-backed playback state.
- Timer survives navigation and player-screen destruction.
- Timer survives background playback/service lifecycle as long as Android keeps the playback service alive.
- Finish-current-track mode is persisted and tied to the actual current media ID.
- Ten-second fade-out is executed by the playback service rather than the Compose screen.
- Manual cancellation restores the timer state without resetting unrelated playback settings.
- UI now reads the persistent timer state and displays remaining time.

## Phase 21 — Playback settings runtime implementation

Implemented:
- Crossfade settings are now consumed by the playback service.
- Crossfade uses two local playback slots with a stable MediaSession-facing switching player.
- The next queue item is prepared on the inactive slot and its volume is ramped up while the current slot ramps down.
- Queue order, shuffle state, playback parameters, EQ effects, notification/session state and playback statistics are transferred/continued across the active slot.
- Crossfade is disabled while a Cast session is active; Cast-session availability transfers playback back to the MediaSession player when needed.
- Gapless setting now controls `pauseAtEndOfMediaItems`; crossfade takes ownership of the transition window when enabled.
- ReplayGain is now actually consumed by the native playback path instead of existing only as stored Settings values.
- Added ReplayGain tag reading for common ReplayGain track/album gain and peak keys using the same Jaudiotagger family used by the reference project.
- Implemented track, album and track-preferred-album modes, preamp, and clipping protection.
- ReplayGain calculation is isolated in a testable native calculator.
- ReplayGain tag reads are performed off the playback main thread and cached per URI.
- Existing green settings and playback architecture were preserved rather than rewritten.

## Phase 22 — Music Player fidelity pass

Implemented:
- Player title now uses the packaged Smooch Sans display face at the reference sizing.
- Artist typography was aligned to the reference 17sp/23sp role.
- Time labels were aligned to the reference 12sp treatment.
- Artwork received the reference-style depth/elevation treatment.
- Lyrics pill was aligned to the reference 142dp x 54dp geometry and elevated surface.
- Main previous/next control geometry was aligned to the reference 58dp controls.
- Play control was aligned to the reference 72dp circular geometry and elevated depth.
- Volume row width, spacing, vertical offset and control treatment were aligned to the reference layout.
- Audio-detail label now uses the packaged Shadows Into Light font.
- Bottom playback action bar width/height/spacing/offset were aligned to the reference 84% / 58dp treatment.
- Added AirPlayr-native back, volume and favorite vector icons for the player instead of using the corresponding Material glyphs.
- Existing seek, queue, playback and sheet behavior was not rewritten.
- Video player code was not changed.

## Validation

Static verification performed after the changes:
- Phase 0–2 verifier: PASS
- Phase 3–5 verifier: PASS
- Phase 6–8 verifier: PASS (updated only to understand the intentional two-slot EQ lifecycle)
- Phase 9–11 verifier: PASS
- Phase 12–15 verifier: PASS
- Python verifier syntax compilation: PASS
- New Phase 19 runtime math tests added
- ZIP integrity checked after packaging

## Build/device boundary

A real Gradle test was attempted. The repository wrapper requires Gradle 9.5.0, but this inspection environment cannot reach `services.gradle.org`, so Gradle could not download the distribution and no APK/AAB was produced here.

No physical Android device/ADB execution was available in this environment.

Therefore this pass is source/static verified, not physically device-certified. In particular, actual audible pitch quality, crossfade smoothness, ReplayGain audibility, sleep-timer behavior under OEM process pressure, and final player pixel rendering on the target phone still require the real-device phase.

## Honest classification

Phases 19–22: IMPLEMENTED at source level and statically verified.

Physical device certification: NOT VERIFIED.

Video parity: intentionally excluded and unchanged.
