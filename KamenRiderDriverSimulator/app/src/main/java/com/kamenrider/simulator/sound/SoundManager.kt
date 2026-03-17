package com.kamenrider.simulator.sound

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import android.util.Log
import com.kamenrider.simulator.common.base.BaseManager
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * SoundManager
 *
 * Manages all audio in the application using Android [SoundPool] for short
 * sound effects (belt jingles, henshin cries) and falls back gracefully when
 * assets are missing.
 *
 * Sound files must be placed at:
 *   app/src/main/assets/sound/<soundId>.mp3  (or .ogg)
 *
 * Features:
 * - Load-on-demand with caching
 * - Simultaneous overlapping streams
 * - Per-stream stop
 * - Delayed playback
 * - Loop support
 */
@Singleton
class SoundManager @Inject constructor(
    @ApplicationContext private val context: Context
) : BaseManager {

    companion object {
        private const val TAG = "SoundManager"
        private const val MAX_STREAMS = 8
        private val EXTENSIONS = listOf("ogg", "mp3", "wav")
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private lateinit var soundPool: SoundPool

    /** soundId → SoundPool resource id */
    private val loadedSounds = mutableMapOf<String, Int>()

    /** soundId → active stream ids (multiple streams per sound allowed) */
    private val activeStreams = mutableMapOf<String, MutableList<Int>>()

    override fun initialize() {
        val audioAttributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_GAME)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()

        soundPool = SoundPool.Builder()
            .setMaxStreams(MAX_STREAMS)
            .setAudioAttributes(audioAttributes)
            .build()

        soundPool.setOnLoadCompleteListener { _, sampleId, status ->
            if (status != 0) {
                Log.w(TAG, "Failed to load sound, sampleId=$sampleId status=$status")
            }
        }
    }

    override fun release() {
        soundPool.release()
        loadedSounds.clear()
        activeStreams.clear()
        scope.cancel()
    }

    // ------------------------------------------------------------------
    // Public API
    // ------------------------------------------------------------------

    /**
     * Pre-load a sound so it is ready for instant playback later.
     * Safe to call multiple times for the same [soundId].
     */
    fun preload(soundId: String) {
        if (soundId in loadedSounds) return
        val resId = resolveAssetResId(soundId) ?: run {
            Log.w(TAG, "Sound asset not found: $soundId")
            return
        }
        val poolId = soundPool.load(context, resId, 1)
        loadedSounds[soundId] = poolId
    }

    /**
     * Play a sound. Auto-loads if not yet cached.
     *
     * @param soundId    logical sound identifier
     * @param loop       true = loop indefinitely until [stop] is called
     * @param volume     0.0 – 1.0
     * @param delayMs    optional delay before playback begins
     */
    fun play(
        soundId: String,
        loop: Boolean = false,
        volume: Float = 1f,
        delayMs: Long = 0L
    ) {
        if (delayMs > 0L) {
            scope.launch {
                delay(delayMs)
                playImmediate(soundId, loop, volume)
            }
        } else {
            playImmediate(soundId, loop, volume)
        }
    }

    /** Stop all active streams for a given sound. */
    fun stop(soundId: String) {
        activeStreams[soundId]?.forEach { streamId ->
            soundPool.stop(streamId)
        }
        activeStreams.remove(soundId)
    }

    /** Stop every currently playing sound. */
    fun stopAll() {
        activeStreams.keys.toList().forEach { stop(it) }
    }

    /** Pause all streams (e.g. when app goes to background). */
    fun pauseAll() = soundPool.autoPause()

    /** Resume after pause. */
    fun resumeAll() = soundPool.autoResume()

    // ------------------------------------------------------------------
    // Internal helpers
    // ------------------------------------------------------------------

    private fun playImmediate(soundId: String, loop: Boolean, volume: Float) {
        val poolId = loadedSounds.getOrPut(soundId) {
            val resId = resolveAssetResId(soundId) ?: run {
                Log.w(TAG, "Cannot play unknown sound: $soundId")
                return
            }
            soundPool.load(context, resId, 1)
        }

        val loopFlag = if (loop) -1 else 0
        val streamId = soundPool.play(poolId, volume, volume, 1, loopFlag, 1f)

        if (streamId == 0) {
            Log.w(TAG, "SoundPool.play returned 0 for $soundId – likely still loading")
        } else {
            activeStreams.getOrPut(soundId) { mutableListOf() }.add(streamId)
        }
    }

    /**
     * Resolve a [soundId] to a raw resource id.
     * Looks in res/raw/<soundId> (all supported extensions).
     *
     * NOTE: For assets/ instead of res/raw/, switch to AssetFileDescriptor
     * and SoundPool.load(AssetFileDescriptor, priority).
     */
    private fun resolveAssetResId(soundId: String): Int? {
        // Try res/raw first (simplest approach for short clips)
        for (ext in EXTENSIONS) {
            val name = soundId.replace("-", "_").replace(" ", "_")
            val resId = context.resources.getIdentifier(name, "raw", context.packageName)
            if (resId != 0) return resId
        }
        return null
    }
}
