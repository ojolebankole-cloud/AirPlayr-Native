package com.airplayr.app.ui

import androidx.compose.foundation.layout.Box

import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.core.tween
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.Modifier
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import android.content.Intent
import android.os.Build
import androidx.compose.runtime.Composable
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.getValue
import androidx.compose.runtime.SideEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Snackbar
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.dp
import com.airplayr.app.di.AppContainer
import com.airplayr.app.di.SafScanUiState
import com.airplayr.app.ui.navigation.AirPlayrNavViewModel
import com.airplayr.app.ui.navigation.LibraryViewModel
import com.airplayr.app.ui.navigation.LibraryViewModelFactory
import com.airplayr.app.ui.screens.HomeScreen
import com.airplayr.app.ui.screens.SafScanOverlay
import com.airplayr.app.ui.screens.LibraryScreen
import com.airplayr.app.ui.screens.MusicScreen
import com.airplayr.app.ui.screens.PlayerScreen
import com.airplayr.app.ui.screens.EqualizerScreen
import com.airplayr.app.ui.screens.SearchScreen
import com.airplayr.app.ui.screens.HistoryScreen
import com.airplayr.app.ui.screens.CollectionsScreen
import com.airplayr.app.ui.screens.VideoScreen
import com.airplayr.app.ui.screens.VideoLibraryScreen
import com.airplayr.app.ui.screens.VideoFolderDetailScreen
import com.airplayr.app.ui.screens.VaultScreen
import com.airplayr.app.ui.screens.VaultVideoScreen
import com.airplayr.app.ui.screens.ExternalVideoScreen
import com.airplayr.app.ui.screens.FavoritesScreen
import com.airplayr.app.ui.screens.PlaylistsScreen
import com.airplayr.app.ui.playback.PlaybackController
import com.airplayr.app.ui.playback.PlaybackControllerFactory
import com.airplayr.app.ui.screens.SettingsScreen
import com.airplayr.app.ui.screens.AboutScreen
import com.airplayr.app.ui.screens.DonateSheet
import com.airplayr.app.ui.screens.StatsScreen
import com.airplayr.app.ui.screens.FoldersScreen
import com.airplayr.app.ui.screens.MyPlaylistsScreen
import com.airplayr.app.ui.screens.PlaylistDetailScreen
import com.airplayr.app.ui.screens.PlaylistGridScreen
import com.airplayr.app.core.data.media.MediaType
import com.airplayr.app.ui.screens.RecentScreen
import com.airplayr.app.ui.screens.FeatureCollectionScreen
import com.airplayr.app.ui.screens.MostPlayedScreen
import com.airplayr.app.ui.screens.BrowseIndexScreen
import com.airplayr.app.ui.screens.BrowseFilterScreen
import com.airplayr.app.ui.screens.RecentCollectionsScreen
import com.airplayr.app.ui.screens.AlbumDetailScreen
import com.airplayr.app.ui.screens.ArtistDetailScreen
import com.airplayr.app.ui.screens.GenreDetailScreen
import com.airplayr.app.ui.screens.MusicMixScreen
import com.airplayr.app.ui.screens.MusicMixMode
import com.airplayr.app.ui.metadata.MetadataScreen
import com.airplayr.app.ui.diagnostics.DiagnosticsScreen
import com.airplayr.app.ui.theme.AirPlayrTheme
import com.airplayr.app.ui.components.AirPlayrBottomDock
import com.airplayr.app.ui.components.AirPlayrMiniPlayerOnly
import com.airplayr.app.ui.components.MenuDrawer
import com.airplayr.app.ui.components.WhatsNewSheet
import com.airplayr.app.ui.components.VideoQueueSheet
import com.airplayr.app.ui.LocalAirPlayrBottomOverlayHeight
import kotlinx.coroutines.launch
import com.airplayr.app.ui.permissions.StartupPermissionFlow
import com.airplayr.app.ui.cast.CastPickerSheet
import com.airplayr.app.ui.feedback.AppFeedback

