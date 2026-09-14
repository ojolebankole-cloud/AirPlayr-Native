package com.airplayr.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.airplayr.app.di.AppContainer
import kotlinx.coroutines.launch

@Composable fun EqualizerScreen(container:AppContainer){
    val scope=rememberCoroutineScope(); var bass by remember{mutableFloatStateOf(0f)}; var loudness by remember{mutableFloatStateOf(0f)}; var virtualizer by remember{mutableFloatStateOf(0f)}
    Column(Modifier.padding(20.dp),verticalArrangement=Arrangement.spacedBy(16.dp)){
        Text("Equalizer",style=MaterialTheme.typography.headlineSmall)
        Text("Bass ${bass.roundToInt()} dB"); Slider(bass, { bass=it }, -15f..15f)
        Text("Loudness ${loudness.roundToInt()} dB"); Slider(loudness, { loudness=it }, 0f..15f)
        Text("Virtualizer ${virtualizer.roundToInt()}%"); Slider(virtualizer, { virtualizer=it }, 0f..100f)
        Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceEvenly){ Button({scope.launch{container.dsp.setEqualizer(true,"manual",(bass*100).roundToInt(),(loudness*100).roundToInt(),(virtualizer*10).roundToInt())}}){Text("Apply")} Button({scope.launch{container.dsp.setEqualizer(true,"flat",0,0,0)} }){Text("Flat")}}
    }
}
