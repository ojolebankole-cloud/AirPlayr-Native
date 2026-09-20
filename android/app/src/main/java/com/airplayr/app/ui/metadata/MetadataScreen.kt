package com.airplayr.app.ui.metadata

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Box
import androidx.compose.ui.Alignment
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.airplayr.app.core.data.media.MediaItem
import com.airplayr.app.core.metadata.MetadataMatch
import com.airplayr.app.di.AppContainer
import com.airplayr.app.ui.components.airPlayrShadow
import com.airplayr.app.ui.navigation.LibraryViewModelFactory
import com.airplayr.app.ui.navigation.LibraryViewModel
import kotlinx.coroutines.launch
import androidx.compose.runtime.rememberCoroutineScope

@Composable
fun MetadataScreen(container: AppContainer, libraryViewModel: LibraryViewModel) {
    val snapshot by libraryViewModel.snapshot.collectAsStateWithLifecycle()
    val audio = snapshot.audio
    var selected by remember { mutableStateOf<MediaItem?>(null) }
    var title by remember { mutableStateOf("") }
    var artist by remember { mutableStateOf("") }
    var album by remember { mutableStateOf("") }
    var results by remember { mutableStateOf<List<MetadataMatch>>(emptyList()) }
    var status by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()

    LaunchedEffect(selected?.id) {
        val item = selected ?: return@LaunchedEffect
        title = item.title; artist = item.artist.orEmpty(); album = item.album.orEmpty(); results = emptyList(); status = ""
    }

    Column(Modifier.fillMaxSize()) {
        LazyColumn(state = rememberSaveable(saver = LazyListState.Saver) { LazyListState(0) },contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = androidx.compose.foundation.layout.WindowInsets.statusBars.asPaddingValues().calculateTopPadding() + 20.dp, bottom = 24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            item { Text("Online Metadata", style = MaterialTheme.typography.headlineMedium) }
            if (!snapshot.ready) {
                item { Box(Modifier.fillMaxWidth().padding(vertical = 48.dp), contentAlignment = Alignment.Center) { CircularProgressIndicator() } }
            } else if (selected == null) {
                item { Text("Choose a music item to look up and optionally apply catalog metadata.", color = MaterialTheme.colorScheme.onSurfaceVariant) }
                items(audio.take(250), key = { it.id }) { item ->
                    Card(Modifier.fillMaxWidth().airPlayrShadow(RoundedCornerShape(12.dp), 4.dp)) {
                        Row(Modifier.fillMaxWidth().padding(14.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                            Column(Modifier.weight(1f)) { Text(item.title, style = MaterialTheme.typography.titleMedium); Text(listOfNotNull(item.artist, item.album).joinToString(" • ").ifBlank { item.filename }, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                            Button(onClick = { selected = item }) { Text("Select") }
                        }
                    }
                }
            } else {
                item { Text("Selected: ${selected!!.title}", style = MaterialTheme.typography.titleMedium) }
                item { OutlinedButton(onClick = { selected = null; results = emptyList() }) { Text("Choose another") } }
                item { OutlinedTextField(title, { title = it }, Modifier.fillMaxWidth(), label = { Text("Title") }) }
                item { OutlinedTextField(artist, { artist = it }, Modifier.fillMaxWidth(), label = { Text("Artist") }) }
                item { OutlinedTextField(album, { album = it }, Modifier.fillMaxWidth(), label = { Text("Album") }) }
                item {
                    Button(onClick = { scope.launch { status = "Searching…"; val item = selected!!.copy(title = title, artist = artist.ifBlank { null }, album = album.ifBlank { null }); results = runCatching { container.metadata.search(item, force = true) }.getOrDefault(emptyList()); status = if (results.isEmpty()) "No matches" else "${results.size} matches" } }) { Text("Search") }
                    if (status.isNotBlank()) Text(status, Modifier.padding(top = 8.dp), color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                items(results, key = { "${it.provider}:${it.id}" }) { match ->
                    Card(Modifier.fillMaxWidth().airPlayrShadow(RoundedCornerShape(12.dp), 4.dp)) {
                        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(match.title, style = MaterialTheme.typography.titleMedium)
                            Text("${match.artist} • ${match.album}", color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("Score ${match.score} • ${match.genre.ifBlank { "Unknown genre" }}")
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Button(onClick = { scope.launch { status = if (container.metadata.downloadArtwork(match) != null) "Artwork cached" else "Artwork unavailable" } }) { Text("Cache artwork") }
                                Button(onClick = { scope.launch { val item = selected!!; val ok = container.metadata.applyToMediaStore(item, match); if (ok) runCatching { container.mediaLibrary.deltaRefresh(item.mediaType) }; status = if (ok) "Metadata applied and library refreshed" else "Metadata update failed" } }) { Text("Apply to file") }
                            }
                        }
                    }
                }
            }
        }
    }
}
