package com.airplayr.app.ui.navigation

import android.content.Intent
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class ExternalMediaIntent(val uri: String, val mimeType: String?, val title: String)

class AirPlayrNavViewModel : ViewModel() {
    private val _route = MutableStateFlow(Route.Home)
    val route: StateFlow<Route> = _route.asStateFlow()
    private val _externalMedia = MutableStateFlow<ExternalMediaIntent?>(null)
    val externalMedia: StateFlow<ExternalMediaIntent?> = _externalMedia.asStateFlow()

    fun select(route: Route) {
        _route.value = route
    }

    fun handleIntent(intent: Intent?) {
        if (intent == null) return
        val data = intent.data
        val type = intent.type.orEmpty()
        if (data != null && (type.startsWith("audio/") || type.startsWith("video/") || data.scheme == "airplayr")) {
            val uri = when {
                data.scheme == "airplayr" -> data.getQueryParameter("uri") ?: data.toString()
                else -> data.toString()
            }
            val title = intent.getStringExtra(Intent.EXTRA_TITLE) ?: data.lastPathSegment?.substringAfterLast('/')?.ifBlank { null } ?: "Shared media"
            _externalMedia.value = ExternalMediaIntent(uri, type.ifBlank { null }, title)
            _route.value = if (type.startsWith("audio/")) Route.Music else Route.Videos
        }
    }

    fun consumeExternalMedia() { _externalMedia.value = null }
}

enum class Route(val label: String) {
    Home("Home"),
    Music("Music"),
    Videos("Videos"),
    Settings("Settings"),
}
