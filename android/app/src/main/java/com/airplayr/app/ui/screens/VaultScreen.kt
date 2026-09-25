@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
package com.airplayr.app.ui.screens

import android.app.Activity
import android.content.ContentUris
import android.os.Build
import android.provider.MediaStore
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.airplayr.app.core.data.media.MediaItem
import com.airplayr.app.core.data.media.MediaType
import com.airplayr.app.core.playback.VideoQueueStore
import com.airplayr.app.di.AppContainer
import com.airplayr.app.ui.feedback.AppFeedback
import com.airplayr.app.ui.theme.AirPlayrFonts
import com.airplayr.app.ui.theme.AirPlayrIcons
import com.airplayr.app.ui.theme.LocalAirPlayrColors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun VaultScreen(
    container: AppContainer,
    onBack: () -> Unit,
    onOpenVideo: (String) -> Unit,
) {
    val manager = container.vaultManager
    val colors = LocalAirPlayrColors.current
    val context = androidx.compose.ui.platform.LocalContext.current
    val scope = rememberCoroutineScope()
    var configured by remember { mutableStateOf(manager.configured) }
    var unlocked by remember { mutableStateOf(manager.unlocked) }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var addOpen by remember { mutableStateOf(false) }
    var entries by remember { mutableStateOf(manager.entries()) }
    var pendingStages by remember { mutableStateOf<List<Pair<MediaItem, com.airplayr.app.core.vault.VaultManager.Entry>>>(emptyList()) }
    var deleteUris by remember { mutableStateOf<List<android.net.Uri>>(emptyList()) }
    var restoreConfirm by remember { mutableStateOf(false) }
    val biometricAvailable = remember(manager) { manager.canUseBiometric() }

    val videos by container.mediaLibrary.observe(MediaType.VIDEO).collectAsStateWithLifecycle(initialValue = emptyList())

    fun setupOrUnlock() {
        if (password.length != 4 || password.any { !it.isDigit() }) { error = "Enter a 4-digit PIN."; return }
        busy = true
        error = null
        scope.launch {
            val ok = if (!configured) {
                if (password != confirmPassword) false else manager.setup(password)
            } else manager.unlock(password)
            busy = false
            if (ok) {
                configured = manager.configured
                unlocked = manager.unlocked
                password = ""
                confirmPassword = ""
                entries = manager.entries()
            } else {
                error = if (!configured) "Passwords do not match." else "Wrong password or this vault belongs to another device."
            }
        }
    }

    fun completeMove() {
        scope.launch {
            val movedCount = pendingStages.size
            pendingStages.forEach { (_, entry) -> manager.finalizeMove(entry) }
            container.structured.hideMediaEverywhereBatch(pendingStages.map { it.first.id })
            runCatching { container.mediaLibrary.deltaRefresh(MediaType.VIDEO) }
            entries = manager.entries()
            addOpen = false
            pendingStages = emptyList()
            deleteUris = emptyList()
            busy = false
            AppFeedback.showSnackbar("$movedCount video${if (movedCount == 1) "" else "s"} secured in Vault.")
        }
    }

    fun cancelMove() {
        scope.launch {
            pendingStages.forEach { (_, entry) -> manager.rollbackStaged(entry) }
            pendingStages = emptyList()
            deleteUris = emptyList()
            busy = false
            AppFeedback.showSnackbar("Vault move cancelled.")
        }
    }

    val deleteLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) completeMove() else cancelMove()
    }

    fun beginMove(selected: List<MediaItem>) {
        if (selected.isEmpty() || busy) return
        val current = VideoQueueStore.currentId.value
        if (selected.any { it.id == current }) {
            AppFeedback.showSnackbar("Stop the currently playing video before moving it to the Vault.")
            return
        }
        busy = true
        scope.launch {
            val staged = ArrayList<Pair<MediaItem, com.airplayr.app.core.vault.VaultManager.Entry>>(selected.size)
            try {
                selected.forEach { item ->
                    val entry = manager.stageMove(item) ?: error("Could not encrypt ${item.title}")
                    staged += item to entry
                }
            } catch (t: Throwable) {
                staged.forEach { manager.rollbackStaged(it.second) }
                busy = false
                AppFeedback.showSnackbar("Could not secure every selected video. No partial Vault move was kept.")
                return@launch
            }
            pendingStages = staged
            val mediaStoreUris = staged.mapNotNull { (item, _) -> item.mediaStoreId?.let { ContentUris.withAppendedId(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, it) } }
            // App-owned media can be removed directly. User-owned MediaStore rows may require
            // Android's one-time confirmation; only request that confirmation for the rows that
            // actually reject direct deletion. This avoids an unnecessary permission sheet for
            // every Vault operation when the platform already grants AirPlayr deletion rights.
            val remaining = mediaStoreUris.filter { uri ->
                runCatching { container.appContext.contentResolver.delete(uri, null, null) > 0 }.getOrDefault(false).not()
            }
            deleteUris = remaining
            if (remaining.isEmpty()) {
                completeMove()
            } else if (Build.VERSION.SDK_INT >= 30) {
                val request = MediaStore.createDeleteRequest(container.appContext.contentResolver, remaining)
                deleteLauncher.launch(IntentSenderRequest.Builder(request.intentSender).build())
            } else {
                cancelMove()
            }
        }
    }

    Box(Modifier.fillMaxSize().background(colors.background)) {
        if (!unlocked) {
            VaultGate(
                configured = configured,
                password = password,
                confirmPassword = confirmPassword,
                busy = busy,
                error = error,
                fingerprint = manager.deviceFingerprint(),
                onPassword = { password = it.filter(Char::isDigit).take(4) },
                onConfirmPassword = { confirmPassword = it.filter(Char::isDigit).take(4) },
                onSubmit = ::setupOrUnlock,
                onBiometric = if (configured && biometricAvailable) {
                    {
                        val activity = context as? Activity
                        if (activity != null) {
                            manager.authenticateBiometric(
                                activity = activity,
                                onSuccess = {
                                    configured = manager.configured
                                    unlocked = manager.unlocked
                                    entries = manager.entries()
                                    error = null
                                },
                                onError = { message -> error = message },
                            )
                        }
                    }
                } else null,
                onBack = onBack,
            )
        } else {
            Column(Modifier.fillMaxSize()) {
                Row(Modifier.fillMaxWidth().statusBarsPadding().padding(top = 8.dp, start = 8.dp, end = 14.dp), verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, "Back", tint = colors.primary) }
                    Column(Modifier.weight(1f)) {
                        Text("Vault", color = colors.textPrimary, fontFamily = AirPlayrFonts.OpenSans, fontSize = 29.sp, fontWeight = FontWeight.SemiBold)
                        Text("Private videos · device protected", color = colors.textSecondary, fontSize = 12.sp)
                    }
                    Surface(
                        onClick = { if (!busy && entries.isNotEmpty()) restoreConfirm = true },
                        shape = CircleShape,
                        color = Color(0xFF193C2C),
                    ) {
                        Icon(Icons.Default.LockOpen, "Restore all Vault videos", tint = Color(0xFFB9F5D1), modifier = Modifier.padding(9.dp).size(20.dp))
                    }
                }
                Surface(Modifier.fillMaxWidth().padding(18.dp), RoundedCornerShape(28.dp), color = Color(0xFF101820)) {
                    Row(Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
                        Surface(Modifier.size(58.dp), CircleShape, color = Color(0xFF1C3D35)) { Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Icon(Icons.Default.Shield, null, tint = Color(0xFF9AF0C5), modifier = Modifier.size(31.dp)) } }
                        Column(Modifier.weight(1f).padding(start = 16.dp)) {
                            Text("Vault is sealed", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                            Text("Videos moved here are encrypted and removed from the device media library.", color = Color(0xFFB9C2C8), fontSize = 12.sp, lineHeight = 18.sp)
                        }
                    }
                }
                if (entries.isEmpty()) {
                    Box(Modifier.fillMaxSize().weight(1f), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.VisibilityOff, null, tint = colors.textSecondary, modifier = Modifier.size(52.dp))
                            Text("Your private video space is empty", color = colors.textPrimary, fontSize = 18.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = 12.dp))
                            Text("Use + to move a video here and hide it from the rest of AirPlayr.", color = colors.textSecondary, fontSize = 13.sp, modifier = Modifier.padding(top = 5.dp))
                        }
                    }
                } else {
                    LazyColumn(Modifier.weight(1f), contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        items(entries, key = { it.id }) { entry ->
                            Surface(onClick = { onOpenVideo(entry.id) }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(24.dp), color = colors.surfaceContainer) {
                                Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                    val thumb = manager.thumbnailFile(entry)
                                    val thumbBitmap = remember(thumb?.absolutePath) { thumb?.let { android.graphics.BitmapFactory.decodeFile(it.absolutePath)?.asImageBitmap() } }
                                    if (thumbBitmap != null) Image(thumbBitmap, contentDescription = entry.title, modifier = Modifier.size(92.dp).background(Color(0xFF20252A), RoundedCornerShape(18.dp)))
                                    else Surface(Modifier.size(92.dp), RoundedCornerShape(18.dp), color = colors.surfaceContainerHigh) { Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Icon(Icons.Default.Videocam, null, tint = colors.primary, modifier = Modifier.size(32.dp)) } }
                                    Column(Modifier.weight(1f).padding(horizontal = 14.dp)) {
                                        Text(entry.title, color = colors.textPrimary, fontSize = 17.sp, fontWeight = FontWeight.SemiBold, maxLines = 2)
                                        Text(entry.artist.orEmpty().ifBlank { "Private video" }, color = colors.textSecondary, fontSize = 12.sp, modifier = Modifier.padding(top = 3.dp))
                                        Text("Encrypted · ${formatBytes(entry.sizeBytes)}", color = Color(0xFF5E8A73), fontSize = 11.sp, modifier = Modifier.padding(top = 6.dp))
                                    }
                                    Icon(Icons.Default.Lock, null, tint = colors.primary, modifier = Modifier.size(20.dp))
                                }
                            }
                        }
                    }
                }
            }
            Surface(
                onClick = { addOpen = true },
                modifier = Modifier.align(Alignment.BottomEnd).padding(end = 22.dp, bottom = 26.dp).size(64.dp),
                shape = CircleShape,
                color = colors.primary,
                shadowElevation = 12.dp,
            ) { Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Icon(Icons.Default.Add, "Add video to Vault", tint = colors.onPrimary, modifier = Modifier.size(31.dp)) } }
        }
    }

    if (restoreConfirm) {
        AlertDialog(
            onDismissRequest = { restoreConfirm = false },
            title = { Text("Restore all Vault videos?") },
            text = { Text("AirPlayr will decrypt every private video back into the Android media library and remove it from the Vault.") },
            confirmButton = {
                TextButton(onClick = {
                    restoreConfirm = false
                    busy = true
                    scope.launch {
                        val count = manager.restoreAllToMediaStore()
                        runCatching { container.mediaLibrary.deltaRefresh(MediaType.VIDEO) }
                        entries = manager.entries()
                        busy = false
                        AppFeedback.showSnackbar(if (count == 0) "No Vault videos were restored." else "$count video${if (count == 1) "" else "s"} restored to your library.")
                    }
                }, enabled = !busy) { Text("Restore all") }
            },
            dismissButton = { TextButton(onClick = { restoreConfirm = false }) { Text("Cancel") } },
        )
    }

    if (addOpen && unlocked) {
        VaultVideoPicker(videos.filter { it.mediaStoreId != null }, onDismiss = { addOpen = false }, onAdd = ::beginMove, busy = busy)
    }
}

