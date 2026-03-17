package com.kamenrider.simulator.object3d

import android.util.Log
import com.google.ar.sceneform.AnchorNode
import com.google.ar.sceneform.Node
import com.google.ar.sceneform.SceneView
import com.google.ar.sceneform.math.Quaternion
import com.google.ar.sceneform.math.Vector3
import com.google.ar.sceneform.rendering.ModelRenderable
import com.kamenrider.simulator.animation.AnimationCommand
import com.kamenrider.simulator.animation.AnimationManager
import com.kamenrider.simulator.common.base.BaseManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * SceneController
 *
 * Owns the Sceneform [SceneView] lifecycle and exposes a clean API for:
 * - Attaching / swapping 3D models
 * - Controlling model transforms (position, rotation, scale)
 * - Playing model-level animations (via Sceneform's AnimationController)
 *
 * Lifecycle: call [attachToView] once the SceneView is ready, and
 * [detachFromView] in onDestroy.
 *
 * This class intentionally does NOT extend Sceneform nodes – it composes them.
 */
@Singleton
class SceneController @Inject constructor(
    private val modelLoader: ModelLoader,
    private val animationManager: AnimationManager
) : BaseManager {

    companion object {
        private const val TAG = "SceneController"
        private val DEFAULT_POSITION = Vector3(0f, -0.5f, -1.5f)  // 1.5m in front
        private val DEFAULT_SCALE    = Vector3(0.3f, 0.3f, 0.3f)
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var sceneView: SceneView? = null
    private var anchorNode: AnchorNode? = null
    private var modelNode: Node? = null
    private var modelAnimJob: Job? = null

    override fun initialize() {
        // Collect model animation commands
        scope.launch {
            animationManager.modelAnimations.collect { command ->
                handleModelAnimation(command)
            }
        }
    }

    override fun release() {
        detachFromView()
        scope.cancel()
    }

    // ------------------------------------------------------------------
    // Lifecycle
    // ------------------------------------------------------------------

    fun attachToView(view: SceneView) {
        sceneView = view
        anchorNode = AnchorNode().also {
            it.setParent(view.scene)
            it.localPosition = DEFAULT_POSITION
        }
        Log.d(TAG, "SceneController attached to SceneView")
    }

    fun detachFromView() {
        modelNode?.setParent(null)
        anchorNode?.setParent(null)
        anchorNode = null
        modelNode = null
        sceneView = null
        Log.d(TAG, "SceneController detached")
    }

    // ------------------------------------------------------------------
    // Model management
    // ------------------------------------------------------------------

    /**
     * Load and display a new model, replacing whatever is currently shown.
     * The load is asynchronous; the old model remains visible until the new
     * one is ready.
     */
    fun showModel(modelId: String) {
        scope.launch {
            val renderable = modelLoader.load(modelId) ?: run {
                Log.w(TAG, "Skipping showModel – renderable null for $modelId")
                return@launch
            }
            swapRenderable(renderable)
        }
    }

    fun clearModel() {
        modelNode?.setParent(null)
        modelNode = null
    }

    // ------------------------------------------------------------------
    // Transform helpers
    // ------------------------------------------------------------------

    fun setPosition(x: Float, y: Float, z: Float) {
        anchorNode?.localPosition = Vector3(x, y, z)
    }

    fun setScale(uniform: Float) {
        modelNode?.localScale = Vector3(uniform, uniform, uniform)
    }

    fun setRotation(degrees: Float, axis: Vector3 = Vector3.up()) {
        modelNode?.localRotation = Quaternion.axisAngle(axis, degrees)
    }

    fun startAutoRotate(degreesPerSecond: Float = 45f) {
        modelAnimJob?.cancel()
        modelAnimJob = scope.launch {
            // Driven by Sceneform's scene update callbacks – wire in onUpdate
            sceneView?.scene?.addOnUpdateListener { frameTime ->
                val node = modelNode ?: return@addOnUpdateListener
                val angle = degreesPerSecond * frameTime.deltaSeconds
                node.localRotation = Quaternion.multiply(
                    node.localRotation,
                    Quaternion.axisAngle(Vector3.up(), angle)
                )
            }
        }
    }

    fun stopAutoRotate() {
        modelAnimJob?.cancel()
    }

    // ------------------------------------------------------------------
    // Internal
    // ------------------------------------------------------------------

    private fun swapRenderable(renderable: ModelRenderable) {
        val anchor = anchorNode ?: return
        if (modelNode == null) {
            modelNode = Node().apply {
                setParent(anchor)
                localScale = DEFAULT_SCALE
            }
        }
        modelNode!!.renderable = renderable
        Log.d(TAG, "Renderable swapped")
    }

    private fun handleModelAnimation(command: AnimationCommand) {
        val node = modelNode ?: return
        val animator = node.renderableInstance?.animate(true) ?: return
        when (command) {
            is AnimationCommand.Play -> {
                // Sceneform AnimationController: play by name or index
                val index = animator.getAnimationIndex(command.animationId)
                if (index >= 0) animator.start() else Log.w(TAG, "No animation '${command.animationId}' in model")
            }
            is AnimationCommand.Stop -> animator.stop()
        }
    }
}
