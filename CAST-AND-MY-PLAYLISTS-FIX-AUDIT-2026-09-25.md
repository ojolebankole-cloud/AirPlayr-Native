# Cast crash + My Playlists correction — 2026-09-25

## Cast crash

The Cast service was manually transferring the active timeline inside `SessionAvailabilityListener` while Media3 `CastPlayer` already owns the local-to-Cast transfer. That duplicated handoff was removed.

The service now follows the Media3 CastPlayer pattern: local ExoPlayer is supplied to `CastPlayer.Builder(this)`, and the CastPlayer itself remains the playback player. This avoids a second competing transfer when the user selects a device in the Cast picker.

Reference: AndroidX Media3 CastPlayer documentation describes CastPlayer as the player that delegates between the local player and remote Cast player and handles the transfer when a Cast session becomes available.

## My Playlists

The Home feature card remains the combined **My playlists** entry for both music and video playlists.

The My Playlists screen now treats playlist `mediaType` correctly when resolving its items. Music playlists resolve against the music library; video playlists resolve against the video library.

The create-playlist dialog now lets the user choose **Music** or **Videos**. Video playlist creation opens the video playlist library, while music playlist creation opens the selected music playlist.

Video playlists now display video counts and open the selected video when Play is used. Music playlists retain the existing music-player flow.

## Verification

- Source structure checks passed for the three edited Kotlin files.
- Gradle compile could not be run in this environment because the Gradle wrapper attempted to download Gradle 9.5.0 and network access is unavailable (`UnknownHostException: services.gradle.org`).
- Final device/GitHub Actions build verification remains required.