data class ExternalVideoRequest(val uri: String, val mimeType: String?, val title: String)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AirPlayrApp(
    container: AppContainer,
    initialViewModel: AirPlayrNavViewModel,
    launchIntent: Intent?,
) {
    val settings by container.preferences.settings.collectAsStateWithLifecycle(initialValue = null)
    val startupTheme by container.startupTheme.collectAsStateWithLifecycle()
    val navController = rememberNavController()
    var initialShellFrame by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(true) }
    androidx.compose.runtime.LaunchedEffect(Unit) { initialShellFrame = false }
    val playbackController: PlaybackController = viewModel(
        factory = PlaybackControllerFactory(container.appContext, container.lyricsPrefetchCoordinator),
    )
    val libraryViewModel: LibraryViewModel = viewModel(
        factory = LibraryViewModelFactory(container.mediaLibrary, container.structured, container.libraryWarmCache),
    )
    var castOpen by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(false) }
    var videoQueueOpen by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(false) }
    var externalVideo by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf<ExternalVideoRequest?>(null) }
    var whatsNewOpen by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(false) }
    var permissionFlowCompleted by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(false) }
    var startupContentReady by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(false) }
    var menuOpen by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(false) }
    var playerOpen by androidx.compose.runtime.saveable.rememberSaveable { androidx.compose.runtime.mutableStateOf(false) }
    var donateOpen by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(false) }
    val syncState by container.syncState.collectAsStateWithLifecycle()
    val safScanState by container.safScanState.collectAsStateWithLifecycle()
    val rootScope = androidx.compose.runtime.rememberCoroutineScope()
    val snackbarHostState = androidx.compose.runtime.remember { SnackbarHostState() }
    val scanMediaLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        runCatching {
            val flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
            container.appContext.contentResolver.takePersistableUriPermission(uri, flags)
        }
        container.startSafMediaScan(uri)
    }
    androidx.compose.runtime.LaunchedEffect(Unit) {
        AppFeedback.snackbar.collect { message ->
            snackbarHostState.currentSnackbarData?.dismiss()
            snackbarHostState.showSnackbar(message)
        }
    }
    val appVersion = com.airplayr.app.BuildConfig.VERSION_NAME

    androidx.compose.runtime.LaunchedEffect(launchIntent) {
        initialViewModel.handleIntent(launchIntent)
    }
    // The startup gate is intentionally separate from permission completion. On a fresh
    // install, the Home shell can render behind the dialogs, but the library is not scanned
    // and the What's New/features sheet is not shown until the complete permission sequence
    // has finished and the first library initialization has completed. Existing users keep
    // their Room cache immediately and only receive a background delta refresh.
    androidx.compose.runtime.LaunchedEffect(permissionFlowCompleted) {
        if (!permissionFlowCompleted || startupContentReady) return@LaunchedEffect
        val initialized = container.preferences.hasCompletedInitialLibraryScan()
        if (initialized) {
            startupContentReady = true
            // The Home frame and cached MiniPlayer state are already render-ready. Start the
            // freshness check immediately on the application scope; it never blocks Compose.
            container.requestLibrarySynchronization()
        } else {
            runCatching { container.ensureInitialLibraryScan() }
                .onSuccess { startupContentReady = true }
        }
    }
    androidx.compose.runtime.LaunchedEffect(startupContentReady, appVersion) {
        if (!startupContentReady || container.preferences.hasSeenWhatsNew(appVersion)) return@LaunchedEffect
        kotlinx.coroutines.delay(350L)
        if (!container.preferences.hasSeenWhatsNew(appVersion)) whatsNewOpen = true
    }
    val externalMedia by initialViewModel.externalMedia.collectAsStateWithLifecycle()
    val primaryRoutes = remember { setOf("home", "music", "videos", "settings") }
    fun navigate(route: String) {
        if (route == "player") {
            playerOpen = true
            return
        }
        if (playerOpen) playerOpen = false
        val isPrimary = route in primaryRoutes
        navController.navigate(route) {
            if (isPrimary) {
                popUpTo(navController.graph.startDestinationId) { saveState = true }
                launchSingleTop = true
                restoreState = true
            } else {
                launchSingleTop = true
            }
        }
    }
    androidx.compose.runtime.LaunchedEffect(externalMedia) {
        externalMedia?.let { external ->
            if (external.mimeType?.startsWith("video/") == true) {
                externalVideo = ExternalVideoRequest(external.uri, external.mimeType, external.title)
                navigate("external-video")
            } else {
                playbackController.playExternal(external.uri, external.mimeType, external.title)
                navigate("player")
            }
            initialViewModel.consumeExternalMedia()
        }
    }
    // startupTheme is resolved while the Android splash is still visible. Using it here as the
    // first-frame source means an existing user's saved light/dark choice is never rendered one
    // frame behind the DataStore settings flow.
    val configuredTheme = settings?.theme ?: startupTheme
    val darkTheme = when (configuredTheme) {
        "light" -> false
        "dark" -> true
        else -> androidx.compose.foundation.isSystemInDarkTheme()
    }
    val accentColor = settings?.accentColor ?: if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) "dynamic" else "blue"

    AirPlayrTheme(
        darkTheme = darkTheme,
        accentColor = accentColor,
        amoledMode = settings?.amoledMode == true,
        highContrast = settings?.highContrast == true,
    ) {
        val context = LocalContext.current
        // Read the composable theme value before entering SideEffect.
        // SideEffect itself is not a composable context.
        val windowBackgroundColor = MaterialTheme.colorScheme.background.toArgb()
        SideEffect {
            val activity = context as? android.app.Activity
            activity?.window?.let { window ->
                val controller = androidx.core.view.WindowCompat.getInsetsController(window, window.decorView)
                controller.isAppearanceLightStatusBars = !darkTheme
                controller.isAppearanceLightNavigationBars = !darkTheme
                window.setBackgroundDrawable(
                    android.graphics.drawable.ColorDrawable(windowBackgroundColor)
                )
                if (android.os.Build.VERSION.SDK_INT >= 29) {
                    window.isNavigationBarContrastEnforced = false
                }
            }
        }
        val currentBackStackEntry = navController.currentBackStackEntryAsState().value
        val currentRoute = currentBackStackEntry?.destination?.route
        val recentType = currentBackStackEntry?.arguments?.getString("type")
        val shellPlaybackState by playbackController.stableState.collectAsStateWithLifecycle()
        androidx.compose.runtime.LaunchedEffect(currentRoute) {
            if (currentRoute == "player" && !playerOpen) {
                navController.popBackStack()
                playerOpen = true
            }
        }
        val isPrimaryDockScreen = currentRoute in primaryRoutes
        val isVideoScreen = currentRoute == "video/{id}" || currentRoute == "external-video" || currentRoute == "vault-video/{id}"
        val isVideoOnlyBrowseScreen = isVideoScreen ||
            currentRoute == "videos" ||
            currentRoute == "video-search" ||
            currentRoute == "video-folder/{id}" ||
            currentRoute == "video-collections" ||
            currentRoute == "continue-watching" ||
            currentRoute == "currently-watching" ||
            currentRoute == "complete-watching" ||
            currentRoute == "completed-watching" ||
            currentRoute == "rediscover" ||
            currentRoute == "rediscover/rewind" ||
            currentRoute == "video-playlists" ||
            currentRoute == "video-playlist/{id}" ||
            (currentRoute == "recent/{type}" && recentType?.startsWith("videos") == true)
        val isVaultScreen = currentRoute == "vault"
        androidx.compose.runtime.LaunchedEffect(currentRoute) {
            if (currentRoute != "vault" && currentRoute != "vault-video/{id}") container.vaultManager.lock()
        }
        val isFullMusicPlayer = playerOpen || currentRoute == "player"
        androidx.activity.compose.BackHandler(enabled = playerOpen) { playerOpen = false }
        val showDock = isPrimaryDockScreen && !isVaultScreen && !isFullMusicPlayer
        val showMiniPlayer = !isPrimaryDockScreen && !isVideoOnlyBrowseScreen && !isFullMusicPlayer && !isVaultScreen
        val miniPlayerVisible = showMiniPlayer &&
            shellPlaybackState.currentId != null &&
            shellPlaybackState.currentMediaType == com.airplayr.app.core.data.media.MediaType.AUDIO
        var bottomOverlayHeightPx by androidx.compose.runtime.remember { androidx.compose.runtime.mutableIntStateOf(0) }
    val density = androidx.compose.ui.platform.LocalDensity.current
    val bottomOverlayHeight = with(density) { bottomOverlayHeightPx.toDp() }
    androidx.compose.runtime.LaunchedEffect(showDock, miniPlayerVisible) {
        if (!showDock && !miniPlayerVisible) bottomOverlayHeightPx = 0
    }

    val reservedBottom = when {
            showDock -> com.airplayr.app.ui.components.AIRPLAYR_BOTTOM_DOCK_BASE_HEIGHT.dp
            miniPlayerVisible -> com.airplayr.app.ui.components.AIRPLAYR_MINI_PLAYER_ONLY_BASE_HEIGHT.dp
            else -> 0.dp
        }

        // Keep the shell edge-to-edge. The persistent bottom controls are an overlay surface,
        // while each screen owns any status-bar inset it actually needs. Home intentionally draws
        // behind the transparent status bar so its content can scroll underneath it.
        androidx.compose.runtime.CompositionLocalProvider(
            LocalAirPlayrBottomOverlayHeight provides bottomOverlayHeight,
        ) {
            Box(Modifier.fillMaxSize()) {
                val contentModifier = Modifier
                    .fillMaxSize()
                NavHost(
                navController = navController,
                startDestination = "home",
                modifier = contentModifier,
                enterTransition = {
                    if (initialShellFrame || settings?.reduceMotion == true) fadeIn(tween(1))
                    else fadeIn(tween(180))
                },
                exitTransition = {
                    if (initialShellFrame || settings?.reduceMotion == true) fadeOut(tween(1))
                    else fadeOut(tween(140))
                },
                popEnterTransition = {
                    if (initialShellFrame || settings?.reduceMotion == true) fadeIn(tween(180))
                    else fadeIn(tween(180))
                },
                popExitTransition = {
                    if (initialShellFrame || settings?.reduceMotion == true) fadeOut(tween(140))
                    else fadeOut(tween(140))
                },
            ) {
                composable("home") {
                    HomeScreen(
                        container = container,
                        libraryViewModel = libraryViewModel,
                        playback = playbackController,
                        onNavigate = { route -> navigate(route) },
                        onMenu = { menuOpen = true },
                        onCast = { castOpen = true },
                    )
                }
                composable("music") {
                    MusicScreen(
                        libraryViewModel = libraryViewModel,
                        container = container,
                        playback = playbackController,
                        onNavigate = { route -> navigate(route) },
                    )
                }
                composable("video-folder/{id}") { entry ->
                    val folderPath = java.net.URLDecoder.decode(entry.arguments?.getString("id").orEmpty(), "UTF-8")
                    VideoFolderDetailScreen(
                        container = container,
                        folderPath = folderPath,
                        libraryViewModel = libraryViewModel,
                        onBack = { navController.popBackStack() },
                        onOpenVideo = { id -> navigate("video/$id") },
                    )
                }
                composable("videos") {
                    VideoLibraryScreen(
                        container = container,
                        libraryViewModel = libraryViewModel,
                        onOpenVideo = { navigate("video/$it") },
                        onSearch = { navigate("video-search") },
                        onCast = { castOpen = true },
                        onNavigate = { route -> navigate(route) },
                    )
                }
                composable("about") { AboutScreen(onBack = { navController.popBackStack() }, appVersion = appVersion) }
                composable("settings") {
                    SettingsScreen(container = container, onDiagnostics = { navigate("diagnostics") }, onMetadata = { navigate("metadata") }, onNavigate = { route -> navigate(route) })
                }
                composable("vault") {
                    VaultScreen(
                        container = container,
                        onBack = { navController.popBackStack() },
                        onOpenVideo = { navigate("vault-video/$it") },
                    )
                }
                composable("vault-video/{id}") { entry ->
                    VaultVideoScreen(
                        container = container,
                        id = entry.arguments?.getString("id").orEmpty(),
                        onCast = { castOpen = true },
                        onNavigate = { route -> navigate(route) },
                    )
                }
                composable("player") { PlayerScreen(container.appContext, playbackController, onBack = { navController.popBackStack() }, onEqualizer = { navigate("equalizer") }, onNavigate = { route -> navigate(route) }) }
                composable("equalizer") { EqualizerScreen(container, onBack = { navController.popBackStack() }) }
                composable("search") {
                    SearchScreen(
                        container = container,
                        libraryViewModel = libraryViewModel,
                        onOpen = { item ->
                            if (item.mediaType == com.airplayr.app.core.data.media.MediaType.VIDEO) {
                                navigate("video/${item.id}")
                            } else {
                                navigate("player")
                            }
                        },
                        onNavigate = { route -> navigate(route) },
                        onBack = { navController.popBackStack() },
                    )
                }
                composable("video-search") {
                    SearchScreen(
                        container = container,
                        libraryViewModel = libraryViewModel,
                        initialFilter = "videos",
                        videoOnly = true,
                        onOpen = { item -> if (item.mediaType == MediaType.VIDEO) navigate("video/${item.id}") },
                        onNavigate = { route -> navigate(route) },
                        onBack = { navController.popBackStack() },
                    )
                }
                composable("music-search") {
                    SearchScreen(
                        container = container,
                        libraryViewModel = libraryViewModel,
                        initialFilter = "all",
                        musicOnly = true,
                        onOpen = { item ->
                            if (item.mediaType == com.airplayr.app.core.data.media.MediaType.AUDIO) navigate("player")
                        },
                        onNavigate = { route -> navigate(route) },
                        onBack = { navController.popBackStack() },
                    )
                }
                composable("history") { HistoryScreen(container, libraryViewModel, playbackController, { id -> navigate("video/$id") }, { navigate("player") }, { route -> navigate(route) }, { navController.popBackStack() }) }
                composable("favorites") { FavoritesScreen(container, libraryViewModel, playbackController, { id -> navigate("video/$id") }, { navigate("player") }, { route -> navigate(route) }, { navController.popBackStack() }) }
                composable("playlists") { PlaylistsScreen(container, libraryViewModel) { id -> navigate("playlist/$id") } }
                composable("collections") { CollectionsScreen(container, libraryViewModel, { route -> navigate(route) }, playbackController) }
                composable("metadata") { MetadataScreen(container, libraryViewModel) }
                composable("diagnostics") { DiagnosticsScreen(container) }
                composable("listening-watching-stats") { StatsScreen(
                    container = container,
                    libraryViewModel = libraryViewModel,
                    playback = playbackController,
                    onOpenMedia = { item ->
                        if (item.mediaType == com.airplayr.app.core.data.media.MediaType.VIDEO) {
                            navigate("video/${item.id}")
                        } else {
                            playbackController.play(item)
                            navigate("player")
                        }
                    },
                    onOpenVideo = { id -> navigate("video/$id") },
                    onBack = { navController.popBackStack() },
                ) }
                composable("folders") { FoldersScreen(container, libraryViewModel, playbackController, onOpen = { navigate("folder/$it") }, onBack = { navController.popBackStack() }) }
                composable("my-playlists") {
                    MyPlaylistsScreen(
                        container = container,
                        libraryViewModel = libraryViewModel,
                        playback = playbackController,
                        onOpen = { navigate("music-playlist/$it") },
                        onOpenGrid = { type ->
                            navigate(if (type == MediaType.VIDEO) "video-playlists" else "music-playlists")
                        },
                        onBack = { navController.popBackStack() },
                        onOpenVideo = { id -> navigate("video/$id") },
                    )
                }
                composable("music-playlists") {
                    PlaylistGridScreen(
                        title = "Music playlists",
                        mediaType = MediaType.AUDIO,
                        container = container,
                        libraryViewModel = libraryViewModel,
                        onOpenPlaylist = { navigate("music-playlist/$it") },
                        onBack = { navController.popBackStack() },
                    )
                }
                composable("video-playlists") {
                    PlaylistGridScreen(
                        title = "Video playlists",
                        mediaType = MediaType.VIDEO,
                        container = container,
                        libraryViewModel = libraryViewModel,
                        onOpenPlaylist = { navigate("video-playlist/$it") },
                        onBack = { navController.popBackStack() },
                    )
                }
                composable("music-playlist/{id}") { entry -> PlaylistDetailScreen(container, libraryViewModel, entry.arguments?.getString("id").orEmpty(), playbackController, mediaType = MediaType.AUDIO, onOpenPlayer = { navigate("player") }) }
                composable("video-playlist/{id}") { entry -> PlaylistDetailScreen(container, libraryViewModel, entry.arguments?.getString("id").orEmpty(), playbackController, mediaType = MediaType.VIDEO, onOpenPlayer = { navigate("player") }, onOpenVideo = { navigate("video/$it") }) }
                composable("playlist/{id}") { entry -> PlaylistDetailScreen(container, libraryViewModel, entry.arguments?.getString("id").orEmpty(), playbackController, onOpenPlayer = { navigate("player") }) }
                composable("recent/{type}") { entry ->
                    val recentType = entry.arguments?.getString("type").orEmpty()
                    if (recentType == "songs" || recentType == "albums" || recentType == "artists" || recentType == "on-repeat") {
                        RecentCollectionsScreen(
                            type = recentType,
                            container = container,
                            libraryViewModel = libraryViewModel,
                            playback = playbackController,
                            onBack = { navController.popBackStack() },
                            onOpenAlbum = { navigate("album/${java.net.URLEncoder.encode(it, "UTF-8")}") },
                            onOpenArtist = { navigate("artist/${java.net.URLEncoder.encode(it, "UTF-8")}") },
                            onOpenPlayer = { navigate("player") },
                        )
                    } else {
                        RecentScreen(
                            type = recentType,
                            viewModel = libraryViewModel,
                            playback = playbackController,
                            onOpen = { navigate("video/$it") },
                            container = container,
                            onOpenPlayer = { navigate("player") },
                            onBack = { navController.popBackStack() },
                        )
                    }
                }
                composable("most-played") { MostPlayedScreen(container, libraryViewModel, playbackController, { id -> navigate("video/$id") }, { navigate("player") }, { route -> navigate(route) }, { navController.popBackStack() }) }
                composable("for-you") { MusicMixScreen(MusicMixMode.FOR_YOU, libraryViewModel, playbackController, container, { navController.popBackStack() }) }
                composable("rediscover") { FeatureCollectionScreen("Rediscover", "From your previous plays", { container.collections.rediscoverVideos() }, libraryViewModel, playbackController, container, { navController.popBackStack() }, { navigate("video/$it") }, onOpenPlayer = { navigate("player") }) }
                composable("rediscover/rewind") { FeatureCollectionScreen("Rediscover", "From your previous plays", { container.collections.rediscoverVideos() }, libraryViewModel, playbackController, container, { navController.popBackStack() }, { navigate("video/$it") }, onOpenPlayer = { navigate("player") }) }
                composable("tuned-in") { MusicMixScreen(MusicMixMode.TIME_OF_DAY, libraryViewModel, playbackController, container, { navController.popBackStack() }) }
                composable("rock-your-world") { MusicMixScreen(MusicMixMode.ROCK_YOUR_WORLD, libraryViewModel, playbackController, container, { navController.popBackStack() }) }
                composable("smart-collections") { CollectionsScreen(container, libraryViewModel, { route -> navigate(route) }, playbackController) }
                composable("continue-watching") { FeatureCollectionScreen(
                    title = "Continue watching",
                    subtitle = "Pick up where you left off",
                    loader = { container.collections.continueWatching() },
                    libraryViewModel = libraryViewModel,
                    playback = playbackController,
                    onVideoOpen = { id -> navigate("video/$id") },
                    onOpenPlayer = { navigate("player") },
                    onBack = { navController.popBackStack() },
                ) }
                composable("video-collections") { FeatureCollectionScreen(
                    title = "Video collections",
                    subtitle = "Continue watching and unwatched videos",
                    loader = { container.collections.continueWatching() + container.collections.unwatched() },
                    libraryViewModel = libraryViewModel,
                    playback = playbackController,
                    onVideoOpen = { id -> navigate("video/$id") },
                    onOpenPlayer = { navigate("player") },
                    onBack = { navController.popBackStack() },
                ) }
                composable("album") { BrowseIndexScreen("Albums", com.airplayr.app.core.data.media.MediaType.AUDIO, libraryViewModel, playbackController) { navigate("album/${it}") } }
                composable("artist") { BrowseIndexScreen("Artists", com.airplayr.app.core.data.media.MediaType.AUDIO, libraryViewModel, playbackController) { navigate("artist/${it}") } }
                composable("genre") { BrowseIndexScreen("Genres", com.airplayr.app.core.data.media.MediaType.AUDIO, libraryViewModel, playbackController) { navigate("genre/${it}") } }
                composable("music-folder/{id}") { entry ->
                    val folder = java.net.URLDecoder.decode(entry.arguments?.getString("id").orEmpty(), "UTF-8")
                    BrowseFilterScreen(
                        title = "Folders",
                        value = folder,
                        type = MediaType.AUDIO,
                        libraryViewModel = libraryViewModel,
                        playback = playbackController,
                        onOpenPlayer = { navigate("player") },
                        onOpenVideo = { navigate("video/$it") },
                        onBack = { navController.popBackStack() },
                        folderMode = "music",
                        folderName = folder.substringAfterLast('/').ifBlank { folder },
                        container = container,
                    )
                }
                composable("folder/{kind}/{id}/{name}") { entry -> BrowseFilterScreen("Folders", java.net.URLDecoder.decode(entry.arguments?.getString("id").orEmpty(), "UTF-8"), com.airplayr.app.core.data.media.MediaType.AUDIO, libraryViewModel, playbackController, onOpenPlayer = { navigate("player") }, onOpenVideo = { navigate("video/$it") }, onBack = { navController.popBackStack() }, folderMode = entry.arguments?.getString("kind"), folderName = java.net.URLDecoder.decode(entry.arguments?.getString("name").orEmpty(), "UTF-8"), container = container) }
                composable("folder/{id}") { entry -> BrowseFilterScreen("Folders", java.net.URLDecoder.decode(entry.arguments?.getString("id").orEmpty(), "UTF-8"), com.airplayr.app.core.data.media.MediaType.AUDIO, libraryViewModel, playbackController, onOpenPlayer = { navigate("player") }, onOpenVideo = { navigate("video/$it") }, onBack = { navController.popBackStack() }) }
                composable("album/{id}") { entry ->
                    AlbumDetailScreen(
                        albumName = java.net.URLDecoder.decode(entry.arguments?.getString("id").orEmpty(), "UTF-8"),
                        libraryViewModel = libraryViewModel,
                        playback = playbackController,
                        onBack = { navController.popBackStack() },
                        onOpenArtist = { navigate("artist/${java.net.URLEncoder.encode(it, "UTF-8")}") },
                        onOpenAlbum = { navigate("album/${java.net.URLEncoder.encode(it, "UTF-8")}") },
                    )
                }
                composable("artist/{id}") { entry ->
                    ArtistDetailScreen(
                        artistName = java.net.URLDecoder.decode(entry.arguments?.getString("id").orEmpty(), "UTF-8"),
                        libraryViewModel = libraryViewModel,
                        playback = playbackController,
                        onBack = { navController.popBackStack() },
                        onOpenAlbum = { navigate("album/${java.net.URLEncoder.encode(it, "UTF-8")}") },
                    )
                }
                composable("genre/{id}") { entry ->
                    GenreDetailScreen(
                        genreName = java.net.URLDecoder.decode(entry.arguments?.getString("id").orEmpty(), "UTF-8"),
                        libraryViewModel = libraryViewModel,
                        playback = playbackController,
                        onBack = { navController.popBackStack() },
                        onOpenAlbum = { navigate("album/${java.net.URLEncoder.encode(it, "UTF-8")}") },
                        onOpenArtist = { navigate("artist/${java.net.URLEncoder.encode(it, "UTF-8")}") },
                    )
                }
                composable("open") { LibraryScreen("Open media", com.airplayr.app.core.data.media.MediaType.AUDIO, libraryViewModel, playbackController, onOpen = { playbackController.play(it); navigate("player") }, container = container) }
                composable("video/{id}") { entry -> VideoScreen(
                    container = container,
                    id = entry.arguments?.getString("id").orEmpty(),
                    onCast = { castOpen = true },
                    onNavigate = { route -> navigate(route) },
                ) }
                composable("external-video") {
                    val request = externalVideo
                    if (request == null) {
                        Box(Modifier.fillMaxSize(), contentAlignment = androidx.compose.ui.Alignment.Center) {
                            androidx.compose.material3.Text("No video selected")
                        }
                    } else {
                        ExternalVideoScreen(container, request.uri, request.mimeType, request.title, onCast = { castOpen = true }, onNavigate = { route -> navigate(route) })
                    }
                }
            }

            androidx.compose.runtime.LaunchedEffect(currentRoute) {
                if (currentRoute != "home") menuOpen = false
            }

            if (playerOpen) {
                PlayerScreen(
                    appContext = container.appContext,
                    controller = playbackController,
                    onBack = { playerOpen = false },
                    onEqualizer = {
                        playerOpen = false
                        navigate("equalizer")
                    },
                    onNavigate = { route -> navigate(route) },
                )
            }

            SnackbarHost(
                hostState = snackbarHostState,
                modifier = Modifier
                    .align(androidx.compose.ui.Alignment.BottomCenter)
                    .padding(bottom = if (reservedBottom > 0.dp) reservedBottom + 12.dp else 20.dp),
                snackbar = { data ->
                    Snackbar(
                        snackbarData = data,
                        shape = androidx.compose.foundation.shape.RoundedCornerShape(18.dp),
                        containerColor = com.airplayr.app.ui.theme.LocalAirPlayrColors.current.primaryContainer,
                        contentColor = com.airplayr.app.ui.theme.LocalAirPlayrColors.current.textPrimary,
                    )
                },
            )

            if (showDock) {
                AirPlayrBottomDock(
                    modifier = Modifier
                        .align(androidx.compose.ui.Alignment.BottomCenter)
                        .onSizeChanged { bottomOverlayHeightPx = it.height },
                    currentRoute = currentRoute,
                    playbackState = shellPlaybackState,
                    playback = playbackController,
                    showMiniPlayer = true,
                    onOpenPlayer = { navigate("player") },
                    onNavigate = { route -> navigate(route) },
                )
            } else if (showMiniPlayer && miniPlayerVisible) {
                AirPlayrMiniPlayerOnly(
                    playbackState = shellPlaybackState,
                    playback = playbackController,
                    onOpenPlayer = { navigate("player") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(androidx.compose.ui.Alignment.BottomCenter)
                        .onSizeChanged { bottomOverlayHeightPx = it.height },
                )
            }

            // Draw the drawer after the persistent bottom system so the drawer is the top
            // interactive layer when open. This prevents the mini-player/dock from covering
            // the final drawer items such as About AirPlayr and Support AirPlayr.
            if (menuOpen && currentRoute == "home") {
                val snapshot by libraryViewModel.snapshot.collectAsStateWithLifecycle()
                MenuDrawer(
                    visible = true,
                    musicCount = snapshot.audio.size,
                    videoCount = snapshot.video.size,
                    folderCount = libraryViewModel.folderCount.collectAsStateWithLifecycle().value,
                    playback = playbackController,
                    latestHistory = snapshot.history.firstOrNull(),
                    onClose = { menuOpen = false },
                    onNavigate = { route -> navigate(route) },
                    onScanMedia = { scanMediaLauncher.launch(null) },
                    onAbout = { navigate("about") },
                    onSupport = { donateOpen = true },
                    onVideoQueue = { videoQueueOpen = true },
                )
            }

            if (safScanState.visible) {
                SafScanOverlay(safScanState)
            }

            StartupPermissionFlow(
                context = container.appContext,
                onCompleted = {
                    // Do not start any media synchronization until the complete startup
                    // permission sequence has finished. The coordinator below chooses the
                    // one-time full scan for a new install or a background delta for an existing one.
                    permissionFlowCompleted = true
                },
            )


            if (donateOpen) {
                DonateSheet(onDismiss = { donateOpen = false })
            }

            if (whatsNewOpen) {
                WhatsNewSheet(version = appVersion, onDismiss = {
                    whatsNewOpen = false
                    rootScope.launch { container.preferences.markWhatsNewSeen(appVersion) }
                })
            }
            if (videoQueueOpen) {
                VideoQueueSheet(
                    library = container.mediaLibrary,
                    onPlay = { item -> videoQueueOpen = false; navigate("video/${item.id}") },
                    onDismiss = { videoQueueOpen = false },
                )
            }
            if (castOpen) {
                ModalBottomSheet(
                    onDismissRequest = { castOpen = false },
                    containerColor = MaterialTheme.colorScheme.surfaceContainer,
                    // The cast sheet is an app surface, not a modal blackout over the current
                    // screen. Keep the underlying Home/library content fully visible.
                    scrimColor = androidx.compose.ui.graphics.Color.Transparent,
                ) {
                    CastPickerSheet(
                        controller = container.cast,
                        onDismiss = { castOpen = false },
                    )
                }
            }
            }
        }
    }
}
