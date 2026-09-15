package com.airplayr.app.ui

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.Modifier
import androidx.compose.runtime.setValue
import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.SideEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.ui.platform.LocalContext
import com.airplayr.app.di.AppContainer
import com.airplayr.app.ui.navigation.AirPlayrNavViewModel
import com.airplayr.app.ui.navigation.LibraryViewModel
import com.airplayr.app.ui.navigation.LibraryViewModelFactory
import com.airplayr.app.ui.screens.HomeScreen
import com.airplayr.app.ui.screens.LibraryScreen
import com.airplayr.app.ui.screens.MusicScreen
import com.airplayr.app.ui.screens.PlayerScreen
import com.airplayr.app.ui.screens.EqualizerScreen
import com.airplayr.app.ui.screens.SearchScreen
import com.airplayr.app.ui.screens.HistoryScreen
import com.airplayr.app.ui.screens.CollectionsScreen
import com.airplayr.app.ui.screens.VideoScreen
import com.airplayr.app.ui.screens.FavoritesScreen
import com.airplayr.app.ui.screens.PlaylistsScreen
import com.airplayr.app.ui.playback.PlaybackController
import com.airplayr.app.ui.playback.PlaybackControllerFactory
import com.airplayr.app.ui.screens.SettingsScreen
import com.airplayr.app.ui.screens.QueueScreen
import com.airplayr.app.ui.screens.StatsScreen
import com.airplayr.app.ui.screens.FoldersScreen
import com.airplayr.app.ui.screens.MyPlaylistsScreen
import com.airplayr.app.ui.screens.PlaylistDetailScreen
import com.airplayr.app.ui.screens.RecentScreen
import com.airplayr.app.ui.screens.FeatureCollectionScreen
import com.airplayr.app.ui.screens.BrowseIndexScreen
import com.airplayr.app.ui.screens.BrowseFilterScreen
import com.airplayr.app.ui.metadata.MetadataScreen
import com.airplayr.app.ui.diagnostics.DiagnosticsScreen
import com.airplayr.app.ui.theme.AirPlayrTheme
import com.airplayr.app.ui.components.AirPlayrBottomDock
import com.airplayr.app.ui.components.LiquidGlassBackground
import com.airplayr.app.ui.components.WhatsNewSheet
import kotlinx.coroutines.launch
import com.airplayr.app.ui.permissions.StartupPermissionFlow
import com.airplayr.app.ui.cast.CastPickerSheet

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AirPlayrApp(
    container: AppContainer,
    initialViewModel: AirPlayrNavViewModel,
    launchIntent: Intent?,
) {
    val settings by container.preferences.settings.collectAsStateWithLifecycle(initialValue = null)
    val navController = rememberNavController()
    val playbackController: PlaybackController = viewModel(factory = PlaybackControllerFactory(container.appContext))
    val libraryViewModel: LibraryViewModel = viewModel(
        factory = LibraryViewModelFactory(container.mediaLibrary),
    )
    var castOpen by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(false) }
    var whatsNewOpen by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(false) }
    var permissionFlowCompleted by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(false) }
    val rootScope = androidx.compose.runtime.rememberCoroutineScope()
    val appVersion = com.airplayr.app.BuildConfig.VERSION_NAME

    androidx.compose.runtime.LaunchedEffect(launchIntent) {
        initialViewModel.handleIntent(launchIntent)
    }
    androidx.compose.runtime.LaunchedEffect(permissionFlowCompleted, appVersion) {
        if (permissionFlowCompleted && !container.preferences.hasSeenWhatsNew(appVersion)) {
            kotlinx.coroutines.delay(2000L)
            whatsNewOpen = true
        }
    }
    val requestedRoute by initialViewModel.route.collectAsStateWithLifecycle()
    val externalMedia by initialViewModel.externalMedia.collectAsStateWithLifecycle()
    androidx.compose.runtime.LaunchedEffect(externalMedia) {
        externalMedia?.let { external ->
            playbackController.playExternal(external.uri, external.mimeType, external.title)
            navController.navigate("player") { launchSingleTop = true }
            initialViewModel.consumeExternalMedia()
        }
    }
    androidx.compose.runtime.LaunchedEffect(requestedRoute) {
        val route = when (requestedRoute) {
            com.airplayr.app.ui.navigation.Route.Home -> "home"
            com.airplayr.app.ui.navigation.Route.Music -> "music"
            com.airplayr.app.ui.navigation.Route.Videos -> "videos"
            com.airplayr.app.ui.navigation.Route.Settings -> "settings"
        }
        if (navController.currentDestination?.route != route) {
            navController.navigate(route) {
                popUpTo(navController.graph.startDestinationId) { saveState = true }
                launchSingleTop = true
                restoreState = true
            }
        }
    }

    val darkTheme = when (settings?.theme) {
        "light" -> false
        "dark" -> true
        else -> androidx.compose.foundation.isSystemInDarkTheme()
    }

    AirPlayrTheme(darkTheme = darkTheme, highContrast = settings?.highContrast == true) {
        val context = LocalContext.current
        SideEffect {
            val activity = context as? android.app.Activity
            activity?.window?.let { window ->
                val controller = androidx.core.view.WindowCompat.getInsetsController(window, window.decorView)
                controller.isAppearanceLightStatusBars = !darkTheme
                controller.isAppearanceLightNavigationBars = !darkTheme
                if (android.os.Build.VERSION.SDK_INT >= 29) {
                    window.isNavigationBarContrastEnforced = false
                }
            }
        }
        LiquidGlassBackground {
        val entry by navController.currentBackStackEntryAsState()
        val destination = entry?.destination
        Box(Modifier.fillMaxSize()) {
            NavHost(
                navController = navController,
                startDestination = "home",
                modifier = androidx.compose.ui.Modifier.fillMaxSize(),
            ) {
                composable("home") { HomeScreen(container = container, libraryViewModel = libraryViewModel, onNavigate = { route -> navController.navigate(route) }, onCast = { castOpen = true }) }
                composable("music") {
                    MusicScreen(
                        libraryViewModel = libraryViewModel,
                        appContext = container.appContext,
                        container = container,
                        onNavigate = { route -> navController.navigate(route) },
                    )
                }
                composable("videos") {
                    LibraryScreen(
                        title = "Videos",
                        type = com.airplayr.app.core.data.media.MediaType.VIDEO,
                        viewModel = libraryViewModel,
                        onOpen = { navController.navigate("video/${it.id}") },
                    )
                }
                composable("settings") {
                    SettingsScreen(container = container, onDiagnostics = { navController.navigate("diagnostics") }, onMetadata = { navController.navigate("metadata") }, onNavigate = { route -> navController.navigate(route) })
                }
                composable("player") { PlayerScreen(container.appContext, onBack = { navController.popBackStack() }, onEqualizer = { navController.navigate("equalizer") }, onQueue = { navController.navigate("queue") }, onNavigate = { route -> navController.navigate(route) }) }
                composable("equalizer") { EqualizerScreen(container) }
                composable("search") { SearchScreen(container) { item -> if (item.mediaType == com.airplayr.app.core.data.media.MediaType.VIDEO) navController.navigate("video/${item.id}") else { playbackController.play(item); navController.navigate("player") } } }
                composable("history") { HistoryScreen(container, playbackController) { id -> navController.navigate("video/$id") } }
                composable("favorites") { FavoritesScreen(container, playbackController) }
                composable("playlists") { PlaylistsScreen(container) { id -> navController.navigate("playlist/$id") } }
                composable("collections") { CollectionsScreen(container, { route -> navController.navigate(route) }, playbackController) }
                composable("metadata") { MetadataScreen(container) }
                composable("diagnostics") { DiagnosticsScreen(container) }
                composable("queue") { QueueScreen(container.appContext) }
                composable("listening-watching-stats") { StatsScreen(container, onOpenMedia = { item ->
                    playbackController.play(item)
                    navController.navigate("player")
                }, onBack = { navController.popBackStack() }) }
                composable("folders") { FoldersScreen(container, libraryViewModel) { navController.navigate("folder/$it") } }
                composable("my-playlists") { MyPlaylistsScreen(container) { navController.navigate("music-playlist/$it") } }
                composable("music-playlist/{id}") { entry -> PlaylistDetailScreen(container, entry.arguments?.getString("id").orEmpty(), playbackController) }
                composable("playlist/{id}") { entry -> PlaylistDetailScreen(container, entry.arguments?.getString("id").orEmpty(), playbackController) }
                composable("recent/{type}") { entry -> RecentScreen(entry.arguments?.getString("type").orEmpty(), libraryViewModel, playbackController, { navController.navigate("video/$it") }, container) }
                composable("most-played") { FeatureCollectionScreen("Most played", "Your most-played local music", { container.collections.mostPlayed() }, playbackController) }
                composable("for-you") { FeatureCollectionScreen("For you now", "Press play & tuned in", { container.collections.forYou() }, playbackController) }
                composable("rediscover") { FeatureCollectionScreen("Rediscover", "From your previous plays", { container.collections.rediscover() }, playbackController) }
                composable("rediscover/rewind") { FeatureCollectionScreen("Worth the rewind", "Previous favorites and repeats", { container.collections.rediscover() }, playbackController) }
                composable("tuned-in") { FeatureCollectionScreen("Tuned in", "Your recent listening", { container.collections.tunedIn() }, playbackController) }
                composable("rock-your-world") { FeatureCollectionScreen("Rock your world", "Turn it up", { container.collections.rockYourWorld() }, playbackController) }
                composable("smart-collections") { CollectionsScreen(container, { route -> navController.navigate(route) }, playbackController) }
                composable("video-collections") { FeatureCollectionScreen("Video collections", "Continue watching and unwatched videos", { container.collections.continueWatching() + container.collections.unwatched() }, playbackController) }
                composable("album") { BrowseIndexScreen("Albums", com.airplayr.app.core.data.media.MediaType.AUDIO, libraryViewModel, playbackController) { navController.navigate("album/${it}") } }
                composable("artist") { BrowseIndexScreen("Artists", com.airplayr.app.core.data.media.MediaType.AUDIO, libraryViewModel, playbackController) { navController.navigate("artist/${it}") } }
                composable("genre") { BrowseIndexScreen("Genres", com.airplayr.app.core.data.media.MediaType.AUDIO, libraryViewModel, playbackController) { navController.navigate("genre/${it}") } }
                composable("music-folder/{id}") { entry -> BrowseFilterScreen("Folders", java.net.URLDecoder.decode(entry.arguments?.getString("id").orEmpty(), "UTF-8"), com.airplayr.app.core.data.media.MediaType.AUDIO, libraryViewModel, playbackController) }
                composable("folder/{id}") { entry -> BrowseFilterScreen("Folders", java.net.URLDecoder.decode(entry.arguments?.getString("id").orEmpty(), "UTF-8"), com.airplayr.app.core.data.media.MediaType.AUDIO, libraryViewModel, playbackController) }
                composable("album/{id}") { entry -> BrowseFilterScreen("Albums", java.net.URLDecoder.decode(entry.arguments?.getString("id").orEmpty(), "UTF-8"), com.airplayr.app.core.data.media.MediaType.AUDIO, libraryViewModel, playbackController) }
                composable("artist/{id}") { entry -> BrowseFilterScreen("Artists", java.net.URLDecoder.decode(entry.arguments?.getString("id").orEmpty(), "UTF-8"), com.airplayr.app.core.data.media.MediaType.AUDIO, libraryViewModel, playbackController) }
                composable("genre/{id}") { entry -> BrowseFilterScreen("Genres", java.net.URLDecoder.decode(entry.arguments?.getString("id").orEmpty(), "UTF-8"), com.airplayr.app.core.data.media.MediaType.AUDIO, libraryViewModel, playbackController) }
                composable("open") { LibraryScreen("Open media", com.airplayr.app.core.data.media.MediaType.AUDIO, libraryViewModel, onOpen = { playbackController.play(it); navController.navigate("player") }) }
                composable("video/{id}") { entry -> VideoScreen(container, entry.arguments?.getString("id").orEmpty()) }
            }

            // reference app's dock floats over the page rather than consuming layout height.
            // Keep that relationship in native so content and the mini-player share the same screen geometry.
            if (destination?.route != "player") {
                val dockState by playbackController.state.collectAsStateWithLifecycle()
                AirPlayrBottomDock(
                    currentRoute = destination?.route,
                    playbackState = dockState,
                    playback = playbackController,
                    onOpenPlayer = { navController.navigate("player") },
                    onNavigate = { route ->
                        navController.navigate(route) {
                            popUpTo(navController.graph.startDestinationId) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                )
            }

            StartupPermissionFlow(
                context = container.appContext,
                onCompleted = { permissionState ->
                    permissionFlowCompleted = true
                    if (permissionState.audio || permissionState.video) libraryViewModel.refresh()
                },
            )

            if (whatsNewOpen) {
                WhatsNewSheet(version = appVersion, onDismiss = {
                    whatsNewOpen = false
                    rootScope.launch { container.preferences.markWhatsNewSeen(appVersion) }
                })
            }
            if (castOpen) {
                ModalBottomSheet(
                    onDismissRequest = { castOpen = false },
                    containerColor = androidx.compose.ui.graphics.Color.Transparent,
                    scrimColor = MaterialTheme.colorScheme.scrim.copy(alpha = 0.46f),
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
