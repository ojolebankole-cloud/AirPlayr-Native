package com.airplayr.app.ui.cast

import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cast
import androidx.compose.material.icons.filled.CastConnected
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.mediarouter.app.MediaRouteButton
import com.google.android.gms.cast.framework.CastButtonFactory
import com.airplayr.app.core.cast.CastController
import com.airplayr.app.ui.theme.AirPlayrFonts
import com.airplayr.app.ui.theme.LocalAirPlayrColors

@Composable
fun CastPickerSheet(
    controller: CastController,
    onDismiss: () -> Unit,
) {
    val colors = LocalAirPlayrColors.current
    val context = LocalContext.current
    val status by controller.status.collectAsState()
    var routeButton by remember { mutableStateOf<MediaRouteButton?>(null) }
    var routeButtonReady by remember { mutableStateOf(false) }
    var pickerRequested by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { controller.start() }
    LaunchedEffect(pickerRequested, routeButtonReady) {
        if (pickerRequested && routeButtonReady) {
            runCatching { routeButton?.performClick() }
                .onFailure { context.startActivity(Intent(Settings.ACTION_WIFI_SETTINGS)) }
            pickerRequested = false
        }
    }

    Card(
        modifier = Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = 12.dp, vertical = 10.dp),
        shape = RoundedCornerShape(32.dp),
        colors = CardDefaults.cardColors(containerColor = colors.surfaceContainerLow),
        elevation = CardDefaults.cardElevation(0.dp),
    ) {
        Column(Modifier.fillMaxWidth().padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("AIRPLAYR CAST", fontFamily = AirPlayrFonts.OpenSans, fontSize = 11.sp, letterSpacing = 1.4.sp, color = colors.primary)
                    Text("Play somewhere bigger", fontFamily = AirPlayrFonts.SmoochSans, fontSize = 29.sp, fontWeight = FontWeight.SemiBold, color = colors.textPrimary)
                }
                IconButton(onClick = onDismiss) { Icon(Icons.Default.Close, "Close", tint = colors.textPrimary) }
            }

            val connected = status.connected
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(28.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (connected) colors.primaryContainer else colors.surfaceContainer,
                ),
                elevation = CardDefaults.cardElevation(0.dp),
            ) {
                Column(Modifier.fillMaxWidth().padding(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Box(Modifier.fillMaxWidth().height(132.dp).clip(RoundedCornerShape(22.dp)).background(
                        Brush.linearGradient(listOf(colors.primary.copy(.95f), colors.secondary.copy(.90f), colors.tertiary.copy(.88f)))
                    )) {
                        Box(Modifier.align(Alignment.Center).size(74.dp).clip(CircleShape).background(Color.White.copy(alpha = .16f)), contentAlignment = Alignment.Center) {
                            Icon(if (connected) Icons.Default.CastConnected else Icons.Default.Cast, null, tint = Color.White, modifier = Modifier.size(38.dp))
                        }
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(
                                when {
                                    connected -> status.deviceName.ifBlank { "Cast device" }
                                    status.state == "connecting" -> "Connecting…"
                                    status.state == "no-devices" -> "No device found yet"
                                    status.state == "unavailable" -> "Cast unavailable on this device"
                                    else -> "Ready to connect"
                                },
                                fontFamily = AirPlayrFonts.SmoochSans, fontSize = 22.sp, color = colors.textPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis,
                            )
                            Text(
                                when {
                                    connected -> "AirPlayr is casting to this device."
                                    status.state == "connecting" -> "Waiting for the Cast session to start."
                                    status.state == "no-devices" -> "Put your phone and TV or receiver on the same Wi-Fi network."
                                    status.state == "unavailable" -> "Google Play services Cast support is unavailable; AirPlayr will remain open."
                                    else -> "Choose a device from the Android Cast picker."
                                },
                                fontFamily = AirPlayrFonts.OpenSans, fontSize = 13.sp, lineHeight = 19.sp, color = colors.textSecondary, modifier = Modifier.padding(top = 3.dp),
                            )
                        }
                    }
                }
            }

            // Do not initialize MediaRouteButton while the Cast sheet is merely opening.
            // The system Cast route is created lazily only after the user explicitly asks for
            // a device, preventing intermittent Cast-framework initialization crashes from
            // taking down the app when the top-level Cast icon is tapped.
            if (pickerRequested && !routeButtonReady) {
                AndroidView(
                    modifier = Modifier.size(1.dp),
                    factory = { ctx ->
                        MediaRouteButton(ctx).also { button ->
                            runCatching {
                                CastButtonFactory.setUpMediaRouteButton(ctx, button)
                                routeButton = button
                                routeButtonReady = true
                            }.onFailure {
                                routeButtonReady = false
                                pickerRequested = false
                            }
                        }
                    },
                )
            }

            if (connected) {
                Button(
                    onClick = { controller.endSession(); onDismiss() },
                    modifier = Modifier.fillMaxWidth().height(54.dp),
                    shape = RoundedCornerShape(18.dp),
                ) {
                    Icon(Icons.Default.CastConnected, null)
                    Text("Disconnect", modifier = Modifier.padding(start = 9.dp), fontFamily = AirPlayrFonts.OpenSans)
                }
            } else {
                Button(
                    onClick = {
                        if (routeButtonReady) {
                            pickerRequested = true
                        } else {
                            pickerRequested = true
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(54.dp),
                    shape = RoundedCornerShape(18.dp),
                ) {
                    Icon(Icons.Default.Cast, null)
                    Text("Choose a Cast device", modifier = Modifier.padding(start = 9.dp), fontFamily = AirPlayrFonts.OpenSans)
                }
                OutlinedButton(
                    onClick = { context.startActivity(Intent(Settings.ACTION_WIFI_SETTINGS)) },
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    shape = RoundedCornerShape(18.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = colors.primary),
                ) {
                    Icon(Icons.Default.Wifi, null)
                    Text("Open Wi-Fi settings", modifier = Modifier.padding(start = 9.dp), fontFamily = AirPlayrFonts.OpenSans)
                }
            }

            Text(
                "Casting uses Android’s system Cast picker; AirPlayr keeps playback and connection state synced with the player.",
                fontFamily = AirPlayrFonts.OpenSans, fontSize = 11.sp, lineHeight = 17.sp, color = colors.textSecondary,
            )
        }
    }
}
