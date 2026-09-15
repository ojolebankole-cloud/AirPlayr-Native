package com.airplayr.app.core.cast

import android.content.Context
import com.google.android.gms.cast.framework.CastContext
import com.google.android.gms.cast.framework.CastSession
import com.google.android.gms.cast.framework.SessionManagerListener
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class CastController(context: Context) {
    private val castContext = CastContext.getSharedInstance(context.applicationContext)
    private val _connected = MutableStateFlow(castContext.sessionManager.currentCastSession != null)
    val connected: StateFlow<Boolean> = _connected.asStateFlow()

    private val listener = object : SessionManagerListener<CastSession> {
        override fun onSessionStarted(session: CastSession, sessionId: String) { _connected.value = true }
        override fun onSessionResumed(session: CastSession, wasSuspended: Boolean) { _connected.value = true }
        override fun onSessionEnded(session: CastSession, error: Int) { _connected.value = false }
        override fun onSessionStartFailed(session: CastSession, error: Int) { _connected.value = false }
        override fun onSessionStarting(session: CastSession) = Unit
        override fun onSessionEnding(session: CastSession) = Unit
        override fun onSessionResuming(session: CastSession, sessionId: String) = Unit
        override fun onSessionResumeFailed(session: CastSession, error: Int) { _connected.value = false }
        override fun onSessionSuspended(session: CastSession, reason: Int) { _connected.value = false }
    }

    fun start() = castContext.sessionManager.addSessionManagerListener(listener, CastSession::class.java)
    fun stop() = castContext.sessionManager.removeSessionManagerListener(listener, CastSession::class.java)
}