@Composable
private fun VaultGate(
    configured: Boolean,
    password: String,
    confirmPassword: String,
    busy: Boolean,
    error: String?,
    fingerprint: String,
    onPassword: (String) -> Unit,
    onConfirmPassword: (String) -> Unit,
    onSubmit: () -> Unit,
    onBiometric: (() -> Unit)?,
    onBack: () -> Unit,
) {
    Column(Modifier.fillMaxSize().navigationBarsPadding().padding(horizontal = 28.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Row(Modifier.fillMaxWidth().padding(top = 16.dp), verticalAlignment = Alignment.CenterVertically) { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, "Back") }; Spacer(Modifier.weight(1f)) }
        Spacer(Modifier.height(34.dp))
        Surface(Modifier.size(104.dp), CircleShape, color = Color(0xFF101820)) { Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Icon(if (configured) Icons.Default.Lock else Icons.Default.Security, null, tint = Color(0xFF8BE5BA), modifier = Modifier.size(48.dp)) } }
        Text(if (configured) "Unlock your Vault" else "Create your Vault", fontFamily = AirPlayrFonts.OpenSans, fontSize = 30.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = 22.dp))
        Text(if (configured) "Enter your 4-digit PIN or use your device biometric." else "Create a 4-digit PIN for this device-bound encrypted Vault.", color = Color.Gray, fontSize = 13.sp, modifier = Modifier.padding(top = 8.dp), textAlign = androidx.compose.ui.text.style.TextAlign.Center)
        Surface(Modifier.fillMaxWidth().padding(top = 20.dp), RoundedCornerShape(18.dp), color = Color(0xFF101820)) { Text("DEVICE KEY  •  $fingerprint", color = Color(0xFF8BE5BA), fontSize = 11.sp, modifier = Modifier.padding(14.dp)) }
        OutlinedTextField(password, onPassword, Modifier.fillMaxWidth().padding(top = 22.dp), label = { Text("4-digit PIN") }, singleLine = true, visualTransformation = PasswordVisualTransformation(), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword), trailingIcon = { if (password.isNotEmpty()) Text("${password.length}/4", fontSize = 11.sp, color = Color.Gray, modifier = Modifier.padding(end = 12.dp)) })
        if (!configured) OutlinedTextField(confirmPassword, onConfirmPassword, Modifier.fillMaxWidth().padding(top = 10.dp), label = { Text("Confirm 4-digit PIN") }, singleLine = true, visualTransformation = PasswordVisualTransformation(), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword))
        if (error != null) Text(error, color = Color(0xFFC94A4A), fontSize = 12.sp, modifier = Modifier.padding(top = 9.dp))
        Button(onClick = onSubmit, enabled = !busy && password.length == 4, modifier = Modifier.fillMaxWidth().padding(top = 18.dp).height(56.dp), shape = RoundedCornerShape(28.dp)) { if (busy) CircularProgressIndicator(Modifier.size(21.dp), strokeWidth = 2.dp) else Text(if (configured) "Unlock with PIN" else "Create secure Vault", fontSize = 16.sp) }
        if (onBiometric != null) {
            androidx.compose.material3.OutlinedButton(onClick = onBiometric, enabled = !busy, modifier = Modifier.fillMaxWidth().padding(top = 10.dp).height(54.dp), shape = RoundedCornerShape(27.dp)) {
                Icon(Icons.Default.Security, null, modifier = Modifier.size(21.dp))
                Text("Use fingerprint / biometric", modifier = Modifier.padding(start = 8.dp), fontSize = 15.sp)
            }
        }
    }
}

