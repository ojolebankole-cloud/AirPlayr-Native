package com.airplayr.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PlaylistPlay
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.airplayr.app.di.AppContainer
import com.airplayr.app.ui.components.AirPlayrCard
import com.airplayr.app.ui.components.AirPlayrScreenHeader
import com.airplayr.app.ui.components.AirPlayrSectionEmpty
import com.airplayr.app.ui.navigation.LibraryViewModel
import com.airplayr.app.ui.theme.AirPlayrFonts
import com.airplayr.app.ui.theme.LocalAirPlayrColors
import kotlinx.coroutines.launch

@Composable
fun PlaylistsScreen(container: AppContainer, libraryViewModel: LibraryViewModel, onOpen: (String) -> Unit) {
    val scope = rememberCoroutineScope()
    val snapshot by libraryViewModel.snapshot.collectAsStateWithLifecycle()
    val lists = snapshot.playlists
    var name by remember { mutableStateOf("") }
    var deleting by remember { mutableStateOf<com.airplayr.app.core.data.db.MusicPlaylistEntity?>(null) }
    val c = LocalAirPlayrColors.current
    Column(Modifier.fillMaxSize().background(c.background)) {
        AirPlayrScreenHeader("Playlists", "Create and organize your music")
        AirPlayrCard(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp), shape = RoundedCornerShape(26.dp), containerColor = c.surfaceSolid, elevation = 6.dp) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("New playlist", fontFamily = AirPlayrFonts.SmoochSans, fontSize = 18.sp, color = c.textPrimary)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(name, { name = it }, Modifier.weight(1f), singleLine = true, placeholder = { Text("Playlist name") })
                    Spacer(Modifier.width(8.dp))
                    Button(enabled = name.isNotBlank(), onClick = { scope.launch { container.structured.createPlaylist(name.trim()); name = "" } }) { Text("Create") }
                }
            }
        }
        if (!snapshot.ready) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = c.primary) }
        } else if (lists.isEmpty()) AirPlayrSectionEmpty("No playlists yet", "Create your first playlist above.")
        else LazyColumn(state = rememberSaveable(saver = LazyListState.Saver) { LazyListState(0) },contentPadding = PaddingValues(bottom = 24.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(lists, key = { it.id }) { playlist ->
                AirPlayrCard(Modifier.fillMaxWidth().padding(horizontal = 16.dp), shape = RoundedCornerShape(22.dp), containerColor = c.surfaceSolid, elevation = 5.dp, onClick = { onOpen(playlist.id) }) {
                    Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                        Surface(Modifier.size(52.dp), shape = RoundedCornerShape(16.dp), color = c.primaryContainer) { Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Icon(Icons.Default.PlaylistPlay, null, tint = c.primary, modifier = Modifier.size(28.dp)) } }
                        Column(Modifier.weight(1f).padding(horizontal = 14.dp)) {
                            Text(playlist.name, fontFamily = AirPlayrFonts.SmoochSans, fontSize = 18.sp, color = c.textPrimary, maxLines = 1)
                            Text("Created ${formatDate(playlist.createdAt)} · updated ${formatDate(playlist.updatedAt)}", fontFamily = AirPlayrFonts.OpenSans, fontSize = 12.sp, color = c.textSecondary, maxLines = 1)
                        }
                        IconButton(onClick = { deleting = playlist }) { Icon(Icons.Default.Delete, "Delete", tint = c.primary) }
                    }
                }
            }
        }
    }
    deleting?.let { playlist ->
        AlertDialog(onDismissRequest = { deleting = null }, title = { Text("Delete playlist?") }, text = { Text("Delete ${playlist.name}? The songs remain on your device.") }, dismissButton = { TextButton(onClick = { deleting = null }) { Text("Cancel") } }, confirmButton = { TextButton(onClick = { scope.launch { container.structured.deletePlaylist(playlist.id); deleting = null } }) { Text("Delete", color = c.error) } })
    }
}
private fun formatDate(ms: Long): String = java.text.SimpleDateFormat("MMM d", java.util.Locale.getDefault()).format(java.util.Date(ms))
