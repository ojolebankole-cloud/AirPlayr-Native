package com.airplayr.app.ui.cast

import android.content.Context
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.mediarouter.app.MediaRouteButton
import com.google.android.gms.cast.framework.CastButtonFactory

@Composable
fun CastButton(modifier: Modifier = Modifier) {
    AndroidView(
        modifier = modifier.size(48.dp),
        factory = { context: Context ->
            MediaRouteButton(context).also { button ->
                runCatching { CastButtonFactory.setUpMediaRouteButton(context, button) }
            }
        },
    )
}
