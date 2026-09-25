# AirPlayr — Three-Dot / Overflow Menu Audit

Date: 2026-09-25

## Scope

Scanned all Android UI source for `MoreVert`, `MoreHoriz`, `AirPlayrMoreButton`, overflow/dropdown controls, menu state, and the action sheets they open.

## Fixed in this pass

1. Music collection list overflow controls for Albums, Artists, and Folders were previously decorative/non-functional. They now open a real media action sheet with:
   - Play all
   - Shuffle
   - Add to queue
   - Open collection
2. Music playlist list overflow control was previously decorative/non-functional. It now opens a real playlist sheet with:
   - Open playlist
   - Play playlist
   - Play next
   - Add to queue
   - Share playlist
   - Rename
   - Delete playlist
3. Genre detail header overflow was previously a no-op. It now opens a genre action sheet with:
   - Play all
   - Shuffle
   - Add all to queue
4. Genre song overflow was previously only a visible icon. It now opens the normal music media action sheet with:
   - Play now
   - Play next
   - Add to queue
5. Album/Artist detail collection sheets contained an `Open` action backed by an empty callback. The dead action is now omitted when the user is already on that collection detail screen.
6. Removed three exposed Folder-menu `Blacklist` actions that only displayed “not available yet”.
7. The External Video screen used a three-dot icon for a fit-mode toggle rather than a menu. The icon/label is now an aspect-ratio/fit control, so it is no longer misleading as a menu.
8. The global Artwork component now only renders the animated playing-wave indicator for `MediaType.AUDIO`. Video artwork never receives the music wave indicator, including video action sheets.

## Complete three-dot / overflow inventory

### Feature collection headers
Used by For You Now, Rock Your World, Rediscover, Tuned In and related feature-collection destinations.

Sheet contents:
- Shuffle
- Play next
- Add to queue
- Add to playlist

### History header
Sheet contents:
- Shuffle
- Play next
- Add to queue
- Add to playlist

### Favorites header
Sheet contents:
- Shuffle
- Play next
- Add to queue
- Add to playlist

### Most Played header
Sheet contents:
- Shuffle
- Play next
- Add to queue
- Add to playlist

### Music Mix header
Sheet contents:
- Shuffle
- Play next
- Add to queue
- Add to playlist

### Home / feature media item overflow
Uses the shared History/Favorites media action sheet where applicable.
Music items expose:
- Favorite / unfavorite
- Play next
- Add to queue
- Add to playlist
- Go to album
- Go to artist
- Go to album artist
- Go to folder
- Tag editor
- Edit lyrics
- Details
- Share
- Delete from device
- Optional Blacklist only where the caller actually supplies a working blacklist action

Video items expose:
- Favorite / unfavorite
- Details
- Share
- Delete from device

### Music screen — individual song overflow
Sheet contents:
- Favorite / unfavorite
- Play now
- Play next
- Add to queue
- Add to playlist
- Edit song

### Music screen — Album / Artist / Folder list overflow
Sheet contents:
- Play all
- Shuffle
- Add to queue
- Open collection

### Music screen — Playlist list overflow
Sheet contents:
- Open playlist
- Play playlist
- Play next
- Add to queue
- Share playlist
- Rename
- Delete playlist

### Album detail header overflow
Sheet contents:
- Play
- Shuffle
- Add to queue

The obsolete no-op `Open` item is no longer shown because the user is already on that album.

### Artist detail header overflow
Sheet contents:
- Play
- Shuffle
- Add to queue

The obsolete no-op `Open` item is no longer shown because the user is already on that artist.

### Album / Artist / collection song overflow
Sheet contents:
- Play now
- Play next
- Add to queue

### Genre detail header overflow
Sheet contents:
- Play all
- Shuffle
- Add all to queue

### Genre detail song overflow
Sheet contents:
- Play now
- Play next
- Add to queue

### Video library — folder overflow
Bottom sheet contents:
- Share folder
- Open folder

### Video library — video overflow
Sheet contents:
- Open
- Details
- Play next
- Add to video queue
- Share
- Favorite / unfavorite

### Video player — More overflow
Sheet contents:
- Play / pause
- Video queue
- Equalizer
- Subtitles
- Add subtitle file
- Fit mode
- Playback speed
- Add bookmark
- Favorite / unfavorite

### Music queue — song overflow
Dropdown contents:
- Play now
- Play next
- Remove from queue

### My Playlists — header overflow
Sheet contents:
- Shuffle all playlist items
- Add all items to queue
- Refresh playlists

### My Playlists — individual playlist overflow
Sheet contents:
- Play
- Play next
- Add to queue
- Add to playlist
- Choose artwork
- Rename
- Save as file
- Share playlist
- Delete playlist

### Search result media overflow
Music sheets include:
- Play next
- Add to queue
- Add to playlist
- Go to album
- Go to artist
- Go to album artist
- Go to folder
- Tag editor
- Edit lyrics
- Details
- Share
- Delete from device

Video sheets include:
- Play next
- Add to video queue
- Details
- Share
- Delete from device

### Statistics media overflow
Sheet contents:
- Play now
- Play next
- Add to queue
- Favorite / unfavorite
- Add to playlist / related media action where exposed by the caller

### Library media overflow
Music/video rows use the shared media action sheet. Video actions include Open, Details, Play next, Add to video queue, Share and Delete; music actions include Open, Details, Play next, Add to queue, Add to playlist, navigation to collection/folder, metadata/lyrics editing, Share and Delete as applicable.

### Bulk selection overflow
Where exposed, it opens the bulk-selection sheet containing:
- Play now
- Play next
- Add to queue
- Add to playlist
- Add to favorites when applicable
- Share
- Delete from device

## Verification notes

- Source scan now finds no UI `onClick = {}` placeholders except the Home scan-overlay scrim, where the no-op click intentionally consumes touches while the scan overlay is visible.
- No remaining exposed “Blacklist is not available yet” menu actions remain.
- Video artwork wave animation is globally restricted to audio media.
- The Android Gradle wrapper could not perform a final build in this environment because Gradle 9.5.0 was not cached and network access to `services.gradle.org` is unavailable.
- Existing repository verification scripts still report several unrelated/pre-existing parity checks as FAIL; those were not treated as successful build verification.
