package com.airplayr.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.airplayr.app.di.AppContainer

@Composable fun HistoryScreen(container:AppContainer){val items by container.structured.history(100).collectAsStateWithLifecycle(emptyList()); Column(Modifier.fillMaxSize().padding(20.dp)){Text("History",style=MaterialTheme.typography.headlineSmall);Spacer(Modifier.height(12.dp));LazyColumn(verticalArrangement=Arrangement.spacedBy(8.dp)){items(items,key={it.mediaId}){h->ListItem(headlineContent={Text(h.title)},supportingContent={Text(listOfNotNull(h.artist,h.album).joinToString(" • ").ifBlank{"Played media"})},trailingContent={Text(h.playCount.toString())})}}}}
