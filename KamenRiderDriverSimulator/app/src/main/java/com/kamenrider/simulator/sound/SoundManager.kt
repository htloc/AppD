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
 * Manages all audio using Android [SoundPool].
 * Sound files are loaded from assets/sound/<soundId>.<ext> (ogg/mp3/wav).
 */
@Singleton
class SoundManager @Inject constructor(
    @ApplicationContext private val context: Context
) : BaseManager {

    companion object {
        private const val TAG = "SoundManager"
        private const val MAX_STREAMS = 8
        private const val SOUND_DIR = "sound"
        private val EXTENSIONS = listOf("ogg", "mp3", "wav")
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private lateinit var soundPool: SoundPool

    /** soundId → SoundPool resource id */
    private val loadedSounds = mutableMapOf<String, Int>()

    /** soundId → active stream ids */
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
     * Pre-load a sound from assets so it is ready for instant playback.
     * Safe to call multiple times for the same [soundId].
     */
    fun preload(soundId: String) {
        if (soundId in loadedSounds) return
        val poolId = loadFromAssets(soundId) ?: run {
            Log.w(TAG, "Sound asset not found for preload: $soundId")
            return
        }
        loadedSounds[soundId] = poolId
    }

    /**
     * Play a sound. Auto-loads from assets if not yet cached.
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
            scope.launch { playImmediate(soundId, loop, volume) }
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
            loadFromAssets(soundId) ?: run {
                Log.w(TAG, "Cannot play unknown sound: $soundId")
                return
            }
        }

        val loopFlag = if (loop) -1 else 0
        val streamId = soundPool.play(poolId, volume, volume, 1, loopFlag, 1f)

        if (streamId == 0) {
            Log.w(TAG, "SoundPool.play returned 0 for $soundId – likely still loading; retrying")
            // Retry after a short delay to let the asset finish loading
            scope.launch {
                delay(200)
                val retryStreamId = soundPool.play(poolId, volume, volume, 1, loopFlag, 1f)
                if (retryStreamId != 0) {
                    activeStreams.getOrPut(soundId) { mutableListOf() }.add(retryStreamId)
                }
            }
        } else {
            activeStreams.getOrPut(soundId) { mutableListOf() }.add(streamId)
        }
    }

    /**
     * Load a sound from assets/sound/<soundId>.<ext> via [AssetFileDescriptor].
     * Returns the SoundPool pool id, or null if the asset cannot be found.
     */
    private fun loadFromAssets(soundId: String): Int? {
        val normalizedId = soundId.replace("-", "_").replace(" ", "_")
        for (ext in EXTENSIONS) {
            val assetPath = "$SOUND_DIR/$normalizedId.$ext"
            try {
                val afd = context.assets.openFd(assetPath)
                val poolId = soundPool.load(afd, 1)
                afd.close()
                Log.d(TAG, "Loaded sound: $assetPath → poolId=$poolId")
                return poolId
            } catch (_: Exception) {
                // Try next extension
            }
        }
        Log.w(TAG, "Sound not found in assets: $normalizedId (tried ${EXTENSIONS.joinToString()})")
        return null
    }
}
