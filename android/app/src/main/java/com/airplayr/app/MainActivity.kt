package com.airplayr.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import com.airplayr.app.ui.AirPlayrApp
import com.airplayr.app.ui.navigation.AirPlayrNavViewModel

class MainActivity : ComponentActivity() {
    private val navViewModel: AirPlayrNavViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            AirPlayrApp(
                container = (application as AirPlayrApplication).container,
                initialViewModel = navViewModel,
                launchIntent = intent,
            )
        }
    }

    override fun onNewIntent(intent: android.content.Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        navViewModel.handleIntent(intent)
    }
}
