package com.airplayr.app

import android.app.Application
import com.airplayr.app.di.AppContainer

import com.airplayr.app.core.diagnostics.CrashReporter

class AirPlayrApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        CrashReporter(this).install()
    }

    val container: AppContainer by lazy(LazyThreadSafetyMode.NONE) {
        AppContainer(this)
    }
}
