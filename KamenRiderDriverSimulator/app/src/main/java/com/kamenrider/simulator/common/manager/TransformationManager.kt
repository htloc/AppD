package com.kamenrider.simulator.common.manager

import android.util.Log
import com.kamenrider.simulator.animation.AnimationManager
import com.kamenrider.simulator.animation.AnimationSequenceStep
import com.kamenrider.simulator.common.base.BaseManager
import com.kamenrider.simulator.data.model.AnimationStep
import com.kamenrider.simulator.data.model.AnimationTarget
import com.kamenrider.simulator.data.model.TransformationForm
import com.kamenrider.simulator.data.repository.DriverRepository
import com.kamenrider.simulator.object3d.SceneController
import com.kamenrider.simulator.sound.SoundManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * TransformationManager
 *
 * The central orchestrator for all Kamen Rider henshin sequences.
 *
 * Flow:
 *  1. [triggerTransformation] is called with a [formId].
 *  2. The form's [TransformationForm] config is fetched from [DriverRepository].
 *  3. Item validation ensures all required items are inserted.
 *  4. Sound, animation, and 3D sequences fire concurrently per their
 *     configured [delayMs] offsets.
 *  5. [state] is updated so the UI can react (show effects, swap artwork, etc.).
 *
 * Adding a new Rider:
 *  - Define a new [TransformationForm] in [GameConfig].
 *  - Place model/sound/image assets in the corresponding asset directories.
 *  - No code changes required in this manager.
 */
@Singleton
class TransformationManager @Inject constructor(
    private val repository: DriverRepository,
    private val soundManager: SoundManager,
    private val animationManager: AnimationManager,
    private val sceneController: SceneController
) : BaseManager {

    companion object {
        private const val TAG = "TransformationManager"
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    // --- Observable state ----------------------------------------------------

    private val _state = MutableStateFlow<TransformationState>(TransformationState.Idle)
    val state: StateFlow<TransformationState> = _state.asStateFlow()

    /** Currently inserted item ids (indexed by slot). */
    private val insertedItems = mutableMapOf<Int, String>()   // slotIndex → itemId

    private var activeTransformJob: Job? = null

    override fun initialize() { /* no-op */ }

    override fun release() {
        scope.cancel()
    }

    // ------------------------------------------------------------------
    // Item insertion
    // ------------------------------------------------------------------

    fun insertItem(itemId: String, slotIndex: Int = 0) {
        insertedItems[slotIndex] = itemId
        _state.value = TransformationState.ItemInserted(
            insertedItems = insertedItems.values.toList()
        )
        Log.d(TAG, "Item inserted: $itemId at slot $slotIndex")
    }

    fun removeItem(slotIndex: Int = 0) {
        insertedItems.remove(slotIndex)
        _state.value = if (insertedItems.isEmpty()) {
            TransformationState.Idle
        } else {
            TransformationState.ItemInserted(insertedItems.values.toList())
        }
    }

    fun clearAllItems() {
        insertedItems.clear()
        _state.value = TransformationState.Idle
    }

    fun getInsertedItemIds(): List<String> = insertedItems.values.toList()

    // ------------------------------------------------------------------
    // Transformation trigger
    // ------------------------------------------------------------------

    /**
     * Begin the henshin sequence for [formId].
     * Validates that all required items are present before starting.
     */
    fun triggerTransformation(formId: String) {
        val form = repository.getFormById(formId) ?: run {
            Log.w(TAG, "Unknown formId: $formId")
            return
        }

        if (!validateItems(form)) {
            _state.value = TransformationState.Error(
                "Missing required items for ${form.name}. " +
                "Required: ${form.requiredItemIds.joinToString()}"
            )
            return
        }

        activeTransformJob?.cancel()
        activeTransformJob = scope.launch {
            executeTransformation(form)
        }
    }

    // ------------------------------------------------------------------
    // Internal
    // ------------------------------------------------------------------

    private fun validateItems(form: TransformationForm): Boolean {
        val presentIds = insertedItems.values.toSet()
        return form.requiredItemIds.all { it in presentIds }
    }

    private suspend fun executeTransformation(form: TransformationForm) {
        Log.d(TAG, "Starting transformation: ${form.name}")
        _state.value = TransformationState.InProgress(form)

        // 1. Fire all sound steps (each has its own delay)
        form.soundSequence.forEach { step ->
            soundManager.play(
                soundId = step.soundId,
                loop = step.loop,
                delayMs = step.delayMs
            )
        }

        // 2. Fire all animation steps
        val animSteps = form.animationSequence.map { step: AnimationStep ->
            AnimationSequenceStep(
                animationId = step.animationId,
                target = step.target,
                delayMs = step.delayMs
            )
        }
        animationManager.playSequence(sequenceId = form.id, steps = animSteps)

        // 3. Swap the 3D model (after a brief delay to sync with animation)
        val modelDelay = form.animationSequence
            .filter { it.target == AnimationTarget.RIDER_MODEL }
            .minOfOrNull { it.delayMs } ?: 0L

        kotlinx.coroutines.delay(modelDelay)
        sceneController.showModel(form.modelAsset.removeSuffix(".sfb").removePrefix("object3d/"))

        // 4. Transition to Completed state
        val totalDuration = maxOf(
            form.soundSequence.maxOfOrNull { it.delayMs } ?: 0L,
            form.animationSequence.maxOfOrNull { it.delayMs } ?: 0L
        ) + 500L  // small buffer

        kotlinx.coroutines.delay(totalDuration)
        _state.value = TransformationState.Completed(form)
        Log.d(TAG, "Transformation complete: ${form.name}")
    }
}

// ---------------------------------------------------------------------------
// State
// ---------------------------------------------------------------------------

sealed class TransformationState {

    /** No items inserted, belt is resting. */
    object Idle : TransformationState()

    /** One or more items inserted – belt is primed. */
    data class ItemInserted(val insertedItems: List<String>) : TransformationState()

    /** Henshin sequence is actively playing. */
    data class InProgress(val form: TransformationForm) : TransformationState()

    /** Henshin sequence finished – Rider is transformed. */
    data class Completed(val form: TransformationForm) : TransformationState()

    /** Something went wrong. */
    data class Error(val message: String) : TransformationState()
}
