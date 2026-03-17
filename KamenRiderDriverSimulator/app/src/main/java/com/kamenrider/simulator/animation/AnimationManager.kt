package com.kamenrider.simulator.animation

import com.kamenrider.simulator.common.base.BaseManager
import com.kamenrider.simulator.data.model.AnimationTarget
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * AnimationManager
 *
 * Owns and dispatches animation commands to the appropriate rendering layer.
 *
 * Architecture:
 * - Animation commands are emitted as [AnimationCommand]s on a [SharedFlow].
 * - Compose UI collects [uiAnimations]; the Sceneform controller collects
 *   [modelAnimations].
 * - Supports chaining and delayed sequences.
 *
 * Types handled:
 * - UI animations  (Compose AnimatedVisibility / Animatable – triggered via state)
 * - 3D animations  (Sceneform model animator)
 * - Screen effects (flash, shake – driven by Compose state)
 */
@Singleton
class AnimationManager @Inject constructor() : BaseManager {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    // --- UI / Screen effects -------------------------------------------------
    private val _uiAnimations = MutableSharedFlow<AnimationCommand>(extraBufferCapacity = 32)
    val uiAnimations: SharedFlow<AnimationCommand> = _uiAnimations.asSharedFlow()

    // --- 3D model animations -------------------------------------------------
    private val _modelAnimations = MutableSharedFlow<AnimationCommand>(extraBufferCapacity = 16)
    val modelAnimations: SharedFlow<AnimationCommand> = _modelAnimations.asSharedFlow()

    /** Active coroutine jobs keyed by animationId for cancellation support. */
    private val activeJobs = mutableMapOf<String, Job>()

    override fun initialize() { /* no-op */ }

    override fun release() {
        scope.cancel()
        activeJobs.clear()
    }

    // ------------------------------------------------------------------
    // Public API
    // ------------------------------------------------------------------

    /**
     * Play a single animation immediately or after [delayMs].
     */
    fun play(animationId: String, target: AnimationTarget, delayMs: Long = 0L) {
        val job = scope.launch {
            if (delayMs > 0) delay(delayMs)
            emit(AnimationCommand.Play(animationId, target))
        }
        activeJobs[animationId] = job
    }

    /**
     * Play a sequence of [AnimationStep]s with their individual delays.
     * The whole sequence shares a single [sequenceId] for cancellation.
     */
    fun playSequence(
        sequenceId: String,
        steps: List<AnimationSequenceStep>
    ) {
        cancelSequence(sequenceId)
        val job = scope.launch {
            steps.forEach { step ->
                launch {
                    if (step.delayMs > 0) delay(step.delayMs)
                    emit(AnimationCommand.Play(step.animationId, step.target))
                }
            }
        }
        activeJobs[sequenceId] = job
    }

    fun stop(animationId: String) {
        activeJobs[animationId]?.cancel()
        activeJobs.remove(animationId)
        scope.launch {
            emit(AnimationCommand.Stop(animationId, AnimationTarget.BELT_UI))
            emit(AnimationCommand.Stop(animationId, AnimationTarget.RIDER_MODEL))
        }
    }

    fun cancelSequence(sequenceId: String) {
        activeJobs[sequenceId]?.cancel()
        activeJobs.remove(sequenceId)
    }

    fun stopAll() {
        activeJobs.values.forEach { it.cancel() }
        activeJobs.clear()
    }

    // ------------------------------------------------------------------
    // Internal
    // ------------------------------------------------------------------

    private suspend fun emit(command: AnimationCommand) {
        when (command.target) {
            AnimationTarget.RIDER_MODEL -> _modelAnimations.emit(command)
            else                        -> _uiAnimations.emit(command)
        }
    }
}

// ---------------------------------------------------------------------------
// Command types
// ---------------------------------------------------------------------------

sealed class AnimationCommand {
    abstract val animationId: String
    abstract val target: AnimationTarget

    data class Play(
        override val animationId: String,
        override val target: AnimationTarget
    ) : AnimationCommand()

    data class Stop(
        override val animationId: String,
        override val target: AnimationTarget
    ) : AnimationCommand()
}

data class AnimationSequenceStep(
    val animationId: String,
    val target: AnimationTarget,
    val delayMs: Long = 0L
)
