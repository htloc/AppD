package com.kamenrider.simulator.common.base

/**
 * Base contract for all manager singletons in the game engine layer.
 * Managers are initialised once and released when the app is destroyed.
 */
interface BaseManager {
    /** Called during Application or Activity onCreate to set up resources. */
    fun initialize()

    /** Called during onDestroy to free resources. */
    fun release()
}
