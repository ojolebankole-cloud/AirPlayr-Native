package com.airplayr.app.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.Image
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
import androidx.compose.material.icons.filled.Copyright
import androidx.compose.material.icons.filled.Feedback
import androidx.compose.material.icons.filled.NewReleases
import androidx.compose.material.icons.filled.PrivacyTip
import androidx.compose.material.icons.filled.RateReview
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.MailOutline
import androidx.compose.material.icons.filled.Star
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.airplayr.app.R
import com.airplayr.app.ui.LocalAirPlayrBottomOverlayHeight
import com.airplayr.app.ui.components.WhatsNewSheet
import com.airplayr.app.ui.feedback.AppFeedback
import com.airplayr.app.ui.theme.AirPlayrFonts
import com.airplayr.app.ui.theme.AirPlayrIcons
import com.airplayr.app.ui.theme.AirPlayrRadii
import com.airplayr.app.ui.theme.LocalAirPlayrColors

private data class SocialAction(
    val label: String,
    val handle: String,
    val iconRes: Int? = null,
    val icon: ImageVector? = null,
    val target: String? = null,
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
            Text("About AirPlayr", style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold), color = colors.textPrimary)
        }

        Column(
            Modifier.fillMaxWidth().weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 28.dp),
            verticalArrangement = Arrangement.spacedBy(22.dp),
        ) {
            Spacer(Modifier.height(2.dp))
            AppIdentityCard(colors = colors, appVersion = appVersion)
            DeveloperCard(colors = colors)
            SocialCard(
                colors = colors,
                actions = listOf(
                    SocialAction("Instagram", "@koled_45", R.drawable.airplayr_instagram, target = "https://instagram.com/koled_45"),
                    SocialAction("X", "@koled_45", R.drawable.airplayr_x_twitter, target = "https://x.com/koled_45"),
                    SocialAction("Threads", "@koled_45", R.drawable.airplayr_threads, target = "https://www.threads.net/@koled_45"),
                    SocialAction("Reddit", "u/koled_45", R.drawable.airplayr_reddit, target = "https://www.reddit.com/user/koled_45/"),
                    SocialAction("Telegram", "@koled_45", R.drawable.airplayr_telegram, target = "https://t.me/koled_45"),
                    SocialAction("Email", "koledesigninfo@gmail.com", icon = Icons.Default.MailOutline, target = "mailto:koledesigninfo@gmail.com"),
                    SocialAction("WhatsApp", "AirPlayr support", R.drawable.airplayr_whatsapp, target = "https://wa.me/2349163149653"),
                    SocialAction("TikTok", "@koled_45", R.drawable.airplayr_tiktok, target = "https://www.tiktok.com/@koled_45"),
                    SocialAction("LinkedIn", "Kole Design Dev", R.drawable.airplayr_linkedin),
                    SocialAction("Facebook", "Kole Design Dev", R.drawable.airplayr_facebook),
                ),
                onAction = { action -> action.target?.let(::openTarget) },
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
                Box(Modifier.size(30.dp).clip(CircleShape).background(colors.primaryContainer), contentAlignment = Alignment.Center) {
                    Icon(AirPlayrIcons.Play, "Created with creativity", tint = colors.primary, modifier = Modifier.size(18.dp))
                }
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
            icon = Icons.Default.PrivacyTip,
            text = "AirPlayr is designed around local media. Access to music, video and folders is granted by Android and used to build your on-device library. A full public privacy policy will be published here as the app's privacy documentation is finalized.",
            onDismiss = { privacyOpen = false },
        )
    }
    if (copyrightOpen) {
        InfoSheet(
            title = "Copyright",
            icon = Icons.Default.Copyright,
            text = "© 2026 Kole Design Dev. AirPlayr and its original interface work are part of this project. Media files remain the property of their respective owners.",
            onDismiss = { copyrightOpen = false },
        )
    }
}

@Composable
private fun AppIdentityCard(
    colors: com.airplayr.app.ui.theme.AirPlayrColors,
    appVersion: String,
) {
    AboutCard(colors) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 22.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                Modifier.size(158.dp).clip(RoundedCornerShape(42.dp)).background(
                    Brush.linearGradient(listOf(colors.primaryContainer, colors.secondaryContainer, colors.tertiaryContainer))
                ),
                contentAlignment = Alignment.Center,
            ) {
                Image(
                    painterResource(R.drawable.airplayr_themeable_logo),
                    contentDescription = "AirPlayr logo",
                    modifier = Modifier.fillMaxWidth(0.78f),
                    contentScale = ContentScale.Fit,
                )
            }
            Spacer(Modifier.height(16.dp))
            Text("AirPlayr", style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold), color = colors.textPrimary)
            Text("Music-first local media player for Android.", color = colors.textSecondary, textAlign = TextAlign.Center, fontSize = 14.sp, modifier = Modifier.padding(top = 4.dp))
            Text("Media, history and settings stay on this device.", color = colors.textSecondary, textAlign = TextAlign.Center, fontSize = 13.sp, modifier = Modifier.padding(top = 2.dp))
            Row(Modifier.padding(top = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                InfoPill("v$appVersion", colors)
                InfoPill("Android ${android.os.Build.VERSION.RELEASE}", colors)
            }
        }
    }
}

