package com.kamenrider.simulator.view.home

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
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
import com.kamenrider.simulator.data.model.ItemRarity
import com.kamenrider.simulator.data.model.RiderItem
import com.kamenrider.simulator.view.components.GameButton

/**
 * Home Screen – entry point of the app.
 *
 * Shows animated title, then driver/item previews, then navigation buttons.
 */
@Composable
fun HomeScreen(
    onNavigateToDrivers: () -> Unit,
    onExit: () -> Unit,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    // Entrance animations
    val titleAlpha   = remember { Animatable(0f) }
    val titleScale   = remember { Animatable(0.7f) }
    val contentAlpha = remember { Animatable(0f) }
    val buttonsAlpha = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        titleAlpha.animateTo(1f, tween(700))
        titleScale.animateTo(1f, tween(700))
        contentAlpha.animateTo(1f, tween(600))
        buttonsAlpha.animateTo(1f, tween(500))
    }

    // Infinite rotation for decorative rings
    val infiniteTransition = rememberInfiniteTransition(label = "home_bg")
    val ringRotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue  = 360f,
        animationSpec = infiniteRepeatable(tween(12000, easing = LinearEasing)),
        label = "ring_rotation"
    )
    val glowPulse by infiniteTransition.animateFloat(
        initialValue = 0.25f,
        targetValue  = 0.55f,
        animationSpec = infiniteRepeatable(
            tween(1800, easing = FastOutSlowInEasing),
            RepeatMode.Reverse
        ),
        label = "glow_pulse"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(Color(0xFF040410), Color(0xFF0A0A1A), Color(0xFF0D1B2A))
                )
            )
    ) {
        // --- Decorative rotating rings in background ---
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .size(360.dp)
                .alpha(glowPulse)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .rotate(ringRotation)
                    .background(
                        Brush.radialGradient(
                            listOf(Color(0x55E91E63), Color.Transparent)
                        ),
                        CircleShape
                    )
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(56.dp))

            // --- Title section ---
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .alpha(titleAlpha.value)
                    .scale(titleScale.value)
            ) {
                Text(
                    text = "KAMEN RIDER",
                    color = Color(0xFFE91E63),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 8.sp
                )

                Spacer(Modifier.height(6.dp))

                Text(
                    text = "DRIVER\nSIMULATOR",
                    color = Color.White,
                    fontSize = 40.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 2.sp,
                    lineHeight = 46.sp,
                    textAlign = TextAlign.Center
                )

                Spacer(Modifier.height(8.dp))

                Text(
                    text = "変身！",
                    color = Color(0xFFE91E63).copy(alpha = 0.8f),
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Black,
                    fontStyle = FontStyle.Italic
                )
            }

            Spacer(Modifier.height(32.dp))

            // --- Driver preview ---
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .alpha(contentAlpha.value),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                SectionLabel(text = "AVAILABLE DRIVERS  ·  ${uiState.drivers.size}")

                LazyRow(
                    contentPadding = PaddingValues(horizontal = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(uiState.drivers, key = { it.id }) { driver ->
                        DriverPreviewCard(
                            name = driver.name,
                            imageAsset = driver.imageAsset,
                            formCount = driver.supportedForms.size,
                            slotCount = driver.insertSlots,
                            onClick = onNavigateToDrivers
                        )
                    }
                }

                Spacer(Modifier.height(4.dp))

                SectionLabel(text = "COLLECTIBLE GASHATS  ·  ${uiState.items.size}")

                LazyRow(
                    contentPadding = PaddingValues(horizontal = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(uiState.items, key = { it.id }) { item ->
                        ItemPreviewChip(item = item)
                    }
                }
            }

            Spacer(Modifier.weight(1f))

            // --- Buttons ---
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .alpha(buttonsAlpha.value),
                verticalArrangement = Arrangement.spacedBy(14.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                GameButton(
                    text = "START GAME",
                    onClick = { viewModel.onStartClicked(onNavigateToDrivers) },
                    modifier = Modifier.fillMaxWidth(0.8f),
                    gradientColors = listOf(Color(0xFFE91E63), Color(0xFFFF5722))
                )

                GameButton(
                    text = "EXIT",
                    onClick = { viewModel.onExitClicked(onExit) },
                    modifier = Modifier.fillMaxWidth(0.8f),
                    gradientColors = listOf(Color(0xFF37474F), Color(0xFF263238))
                )
            }

            Spacer(Modifier.height(32.dp))

            Text(
                text = "v1.0.0",
                color = Color.White.copy(alpha = 0.2f),
                fontSize = 11.sp
            )

            Spacer(Modifier.height(24.dp))
        }
    }
}

// ---------------------------------------------------------------------------
// Helper composables
// ---------------------------------------------------------------------------

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text,
        color = Color(0xFF42A5F5),
        fontSize = 11.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 2.sp,
        modifier = Modifier.padding(start = 4.dp)
    )
}

@Composable
private fun DriverPreviewCard(
    name: String,
    imageAsset: String,
    formCount: Int,
    slotCount: Int,
    onClick: () -> Unit
) {
    val shape = RoundedCornerShape(16.dp)
    Column(
        modifier = Modifier
            .width(160.dp)
            .clip(shape)
            .background(Brush.verticalGradient(listOf(Color(0xFF0F3460), Color(0xFF16213E))))
            .border(1.dp, Color(0xFF3D5A80), shape)
            .clickable(onClick = onClick)
            .padding(bottom = 12.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1.4f)
                .background(Color.Black.copy(alpha = 0.35f))
        ) {
            AsyncImage(
                model = "file:///android_asset/$imageAsset",
                contentDescription = name,
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(8.dp)
            )
        }
        Spacer(Modifier.height(8.dp))
        Text(
            text = name,
            color = Color.White,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = 10.dp)
        )
        Spacer(Modifier.height(4.dp))
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(horizontal = 10.dp)
        ) {
            StatChip(label = "$formCount Forms", color = Color(0xFF42A5F5))
            StatChip(label = "$slotCount Slots", color = Color(0xFFAB47BC))
        }
    }
}

@Composable
private fun StatChip(label: String, color: Color) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(color.copy(alpha = 0.15f))
            .border(0.5.dp, color.copy(alpha = 0.5f), RoundedCornerShape(6.dp))
            .padding(horizontal = 6.dp, vertical = 2.dp)
    ) {
        Text(
            text = label,
            color = color,
            fontSize = 10.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
private fun ItemPreviewChip(item: RiderItem) {
    val rarityColor = when (item.rarity) {
        ItemRarity.COMMON     -> Color(0xFFAAAAAA)
        ItemRarity.RARE       -> Color(0xFF42A5F5)
        ItemRarity.SUPER_RARE -> Color(0xFFAB47BC)
        ItemRarity.LEGENDARY  -> Color(0xFFFFD700)
    }
    val shape = RoundedCornerShape(12.dp)
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .clip(shape)
            .background(Color(0xFF1A1A2E))
            .border(1.dp, rarityColor.copy(alpha = 0.5f), shape)
            .padding(horizontal = 10.dp, vertical = 8.dp)
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(Color.Black.copy(alpha = 0.4f))
        ) {
            AsyncImage(
                model = "file:///android_asset/${item.imageAsset}",
                contentDescription = item.name,
                contentScale = ContentScale.Fit,
                modifier = Modifier.fillMaxSize()
            )
        }
        Spacer(Modifier.width(8.dp))
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = item.name,
                color = Color.White,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = item.rarity.name.replace("_", " "),
                color = rarityColor,
                fontSize = 10.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}
