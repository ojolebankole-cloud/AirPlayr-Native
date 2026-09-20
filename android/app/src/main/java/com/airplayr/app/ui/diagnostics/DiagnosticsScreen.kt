package com.airplayr.app.ui.diagnostics

import android.content.Intent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.airplayr.app.core.diagnostics.DiagnosticsRepository
import com.airplayr.app.core.diagnostics.DiagnosticsSnapshot
import com.airplayr.app.core.diagnostics.CrashReporter
import com.airplayr.app.core.playback.queue.QueueStore
import com.airplayr.app.di.AppContainer
import com.airplayr.app.ui.components.airPlayrShadow
import kotlinx.coroutines.launch

@Composable
fun DiagnosticsScreen(container: AppContainer) {
    val context = LocalContext.current
    var snapshot by remember { mutableStateOf<DiagnosticsSnapshot?>(null) }
    var report by remember { mutableStateOf("") }
    var recoveryStatus by remember { mutableStateOf("") }
    var crash by remember { mutableStateOf<String?>(null) }
    val scope = androidx.compose.runtime.rememberCoroutineScope()
    LaunchedEffect(Unit) { runCatching { DiagnosticsRepository(context).snapshot() }.onSuccess { snapshot = it; report = DiagnosticsRepository(context).format(it); crash = CrashReporter(context).lastCrash() } }
    Column(Modifier.fillMaxSize()) {
        LazyColumn(state = rememberSaveable(saver = LazyListState.Saver) { LazyListState(0) },contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            item { Text("Diagnostics", style = MaterialTheme.typography.headlineMedium) }
            item { Card(Modifier.fillMaxWidth().airPlayrShadow(RoundedCornerShape(12.dp), 4.dp)) { Text(report.ifBlank { "Collecting diagnostics…" }, Modifier.padding(16.dp)) } }
            item {
                Button(onClick = { scope.launch { recoveryStatus = runCatching { container.mediaLibrary.fullRescan(); "Library rebuild completed" }.getOrElse { "Recovery failed: ${it.message ?: "unknown error"}" } } }) { Text("Rebuild library") }
            }
            item {
                Button(onClick = { scope.launch { QueueStore(context).clear(); recoveryStatus = "Saved playback queue cleared" } }) { Text("Clear saved playback queue") }
            }
            item {
                Button(onClick = { scope.launch { container.preferences.resetToDefaults(); recoveryStatus = "Playback defaults reset" } }) { Text("Reset playback defaults") }
            }
            item {
                Button(enabled = report.isNotBlank(), onClick = {
                    context.startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).apply { type = "text/plain"; putExtra(Intent.EXTRA_TEXT, report) }, "Share AirPlayr diagnostics"))
                }) { Text("Share diagnostics") }
            }
            if (recoveryStatus.isNotBlank()) item { Text(recoveryStatus, color = MaterialTheme.colorScheme.onSurfaceVariant) }
            crash?.let { text ->
                item { Card(Modifier.fillMaxWidth().airPlayrShadow(RoundedCornerShape(12.dp), 4.dp)) { Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) { Text("Last recorded crash", style = MaterialTheme.typography.titleMedium); Text(text) ; Button(onClick = { CrashReporter(context).clear(); crash = null }) { Text("Clear crash record") } } } }
            }
        }
    }
}
