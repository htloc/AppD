package com.kamenrider.simulator.view.drivers

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.kamenrider.simulator.data.model.Driver
import com.kamenrider.simulator.data.model.TransformationForm
import com.kamenrider.simulator.view.components.GameButton

/**
 * Drivers Screen – grid of available Driver belts with inline details panel.
 */
@Composable
fun DriversScreen(
    onNavigateToShow: (driverId: String) -> Unit,
    onBack: () -> Unit,
    viewModel: DriversViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(listOf(Color(0xFF040410), Color(0xFF0A0A1A), Color(0xFF0D1B2A)))
            )
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Top bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 8.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = Color.White
                    )
                }
                Column {
                    Text(
                        text = "SELECT DRIVER",
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 2.sp
                    )
                    Text(
                        text = "${uiState.drivers.size} driver(s) available",
                        color = Color.White.copy(alpha = 0.4f),
                        fontSize = 11.sp
                    )
                }
            }

            // Driver grid
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                contentPadding = PaddingValues(
                    start = 16.dp, end = 16.dp,
                    top = 8.dp, bottom = 24.dp
                ),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.weight(1f)
            ) {
                items(uiState.drivers, key = { it.id }) { driver ->
                    EnhancedDriverCard(
                        driver = driver,
                        isSelected = uiState.selectedDriver?.id == driver.id,
                        onClick = {
                            viewModel.onDriverHovered(driver)
                            viewModel.onDriverSelected(driver) { id -> onNavigateToShow(id) }
                        }
                    )
                }
            }

            // Driver detail panel (slides up when a driver is selected)
            AnimatedVisibility(
                visible = uiState.selectedDriver != null,
                enter = fadeIn(tween(300)) + slideInVertically(tween(300)) { it },
                exit  = fadeOut(tween(200))
            ) {
                uiState.selectedDriver?.let { driver ->
                    DriverDetailPanel(
                        driver = driver,
                        forms = uiState.selectedDriverForms,
                        onSelect = { onNavigateToShow(driver.id) }
                    )
                }
            }
        }
    }
}

@Composable
private fun EnhancedDriverCard(
    driver: Driver,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val borderColor by animateColorAsState(
        targetValue = if (isSelected) Color(0xFFE91E63) else Color(0xFF3D5A80),
        animationSpec = tween(300),
        label = "border_color"
    )
    val shape = RoundedCornerShape(20.dp)

    Column(
        modifier = Modifier
            .clip(shape)
            .background(
                Brush.verticalGradient(
                    if (isSelected) listOf(Color(0xFF1C0A1C), Color(0xFF16213E))
                    else listOf(Color(0xFF0F3460), Color(0xFF16213E))
                )
            )
            .border(if (isSelected) 2.dp else 1.dp, borderColor, shape)
            .clickable(onClick = onClick)
    ) {
        // Image area
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1.4f)
                .background(Color.Black.copy(alpha = 0.4f))
        ) {
            AsyncImage(
                model = "file:///android_asset/${driver.imageAsset}",
                contentDescription = driver.name,
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.Center)
                    .padding(8.dp)
            )
            if (isSelected) {
                Icon(
                    imageVector = Icons.Filled.CheckCircle,
                    contentDescription = "Selected",
                    tint = Color(0xFFE91E63),
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp)
                        .size(20.dp)
                )
            }
        }

        // Text area
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = driver.name,
                color = Color.White,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = driver.description,
                color = Color.White.copy(alpha = 0.55f),
                fontSize = 10.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                MiniChip(label = "${driver.supportedForms.size} Forms", color = Color(0xFF42A5F5))
                MiniChip(label = "${driver.insertSlots} Slots", color = Color(0xFFAB47BC))
            }
        }
    }
}

@Composable
private fun MiniChip(label: String, color: Color) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(color.copy(alpha = 0.15f))
            .border(0.5.dp, color.copy(alpha = 0.5f), RoundedCornerShape(6.dp))
            .padding(horizontal = 6.dp, vertical = 2.dp)
    ) {
        Text(label, color = color, fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun DriverDetailPanel(
    driver: Driver,
    forms: List<TransformationForm>,
    onSelect: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                Brush.verticalGradient(listOf(Color(0xFF0F1C36), Color(0xFF0A0A1A)))
            )
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = driver.name,
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "${driver.insertSlots} gashat slot(s)",
                    color = Color.White.copy(alpha = 0.5f),
                    fontSize = 12.sp
                )
            }
            GameButton(
                text = "USE DRIVER",
                onClick = onSelect,
                gradientColors = listOf(Color(0xFFE91E63), Color(0xFFFF5722)),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp)
            )
        }

        if (forms.isNotEmpty()) {
            Text(
                text = "AVAILABLE FORMS",
                color = Color(0xFF42A5F5),
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 2.sp
            )
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                forms.forEach { form ->
                    FormRow(form = form)
                }
            }
        }
    }
}

@Composable
private fun FormRow(form: TransformationForm) {
    val flashColor = remember(form.uiEffect.flashColor) {
        try {
            val c = android.graphics.Color.parseColor(form.uiEffect.flashColor)
            Color(c)
        } catch (_: Exception) { Color(0xFFE91E63) }
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(Color(0xFF16213E).copy(alpha = 0.7f))
            .border(1.dp, flashColor.copy(alpha = 0.3f), RoundedCornerShape(10.dp))
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Form image
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(Color.Black.copy(alpha = 0.4f))
        ) {
            AsyncImage(
                model = "file:///android_asset/${form.imageAsset}",
                contentDescription = form.name,
                contentScale = ContentScale.Fit,
                modifier = Modifier.fillMaxSize()
            )
        }
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(
                text = form.name,
                color = Color.White,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = "Requires: ${form.requiredItemIds.size} Gashat(s)",
                color = Color.White.copy(alpha = 0.5f),
                fontSize = 11.sp
            )
        }
        // Flash color dot
        Box(
            modifier = Modifier
                .size(10.dp)
                .background(flashColor, RoundedCornerShape(5.dp))
        )
    }
}
