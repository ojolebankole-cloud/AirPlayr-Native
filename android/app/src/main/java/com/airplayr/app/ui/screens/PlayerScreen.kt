package com.airplayr.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Replay10
import androidx.compose.material.icons.filled.Forward10
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.airplayr.app.core.playback.dsp.DspRepository
import com.airplayr.app.ui.playback.PlaybackController
import com.airplayr.app.ui.playback.PlaybackControllerFactory
import com.airplayr.app.ui.cast.CastButton

@Composable fun PlayerScreen(appContext: android.content.Context, onEqualizer: () -> Unit) {
    val controller: PlaybackController = viewModel(factory=PlaybackControllerFactory(appContext)); val state by controller.state.collectAsStateWithLifecycle(); val dspFlow = remember(appContext) { DspRepository(appContext).state }; val dsp by dspFlow.collectAsStateWithLifecycle(initialValue=com.airplayr.app.core.playback.dsp.DspState())
    val d=dsp
    var speed by remember(state.currentId,d?.speed){ mutableFloatStateOf((d?.speed ?: 1.0).toFloat()) }
    var pitch by remember(d?.pitchSemitones){ mutableIntStateOf(d?.pitchSemitones ?: 0) }
    var preserve by remember(d?.preservePitch){ mutableStateOf(d?.preservePitch ?: true) }
    Column(Modifier.fillMaxSize().padding(20.dp),verticalArrangement=Arrangement.spacedBy(14.dp)){
        Text(state.currentTitle ?: "Nothing playing",style=MaterialTheme.typography.headlineSmall); Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Column(Modifier.weight(1f)) { Text(state.currentArtist.orEmpty(),color=MaterialTheme.colorScheme.onSurfaceVariant) }; CastButton() }
        val frac=if(state.durationMs>0)(state.positionMs.toFloat()/state.durationMs).coerceIn(0f,1f) else 0f
        LinearProgressIndicator({frac},Modifier.fillMaxWidth())
        Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceEvenly){IconButton({controller.seekTo((state.positionMs-10_000).coerceAtLeast(0))}){Icon(Icons.Default.Replay10,"Back 10 seconds")};IconButton({controller.skipPrevious()}){Icon(Icons.Default.SkipPrevious,"Previous")};IconButton({controller.togglePlayPause()}){Icon(if(state.isPlaying)Icons.Default.Pause else Icons.Default.PlayArrow,if(state.isPlaying)"Pause" else "Play")};IconButton({controller.skipNext()}){Icon(Icons.Default.SkipNext,"Next")};IconButton({controller.seekTo(state.positionMs+10_000)}){Icon(Icons.Default.Forward10,"Forward 10 seconds")}}
        Text("${time(state.positionMs)} / ${time(state.durationMs)}")
        Text("Speed ${"%.2f".format(speed)}×"); Slider(speed,{speed=it},0.25f..4f,steps=14,onValueChangeFinished={controller.setSpeed(speed)})
        Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween){Text("Preserve pitch");Switch(preserve,{preserve=it;controller.setPreservePitch(it)})}
        Text("Pitch $pitch semitones"); Slider(pitch.toFloat(),{pitch=it.toInt() },-12f..12f,steps=23,onValueChangeFinished={controller.setPitchSemitones(pitch)})
        Button(onClick=onEqualizer,modifier=Modifier.fillMaxWidth()){Icon(Icons.Default.GraphicEq,null);Text("Equalizer")}
    }
}
private fun time(ms:Long):String{val s=(ms/1000).coerceAtLeast(0);return "%d:%02d".format(s/60,s%60)}
