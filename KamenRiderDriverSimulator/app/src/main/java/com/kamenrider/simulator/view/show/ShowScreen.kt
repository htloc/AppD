package com.kamenrider.simulator.view.show

import android.view.ViewGroup
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Snackbar
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.google.ar.sceneform.SceneView
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
 *  2. 3D SceneView (Sceneform)
 *  3. Rider form name panel
 *  4. Belt UI (BeltView)
 *  5. Action row (Add Item / Reset)
 *
 * All input routes through InputManager → ActionManager → execution systems.
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

    // Attach Sceneform SceneView
    var sceneView by remember { mutableStateOf<SceneView?>(null) }
    DisposableEffect(sceneView) {
        sceneView?.let { sceneController.attachToView(it) }
        onDispose { sceneController.detachFromView() }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .offset { shakeOffset }
            .background(Brush.verticalGradient(listOf(Color(0xFF0A0A1A), Color(0xFF0D1B2A))))
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

            // ---- 3D scene view ------------------------------------------
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                AndroidView(
                    factory = { ctx ->
                        SceneView(ctx).also { sv ->
                            sv.layoutParams = ViewGroup.LayoutParams(
                                ViewGroup.LayoutParams.MATCH_PARENT,
                                ViewGroup.LayoutParams.MATCH_PARENT
                            )
                            sceneView = sv
                        }
                    },
                    modifier = Modifier.fillMaxSize()
                )

                // Rider artwork overlay (fades in on transform)
                AnimatedVisibility(
                    visible = uiState.currentForm != null,
                    enter = fadeIn(tween(600)) + scaleIn(tween(600), initialScale = 0.85f),
                    exit  = fadeOut(tween(300))
                ) {
                    uiState.currentForm?.let { form ->
                        AsyncImage(
                            model = "file:///android_asset/${form.imageAsset}",
                            contentDescription = form.name,
                            contentScale = ContentScale.Fit,
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(16.dp)
                                .alpha(if (isTransformed) 1f else 0.75f)
                        )
                    }
                }

                // Idle placeholder
                if (uiState.currentForm == null) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Insert an item\nand press HENSHIN",
                            color = Color.White.copy(alpha = 0.3f),
                            fontSize = 16.sp,
                            textAlign = TextAlign.Center,
                            lineHeight = 24.sp
                        )
                    }
                }
            }

            // ---- Form name ----------------------------------------------
            AnimatedContent(
                targetState = uiState.currentForm?.name,
                transitionSpec = {
                    (fadeIn(tween(400)) + scaleIn(tween(400), 0.9f))
                        .togetherWith(fadeOut(tween(300)))
                },
                label = "form_name"
            ) { formName ->
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    if (formName != null) {
                        Text(
                            text = formName,
                            color = Color.White,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }

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
                if (isTransformed) {
                    GameButton(
                        text     = "RESET",
                        onClick  = { viewModel.onItemRemovedFromSlot(0) },
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
