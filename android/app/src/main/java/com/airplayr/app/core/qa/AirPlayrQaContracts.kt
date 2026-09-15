package com.airplayr.app.core.qa

/** Stable IDs used by screenshot/UI automation and by the final parity audit. */
object AirPlayrQaTags {
    const val HOME = "airplayr.home"
    const val MUSIC = "airplayr.music"
    const val PLAYER = "airplayr.player"
    const val SETTINGS = "airplayr.settings"
    const val DRAWER = "airplayr.drawer"
    const val MINI_PLAYER = "airplayr.mini_player"
    const val DOCK = "airplayr.bottom_dock"
    const val PLAYER_BACK = "airplayr.player.back"
    const val PLAYER_LYRICS = "airplayr.player.lyrics"
    const val PLAYER_MORE = "airplayr.player.more"
    const val PLAYER_SEEK = "airplayr.player.seek"
    const val PLAYER_PREVIOUS = "airplayr.player.previous"
    const val PLAYER_PLAY = "airplayr.player.play"
    const val PLAYER_NEXT = "airplayr.player.next"
    const val PLAYER_SHUFFLE = "airplayr.player.shuffle"
    const val PLAYER_FAVORITE = "airplayr.player.favorite"
    const val PLAYER_SLEEP = "airplayr.player.sleep"
    const val PLAYER_QUEUE = "airplayr.player.queue"
    const val PLAYER_REPEAT = "airplayr.player.repeat"
    const val SHEET_LYRICS = "airplayr.sheet.lyrics"
    const val SHEET_SPEED_PITCH = "airplayr.sheet.speed_pitch"
    const val SHEET_EQUALIZER = "airplayr.sheet.equalizer"
    const val SHEET_SLEEP = "airplayr.sheet.sleep"
    const val SHEET_MORE = "airplayr.sheet.more"
}

/**
 * Interaction contract mirrored from the navigation/actions inventory.
 * The verifier uses this as an explicit release checklist so missing wiring is
 * detectable without relying on visual inspection alone.
 */
object AirPlayrInteractionContract {
    val rootRoutes = listOf("home", "music", "videos", "settings")
    val musicRoutes = listOf(
        "favorites", "history", "playlists", "collections", "metadata", "queue",
        "listening-watching-stats", "folders", "my-playlists", "search", "most-played",
        "for-you", "rediscover", "rediscover/rewind", "tuned-in", "rock-your-world",
        "smart-collections", "album", "artist", "genre", "open",
    )
    val playerActions = listOf(
        "back", "lyrics", "more", "seek", "previous", "play", "next", "shuffle",
        "favorite", "sleep", "queue", "repeat", "equalizer", "speed-pitch",
    )
    val moreActions = listOf(
        "equalizer", "speed-pitch", "save-queue", "play-next", "play-last", "clear-queue",
        "album", "artist", "album-artist", "folder", "playlist", "metadata", "lyrics",
        "details", "share", "delete",
    )
    val settingsDomains = listOf(
        "appearance", "playback", "replaygain", "video", "subtitles", "audio",
        "gestures", "library", "android", "history",
    )
}
