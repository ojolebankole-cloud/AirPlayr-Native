# AirPlayr — Video/UI/Gesture Fix Audit
Date: 26 September 2026
Source: AirPlayr-rerun-build-no-git-assets.zip

## Highest-priority investigation: video gestures

The main video screen had two independent full-screen pointer-input detectors layered on the same playback surface: one `detectTapGestures` detector and one `detectDragGestures` detector. That is a fragile event-consumption arrangement for a video surface because the tap detector can consume the pointer stream before the drag detector gets a usable drag sequence. The same pattern also existed in ExternalVideoScreen.

### What was changed
- Replaced the main VideoScreen full-screen tap/drag combination with one unified pointer gesture state machine.
- Replaced the same conflicting arrangement in ExternalVideoScreen.
- Vertical gestures are explicitly split by the starting half of the screen:
  - left half = music volume
  - right half = screen brightness
- Horizontal gestures seek continuously:
  - swipe right = forward
  - swipe left = rewind
- Gesture sensitivity was increased so normal finger movement produces visible changes.
- Single tap still toggles controls.
- Double tap still toggles play/pause when enabled.
- Long press still switches to 2× while held, then restores the configured speed on release when enabled.
- Pinch-to-zoom remains a separate transform gesture.
- Gestures are ignored while the playback lock is active.

This is the most important source-level change in this revision.

## Video queue behavior

- The existing `VideoQueueStore` already had next/previous, shuffle and repeat logic and end-of-video continuation.
- The missing part was reliable queue initialization from direct video-library/folder entry points.
- VideoLibraryScreen now seeds the queue from the active ordered video list before opening a video.
- Folder browse video entries now seed the queue from that folder's filtered video list.
- Video playlist playback now uses `VideoQueueStore` instead of the audio `PlaybackController`.
- The next button therefore resolves against the active video sequence rather than an unrelated/stale queue.

## Shuffle

- Shuffle remains backed by `VideoQueueStore`.
- The playback-screen shuffle control now has an explicit selected/highlighted visual state when enabled.

## Subtitles

- Tapping the subtitle icon now opens the existing subtitle sheet instead of immediately launching Android's file picker.
- The subtitle sheet now contains the file connection action.
- Subtitle placement now supports Top, Center and Bottom; Bottom remains the default/fallback.
- The subtitle overlay uses the video surface itself rather than a separate background surface.
- The Settings screen's standalone subtitle section was removed. Subtitle controls are now concentrated in the video playback subtitle sheet.
- External video subtitle rendering was updated to use the same position model.

## HW/SW and quality sheet

- Added a small video-playback-only information sheet.
- The sheet reports the actual decoder class (HW/SW) and source resolution quality (HD/FHD/2K/4K/SD).
- The quality label in the playback header opens that sheet.
- No general Settings entry was added for decoder/quality.

## Video speed control

- Replaced the header's speed text placeholder with the Material `Speed` icon while retaining the actual speed value in the accessibility label.
- The icon cycles through the supported playback speeds.
- The More menu continues to expose the same speed operation.

## Video header / status bar

- The video header now uses explicit status-bar window-inset padding, matching the edge-to-edge treatment used elsewhere and preventing the header content from sitting under the status bar.
- External video playback received the same header inset treatment.

## Feature hero wave animation

- The large hero cards for `For you now` and `Tuned in` now use the supplied AirPlayr wave illustration animation.
- The animation receives the actual music playback state: it runs while music is playing and pauses when music is paused/stopped.

## Most Played hero

- The large Most Played hero now also receives the global audio playback state rather than only the specific top-track item's state.
- The wave illustration is layered over the hero and pauses when no music is playing.

## Playlist UI

- Playlist detail hero treatment was moved closer to the Genre detail visual structure: compact genre-style eyebrow, 42sp title treatment, tighter content padding, multi-art hero and the same general play/shuffle presentation.
- Video playlists now use the video queue for play/next/add-to-queue operations.

## Splash investigation

There were two splash layers:
1. Android's native SplashScreen API splash using `airplayr_about_logo`.
2. A second Compose splash overlay in `AirPlayrApp` that waited 850 ms and rendered the same `airplayr_about_logo` again over a gradient.

The duplicate Compose layer was removed. The native Android splash remains, using the same About logo drawable. This removes the artificial second splash/re-entry.

## Verification performed

- Changed-file structural brace/parenthesis check: passed for the changed Kotlin files except a conservative text-only checker reported a false mismatch in the very large `ParityScreens.kt`; the Kotlin compiler parser did not report a syntax/expecting error for that file.
- Kotlin compiler parser/front-end pass was run against the changed source set. No syntax errors were reported in VideoScreen, ExternalVideoScreen, MostPlayedScreen, VideoSubtitleSettingsSheet, SettingsScreen or AirPlayrApp.
- Existing project `android/tools/phase3-static-check.sh`: PASS.
- Existing phase verification scripts were also run. Several historical/legacy scripts still report unrelated baseline failures (for example old LiquidGlass matcher expectations and older feature-card contracts); those were not rewritten to manufacture green results.
- A full Gradle compile could not be performed because Gradle 9.5.0 is not cached in this environment and network access to `services.gradle.org` is unavailable. Therefore an APK build and physical-device runtime verification are NOT claimed.

## Honest status

Source-level implementation is complete for the requested gesture, video queue, subtitle-sheet, playback-info, speed-control, splash, hero-animation and playlist-layout changes described above.

The remaining verification gap is the real Android build/device pass. In particular, gesture behavior must still be physically exercised with a finger on the target device because static source analysis cannot prove touch-event behavior at runtime.
