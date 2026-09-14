package com.airplayr.app.ui.navigation

import android.content.Intent
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class AirPlayrNavViewModel : ViewModel() {
    private val _route = MutableStateFlow(Route.Home)
    val route: StateFlow<Route> = _route.asStateFlow()

    fun select(route: Route) {
        _route.value = route
    }

    fun handleIntent(intent: Intent?) {
        val data = intent?.data ?: return
        val scheme = data.scheme
        val type = intent.type.orEmpty()
        if (scheme == "airplayr" || type.startsWith("video/")) {
            _route.value = Route.Videos
        }
    }
}

enum class Route(val label: String) {
    Home("Home"),
    Music("Music"),
    Videos("Videos"),
    Settings("Settings"),
}
