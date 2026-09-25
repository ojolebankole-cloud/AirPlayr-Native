package com.airplayr.app.ui.cast

data class CastUiStatus(
    val connected: Boolean = false,
    val deviceName: String = "",
    val state: String = "not-connected",
) {
    val copyText: String
        get() = when {
            connected -> "Connected${deviceName.takeIf { it.isNotBlank() }?.let { " to $it" } ?: ""}"
            state == "no-devices" -> "No Cast devices available"
            state == "connecting" -> "Connecting to a Cast device…"
            state == "unavailable" -> "Cast is unavailable on this device"
            else -> "No Cast device connected"
        }
}
