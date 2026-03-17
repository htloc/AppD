package com.kamenrider.simulator.common.manager

import androidx.compose.ui.geometry.Offset
import com.kamenrider.simulator.common.base.BaseManager
import com.kamenrider.simulator.common.event.DragState
import com.kamenrider.simulator.common.event.InputEvent
import com.kamenrider.simulator.common.event.SwipeDirection
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.abs
import kotlin.math.sqrt

/**
 * InputManager – central hub for all user input.
 *
 * The UI layer calls [onTap], [onSwipeEnd], [onDragUpdate], etc. and this
 * manager emits structured [InputEvent]s via [inputEvents].
 *
 * Design decisions:
 * - Uses a [SharedFlow] so multiple collectors (ActionManager, debug overlay)
 *   can subscribe independently.
 * - The manager never performs business logic – it only classifies gestures.
 */
@Singleton
class InputManager @Inject constructor() : BaseManager {

    companion object {
        private const val SWIPE_MIN_DISTANCE_PX = 80f
        private const val SWIPE_MIN_VELOCITY = 100f   // px/s
    }

    private val _inputEvents = MutableSharedFlow<InputEvent>(extraBufferCapacity = 32)
    val inputEvents: SharedFlow<InputEvent> = _inputEvents.asSharedFlow()

    // Drag tracking state
    private var dragStart: Offset = Offset.Zero

    override fun initialize() { /* nothing to allocate */ }
    override fun release()    { /* nothing to free   */ }

    // ------------------------------------------------------------------
    // Public API called from Compose gesture detectors
    // ------------------------------------------------------------------

    /**
     * Report a tap on a specific UI target.
     * @param targetId logical id of the tapped composable (can be null for
     *                 taps on the open canvas).
     * @param position screen position in px.
     */
    fun onTap(targetId: String?, position: Offset) {
        emit(InputEvent.Tap(targetId = targetId, position = position))
    }

    /**
     * Report the completion of a swipe gesture.
     * Direction and validity are calculated internally.
     */
    fun onSwipeEnd(
        targetId: String?,
        start: Offset,
        end: Offset,
        durationMs: Long
    ) {
        val dx = end.x - start.x
        val dy = end.y - start.y
        val distance = sqrt(dx * dx + dy * dy)

        if (distance < SWIPE_MIN_DISTANCE_PX) return   // too short – ignore

        val velocity = distance / (durationMs.coerceAtLeast(1) / 1000f)
        if (velocity < SWIPE_MIN_VELOCITY) return        // too slow  – ignore

        val direction = if (abs(dx) > abs(dy)) {
            if (dx > 0) SwipeDirection.RIGHT else SwipeDirection.LEFT
        } else {
            if (dy > 0) SwipeDirection.DOWN else SwipeDirection.UP
        }

        emit(
            InputEvent.Swipe(
                targetId = targetId,
                direction = direction,
                startPosition = start,
                endPosition = end,
                velocity = velocity
            )
        )
    }

    fun onDragStart(targetId: String?, position: Offset) {
        dragStart = position
        emit(
            InputEvent.Drag(
                targetId = targetId,
                startPosition = position,
                currentPosition = position,
                state = DragState.START
            )
        )
    }

    fun onDragUpdate(targetId: String?, position: Offset) {
        emit(
            InputEvent.Drag(
                targetId = targetId,
                startPosition = dragStart,
                currentPosition = position,
                state = DragState.ONGOING
            )
        )
    }

    fun onDragEnd(targetId: String?, position: Offset) {
        emit(
            InputEvent.Drag(
                targetId = targetId,
                startPosition = dragStart,
                currentPosition = position,
                state = DragState.END
            )
        )
        dragStart = Offset.Zero
    }

    fun onDragCancel(targetId: String?) {
        emit(
            InputEvent.Drag(
                targetId = targetId,
                startPosition = dragStart,
                currentPosition = dragStart,
                state = DragState.CANCELLED
            )
        )
        dragStart = Offset.Zero
    }

    // ------------------------------------------------------------------
    // Internal
    // ------------------------------------------------------------------

    private fun emit(event: InputEvent) {
        _inputEvents.tryEmit(event)
    }
}
