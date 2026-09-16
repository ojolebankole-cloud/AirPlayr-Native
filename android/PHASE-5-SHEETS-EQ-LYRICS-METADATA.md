# AirPlayr Native Phase 5 — Sheets, Dynamic Theme Alignment, EQ, Lyrics & Metadata

## Implemented on Android Native only

- Version-aware one-time “What's New?” sheet, persisted with DataStore and shown after the startup permission sequence.
- Sleep Timer sheet redesigned around the Expo interaction model: circular progress presentation, presets, finish-current-song, fade-out, active-timer cancellation/update.
- Sleep timer fade-out is now functional through the native playback controller volume path instead of being visual-only.
- Speed & Pitch sheet redesigned with speed presets, -12..+12 semitone control, preserve-pitch, pitch-lock, summary tiles and reset.
- Equalizer screen rebuilt with ten-band curve visualization, preset chips, per-band controls, bass/loudness/virtualizer shaping, enable switch and reset, backed by the native DSP repository.
- Lyrics sheet upgraded with provider status, refresh, synchronized cue highlighting/seeking and direct entry into the native Lyrics Editor.
- Lyrics Editor added for plain-text and LRC lyrics, persisted in the native lyrics cache and immediately usable by the player.
- Tag Editor added as a native sheet with editable title/artist/album/album-artist/genre/year/track/disc fields, online metadata lookup, match selection and MediaStore write-through for fields exposed by Android.
- After tag writes the media library is refreshed so the native UI can reconcile updated values.
- Player “More” sheet now links directly to Lyrics and Tag Editor in addition to Equalizer and queue state.
- Player resolves the current Room media record so metadata/lyrics sheets operate on the actual native media item rather than only transient player text.
- Native Material 3 dynamic color remains the system source on Android 12+ with the AirPlayr semantic fallback palette preserved; the dynamic path is kept aligned with the Expo role model rather than replacing typography/design tokens.
- No new third-party UI dependency was introduced.

## Verification

- AirPlayr Phase 3 static checks: PASS
- Native asset/feature parity checks: PASS
- No stock Material NavigationBarItem regression introduced
- Exact Gradle build was not executed in this workspace because the Gradle wrapper distribution is not locally available and the environment cannot reach services.gradle.org. GitHub Actions remains the authoritative build validation.

## Known scope limits

- Android MediaStore does not expose every embedded tag field with a portable write API; the Tag Editor therefore writes the fields Android safely exposes and does not claim unsupported embedded-container rewrites.
- Lyrics providers are the native implementations already present in LyricsService (LRCLIB, NetEase, Kugou, lyrics.ovh); the editor adds manual/local persistence but does not invent provider capabilities that do not exist.
- Pixel-perfect animation/gesture parity for every remaining Expo sheet is not claimed by this phase.
