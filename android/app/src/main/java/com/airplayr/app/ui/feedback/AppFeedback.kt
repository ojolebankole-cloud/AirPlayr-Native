package com.airplayr.app.ui.feedback

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/**
 * App-wide transient feedback channel. UI/features can publish a snackbar without
 * owning a host; AirPlayrApp renders the single host above every destination.
 */
object AppFeedback {
    private val _snackbar = MutableSharedFlow<String>(extraBufferCapacity = 32)
    val snackbar = _snackbar.asSharedFlow()

    fun showSnackbar(message: String) {
        if (message.isNotBlank()) _snackbar.tryEmit(message)
    }
}
