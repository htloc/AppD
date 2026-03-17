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
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
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
import androidx.compose.runtime.setValue
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.kamenrider.simulator.common.manager.TransformationState
import com.kamenrider.simulator.data.model.ItemRarity
import com.kamenrider.simulator.data.model.RiderItem
import com.kamenrider.simulator.data.model.TransformationForm
import com.kamenrider.simulator.view.components.GameButton
import com.kamenrider.simulator.view.components.ScreenFlash
import com.kamenrider.simulator.view.components.rememberShakeOffset
import kotlinx.coroutines.delay

@Composable
fun ShowScreen(
    driverId: String,
    inputManager: com.kamenrider.simulator.common.manager.InputManager,
    sceneController: com.kamenrider.simulator.object3d.SceneController,
    onNavigateToItems: (driverId: String) -> Unit,
    onBack: () -> Unit,
    viewModel: ShowViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val transformState by viewModel.transformationState.collectAsState()

    val isTransforming = transformState is TransformationState.InProgress
    val isTransformed = transformState is TransformationState.Completed

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
        Column(modifier = Modifier.fillMaxSize()) {
            // Top bar
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

            // Rider artwork
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                RiderArtworkBackground(isTransformed, isTransforming, uiState.currentForm?.uiEffect?.flashColor)

                AnimatedContent(
                    targetState = uiState.currentForm,
                    transitionSpec = {
                        (fadeIn(tween(600)) + scaleIn(tween(600), initialScale = 0.8f))
                            .togetherWith(fadeOut(tween(300)) + scaleOut(tween(300), targetScale = 1.1f))
                    },
                    label = "rider_artwork"
                ) { form ->
                    if (form != null) {
                        RiderArtworkDisplay(form.imageAsset, form.name, isTransforming, isTransformed)
                    } else {
                        IdleDisplay(uiState.driver?.imageAsset, uiState.insertedItemIds.any { it != null })
                    }
                }
            }

            // Form info / slot status
            FormInfoPanel(
                formName = uiState.currentForm?.name,
                insertedCount = uiState.insertedItemIds.count { it != null },
                totalSlots = uiState.driver?.insertSlots ?: 1
            )

            // Items row - select item to insert
            ItemsSelectionRow(
                items = uiState.availableItems,
                insertedItemIds = uiState.insertedItemIds,
                selectedItem = uiState.selectedItem,
                onItemSelected = viewModel::onItemSelected,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )

            // Belt slots - tap to insert selected item or remove
            BeltSlotsView(
                slotCount = uiState.driver?.insertSlots ?: 1,
                insertedItemIds = uiState.insertedItemIds,
                items = uiState.availableItems,
                selectedItem = uiState.selectedItem,
                isActive = uiState.insertedItemIds.any { it != null },
                onSlotClick = { slotIndex ->
                    if (uiState.selectedItem != null) {
                        viewModel.onInsertToSlot(slotIndex)
                    } else {
                        // Toggle: if has item, remove it
                        if (uiState.insertedItemIds.getOrNull(slotIndex) != null) {
                            viewModel.onItemRemovedFromSlot(slotIndex)
                        }
                    }
                },
                modifier = Modifier.padding(horizontal = 16.dp)
            )

            Spacer(Modifier.height(12.dp))

            // Action buttons
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (isTransformed && uiState.availableForms.size > 1) {
                    // Show form switcher
                    GameButton(
                        text = "CHANGE LEVEL",
                        onClick = { /* Could show form picker dialog */ },
                        modifier = Modifier.weight(1f),
                        gradientColors = listOf(Color(0xFF7B1FA2), Color(0xFF9C27B0))
                    )
                }

                GameButton(
                    text = "HENSHIN!",
                    onClick = { viewModel.onHenshinPressed() },
                    modifier = Modifier.weight(1f),
                    gradientColors = if (uiState.insertedItemIds.any { it != null })
                        listOf(Color(0xFFE91E63), Color(0xFFFF5722))
                    else
                        listOf(Color(0xFF424242), Color(0xFF616161)),
                    enabled = uiState.insertedItemIds.any { it != null }
                )
            }

            if (isTransformed || uiState.insertedItemIds.any { it != null }) {
                Spacer(Modifier.height(8.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    GameButton(
                        text = "RESET",
                        onClick = { viewModel.onResetAll() },
                        modifier = Modifier.weight(1f),
                        gradientColors = listOf(Color(0xFF37474F), Color(0xFF546E7A))
                    )
                }
            }

            Spacer(Modifier.height(24.dp))
        }

        // Screen flash
        ScreenFlash(
            trigger = flashTrigger.value,
            hexColor = uiState.currentForm?.uiEffect?.flashColor ?: "#FFFFFF"
        )

        // Snackbar
        uiState.snackbarMessage?.let { msg ->
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 100.dp, start = 16.dp, end = 16.dp)
            ) {
                Snackbar(
                    containerColor = Color(0xFF1A1A2E),
                    contentColor = Color.White
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

@Composable
private fun ItemsSelectionRow(
    items: List<RiderItem>,
    insertedItemIds: List<String?>,
    selectedItem: RiderItem?,
    onItemSelected: (RiderItem) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Text(
            text = "TAP ITEM TO SELECT → TAP SLOT TO INSERT",
            color = Color(0xFF42A5F5),
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp,
            modifier = Modifier.padding(bottom = 6.dp)
        )
        
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            contentPadding = PaddingValues(horizontal = 4.dp)
        ) {
            items(items, key = { it.id }) { item ->
                val isInserted = item.id in insertedItemIds
                val isSelected = item.id == selectedItem?.id
                val rarityColor = when (item.rarity) {
                    ItemRarity.COMMON -> Color(0xFFAAAAAA)
                    ItemRarity.RARE -> Color(0xFF42A5F5)
                    ItemRarity.SUPER_RARE -> Color(0xFFAB47BC)
                    ItemRarity.LEGENDARY -> Color(0xFFFFD700)
                }
                
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .width(70.dp)
                        .clickable { onItemSelected(item) }
                ) {
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFF1A1A2E))
                            .border(
                                width = if (isSelected) 2.dp else 1.dp,
                                color = when {
                                    isSelected -> Color(0xFFE91E63)
                                    isInserted -> Color(0xFF4CAF50)
                                    else -> rarityColor.copy(alpha = 0.5f)
                                },
                                shape = RoundedCornerShape(12.dp)
                            )
                            .padding(4.dp)
                    ) {
                        AsyncImage(
                            model = "file:///android_asset/${item.imageAsset}",
                            contentDescription = item.name,
                            contentScale = ContentScale.Fit,
                            modifier = Modifier.fillMaxSize()
                        )
                        if (isInserted) {
                            Icon(
                                imageVector = Icons.Filled.CheckCircle,
                                contentDescription = "Inserted",
                                tint = Color(0xFF4CAF50),
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .size(16.dp)
                            )
                        }
                    }
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = item.name.take(8),
                        color = if (isSelected) Color(0xFFE91E63) else Color.White,
                        fontSize = 9.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

@Composable
private fun BeltSlotsView(
    slotCount: Int,
    insertedItemIds: List<String?>,
    items: List<RiderItem>,
    selectedItem: RiderItem?,
    isActive: Boolean,
    onSlotClick: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Text(
            text = if (selectedItem != null) "TAP SLOT TO INSERT" else "TAP SLOT TO REMOVE",
            color = if (selectedItem != null) Color(0xFFE91E63) else Color.White.copy(alpha = 0.5f),
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp,
            modifier = Modifier.padding(bottom = 6.dp)
        )
        
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            repeat(slotCount) { index ->
                val itemId = insertedItemIds.getOrNull(index)
                val insertedItem = itemId?.let { id -> items.find { it.id == id } }
                val isEmpty = itemId == null
                val canInsert = selectedItem != null && isEmpty
                
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .aspectRatio(1.2f)
                        .clip(RoundedCornerShape(16.dp))
                        .background(
                            Brush.verticalGradient(
                                if (canInsert) listOf(Color(0xFF2A0A14), Color(0xFF1A0A14))
                                else listOf(Color(0xFF0F1C36), Color(0xFF0A0F1A))
                            )
                        )
                        .border(
                            width = if (canInsert) 2.dp else 1.dp,
                            color = when {
                                canInsert -> Color(0xFFE91E63)
                                !isEmpty -> Color(0xFF42A5F5)
                                else -> Color(0xFF3D5A80)
                            },
                            shape = RoundedCornerShape(16.dp)
                        )
                        .clickable { onSlotClick(index) },
                    contentAlignment = Alignment.Center
                ) {
                    if (insertedItem != null) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            AsyncImage(
                                model = "file:///android_asset/${insertedItem.imageAsset}",
                                contentDescription = insertedItem.name,
                                contentScale = ContentScale.Fit,
                                modifier = Modifier
                                    .fillMaxSize(0.6f)
                            )
                            Text(
                                text = "SLOT ${index + 1}",
                                color = Color(0xFF42A5F5),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    } else {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = if (selectedItem != null) "+" else "",
                                color = if (canInsert) Color(0xFFE91E63) else Color.White.copy(alpha = 0.2f),
                                fontSize = 28.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "SLOT ${index + 1}",
                                color = Color.White.copy(alpha = 0.3f),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FormInfoPanel(
    formName: String?,
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
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = name,
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                }
            } else {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFF0F1C36).copy(alpha = 0.6f))
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.Center
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

// Sub-composables (reusing from before)
@Composable
private fun RiderArtworkBackground(isTransformed: Boolean, isTransforming: Boolean, flashColor: String?) {
    val infiniteTransition = rememberInfiniteTransition(label = "bg_anim")
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.15f,
        targetValue = if (isTransforming) 0.7f else if (isTransformed) 0.45f else 0.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(if (isTransforming) 400 else 1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glow_alpha"
    )
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(8000, easing = LinearEasing)),
        label = "rotation"
    )
    val color = remember(flashColor) {
        flashColor?.let {
            try { Color(android.graphics.Color.parseColor(it)) } catch (_: Exception) { Color(0xFFE91E63) }
        } ?: Color(0xFFE91E63)
    }
    Box(modifier = Modifier.fillMaxSize().alpha(glowAlpha)) {
        Box(modifier = Modifier.align(Alignment.Center).size(280.dp).rotate(rotation).background(Brush.radialGradient(listOf(color.copy(alpha = 0.3f), Color.Transparent)), CircleShape))
        Box(modifier = Modifier.align(Alignment.Center).size(200.dp).rotate(-rotation * 0.7f).background(Brush.radialGradient(listOf(color.copy(alpha = 0.2f), Color.Transparent)), CircleShape))
    }
}

