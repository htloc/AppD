package com.kamenrider.simulator

import android.app.Application
import com.kamenrider.simulator.di.EngineInitializer
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

/**
 * Application class.
 *
 * Annotated with [@HiltAndroidApp] to trigger Hilt's code generation and
 * serve as the top-level DI component.
 */
@HiltAndroidApp
class KamenRiderApp : Application() {

    @Inject
    lateinit var engineInitializer: EngineInitializer

    override fun onCreate() {
        super.onCreate()
        engineInitializer.init()
    }

    override fun onTerminate() {
        engineInitializer.release()
        super.onTerminate()
    }
}
