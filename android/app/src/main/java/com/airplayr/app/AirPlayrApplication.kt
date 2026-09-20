package com.airplayr.app

import android.app.Application
import com.airplayr.app.di.AppContainer

import com.airplayr.app.core.diagnostics.CrashReporter

class AirPlayrApplication : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        CrashReporter(this).install()
        // Create the lightweight application container after Android has attached the Application
        // context. Its Home observer immediately starts warming on the background IO scope.
        container = AppContainer(this)
    }
}
