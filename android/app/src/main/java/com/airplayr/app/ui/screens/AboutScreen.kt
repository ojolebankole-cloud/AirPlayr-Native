package com.airplayr.app.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MailOutline
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material.icons.filled.Timeline
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.airplayr.app.ui.LocalAirPlayrBottomOverlayHeight
import com.airplayr.app.ui.components.WhatsNewSheet
import com.airplayr.app.ui.feedback.AppFeedback
import com.airplayr.app.ui.theme.AirPlayrFonts
import com.airplayr.app.ui.theme.AirPlayrIcons
import com.airplayr.app.ui.theme.AirPlayrRadii
import com.airplayr.app.ui.theme.LocalAirPlayrColors

private data class ContactAction(
    val label: String,
    val target: String?,
    val placeholder: Boolean = false,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutScreen(
    onBack: () -> Unit,
    appVersion: String,
) {
    val colors = LocalAirPlayrColors.current
    val context = LocalContext.current
    val bottomOverlay = LocalAirPlayrBottomOverlayHeight.current
    var donateOpen by remember { mutableStateOf(false) }
    var whatsNewOpen by remember { mutableStateOf(false) }
    var privacyOpen by remember { mutableStateOf(false) }
    var copyrightOpen by remember { mutableStateOf(false) }
    var loved by remember { mutableStateOf(false) }

    fun openTarget(target: String) {
        runCatching {
            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(target)))
        }.onFailure {
            AppFeedback.showSnackbar("No app is available to open that link.")
        }
    }

    Column(
        Modifier
            .fillMaxSize()
            .background(colors.background)
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(bottom = bottomOverlay + 8.dp),
    ) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack, modifier = Modifier.size(48.dp)) {
                Icon(Icons.Default.ArrowBack, "Back", tint = colors.textPrimary, modifier = Modifier.size(28.dp))
            }
            Text("About", style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold), color = colors.textPrimary)
        }

        Column(
            Modifier.fillMaxWidth().weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 28.dp),
            verticalArrangement = Arrangement.spacedBy(22.dp),
        ) {
            Spacer(Modifier.height(2.dp))
            AppIdentityCard(
                colors = colors,
                appVersion = appVersion,
                onInstagram = { openTarget("https://instagram.com/koled_45") },
                onX = { openTarget("https://x.com/koled_45") },
            )

            DeveloperCard(
                colors = colors,
                onThread = { openTarget("https://www.threads.net/@koled_45") },
                onReddit = { openTarget("https://www.reddit.com/user/koled_45/") },
                onTelegram = { openTarget("https://t.me/koled_45") },
                onEmail = { openTarget("mailto:koledesigninfo@gmail.com") },
            )

            ContactCard(
                colors = colors,
                actions = listOf(
                    ContactAction("WhatsApp", "https://wa.me/2349163149653"),
                    ContactAction("TikTok", "https://www.tiktok.com/@koled_45"),
                    ContactAction("LinkedIn", null, placeholder = true),
                    ContactAction("Facebook", null, placeholder = true),
                ),
                onAction = { action -> action.target?.let(::openTarget) ?: AppFeedback.showSnackbar("${action.label} is coming soon.") },
            )

            SupportCard(
                colors = colors,
                loved = loved,
                onWhatsNew = { whatsNewOpen = true },
                onDonate = { loved = true; donateOpen = true },
                onFeedback = { openTarget("mailto:koledesigninfo@gmail.com?subject=AirPlayr%20feedback") },
                onLoveComment = { openTarget("https://wa.me/2349163149653?text=Hello%20Kole%20Design%20Dev%2C%20I%20would%20like%20to%20send%20some%20love%20for%20AirPlayr.%20%F0%9F%96%A4") },
                onPrivacy = { privacyOpen = true },
                onCopyright = { copyrightOpen = true },
                appVersion = appVersion,
            )

            Row(
                Modifier.fillMaxWidth().padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("Created with creativity", color = colors.textSecondary, fontSize = 15.sp)
                Spacer(Modifier.width(6.dp))
                Icon(AirPlayrIcons.Play, "Created with creativity", tint = colors.primary, modifier = Modifier.size(18.dp))
            }
            Spacer(Modifier.height(10.dp))
        }
    }

    if (whatsNewOpen) {
        WhatsNewSheet(
            version = appVersion,
            onDismiss = { whatsNewOpen = false },
        )
    }
    if (donateOpen) {
        DonateSheet(onDismiss = { donateOpen = false })
    }
    if (privacyOpen) {
        InfoSheet(
            title = "Privacy policy",
            icon = Icons.Default.Lock,
            text = "AirPlayr is designed around local media. Access to music, video and folders is granted by Android and used to build your on-device library. A full public privacy policy will be published here as the app's privacy documentation is finalized.",
            onDismiss = { privacyOpen = false },
        )
    }
    if (copyrightOpen) {
        InfoSheet(
            title = "Copyright",
            icon = Icons.Default.Info,
            text = "© 2026 Kole Design Dev. AirPlayr and its original interface work are part of this project. Media files remain the property of their respective owners.",
            onDismiss = { copyrightOpen = false },
        )
    }
}

