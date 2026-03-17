package com.kamenrider.simulator.common.manager

import com.kamenrider.simulator.common.base.BaseManager
import com.kamenrider.simulator.common.event.GameAction
import com.kamenrider.simulator.common.event.InputEvent
import com.kamenrider.simulator.common.event.SwipeDirection
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * ActionManager – the nerve centre of the Input → Action → Execution pipeline.
 *
 * Responsibilities:
 * 1. Subscribe to [InputManager.inputEvents].
 * 2. Map each [InputEvent] to one or more [GameAction]s using the active
 *    [ActionMapping] table.
 * 3. Publish [GameAction]s on [actionFlow] so downstream executors
 *    (SoundManager, AnimationManager, TransformationManager) can react.
 *
 * The mapping table is swappable at runtime (e.g. home-screen vs show-screen
 * have different belt interaction rules), making this fully context-aware.
 */
@Singleton
class ActionManager @Inject constructor(
    private val inputManager: InputManager
) : BaseManager {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private val _actionFlow = MutableSharedFlow<GameAction>(extraBufferCapacity = 64)
    val actionFlow: SharedFlow<GameAction> = _actionFlow.asSharedFlow()

    /**
     * Mutable mapping table.
     * Key   = "${inputType}_${targetId}" e.g. "tap_btn_henshin", "swipe_right_belt"
     * Value = factory that produces a list of [GameAction]s
     */
    private val mappings = mutableMapOf<String, (InputEvent) -> List<GameAction>>()

    override fun initialize() {
        scope.launch {
            inputManager.inputEvents.collect { event ->
                dispatch(event)
            }
        }
    }

    override fun release() {
        scope.cancel()
    }

    // ------------------------------------------------------------------
    // Registration API
    // ------------------------------------------------------------------

    /** Register a static list of actions for a specific input key. */
    fun register(inputKey: String, actions: List<GameAction>) {
        mappings[inputKey] = { actions }
    }

    /** Register a dynamic factory for a specific input key. */
    fun registerFactory(inputKey: String, factory: (InputEvent) -> List<GameAction>) {
        mappings[inputKey] = factory
    }

    /** Remove a previously registered mapping. */
    fun unregister(inputKey: String) {
        mappings.remove(inputKey)
    }

    /** Remove all mappings (e.g. when leaving a screen). */
    fun clearMappings() {
        mappings.clear()
    }

    /**
     * Directly emit an action, bypassing the input system.
     * Useful for programmatic triggers (e.g. demo / tutorial).
     */
    fun dispatchAction(action: GameAction) {
        scope.launch { _actionFlow.emit(action) }
    }

    fun dispatchActions(actions: List<GameAction>) {
        actions.forEach { dispatchAction(it) }
    }

    // ------------------------------------------------------------------
    // Internal dispatch
    // ------------------------------------------------------------------

    private fun dispatch(event: InputEvent) {
        val keys = buildLookupKeys(event)
        val actions = keys
            .firstNotNullOfOrNull { key -> mappings[key]?.invoke(event) }
            ?: return

        scope.launch {
            actions.forEach { action -> _actionFlow.emit(action) }
        }
    }

    /**
     * Build candidate lookup keys from an event, ordered from most specific
     * to least specific so we always prefer the more precise match.
     *
     * Examples:
     *   tap + targetId "btn_henshin" → ["tap_btn_henshin", "tap_*"]
     *   swipe_right + targetId "belt" → ["swipe_right_belt", "swipe_right_*"]
     */
    private fun buildLookupKeys(event: InputEvent): List<String> {
        val type = when (event) {
            is InputEvent.Tap        -> "tap"
            is InputEvent.Swipe      -> "swipe_${event.direction.name.lowercase()}"
            is InputEvent.Drag       -> "drag_${event.state.name.lowercase()}"
            is InputEvent.MultiTouch -> "multitouch_${event.gesture.name.lowercase()}"
        }
        val targetId = event.targetId

        return buildList {
            if (targetId != null) add("${type}_${targetId}")
            add("${type}_*")
        }
    }
}

// ---------------------------------------------------------------------------
// Convenience builder for registering Show-screen belt mappings
// ---------------------------------------------------------------------------

/**
 * Builds the default action mapping for the Show (main game) screen.
 * Called from the ShowViewModel to install belt-specific bindings.
 */
fun ActionManager.installShowScreenMappings(
    currentFormId: () -> String?,
    insertedItemIds: () -> List<String>
) {
    clearMappings()

    // Tap on the henshin button → trigger transformation if items are inserted
    registerFactory("tap_btn_henshin") { _ ->
        val formId = currentFormId()
        if (formId != null) {
            listOf(GameAction.TriggerTransformation(formId))
        } else {
            listOf(GameAction.ShowMessage("Insert a Gashat first!"))
        }
    }

    // Swipe RIGHT on the belt → activate henshin (same as button)
    registerFactory("swipe_right_belt") { _ ->
        val formId = currentFormId()
        if (formId != null) {
            listOf(GameAction.TriggerTransformation(formId))
        } else {
            listOf(GameAction.ShowMessage("Swipe again after inserting an item!"))
        }
    }

    // Tap on an item slot → show insert prompt (handled by UI layer)
    register(
        "tap_slot_0",
        listOf(GameAction.ShowMessage("Tap an item from the list to insert it."))
    )

    // Swipe LEFT on belt → remove inserted item
    registerFactory("swipe_left_belt") { _ ->
        buildList {
            val ids = insertedItemIds()
            if (ids.isNotEmpty()) {
                add(GameAction.RemoveItem(slotIndex = 0))
                add(GameAction.PlaySound("menu_back"))
            }
        }
    }
}
