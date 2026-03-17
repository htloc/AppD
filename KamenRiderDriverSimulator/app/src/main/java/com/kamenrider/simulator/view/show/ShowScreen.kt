package com.kamenrider.simulator.view.show

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Snackbar
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.kamenrider.simulator.common.manager.InputManager
import com.kamenrider.simulator.common.manager.TransformationState
import com.kamenrider.simulator.object3d.SceneController
import com.kamenrider.simulator.view.components.BeltView
import com.kamenrider.simulator.view.components.GameButton
import com.kamenrider.simulator.view.components.ScreenFlash
import com.kamenrider.simulator.view.components.rememberShakeOffset
import kotlinx.coroutines.delay

/**
 * ShowScreen – the main game screen.
 *
 * Layout (top to bottom):
 *  1. Top bar (back + driver name)
 *  2. Rider artwork panel with animated 2D visualization
 *  3. Rider form name + description panel
 *  4. Belt UI (BeltView)
 *  5. Action row (Add Item / Reset)
 */
@Composable
fun ShowScreen(
    driverId: String,
    inputManager: InputManager,
    sceneController: SceneController,
    onNavigateToItems: (driverId: String) -> Unit,
    onBack: () -> Unit,
    viewModel: ShowViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val transformState by viewModel.transformationState.collectAsState()

    val isTransforming = transformState is TransformationState.InProgress
    val isTransformed  = transformState is TransformationState.Completed

    // Screen flash & shake triggered when transformation fires
    val flashTrigger = remember { mutableStateOf(false) }
    val shakeTrigger = remember { mutableStateOf(false) }
    LaunchedEffect(isTransforming) {
        if (isTransforming) {
            flashTrigger.value = !flashTrigger.value
            shakeTrigger.value = !shakeTrigger.value
        }
    }
    val shakeOffset = rememberShakeOffset(trigger = shakeTrigger.value)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .offset { shakeOffset }
            .background(Brush.verticalGradient(listOf(Color(0xFF040410), Color(0xFF0A0A1A), Color(0xFF0D1B2A))))
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // ---- Top bar ------------------------------------------------
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 8.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = Color.White
                    )
                }
                Text(
                    text = uiState.driver?.name?.uppercase() ?: "DRIVER",
                    color = Color.White,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.5.sp
                )
            }

            // ---- Rider artwork panel ------------------------------------
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                // Animated background glow
                RiderArtworkBackground(
                    isTransformed = isTransformed,
                    isTransforming = isTransforming,
                    flashColor = uiState.currentForm?.uiEffect?.flashColor
                )

                // Rider artwork or idle state
                AnimatedContent(
                    targetState = uiState.currentForm,
                    transitionSpec = {
                        (fadeIn(tween(600)) + scaleIn(tween(600), initialScale = 0.8f))
                            .togetherWith(fadeOut(tween(300)) + scaleOut(tween(300), targetScale = 1.1f))
                    },
                    label = "rider_artwork"
                ) { form ->
                    if (form != null) {
                        RiderArtworkDisplay(
                            imageAsset = form.imageAsset,
                            name = form.name,
                            isTransforming = isTransforming,
                            isTransformed = isTransformed
                        )
                    } else {
                        IdleDisplay(
                            driverImageAsset = uiState.driver?.imageAsset,
                            hasItems = uiState.insertedItemIds.any { it != null }
                        )
                    }
                }
            }

            // ---- Form info panel ----------------------------------------
            FormInfoPanel(
                formName = uiState.currentForm?.name,
                formDescription = when {
                    isTransforming -> "HENSHIN! Transformation in progress..."
                    isTransformed  -> uiState.currentForm?.let {
                        "Required: ${it.requiredItemIds.size} Gashat(s)"
                    } ?: ""
                    else -> null
                },
                insertedCount = uiState.insertedItemIds.count { it != null },
                totalSlots = uiState.driver?.insertSlots ?: 1
            )

            // ---- Belt ---------------------------------------------------
            BeltView(
                inputManager    = inputManager,
                slotCount       = uiState.driver?.insertSlots ?: 1,
                insertedItemLabels = uiState.insertedItemIds.map { id ->
                    if (id != null) {
                        uiState.availableItems.find { it.id == id }?.name?.take(8)
                    } else null
                },
                isActive        = uiState.insertedItemIds.any { it != null },
                isTransforming  = isTransforming,
                onHenshinTap    = { viewModel.onHenshinPressed() },
                modifier        = Modifier.padding(horizontal = 16.dp)
            )

            Spacer(Modifier.height(12.dp))

            // ---- Action buttons -----------------------------------------
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                GameButton(
                    text     = "ADD ITEM",
                    onClick  = { onNavigateToItems(driverId) },
                    modifier = Modifier.weight(1f),
                    gradientColors = listOf(Color(0xFF1565C0), Color(0xFF42A5F5))
                )
                if (isTransformed || uiState.insertedItemIds.any { it != null }) {
                    GameButton(
                        text     = "RESET",
                        onClick  = { viewModel.onResetAll() },
                        modifier = Modifier.weight(1f),
                        gradientColors = listOf(Color(0xFF37474F), Color(0xFF546E7A))
                    )
                }
            }

            Spacer(Modifier.height(24.dp))
        }

        // ---- Screen flash -----------------------------------------------
        ScreenFlash(
            trigger  = flashTrigger.value,
            hexColor = uiState.currentForm?.uiEffect?.flashColor ?: "#FFFFFF"
        )

        // ---- Snackbar ---------------------------------------------------
        uiState.snackbarMessage?.let { msg ->
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 100.dp, start = 16.dp, end = 16.dp)
            ) {
                Snackbar(
                    containerColor = Color(0xFF1A1A2E),
                    contentColor   = Color.White
                ) {
                    Text(msg)
                }
            }
            LaunchedEffect(msg) {
                delay(2500)
                viewModel.onSnackbarDismissed()
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Sub-composables
// ---------------------------------------------------------------------------

@Composable
private fun RiderArtworkBackground(
    isTransformed: Boolean,
    isTransforming: Boolean,
    flashColor: String?
) {
    val infiniteTransition = rememberInfiniteTransition(label = "bg_anim")

    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.15f,
        targetValue  = if (isTransforming) 0.7f else if (isTransformed) 0.45f else 0.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(if (isTransforming) 400 else 1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glow_alpha"
    )

    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue  = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(8000, easing = LinearEasing)
        ),
        label = "rotation"
    )

    val color = remember(flashColor) {
        flashColor?.let {
            try {
                val int = android.graphics.Color.parseColor(it)
                Color(int)
            } catch (_: Exception) { Color(0xFFE91E63) }
        } ?: Color(0xFFE91E63)
    }

    // Rotating glow rings
    Box(
        modifier = Modifier
            .fillMaxSize()
            .alpha(glowAlpha)
    ) {
        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .size(280.dp)
                .rotate(rotation)
                .background(
                    Brush.radialGradient(
                        listOf(color.copy(alpha = 0.3f), Color.Transparent)
                    ),
                    CircleShape
                )
        )
        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .size(200.dp)
                .rotate(-rotation * 0.7f)
                .background(
                    Brush.radialGradient(
                        listOf(color.copy(alpha = 0.2f), Color.Transparent)
                    ),
                    CircleShape
                )
        )
    }
}