@Composable
private fun AppIdentityCard(
    colors: com.airplayr.app.ui.theme.AirPlayrColors,
    appVersion: String,
    onInstagram: () -> Unit,
    onX: () -> Unit,
) {
    AboutCard(colors) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 26.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                Modifier.size(132.dp).clip(RoundedCornerShape(38.dp)).background(colors.primaryContainer),
                contentAlignment = Alignment.Center,
            ) {
                Icon(AirPlayrIcons.Music, "AirPlayr logo", tint = colors.primary, modifier = Modifier.size(74.dp))
            }
            Spacer(Modifier.height(20.dp))
            Text("AirPlayr", style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold), color = colors.textPrimary)
            Box(Modifier.clip(RoundedCornerShape(50.dp)).background(colors.surfaceContainer).padding(horizontal = 13.dp, vertical = 6.dp), contentAlignment = Alignment.Center) {
                Text("v$appVersion", color = colors.primary, fontSize = 12.sp)
            }
            Text("Local music and video, made personal.", color = colors.textSecondary, textAlign = TextAlign.Center, modifier = Modifier.padding(top = 5.dp))
            Spacer(Modifier.height(20.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                ContactButton("Instagram  ·  @koled_45", "◎", colors, Modifier.weight(1f), onInstagram)
                ContactButton("X (Twitter)  ·  @koled_45", "𝕏", colors, Modifier.weight(1f), onX)
            }
        }
    }
}

@Composable
private fun DeveloperCard(
    colors: com.airplayr.app.ui.theme.AirPlayrColors,
    onThread: () -> Unit,
    onReddit: () -> Unit,
    onTelegram: () -> Unit,
    onEmail: () -> Unit,
) {
    AboutCard(colors) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 22.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                Modifier.size(104.dp).clip(CircleShape).background(colors.primaryContainer),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Default.Person, "Developer", tint = colors.primary, modifier = Modifier.size(58.dp))
            }
            Spacer(Modifier.height(14.dp))
            Text("DEVELOPED AND DESIGNED BY", fontFamily = AirPlayrFonts.DancingScript, fontSize = 16.sp, color = colors.primary)
            Spacer(Modifier.height(5.dp))
            Text("Kole Design Dev", style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Medium), color = colors.textPrimary)
            Spacer(Modifier.height(15.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                SocialCircle("@", colors, onThread)
                SocialCircle("●", colors, onReddit)
                SocialCircle("✈", colors, onTelegram)
                SocialCircle("M", colors, onEmail)
            }
        }
    }
}

@Composable
private fun ContactCard(
    colors: com.airplayr.app.ui.theme.AirPlayrColors,
    actions: List<ContactAction>,
    onAction: (ContactAction) -> Unit,
) {
    AboutCard(colors) {
        Column(Modifier.fillMaxWidth().padding(20.dp)) {
            Text("CONTACT", fontFamily = AirPlayrFonts.DancingScript, fontSize = 17.sp, color = colors.primary)
            Spacer(Modifier.height(12.dp))
            actions.chunked(2).forEach { row ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    row.forEach { action ->
                        ContactButton(
                            label = action.label + if (action.placeholder) " · soon" else "",
                            symbol = when (action.label) {
                                "WhatsApp" -> "◔"
                                "TikTok" -> "♪"
                                "LinkedIn" -> "in"
                                else -> "f"
                            },
                            colors = colors,
                            modifier = Modifier.weight(1f),
                            onClick = { onAction(action) },
                            enabled = !action.placeholder,
                        )
                    }
                    if (row.size == 1) Spacer(Modifier.weight(1f))
                }
                Spacer(Modifier.height(10.dp))
            }
        }
    }
}

