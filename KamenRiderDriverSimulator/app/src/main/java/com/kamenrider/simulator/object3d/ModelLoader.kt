package com.kamenrider.simulator.object3d

import android.content.Context
import android.util.Log
import com.google.ar.sceneform.rendering.ModelRenderable
import com.kamenrider.simulator.data.config.GameConfig
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.util.concurrent.CompletableFuture
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * ModelLoader
 *
 * Responsible for loading Sceneform 3D model assets (.sfb / .glb) from the
 * assets/ directory and caching them for reuse.
 *
 * Usage:
 * ```kotlin
 * val renderable = modelLoader.load(GameConfig.Models.EXAID_LEVEL2)
 * sceneController.setModel(renderable)
 * ```
 *
 * Model files live at:  app/src/main/assets/object3d/<modelId>.sfb
 */
@Singleton
class ModelLoader @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        private const val TAG = "ModelLoader"
        private const val MODEL_DIR = "object3d"
        private val EXTENSIONS = listOf("sfb", "glb", "gltf")
    }

    /** Cache: modelId → pre-built ModelRenderable */
    private val cache = mutableMapOf<String, ModelRenderable>()

    // ------------------------------------------------------------------
    // Public API
    // ------------------------------------------------------------------

    /**
     * Load a model by its logical [modelId].
     * Returns a cached instance if already loaded.
     * Returns null if the asset file cannot be found.
     *
     * Must be called from a coroutine scope (IO or Default dispatcher).
     */
    suspend fun load(modelId: String): ModelRenderable? {
        cache[modelId]?.let { return it }

        val assetPath = resolveAssetPath(modelId) ?: run {
            Log.w(TAG, "No asset found for modelId='$modelId'. " +
                    "Place file at assets/$MODEL_DIR/$modelId.[sfb|glb]")
            return null
        }

        return withContext(Dispatchers.IO) {
            try {
                buildRenderable(assetPath).also { renderable ->
                    cache[modelId] = renderable
                    Log.d(TAG, "Loaded model: $modelId from $assetPath")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to load model '$modelId': ${e.message}")
                null
            }
        }
    }

    /**
     * Pre-warm the cache for a list of model ids.
     * Call this when entering the Show screen to avoid jank during transformation.
     */
    suspend fun preload(modelIds: List<String>) {
        modelIds.forEach { load(it) }
    }

    fun evict(modelId: String) {
        cache.remove(modelId)
    }

    fun clearCache() {
        cache.clear()
    }

    // ------------------------------------------------------------------
    // Internal
    // ------------------------------------------------------------------

    private fun resolveAssetPath(modelId: String): String? {
        for (ext in EXTENSIONS) {
            val path = "$MODEL_DIR/$modelId.$ext"
            try {
                context.assets.open(path).close()
                return path
            } catch (_: Exception) { /* try next */ }
        }
        return null
    }

    /**
     * Wrap the [CompletableFuture]-based Sceneform API in a suspending call.
     */
    private suspend fun buildRenderable(assetPath: String): ModelRenderable =
        suspendCancellableCoroutine { cont ->
            ModelRenderable.builder()
                .setSource(context) { context.assets.open(assetPath) }
                .setIsFilamentGltf(assetPath.endsWith(".glb") || assetPath.endsWith(".gltf"))
                .build()
                .thenAccept { renderable -> cont.resume(renderable) }
                .exceptionally { throwable ->
                    cont.resumeWithException(throwable)
                    null
                }
        }
}