@Composable
private fun RiderArtworkDisplay(imageAsset: String, name: String, isTransforming: Boolean, isTransformed: Boolean) {
    val scale by animateFloatAsState(targetValue = if (isTransforming) 1.08f else 1f, animationSpec = tween(300), label = "artwork_scale")
    val alpha by animateFloatAsState(targetValue = if (isTransformed) 1f else 0.88f, animationSpec = tween(600), label = "artwork_alpha")
    Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxWidth().aspectRatio(0.65f).padding(horizontal = 32.dp, vertical = 8.dp)) {
        AsyncImage(model = "file:///android_asset/$imageAsset", contentDescription = null, contentScale = ContentScale.Fit, modifier = Modifier.fillMaxSize().scale(1.15f).blur(20.dp).alpha(0.4f))
        AsyncImage(model = "file:///android_asset/$imageAsset", contentDescription = name, contentScale = ContentScale.Fit, modifier = Modifier.fillMaxSize().scale(scale).alpha(alpha))
    }
}

@Composable
private fun IdleDisplay(driverImageAsset: String?, hasItems: Boolean) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center, modifier = Modifier.fillMaxWidth().padding(32.dp)) {
        if (driverImageAsset != null) {
            AsyncImage(model = "file:///android_asset/$driverImageAsset", contentDescription = "Driver", contentScale = ContentScale.Fit, modifier = Modifier.fillMaxWidth(0.6f).aspectRatio(1.4f).alpha(0.55f))
            Spacer(Modifier.height(24.dp))
        }
        Text(text = if (hasItems) "Select item → Tap slot to insert!" else "Select a Gashat\nand tap a slot", color = Color.White.copy(alpha = if (hasItems) 0.85f else 0.35f), fontSize = 16.sp, fontWeight = if (hasItems) FontWeight.SemiBold else FontWeight.Normal, textAlign = TextAlign.Center, lineHeight = 24.sp)
        if (hasItems) {
            Spacer(Modifier.height(12.dp))
            Text(text = "Press HENSHIN! to transform", color = Color(0xFFE91E63).copy(alpha = 0.8f), fontSize = 18.sp, fontWeight = FontWeight.Bold, fontStyle = FontStyle.Italic)
        }
    }
}
