# AirPlayr implementation audit — 2026-09-25

## Implemented in this pass

- **For You Now / Time of Day wave:** retained the existing `AirPlayrWaveAnimation` playback-aware behavior: animation fades/pauses when the current song is paused and resumes when playback resumes.
- **Most Played hero:** the listening-girl Lottie illustration now follows the same playback-aware rule on the Time/intro hero: it animates only while the corresponding top track is actually playing.
- **Home light-mode cards:** On repeat, Recently added songs, Listening & watching stats, and Current playing cards now use a true white card surface when the app canvas is in light mode; dark mode keeps its existing dark Material surfaces.
- **Cast sheet:** the Choose-a-device action now opens Android Wi-Fi settings as requested.
- **On repeat:** made the screen resilient to a populated repeat-history table even if a library row is temporarily missing, and fixed same-track replay recording so replaying the same current media item can increment its play count instead of depending only on media-item transitions.
- **Album/artist headers:** removed the filled background behind the back and overflow header buttons.
- **Music fast scroll:** canceled stale queued drag-scroll jobs, kept the handle visible briefly after a normal swipe, and preserved direct drag-to-position behavior for both list and grid.
- **Video header:** verified the main video controls header already uses `statusBarsPadding()` in the supplied baseline, so it remains protected from the status bar rather than introducing a duplicate inset.
- **Video folders:** folder rows no longer show the right-side play/library icon; they now have a three-dot action menu. Folder contents use the same shared `AirPlayrMediaRow` presentation as the recently-added media list. The current-video floating control now animates its title away while scrolling, leaving the play button.
- **Vault:**
  - header now respects the status-bar inset;
  - top unlocked padlock action now offers restore-all behavior, decrypting Vault videos back into Android MediaStore under `Movies/AirPlayr` and removing them from the encrypted Vault after successful restoration;
  - entries are cached in memory to avoid repeated JSON/loading work;
  - playback decryption cache is reused during the unlocked session and cleared when the Vault locks;
  - multi-video staging is sequential, streaming, and transactional at the staging level so a failed selection does not leave a partial Vault move;
  - direct MediaStore deletion is attempted first, with Android's recoverable deletion confirmation requested only for rows that reject direct deletion.
- **Vault unlock:** changed the new Vault credential UI to a **4-digit numeric PIN**, added real Android `BiometricPrompt` fingerprint/device-biometric authentication, and made biometric unlock bypass the PBKDF2 PIN derivation. PIN KDF work is kept off the UI thread.
- **App splash:** system splash now uses the same `airplayr_about_logo` artwork used by About AirPlayr, and the app shell adds a short custom blue gradient splash with that same logo instead of the former blank/old-logo presentation.
- **App icon:** regenerated Android launcher assets from the newly supplied `/mnt/data/1000166754.png` artwork at all launcher densities. The supplied image was not used as the splash artwork.
- **AMOLED display mode:** added a persisted Settings switch. In Dark theme it makes only the app background true black while preserving themed cards/surfaces.

## Verification performed

- Extracted and inspected the supplied ZIP before editing.
- Compared the edited tree against the supplied ZIP; changes are limited to the implementation/audit files and launcher assets listed by the edits.
- Kotlin delimiter/balance checks passed for every modified Kotlin file.
- Launcher image dimensions were generated for mdpi/hdpi/xhdpi/xxhdpi/xxxhdpi and checked after generation.

## Verification limitation

A full Android Gradle compile could **not** be completed in this environment. The supplied wrapper requires Gradle 9.5.0 and this environment has no network access to `services.gradle.org`, so the wrapper stops before compilation. The source was therefore structurally inspected, but final Android dependency/resource compilation and on-device behavior still need to be verified by the project's normal GitHub Actions/Termux Android build environment.

## Android platform limitation

For videos owned by other applications/users in MediaStore, Android may require its system recoverable-delete confirmation when AirPlayr removes the original after encryption. The implementation avoids asking for that confirmation when Android allows direct deletion, but AirPlayr cannot legitimately bypass Android's confirmation for media that the OS protects.

## Vault credential note

The new Vault setup format is 4-digit-PIN based. A Vault previously created by an older build with a non-4-digit password is not automatically converted by this pass; such a legacy Vault should be migrated/re-created before relying on the new PIN-only gate.
