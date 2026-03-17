package com.kamenrider.simulator.common.event

import androidx.compose.ui.geometry.Offset

/**
 * Sealed hierarchy of all possible raw input events captured by the UI.
 * These are intentionally dumb – they carry no business logic.
 */
sealed class InputEvent {
    /** Unique identifier of the UI element that was touched, if any. */
    abstract val targetId: String?

    data class Tap(
        override val targetId: String?,
        val position: Offset
    ) : InputEvent()

    data class Swipe(
        override val targetId: String?,
        val direction: SwipeDirection,
        val startPosition: Offset,
        val endPosition: Offset,
        val velocity: Float               // pixels / second
    ) : InputEvent()

    data class Drag(
        override val targetId: String?,
        val startPosition: Offset,
        val currentPosition: Offset,
        val state: DragState
    ) : InputEvent()

    data class MultiTouch(
        override val targetId: String?,
        val pointers: List<Offset>,
        val gesture: MultiTouchGesture
    ) : InputEvent()
}

enum class SwipeDirection { LEFT, RIGHT, UP, DOWN }

enum class DragState { START, ONGOING, END, CANCELLED }

enum class MultiTouchGesture { PINCH_IN, PINCH_OUT, ROTATE }