@Composable
private fun VaultVideoPicker(videos: List<MediaItem>, onDismiss: () -> Unit, onAdd: (List<MediaItem>) -> Unit, busy: Boolean) {
    val colors = LocalAirPlayrColors.current
    var selected by remember { mutableStateOf(emptySet<String>()) }
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true), containerColor = colors.surfaceContainerLow) {
        Column(Modifier.fillMaxWidth().navigationBarsPadding().padding(bottom = 8.dp)) {
            Text("Hide videos in Vault", color = colors.textPrimary, fontSize = 27.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(horizontal = 22.dp, vertical = 12.dp))
            Text("Selected videos will be encrypted, removed from the device library, and available only inside Vault.", color = colors.textSecondary, fontSize = 12.sp, modifier = Modifier.padding(horizontal = 22.dp))
            LazyColumn(Modifier.fillMaxWidth().heightIn(max = 520.dp), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                items(videos, key = { it.id }) { item ->
                    val checked = selected.contains(item.id)
                    Surface(onClick = { selected = if (checked) selected - item.id else selected + item.id }, shape = RoundedCornerShape(18.dp), color = if (checked) colors.primaryContainer else colors.surfaceContainer) {
                        Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Surface(Modifier.size(58.dp), RoundedCornerShape(15.dp), color = colors.surfaceContainerHigh) { Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Icon(Icons.Default.Videocam, null, tint = colors.primary, modifier = Modifier.size(27.dp)) } }
                            Column(Modifier.weight(1f).padding(horizontal = 12.dp)) { Text(item.title, color = colors.textPrimary, fontSize = 16.sp, maxLines = 1); Text(item.artist.orEmpty().ifBlank { item.filename }, color = colors.textSecondary, fontSize = 11.sp) }
                            Surface(Modifier.size(30.dp), CircleShape, color = if (checked) colors.primary else Color.Transparent, border = androidx.compose.foundation.BorderStroke(2.dp, if (checked) colors.primary else colors.textSecondary)) { Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { if (checked) Icon(Icons.Default.Check, null, tint = colors.onPrimary, modifier = Modifier.size(18.dp)) } }
                        }
                    }
                }
            }
            Button(onClick = { onAdd(videos.filter { selected.contains(it.id) }) }, enabled = selected.isNotEmpty() && !busy, modifier = Modifier.fillMaxWidth().padding(horizontal = 18.dp).height(54.dp), shape = RoundedCornerShape(28.dp)) { Text(if (busy) "Securing…" else "Move ${selected.size} video${if (selected.size == 1) "" else "s"} to Vault") }
        }
    }
}

private fun formatBytes(value: Long): String = when {
    value >= 1024L * 1024L * 1024L -> String.format("%.1f GB", value / (1024.0 * 1024.0 * 1024.0))
    value >= 1024L * 1024L -> String.format("%.1f MB", value / (1024.0 * 1024.0))
    else -> String.format("%.0f KB", value / 1024.0)
}
