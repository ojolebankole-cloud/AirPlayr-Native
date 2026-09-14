package com.airplayr.app.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.airplayr.app.core.data.media.MediaItem
import com.airplayr.app.di.AppContainer
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@Composable fun SearchScreen(container: AppContainer, onOpen: (MediaItem) -> Unit) {
    var query by remember { mutableStateOf("") }
    var results by remember { mutableStateOf<List<MediaItem>>(emptyList()) }
    LaunchedEffect(query) {
        val q = query.trim()
        if (q.length < 2) { results = emptyList(); return@LaunchedEffect }
        container.search.search(q).collectLatest { results = it }
    }
    Column(Modifier.fillMaxSize().padding(20.dp)) {
        OutlinedTextField(value=query,onValueChange={query=it},modifier=Modifier.fillMaxWidth(),singleLine=true,label={Text("Search music and video")},leadingIcon={Icon(Icons.Default.Search,null)})
        Spacer(Modifier.height(12.dp))
        LazyColumn(verticalArrangement=Arrangement.spacedBy(8.dp)) { items(results,key={it.id}) { item ->
            ListItem(headlineContent={Text(item.title)},supportingContent={Text(listOfNotNull(item.artist,item.album).joinToString(" • ").ifBlank{item.filename})},modifier=Modifier.clickable{onOpen(item)})
        } }
    }
}
