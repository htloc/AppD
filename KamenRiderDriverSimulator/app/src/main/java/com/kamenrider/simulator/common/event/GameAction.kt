package com.kamenrider.simulator.common.event

/**
 * Sealed hierarchy of high-level game actions produced by the ActionMapper.
 *
 * Actions are the bridge between raw [InputEvent]s and the execution systems
 * (sound, animation, 3D, transformation).  A single user gesture can expand
 * into a list of actions.
 */
sealed class GameAction {

    // --- Audio ----------------------------------------------------------------

    /** Play a one-shot or looping sound. */
    data class PlaySound(
        val soundId: String,
        val loop: Boolean = false,
        val delayMs: Long = 0L
    ) : GameAction()

    /** Stop a currently playing sound. */
    data class StopSound(val soundId: String) : GameAction()

    // --- Animation ------------------------------------------------------------

    /** Trigger a named UI or model animation. */
    data class PlayAnimation(
        val animationId: String,
        val target: String = "belt_ui",  // logical target tag
        val delayMs: Long = 0L
    ) : GameAction()

    data class StopAnimation(val animationId: String) : GameAction()

    // --- 3D Model -------------------------------------------------------------

    /** Load or swap the active 3D model in the scene. */
    data class LoadModel(val modelId: String) : GameAction()

    /** Reset the scene to its default/empty state. */
    object ClearModel : GameAction()

    // --- Transformation -------------------------------------------------------

    /** Trigger a full transformation sequence defined by [formId]. */
    data class TriggerTransformation(val formId: String) : GameAction()

    /** Insert a collectible item into the driver. */
    data class InsertItem(val itemId: String, val slotIndex: Int = 0) : GameAction()

    /** Remove an item from the driver. */
    data class RemoveItem(val slotIndex: Int = 0) : GameAction()

    // --- UI -------------------------------------------------------------------

    /** Navigate to a different screen. */
    data class NavigateTo(val route: String) : GameAction()

    /** Show a toast / snackbar message. */
    data class ShowMessage(val message: String) : GameAction()
}
