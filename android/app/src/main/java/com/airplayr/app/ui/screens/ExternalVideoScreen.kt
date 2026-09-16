package com.airplayr.app.ui.screens

import android.app.Activity
import android.app.PictureInPictureParams
import android.os.Build
import android.view.ViewGroup
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.weight
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.Player
import androidx.media3.ui.PlayerView
import com.airplayr.app.di.AppContainer

@Composable
fun ExternalVideoScreen(
    container: AppContainer,
    uri: String,
    mimeType: String?,
    title: String,
) {
    val context = LocalContext.current
    var player by remember { mutableStateOf<Player?>(null) }

    LaunchedEffect(uri) {
        container.videoController.connectAsync { ready ->
            player = ready
            container.videoController.playExternal(ready, uri, mimeType, title)
        }
    }

    if (Build.VERSION.SDK_INT >= 26) {
        androidx.compose.runtime.SideEffect {
            runCatching {
                (context as? Activity)?.setPictureInPictureParams(
                    PictureInPictureParams.Builder()
                        .setAspectRatio(android.util.Rational(16, 9))
                        .build(),
                )
            }
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            player?.pause()
            container.videoController.release()
        }
    }

    Column(Modifier.fillMaxSize()) {
        player?.let {
            AndroidView(
                factory = { ctx ->
                    PlayerView(ctx).apply {
                        layoutParams = ViewGroup.LayoutParams(-1, -1)
                        useController = true
                        this.player = it
                    }
                },
                update = { view -> view.player = it },
                modifier = Modifier.fillMaxWidth().weight(1f),
            )
        } ?: Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Loading video…")
        }
    }
}
