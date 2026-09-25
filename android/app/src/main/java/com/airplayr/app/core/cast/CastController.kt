package com.airplayr.app.core.cast

import android.content.Context
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import com.airplayr.app.ui.cast.CastUiStatus
import com.google.android.gms.cast.framework.CastContext
import com.google.android.gms.cast.framework.CastSession
import com.google.android.gms.cast.framework.SessionManagerListener
import com.google.android.gms.cast.framework.CastState
import com.google.android.gms.cast.framework.CastStateListener
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class CastController(context: Context) : DefaultLifecycleObserver {
    // Cast framework initialization can fail on devices/configurations where Google Play
    // services is unavailable or the Cast provider has not initialized yet. Cast is optional
    // to AirPlayr, so that failure must never be allowed to terminate the app process.
    private val castContext: CastContext? = runCatching {
        CastContext.getSharedInstance(context.applicationContext)
    }.getOrNull()
    private val _status = MutableStateFlow(currentStatus())
    val status: StateFlow<CastUiStatus> = _status.asStateFlow()
    private val castStateListener = CastStateListener { emit() }
    private val listener = object : SessionManagerListener<CastSession> {
        override fun onSessionStarted(session: CastSession, sessionId: String) = emit()
        override fun onSessionResumed(session: CastSession, wasSuspended: Boolean) = emit()
        override fun onSessionEnded(session: CastSession, error: Int) = emit()
        override fun onSessionStartFailed(session: CastSession, error: Int) = emit()
        override fun onSessionStarting(session: CastSession) = emit()
        override fun onSessionEnding(session: CastSession) = emit()
        override fun onSessionResuming(session: CastSession, sessionId: String) = emit()
        override fun onSessionResumeFailed(session: CastSession, error: Int) = emit()
        override fun onSessionSuspended(session: CastSession, reason: Int) = emit()
    }
    private var started = false

    init { ProcessLifecycleOwner.get().lifecycle.addObserver(this) }

    fun start() {
        val context = castContext ?: return
        if (started) return
        started = true
        runCatching {
            context.sessionManager.addSessionManagerListener(listener, CastSession::class.java)
            context.addCastStateListener(castStateListener)
            emit()
        }.onFailure {
            started = false
            _status.value = CastUiStatus(state = "unavailable")
        }
    }

    fun stop() {
        val context = castContext ?: return
        if (!started) return
        started = false
        runCatching { context.sessionManager.removeSessionManagerListener(listener, CastSession::class.java) }
        runCatching { context.removeCastStateListener(castStateListener) }
    }

    fun endSession() {
        castContext?.let { runCatching { it.sessionManager.endCurrentSession(true) } }
        emit()
    }

    private fun currentStatus(): CastUiStatus {
        val context = castContext ?: return CastUiStatus(state = "unavailable")
        val session = runCatching { context.sessionManager.currentCastSession }.getOrNull()
        val state = runCatching { context.castState }.getOrDefault(CastState.NO_DEVICES_AVAILABLE)
        return CastUiStatus(
            connected = session != null,
            deviceName = session?.castDevice?.friendlyName.orEmpty(),
            state = when {
                session != null -> "connected"
                state == CastState.NO_DEVICES_AVAILABLE -> "no-devices"
                state == CastState.CONNECTING -> "connecting"
                else -> "not-connected"
            },
        )
    }

    private fun emit() { _status.value = currentStatus() }

    override fun onStart(owner: LifecycleOwner) = start()
    override fun onStop(owner: LifecycleOwner) = stop()
}
