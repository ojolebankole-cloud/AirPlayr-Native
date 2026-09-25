package com.airplayr.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.airplayr.app.di.AppContainer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

@Composable
fun VaultVideoScreen(
    container: AppContainer,
    id: String,
    onCast: () -> Unit = {},
    onNavigate: (String) -> Unit = {},
) {
    var prepared by remember(id) { mutableStateOf<Triple<File, String?, String>?>(null) }
    var failed by remember(id) { mutableStateOf(false) }
    LaunchedEffect(id) {
        val result = withContext(Dispatchers.IO) { container.vaultManager.decryptToCache(id) }
        if (result == null) failed = true else prepared = Triple(result.first, result.second.mimeType, result.second.title)
    }
    val item = prepared
    DisposableEffect(item?.first?.absolutePath) {
        onDispose { item?.first?.delete() }
    }
    if (failed) {
        Box(Modifier.fillMaxSize().background(Color.Black), contentAlignment = Alignment.Center) { Text("This Vault video could not be opened.", color = Color.White, fontSize = 15.sp) }
    } else if (item == null) {
        Box(Modifier.fillMaxSize().background(Color.Black), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = Color.White, strokeWidth = 2.dp) }
    } else {
        ExternalVideoScreen(container, android.net.Uri.fromFile(item.first).toString(), item.second, item.third, onCast, onNavigate)
    }
}
