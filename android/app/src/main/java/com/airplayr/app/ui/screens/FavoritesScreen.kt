package com.airplayr.app.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.airplayr.app.core.data.media.MediaType
import com.airplayr.app.di.AppContainer
import com.airplayr.app.ui.playback.PlaybackController

@Composable fun FavoritesScreen(container:AppContainer, playback:PlaybackController){
    val favs by container.structured.favorites().collectAsStateWithLifecycle(emptyList())
    val audio=container.mediaLibrary.observe(MediaType.AUDIO).collectAsStateWithLifecycle(emptyList()).value
    val video=container.mediaLibrary.observe(MediaType.VIDEO).collectAsStateWithLifecycle(emptyList()).value
    val lookup=(audio+video).associateBy{it.id}; val items=favs.mapNotNull{lookup[it.mediaId]}
    Column(Modifier.fillMaxSize().padding(20.dp)){Text("Favorites",style=MaterialTheme.typography.headlineSmall);Spacer(Modifier.height(12.dp));LazyColumn(verticalArrangement=Arrangement.spacedBy(8.dp)){items(items,key={it.id}){item->ListItem(headlineContent={Text(item.title)},supportingContent={Text(listOfNotNull(item.artist,item.album).joinToString(" • ").ifBlank{item.filename})},modifier=Modifier.clickable{if(item.mediaType==MediaType.AUDIO) playback.play(item)})}}}
}
