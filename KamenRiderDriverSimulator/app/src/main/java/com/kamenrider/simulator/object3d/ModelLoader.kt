package com.kamenrider.simulator.object3d

import android.content.Context
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * ModelLoader - stub implementation (3D rendering disabled).
 * 
 * Since we're using 2D artwork instead of 3D models, this class
 * is kept as a no-op to maintain API compatibility.
 */
@Singleton
class ModelLoader @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        private const val TAG = "ModelLoader"
    }

    suspend fun load(modelId: String): Any? {
        Log.d(TAG, "load called for $modelId (3D disabled, returns null)")
        return null
    }

    suspend fun preload(modelIds: List<String>) {
        Log.d(TAG, "preload called for ${modelIds.size} models (3D disabled)")
    }

    fun evict(modelId: String) {
        Log.d(TAG, "evict called for $modelId")
    }

    fun clearCache() {
        Log.d(TAG, "clearCache called")
    }
}