@Composable
private fun RiderArtworkDisplay(
    imageAsset: String,
    name: String,
    isTransforming: Boolean,
    isTransformed: Boolean
) {
    val scale by animateFloatAsState(
        targetValue = if (isTransforming) 1.08f else 1f,
        animationSpec = tween(300),
        label = "artwork_scale"
    )

    val alpha by animateFloatAsState(
        targetValue = if (isTransformed) 1f else 0.88f,
        animationSpec = tween(600),
        label = "artwork_alpha"
    )

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(0.65f)
            .padding(horizontal = 32.dp, vertical = 8.dp)
    ) {
        // Blurred glow behind the artwork
        AsyncImage(
            model = "file:///android_asset/$imageAsset",
            contentDescription = null,
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .fillMaxSize()
                .scale(scale * 1.15f)
                .blur(20.dp)
                .alpha(0.4f)
        )

        // Main artwork
        AsyncImage(
            model = "file:///android_asset/$imageAsset",
            contentDescription = name,
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .fillMaxSize()
                .scale(scale)
                .alpha(alpha)
        )
    }
}

@Composable
private fun IdleDisplay(
    driverImageAsset: String?,
    hasItems: Boolean
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier
            .fillMaxWidth()
            .padding(32.dp)
    ) {
        if (driverImageAsset != null) {
            AsyncImage(
                model = "file:///android_asset/$driverImageAsset",
                contentDescription = "Driver",
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .fillMaxWidth(0.6f)
                    .aspectRatio(1.4f)
                    .alpha(0.55f)
            )
            Spacer(Modifier.height(24.dp))
        }

        Text(
            text = if (hasItems) "Press HENSHIN to transform!" else "Insert a Gashat\nand press HENSHIN",
            color = Color.White.copy(alpha = if (hasItems) 0.85f else 0.35f),
            fontSize = 16.sp,
            fontWeight = if (hasItems) FontWeight.SemiBold else FontWeight.Normal,
            textAlign = TextAlign.Center,
            lineHeight = 24.sp
        )

        if (hasItems) {
            Spacer(Modifier.height(12.dp))
            Text(
                text = "「変身!」",
                color = Color(0xFFE91E63).copy(alpha = 0.8f),
                fontSize = 24.sp,
                fontWeight = FontWeight.Black,
                fontStyle = FontStyle.Italic
            )
        }
    }
}

@Composable
private fun FormInfoPanel(
    formName: String?,
    formDescription: String?,
    insertedCount: Int,
    totalSlots: Int
) {
    AnimatedContent(
        targetState = formName,
        transitionSpec = {
            (fadeIn(tween(400)) + scaleIn(tween(400), 0.9f))
                .togetherWith(fadeOut(tween(300)))
        },
        label = "form_info"
    ) { name ->
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp)
        ) {
            if (name != null) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFF0F1C36).copy(alpha = 0.85f))
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = name,
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                    if (formDescription != null) {
                        Text(
                            text = formDescription,
                            color = Color(0xFF42A5F5),
                            fontSize = 11.sp,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            } else {
                // Show slot status when no form active
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFF0F1C36).copy(alpha = 0.6f))
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "SLOTS: $insertedCount / $totalSlots",
                        color = Color.White.copy(alpha = 0.5f),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = 1.5.sp
                    )
                }
            }
        }
    }
}
