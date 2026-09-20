package com.airplayr.app

import android.content.Intent
import android.os.Bundle
import android.content.res.Configuration
import com.airplayr.app.R
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
        // Read the launch theme from the synchronous mirror before installing the splash.
        // The splash itself is never held on an asynchronous preference/database readiness gate.
        val appContainer = (application as AirPlayrApplication).container
        val mirroredTheme = getSharedPreferences("airplayr_startup", MODE_PRIVATE).getString("theme", null)
        val dark = when (mirroredTheme) {
            "dark" -> true
            "light" -> false
            else -> (resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES
        }
        setTheme(if (dark) R.style.Theme_App_SplashScreen_Dark else R.style.Theme_App_SplashScreen_Light)
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            AirPlayrApp(
                container = appContainer,
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
