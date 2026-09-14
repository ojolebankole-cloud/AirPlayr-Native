package com.airplayr.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.airplayr.app.di.AppContainer
import kotlinx.coroutines.launch

@Composable fun PlaylistsScreen(container:AppContainer){
    val scope=rememberCoroutineScope(); val lists by container.structured.playlists().collectAsStateWithLifecycle(emptyList()); var name by remember{mutableStateOf("")}
    Column(Modifier.fillMaxSize().padding(20.dp)){Text("Playlists",style=MaterialTheme.typography.headlineSmall);Spacer(Modifier.height(12.dp));OutlinedTextField(name,{name=it},Modifier.fillMaxWidth(),singleLine=true,label={Text("New playlist name")});Button({scope.launch{container.structured.createPlaylist(name);name=""}},enabled=name.isNotBlank(),modifier=Modifier.padding(vertical=10.dp)){Text("Create playlist")};LazyColumn(verticalArrangement=Arrangement.spacedBy(8.dp)){items(lists,key={it.id}){p->Card(Modifier.fillMaxWidth()){ListItem(headlineContent={Text(p.name)},supportingContent={Text("Updated ${p.updatedAt}")})}}}}
}
