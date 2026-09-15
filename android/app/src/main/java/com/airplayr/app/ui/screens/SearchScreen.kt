package com.airplayr.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.airplayr.app.core.data.media.MediaItem
import com.airplayr.app.di.AppContainer
import com.airplayr.app.ui.components.AirPlayrCard
import com.airplayr.app.ui.components.AirPlayrMediaRow
import com.airplayr.app.ui.components.AirPlayrScreenHeader
import com.airplayr.app.ui.theme.AirPlayrFonts
import com.airplayr.app.ui.theme.LocalAirPlayrColors
import kotlinx.coroutines.flow.collectLatest

@Composable
fun SearchScreen(container: AppContainer, onOpen: (MediaItem) -> Unit) {
    val colors = LocalAirPlayrColors.current
    val focus = LocalFocusManager.current
    var query by remember { mutableStateOf("") }
    var filter by remember { mutableStateOf("all") }
    var results by remember { mutableStateOf<List<MediaItem>>(emptyList()) }
    val filters = listOf("all" to "All", "songs" to "Songs", "albums" to "Albums", "artists" to "Artists", "playlists" to "Playlists", "genres" to "Genres", "videos" to "Videos")

    LaunchedEffect(query, filter) {
        val q = query.trim()
        if (q.length < 2) { results = emptyList(); return@LaunchedEffect }
        container.search.search(q).collectLatest { incoming ->
            results = when (filter) {
                "videos" -> incoming.filter { it.mediaType.name.equals("VIDEO", true) }
                "songs" -> incoming.filter { it.mediaType.name.equals("AUDIO", true) }
                else -> incoming
            }
        }
    }

    Column(Modifier.fillMaxSize().background(colors.background)) {
        AirPlayrScreenHeader("Search", "Find songs, albums, artists and more")
        Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
            Surface(Modifier.weight(1f).height(48.dp), shape = RoundedCornerShape(24.dp), color = colors.surfaceContainerHigh, tonalElevation = 0.dp) {
                Row(Modifier.fillMaxSize().padding(horizontal = 14.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Search, null, tint = colors.primary, modifier = Modifier.size(22.dp))
                    androidx.compose.foundation.text.BasicTextField(
                        value = query,
                        onValueChange = { query = it },
                        modifier = Modifier.weight(1f).padding(horizontal = 10.dp),
                        singleLine = true,
                        textStyle = LocalTextStyle.current.copy(color = colors.textPrimary, fontSize = 17.sp, fontFamily = AirPlayrFonts.OpenSans),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                        decorationBox = { inner -> if (query.isEmpty()) Text("Search your music", color = colors.textSecondary, fontSize = 15.sp, fontFamily = AirPlayrFonts.SmoochSans); inner() },
                    )
                    if (query.isNotEmpty()) IconButton(onClick = { query = ""; focus.clearFocus() }, modifier = Modifier.size(32.dp)) { Icon(Icons.Default.Close, "Clear", tint = colors.primary) }
                }
            }
        }
        Row(Modifier.horizontalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 10.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            filters.forEach { (key, label) ->
                FilterChip(selected = filter == key, onClick = { filter = key }, label = { Text(label, fontFamily = AirPlayrFonts.SmoochSans, fontSize = 15.sp) }, shape = RoundedCornerShape(23.dp))
            }
        }

        if (query.trim().length < 2) {
            Column(Modifier.fillMaxWidth().padding(top = 50.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Default.Search, null, Modifier.size(42.dp), tint = colors.primary)
                Text("Search your library", fontFamily = AirPlayrFonts.SmoochSans, fontSize = 26.sp, color = colors.textPrimary, modifier = Modifier.padding(top = 14.dp))
                Text("Start typing to find your music and media.", fontFamily = AirPlayrFonts.OpenSans, fontSize = 13.sp, color = colors.textPrimary, modifier = Modifier.padding(top = 6.dp))
            }
        } else if (results.isEmpty()) {
            Column(Modifier.fillMaxWidth().padding(top = 50.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Default.Search, null, Modifier.size(42.dp), tint = colors.primary)
                Text("No results found", fontFamily = AirPlayrFonts.SmoochSans, fontSize = 26.sp, color = colors.textPrimary, modifier = Modifier.padding(top = 14.dp))
                Text("Nothing matched “${query.trim()}”.", fontFamily = AirPlayrFonts.OpenSans, fontSize = 13.sp, color = colors.textPrimary, modifier = Modifier.padding(top = 6.dp))
            }
        } else {
            LazyColumn(contentPadding = PaddingValues(bottom = 110.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                item { Text("RESULTS", fontFamily = AirPlayrFonts.SmoochSans, fontSize = 12.sp, letterSpacing = 1.1.sp, color = colors.textPrimary, modifier = Modifier.padding(horizontal = 24.dp, vertical = 10.dp)) }
                items(results, key = { it.id }) { item ->
                    AirPlayrCard(Modifier.fillMaxWidth().padding(horizontal = 8.dp), shape = RoundedCornerShape(18.dp), containerColor = colors.surfaceSolid, elevation = 0.dp, onClick = { onOpen(item) }) {
                        AirPlayrMediaRow(item, onOpen = { onOpen(item) }, compact = false)
                    }
                }
            }
        }
    }
}