@Composable
private fun SupportCard(
    colors: com.airplayr.app.ui.theme.AirPlayrColors,
    loved: Boolean,
    onWhatsNew: () -> Unit,
    onDonate: () -> Unit,
    onFeedback: () -> Unit,
    onLoveComment: () -> Unit,
    onPrivacy: () -> Unit,
    onCopyright: () -> Unit,
    appVersion: String,
) {
    AboutCard(colors) {
        Column(Modifier.fillMaxWidth().padding(vertical = 10.dp)) {
            Text("SUPPORT", fontFamily = AirPlayrFonts.DancingScript, fontSize = 17.sp, color = colors.primary, modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp))
            SupportRow("What's new", Icons.Default.AutoAwesome, colors, onWhatsNew)
            SupportRow("Donate & support", if (loved) Icons.Default.Favorite else Icons.Default.FavoriteBorder, colors, onDonate, iconTint = if (loved) Color.Red else colors.textPrimary)
            SupportRow("Send feedback", Icons.Default.MailOutline, colors, onFeedback)
            SupportRow("Send your love with a comment", Icons.Default.ThumbUp, colors, onLoveComment)
            SupportRow("Privacy policy", Icons.Default.Lock, colors, onPrivacy)
            SupportRow("Copyright", Icons.Default.Info, colors, onCopyright)
            SupportRow("App version", Icons.Default.Info, colors, {}, value = appVersion, clickable = false)
        }
    }
}

