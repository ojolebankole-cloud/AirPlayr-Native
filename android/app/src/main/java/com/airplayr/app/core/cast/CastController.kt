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
    private val castContext = CastContext.getSharedInstance(context.applicationContext)
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
        if (started) return
        started = true
        castContext.sessionManager.addSessionManagerListener(listener, CastSession::class.java)
        castContext.addCastStateListener(castStateListener)
        emit()
    }

    fun stop() {
        if (!started) return
        started = false
        castContext.sessionManager.removeSessionManagerListener(listener, CastSession::class.java)
        castContext.removeCastStateListener(castStateListener)
    }

    fun endSession() { castContext.sessionManager.endCurrentSession(true); emit() }

    private fun currentStatus(): CastUiStatus {
        val session = castContext.sessionManager.currentCastSession
        return CastUiStatus(
            connected = session != null,
            deviceName = session?.castDevice?.friendlyName.orEmpty(),
            state = when {
                session != null -> "connected"
                castContext.castState == CastState.NO_DEVICES_AVAILABLE -> "no-devices"
                castContext.castState == CastState.CONNECTING -> "connecting"
                else -> "not-connected"
            },
        )
    }

    private fun emit() { _status.value = currentStatus() }

    override fun onStart(owner: LifecycleOwner) = start()
    override fun onStop(owner: LifecycleOwner) = stop()
}