@Composable
private fun DeveloperCard(colors: com.airplayr.app.ui.theme.AirPlayrColors) {
    AboutCard(colors) {
        Column(Modifier.fillMaxWidth().padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Image(
                painterResource(R.drawable.about_developer_photo),
                contentDescription = "Developer profile photo",
                modifier = Modifier.size(112.dp).clip(CircleShape),
                contentScale = ContentScale.Crop,
            )
            Text("DEVELOPED AND DESIGNED BY", fontFamily = AirPlayrFonts.DancingScript, fontSize = 15.sp, color = colors.primary, modifier = Modifier.padding(top = 12.dp))
            Text("Kole Design Dev", style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.SemiBold), color = colors.textPrimary, modifier = Modifier.padding(top = 4.dp))
            Text("Building a focused, personal music experience with native Android tools.", color = colors.textSecondary, fontSize = 13.sp, lineHeight = 18.sp, textAlign = TextAlign.Center, modifier = Modifier.padding(top = 6.dp))
        }
    }
}

@Composable
private fun SocialCard(
    colors: com.airplayr.app.ui.theme.AirPlayrColors,
    actions: List<SocialAction>,
    onAction: (SocialAction) -> Unit,
) {
    AboutCard(colors) {
        Column(Modifier.fillMaxWidth().padding(16.dp)) {
            Text("CONNECT", fontFamily = AirPlayrFonts.DancingScript, fontSize = 17.sp, color = colors.primary, modifier = Modifier.padding(horizontal = 4.dp, vertical = 5.dp))
            actions.chunked(2).forEach { row ->
                Row(Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    row.forEach { action ->
                        SocialButton(action, colors, Modifier.weight(1f)) { onAction(action) }
                    }
                    if (row.size == 1) Spacer(Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun SocialButton(
    action: SocialAction,
    colors: com.airplayr.app.ui.theme.AirPlayrColors,
    modifier: Modifier,
    onClick: () -> Unit,
) {
    Row(
        modifier
            .height(64.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(colors.surfaceContainer)
            .then(if (action.target != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(38.dp).clip(CircleShape).background(colors.primaryContainer), contentAlignment = Alignment.Center) {
            if (action.iconRes != null) Icon(painterResource(action.iconRes), action.label, tint = colors.primary, modifier = Modifier.size(21.dp))
            else action.icon?.let { Icon(it, action.label, tint = colors.primary, modifier = Modifier.size(21.dp)) }
        }
        Column(Modifier.weight(1f).padding(start = 9.dp)) {
            Text(action.label, fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = colors.textPrimary, maxLines = 1)
            Text(action.handle, fontSize = 10.sp, color = colors.textSecondary, maxLines = 1)
        }
    }
}

@Composable
private fun InfoPill(text: String, colors: com.airplayr.app.ui.theme.AirPlayrColors) {
    Box(Modifier.clip(RoundedCornerShape(50)).background(colors.surfaceContainer).padding(horizontal = 11.dp, vertical = 6.dp)) {
        Text(text, fontSize = 11.sp, color = colors.primary)
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
            SupportRow("What's new", Icons.Default.NewReleases, colors, onWhatsNew)
            SupportRow("Support AirPlayr", Icons.Default.Favorite, colors, onDonate, iconTint = if (loved) Color.Red else colors.primary)
            SupportRow("Send feedback", Icons.Default.Feedback, colors, onFeedback)
            SupportRow("Send me love with a comment", Icons.Default.RateReview, colors, onLoveComment)
            SupportRow("Privacy policy", Icons.Default.PrivacyTip, colors, onPrivacy)
            SupportRow("Copyright", Icons.Default.Copyright, colors, onCopyright)
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
        Icon(icon, label, tint = iconTint, modifier = Modifier.size(25.dp))
        Text(label, color = colors.textPrimary, fontSize = 14.sp, modifier = Modifier.weight(1f).padding(horizontal = 16.dp))
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DonateSheet(onDismiss: () -> Unit) {
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
        elevation = androidx.compose.material3.CardDefaults.cardElevation(defaultElevation = 0.dp)) {
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
            Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(28.dp), colors = CardDefaults.cardColors(containerColor = colors.successContainer), elevation = androidx.compose.material3.CardDefaults.cardElevation(defaultElevation = 0.dp)) {
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
    Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(26.dp), colors = CardDefaults.cardColors(containerColor = colors.surfaceContainerLow), elevation = androidx.compose.material3.CardDefaults.cardElevation(defaultElevation = 0.dp)) {
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
    Card(modifier, shape = RoundedCornerShape(26.dp), colors = CardDefaults.cardColors(containerColor = colors.surfaceContainerLow), elevation = androidx.compose.material3.CardDefaults.cardElevation(defaultElevation = 0.dp)) {
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
