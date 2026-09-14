package com.airplayr.app.ui

import android.content.Intent
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
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
import com.airplayr.app.ui.metadata.MetadataScreen
import com.airplayr.app.ui.diagnostics.DiagnosticsScreen
import com.airplayr.app.ui.theme.AirPlayrTheme

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

    androidx.compose.runtime.LaunchedEffect(launchIntent) {
        initialViewModel.handleIntent(launchIntent)
    }
    val requestedRoute by initialViewModel.route.collectAsStateWithLifecycle()
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
        val entry by navController.currentBackStackEntryAsState()
        val destination = entry?.destination
        Scaffold(
            bottomBar = {
                AirPlayrBottomDock(
                    currentRoute = destination?.route,
                    onNavigate = { route ->
                        navController.navigate(route) {
                            popUpTo(navController.graph.startDestinationId) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                )
            },
        ) { padding ->
            NavHost(
                navController = navController,
                startDestination = "home",
                modifier = androidx.compose.ui.Modifier.padding(padding),
            ) {
                composable("home") { HomeScreen(container = container, libraryViewModel = libraryViewModel) { route -> navController.navigate(route) } }
                composable("music") {
                    MusicScreen(
                        libraryViewModel,
                        container.appContext,
                        onCategory = { category ->
                            when (category) {
                                "albums" -> navController.navigate("album")
                                "artists" -> navController.navigate("artist")
                                "genres" -> navController.navigate("genre")
                                "folders" -> navController.navigate("folders")
                                "playlists" -> navController.navigate("my-playlists")
                                "likes" -> navController.navigate("favorites")
                            }
                        },
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
                    SettingsScreen(container = container, onDiagnostics = { navController.navigate("diagnostics") }, onMetadata = { navController.navigate("metadata") })
                }
                composable("player") { PlayerScreen(container.appContext) { navController.navigate("equalizer") } }
                composable("equalizer") { EqualizerScreen(container) }
                composable("search") { SearchScreen(container) { item -> if (item.mediaType == com.airplayr.app.core.data.media.MediaType.VIDEO) navController.navigate("video/${item.id}") else { playbackController.play(item); navController.navigate("player") } } }
                composable("history") { HistoryScreen(container) }
                composable("favorites") { FavoritesScreen(container, playbackController) }
                composable("playlists") { PlaylistsScreen(container) }
                composable("collections") { CollectionsScreen(container) }
                composable("metadata") { MetadataScreen(container) }
                composable("diagnostics") { DiagnosticsScreen(container) }
                composable("queue") { QueueScreen(container.appContext) }
                composable("listening-watching-stats") { StatsScreen(container) }
                composable("folders") { FoldersScreen(container, libraryViewModel) { navController.navigate("folder/$it") } }
                composable("my-playlists") { MyPlaylistsScreen(container) { navController.navigate("music-playlist/$it") } }
                composable("music-playlist/{id}") { entry -> PlaylistDetailScreen(container, entry.arguments?.getString("id").orEmpty(), playbackController) }
                composable("playlist/{id}") { entry -> PlaylistDetailScreen(container, entry.arguments?.getString("id").orEmpty(), playbackController) }
                composable("recent/{type}") { entry -> RecentScreen(entry.arguments?.getString("type").orEmpty(), libraryViewModel, playbackController) { navController.navigate("video/$it") } }
                composable("most-played") { FeatureCollectionScreen("Most played", "Your most-played local music", { container.collections.mostPlayed() }, playbackController) }
                composable("for-you") { FeatureCollectionScreen("For you now", "Press play & tuned in", { container.collections.forYou() }, playbackController) }
                composable("rediscover") { FeatureCollectionScreen("Rediscover", "From your previous plays", { container.collections.rediscover() }, playbackController) }
                composable("rediscover/rewind") { FeatureCollectionScreen("Worth the rewind", "Previous favorites and repeats", { container.collections.rediscover() }, playbackController) }
                composable("tuned-in") { FeatureCollectionScreen("Tuned in", "Your recent listening", { container.collections.tunedIn() }, playbackController) }
                composable("rock-your-world") { FeatureCollectionScreen("Rock your world", "Turn it up", { container.collections.rockYourWorld() }, playbackController) }
                composable("smart-collections") { CollectionsScreen(container) }
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
        }
    }
}

@Composable
private fun AirPlayrBottomDock(
    currentRoute: String?,
    onNavigate: (String) -> Unit,
) {
    val items = listOf(
        NavItem("home", "Home", androidx.compose.material.icons.Icons.Filled.Home),
        NavItem("music", "Music", androidx.compose.material.icons.Icons.Filled.MusicNote),
        NavItem("videos", "Videos", androidx.compose.material.icons.Icons.Filled.VideoLibrary),
        NavItem("settings", "Settings", androidx.compose.material.icons.Icons.Filled.Settings),
    )
    androidx.compose.material3.NavigationBar(
        windowInsets = androidx.compose.foundation.layout.WindowInsets.navigationBars,
    ) {
        items.forEach { item ->
            val selected = currentRoute == item.route || currentRoute?.startsWith("${item.route}/") == true
            androidx.compose.material3.NavigationBarItem(
                selected = selected,
                onClick = { onNavigate(item.route) },
                icon = { androidx.compose.material3.Icon(item.icon, contentDescription = item.label) },
                label = { androidx.compose.material3.Text(item.label) },
            )
        }
    }
}

private data class NavItem(
    val route: String,
    val label: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
)
