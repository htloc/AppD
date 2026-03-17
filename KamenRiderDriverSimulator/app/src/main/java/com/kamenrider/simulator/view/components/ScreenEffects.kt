package com.kamenrider.simulator.view.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.core.graphics.toColorInt

/**
 * Full-screen flash overlay. Triggered whenever [trigger] changes to true.
 * The alpha animates: 1 → 0 over [durationMs].
 */
@Composable
fun ScreenFlash(
    trigger: Boolean,
    hexColor: String = "#FFFFFF",
    durationMs: Int = 400,
    modifier: Modifier = Modifier
) {
    val alpha = remember { Animatable(0f) }

    LaunchedEffect(trigger) {
        if (trigger) {
            alpha.snapTo(0.85f)
            alpha.animateTo(0f, animationSpec = tween(durationMillis = durationMs))
        }
    }

    val color = remember(hexColor) {
        try { Color(hexColor.toColorInt()) } catch (_: Exception) { Color.White }
    }

    if (alpha.value > 0f) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(color.copy(alpha = alpha.value))
        )
    }
}

/**
 * Screen shake effect using Modifier.offset animated via [Animatable].
 * Apply this to the root container of the show screen.
 */
@Composable
fun rememberShakeOffset(trigger: Boolean): androidx.compose.ui.unit.IntOffset {
    val offsetX = remember { Animatable(0f) }

    LaunchedEffect(trigger) {
        if (trigger) {
            val shakeAmplitude = 12f
            val shakeDuration = 60
            repeat(5) {
                offsetX.animateTo(shakeAmplitude, tween(shakeDuration))
                offsetX.animateTo(-shakeAmplitude, tween(shakeDuration))
            }
            offsetX.animateTo(0f, tween(shakeDuration))
        }
    }

    return androidx.compose.ui.unit.IntOffset(offsetX.value.toInt(), 0)
}
