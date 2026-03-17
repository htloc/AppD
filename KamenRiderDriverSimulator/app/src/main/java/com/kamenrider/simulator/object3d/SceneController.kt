package com.kamenrider.simulator.object3d

import android.util.Log
import com.kamenrider.simulator.animation.AnimationCommand
import com.kamenrider.simulator.animation.AnimationManager
import com.kamenrider.simulator.common.base.BaseManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * SceneController - stub implementation (3D rendering disabled).
 * 
 * Since we're using 2D artwork instead of 3D models, this class
 * is kept as a no-op to maintain API compatibility.
 */
@Singleton
class SceneController @Inject constructor(
    private val modelLoader: ModelLoader,
    private val animationManager: AnimationManager
) : BaseManager {

    companion object {
        private const val TAG = "SceneController"
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    override fun initialize() {
        scope.launch {
            animationManager.modelAnimations.collect { command ->
                Log.d(TAG, "Received animation command (not used in 2D mode): $command")
            }
        }
    }

    override fun release() {
        scope.cancel()
    }

    fun attachToView(view: Any) {
        Log.d(TAG, "attachToView called (3D disabled)")
    }

    fun detachFromView() {
        Log.d(TAG, "detachFromView called")
    }

    fun showModel(modelId: String) {
        Log.d(TAG, "showModel called for $modelId (3D disabled)")
    }

    fun clearModel() {
        Log.d(TAG, "clearModel called")
    }

    fun setPosition(x: Float, y: Float, z: Float) {}
    fun setScale(uniform: Float) {}
    fun setRotation(degrees: Float, axis: Any = Any()) {}
    fun startAutoRotate(degreesPerSecond: Float = 45f) {}
    fun stopAutoRotate() {}
}
