package com.kamenrider.simulator.view.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kamenrider.simulator.common.manager.InputManager

/**
 * BeltView – the interactive Kamen Rider Driver UI.
 *
 * Behaviour:
 * - Tap on a slot to "insert" an item (triggers via [onSlotTap])
 * - Tap on the henshin button to activate ([onHenshinTap])
 * - Swipe left/right on the belt body to signal belt actions
 *
 * All input is forwarded to [InputManager] using its logical target IDs so the
 * [ActionManager] mapping table can match and dispatch the right [GameAction].
 */
@Composable
fun BeltView(
    inputManager: InputManager,
    slotCount: Int = 1,
    insertedItemLabels: List<String?> = emptyList(),
    isActive: Boolean = false,
    isTransforming: Boolean = false,
    onSlotTap: (slotIndex: Int) -> Unit = {},
    onHenshinTap: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "belt_glow")
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glow_alpha"
    )

    val beltColor by animateColorAsState(
        targetValue = when {
            isTransforming -> Color(0xFFFF69B4)
            isActive       -> Color(0xFF42A5F5)
            else           -> Color(0xFF3D5A80)
        },
        animationSpec = tween(300),
        label = "belt_color"
    )

    val shape = RoundedCornerShape(24.dp)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(
                Brush.horizontalGradient(
                    listOf(Color(0xFF0D0D0D), Color(0xFF1A1A2E), Color(0xFF0D0D0D))
                )
            )
            .border(
                width = 2.dp,
                color = beltColor.copy(alpha = if (isActive || isTransforming) glowAlpha else 0.5f),
                shape = shape
            )
            // Swipe detection on the belt body
            .pointerInput("belt") {
                var swipeStart = Offset.Zero
                var swipeStartTime = 0L
                detectDragGestures(
                    onDragStart = { offset ->
                        swipeStart = offset
                        swipeStartTime = System.currentTimeMillis()
                        inputManager.onDragStart("belt", offset)
                    },
                    onDrag = { change, _ ->
                        inputManager.onDragUpdate("belt", change.position)
                    },
                    onDragEnd = {
                        // If drag distance is large enough, treat as swipe
                        inputManager.onDragEnd("belt", Offset.Zero)
                    },
                    onDragCancel = { inputManager.onDragCancel("belt") }
                )
            }
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // --- Belt header ---
            Text(
                text = "GAMER DRIVER",
                color = beltColor,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 3.sp
            )

            // --- Gashat slots + henshin button ---
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Item slots
                repeat(slotCount) { index ->
                    val label = insertedItemLabels.getOrNull(index)
                    ItemSlot(
                        slotIndex = index,
                        label = label,
                        isActive = label != null,
                        inputManager = inputManager,
                        onTap = { onSlotTap(index) }
                    )
                }

                Spacer(Modifier.width(8.dp))

                // Henshin button
                HenshinButton(
                    isActive = insertedItemLabels.any { it != null },
                    isTransforming = isTransforming,
                    glowAlpha = glowAlpha,
                    inputManager = inputManager,
                    onTap = onHenshinTap
                )
            }

            // --- Status label ---
            Text(
                text = when {
                    isTransforming -> "HENSHIN!"
                    insertedItemLabels.any { it != null } -> "READY"
                    else -> "INSERT ITEM"
                },
                color = Color.White.copy(alpha = 0.7f),
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 2.sp
            )
        }
    }
}

@Composable
private fun ItemSlot(
    slotIndex: Int,
    label: String?,
    isActive: Boolean,
    inputManager: InputManager,
    onTap: () -> Unit,
    modifier: Modifier = Modifier
) {
    val targetId = "slot_$slotIndex"
    val shape = RoundedCornerShape(12.dp)
    val borderColor = if (isActive) Color(0xFF42A5F5) else Color(0xFF3D5A80)

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size(64.dp)
            .clip(shape)
            .background(
                if (isActive) Color(0xFF0D47A1).copy(alpha = 0.6f)
                else Color.Black.copy(alpha = 0.4f)
            )
            .border(1.5.dp, borderColor, shape)
            .pointerInput(targetId) {
                detectTapGestures { offset ->
                    inputManager.onTap(targetId, offset)
                    onTap()
                }
            }
    ) {
        if (isActive && label != null) {
            Text(
                text = label.take(6),
                color = Color.White,
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 2
            )
        } else {
            Text(
                text = "${slotIndex + 1}",
                color = Color.White.copy(alpha = 0.3f),
                fontSize = 20.sp,
                fontWeight = FontWeight.Light
            )
        }
    }
}

@Composable
private fun HenshinButton(
    isActive: Boolean,
    isTransforming: Boolean,
    glowAlpha: Float,
    inputManager: InputManager,
    onTap: () -> Unit,
    modifier: Modifier = Modifier
) {
    val buttonColor = when {
        isTransforming -> Color(0xFFFF69B4)
        isActive       -> Color(0xFFE91E63)
        else           -> Color(0xFF37474F)
    }

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size(80.dp)
            .clip(CircleShape)
            .background(
                Brush.radialGradient(
                    listOf(buttonColor.copy(alpha = 0.9f), buttonColor.copy(alpha = 0.5f))
                )
            )
            .border(
                width = if (isActive) 2.dp else 1.dp,
                color = buttonColor.copy(alpha = if (isActive) glowAlpha else 0.3f),
                shape = CircleShape
            )
            .drawBehind {
                if (isActive) {
                    drawCircle(
                        color = buttonColor.copy(alpha = glowAlpha * 0.3f),
                        radius = size.minDimension / 1.6f,
                        style = Stroke(width = 6.dp.toPx())
                    )
                }
            }
            .scale(if (isTransforming) 1.05f else 1f)
            .pointerInput("btn_henshin") {
                detectTapGestures { offset ->
                    inputManager.onTap("btn_henshin", offset)
                    onTap()
                }
            }
    ) {
        Text(
            text = if (isTransforming) "!!!" else "HENSHIN",
            color = Color.White,
            fontSize = if (isTransforming) 14.sp else 8.sp,
            fontWeight = FontWeight.Black,
            letterSpacing = if (isTransforming) 0.sp else 1.sp
        )
    }
}