@Composable
private fun SupportRow(
    label: String,
    icon: ImageVector,
    colors: com.airplayr.app.ui.theme.AirPlayrColors,
    onClick: () -> Unit,
    iconTint: Color = colors.textPrimary,
    value: String? = null,
    clickable: Boolean = true,
) {
    Row(
        Modifier.fillMaxWidth()
            .then(if (clickable) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(horizontal = 20.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, label, tint = iconTint, modifier = Modifier.size(27.dp))
        Text(label, color = colors.textPrimary, fontSize = 16.sp, modifier = Modifier.weight(1f).padding(horizontal = 18.dp))
        if (value != null) {
            Text(value, color = colors.textSecondary, fontSize = 13.sp)
        } else if (clickable) {
            Text("›", color = colors.textSecondary, fontSize = 28.sp)
        }
    }
}

@Composable
private fun AboutCard(colors: com.airplayr.app.ui.theme.AirPlayrColors, content: @Composable () -> Unit) {
    Card(
        Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(AirPlayrRadii.xxl),
        colors = CardDefaults.cardColors(containerColor = colors.surfaceContainerLow),
        border = androidx.compose.foundation.BorderStroke(1.dp, colors.outlineVariant.copy(alpha = .52f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) { content() }
}

@Composable
private fun ContactButton(label: String, symbol: String, colors: com.airplayr.app.ui.theme.AirPlayrColors, modifier: Modifier, onClick: () -> Unit, enabled: Boolean = true) {
    Row(
        modifier
            .height(58.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(if (enabled) colors.surfaceContainer else colors.surfaceContainerHigh.copy(alpha = .55f))
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        Text(symbol, color = if (enabled) colors.textPrimary else colors.textSecondary, fontWeight = FontWeight.Bold, fontSize = 21.sp)
        Spacer(Modifier.width(10.dp))
        Text(label, color = if (enabled) colors.textPrimary else colors.textSecondary, fontSize = 13.sp, maxLines = 1)
    }
}

@Composable
private fun SocialCircle(label: String, colors: com.airplayr.app.ui.theme.AirPlayrColors, onClick: () -> Unit) {
    Box(
        Modifier.size(58.dp).clip(CircleShape).background(colors.surfaceContainer).clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) { Text(label, color = colors.textPrimary, fontWeight = FontWeight.Bold, fontSize = if (label == "in") 18.sp else 20.sp) }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DonateSheet(onDismiss: () -> Unit) {
    val colors = LocalAirPlayrColors.current
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = colors.background,
        dragHandle = { Box(Modifier.width(62.dp).height(5.dp).clip(RoundedCornerShape(50.dp)).background(colors.textSecondary.copy(alpha = .65f))) },
    ) {
        Column(
            Modifier.fillMaxWidth().navigationBarsPadding().verticalScroll(rememberScrollState()).padding(horizontal = 28.dp, vertical = 4.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Card(
                Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(34.dp),
                colors = CardDefaults.cardColors(containerColor = colors.surfaceContainerLow),
            ) {
                Box(
                    Modifier.fillMaxWidth().background(Brush.linearGradient(listOf(colors.primaryContainer.copy(alpha = .58f), colors.surfaceContainerLow))),
                ) {
                    Column(Modifier.fillMaxWidth().padding(28.dp)) {
                        Box(Modifier.size(92.dp).clip(CircleShape).background(colors.primary), contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.Favorite, "Love", tint = colors.onPrimary, modifier = Modifier.size(52.dp))
                        }
                        Text("✦", color = colors.primary, fontSize = 28.sp, modifier = Modifier.align(Alignment.End))
                        Spacer(Modifier.height(18.dp))
                        Text("Thank you for using AirPlayr", style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold), color = colors.textPrimary)
                        Text("It's made by one person, in spare hours. A tip, or a kind review, keeps it going. Either means the world.", color = colors.textSecondary, fontSize = 15.sp, modifier = Modifier.padding(top = 12.dp))
                    }
                }
            }
            DonateInfoCard(colors, Icons.Default.Star, "Rate 5 stars, it's free", "A review on the Play Store helps just as much.")
            Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(28.dp), colors = CardDefaults.cardColors(containerColor = colors.successContainer)) {
                Row(Modifier.fillMaxWidth().padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(62.dp).clip(RoundedCornerShape(20.dp)).background(colors.onSuccess.copy(alpha = .72f)), contentAlignment = Alignment.Center) { Text("☕", fontSize = 30.sp) }
                    Column(Modifier.weight(1f).padding(start = 16.dp)) {
                        Text("MOST LOVED", color = colors.onSuccessContainer, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                        Text("Buy me a cup of coffee", color = colors.onSuccessContainer, style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold), modifier = Modifier.padding(top = 5.dp))
                    }
                    Text("₦810.00", color = colors.onSuccessContainer, style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold))
                }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                DonateChoice(colors, "♡", "Your love", "₦405.00", Modifier.weight(1f))
                DonateChoice(colors, "♧", "Buy me a movie ticket", "₦1,960.00", Modifier.weight(1f))
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                DonateChoice(colors, "◈", "Buy me a pizza", "₦3,980.00", Modifier.weight(1f))
                DonateChoice(colors, "☷", "Buy me a meal", "₦6,340.00", Modifier.weight(1f))
            }
            TextButton(onClick = onDismiss, Modifier.fillMaxWidth()) { Text("Maybe later", color = colors.primary) }
            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
private fun DonateInfoCard(colors: com.airplayr.app.ui.theme.AirPlayrColors, icon: ImageVector, title: String, subtitle: String) {
    Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(26.dp), colors = CardDefaults.cardColors(containerColor = colors.surfaceContainerLow)) {
        Row(Modifier.fillMaxWidth().padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(58.dp).clip(RoundedCornerShape(18.dp)).background(colors.successContainer), contentAlignment = Alignment.Center) { Icon(icon, null, tint = colors.onSuccessContainer, modifier = Modifier.size(28.dp)) }
            Column(Modifier.weight(1f).padding(start = 16.dp)) {
                Text(title, color = colors.textPrimary, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Text(subtitle, color = colors.textSecondary, fontSize = 14.sp, modifier = Modifier.padding(top = 4.dp))
            }
            Text("★★★★★", color = colors.primary, fontSize = 15.sp)
        }
    }
}

@Composable
private fun DonateChoice(colors: com.airplayr.app.ui.theme.AirPlayrColors, icon: String, title: String, amount: String, modifier: Modifier) {
    Card(modifier, shape = RoundedCornerShape(26.dp), colors = CardDefaults.cardColors(containerColor = colors.surfaceContainerLow)) {
        Column(Modifier.fillMaxWidth().padding(22.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(Modifier.size(52.dp).clip(RoundedCornerShape(18.dp)).background(colors.surfaceContainerHigh), contentAlignment = Alignment.Center) { Text(icon, color = colors.textPrimary, fontSize = 27.sp) }
            Text(title, color = colors.textPrimary, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            Text(amount, color = colors.textSecondary, fontSize = 15.sp, fontWeight = FontWeight.Medium)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun InfoSheet(title: String, icon: ImageVector, text: String, onDismiss: () -> Unit) {
    val colors = LocalAirPlayrColors.current
    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = colors.background) {
        Column(Modifier.fillMaxWidth().navigationBarsPadding().padding(24.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(54.dp).clip(CircleShape).background(colors.primaryContainer), contentAlignment = Alignment.Center) { Icon(icon, null, tint = colors.primary) }
                Spacer(Modifier.width(14.dp))
                Text(title, style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold))
            }
            Text(text, color = colors.textSecondary, fontSize = 15.sp, modifier = Modifier.padding(top = 18.dp))
            TextButton(onClick = onDismiss, Modifier.fillMaxWidth().padding(top = 10.dp)) { Text("Close") }
        }
    }
}
