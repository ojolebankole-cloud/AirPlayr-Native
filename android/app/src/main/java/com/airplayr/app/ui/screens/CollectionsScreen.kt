package com.airplayr.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.airplayr.app.di.AppContainer
import kotlinx.coroutines.launch

@Composable fun CollectionsScreen(container:AppContainer){val scope=rememberCoroutineScope();var lines by remember{mutableStateOf(listOf<String>())};LaunchedEffect(Unit){lines=buildList{add("For You: ${container.collections.forYou().size}");add("Rediscover: ${container.collections.rediscover().size}");add("Tuned In: ${container.collections.tunedIn().size}");add("Rock Your World: ${container.collections.rockYourWorld().size}");add("Favorites: ${container.collections.favorites().size}");add("Recently added: ${container.collections.recentlyAdded().size}");add("Most played: ${container.collections.mostPlayed().size}");add("Continue watching: ${container.collections.continueWatching().size}");add("Unwatched: ${container.collections.unwatched().size}")} }; Column(Modifier.fillMaxSize().padding(20.dp)){Text("Smart Collections",style=MaterialTheme.typography.headlineSmall);Spacer(Modifier.height(12.dp));LazyColumn(verticalArrangement=Arrangement.spacedBy(8.dp)){items(lines.size){i->Card(Modifier.fillMaxWidth()){ListItem(headlineContent={Text(lines[i])},supportingContent={Text("Native Room-backed collection")})}}};Button({scope.launch{container.structured.clearHistory()}},Modifier.padding(top=12.dp)){Text("Clear history")}}}
