package com.airplayr.app

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.activity.viewModels
import com.airplayr.app.ui.AirPlayrApp
import com.airplayr.app.ui.navigation.AirPlayrNavViewModel

class MainActivity : ComponentActivity() {
    private val navViewModel: AirPlayrNavViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        // Must be installed before super.onCreate so Android 12+ and the compat
        // library own the launch window from the first rendered frame.
        installSplashScreen()
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

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        navViewModel.handleIntent(intent)
    }
}
