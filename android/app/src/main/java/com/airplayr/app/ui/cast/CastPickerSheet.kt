package com.airplayr.app.ui.cast

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cast
import androidx.compose.material.icons.filled.CastConnected
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.mediarouter.app.MediaRouteButton
import com.google.android.gms.cast.framework.CastButtonFactory
import com.airplayr.app.core.cast.CastController
import com.airplayr.app.ui.components.LiquidGlassSurface

@Composable
fun CastPickerSheet(
    controller: CastController,
    onDismiss: () -> Unit,
) {
    val status by controller.status.collectAsState()
    LaunchedEffect(Unit) { controller.start() }
    LiquidGlassSurface(
        modifier = Modifier.fillMaxWidth().padding(14.dp),
        shape = RoundedCornerShape(30.dp),
    ) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("OUTPUT", style = MaterialTheme.typography.labelSmall)
                    Text("Cast AirPlayr", style = MaterialTheme.typography.headlineSmall)
                }
                IconButton(onClick = onDismiss) { Icon(Icons.Default.Close, "Close") }
            }
            LiquidGlassSurface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                tint = MaterialTheme.colorScheme.surfaceContainerLow.copy(alpha = 0.72f),
            ) {
                Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Icon(if (status.connected) Icons.Default.CastConnected else Icons.Default.Cast, null, Modifier.size(28.dp), tint = MaterialTheme.colorScheme.primary)
                    Column(Modifier.weight(1f)) {
                        Text(if (status.connected) (status.deviceName.ifBlank { "Cast device" }) else "Cast devices", style = MaterialTheme.typography.titleMedium)
                        Text(status.copyText, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
            AndroidView(
                modifier = Modifier.size(56.dp),
                factory = { context ->
                    MediaRouteButton(context).also { CastButtonFactory.setUpMediaRouteButton(context, it) }
                },
            )
            if (status.connected) {
                Button(onClick = { controller.endSession(); onDismiss() }, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Default.CastConnected, null)
                    Text("Disconnect", modifier = Modifier.padding(start = 8.dp))
                }
            }
            Text("Choose a Cast device using Android's system Cast picker. AirPlayr keeps the connection state in sync with the player.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
