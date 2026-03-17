package com.kamenrider.simulator.view.items

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.kamenrider.simulator.data.model.ItemRarity
import com.kamenrider.simulator.data.model.RiderItem
import com.kamenrider.simulator.view.components.GameButton
import com.kamenrider.simulator.view.components.ItemCard
import com.kamenrider.simulator.view.components.RarityBadge

/**
 * Items Screen – vertical list of collectible Gashats with slot picker.
 */
@Composable
fun ItemsScreen(
    onBack: () -> Unit,
    onItemInserted: () -> Unit,
    viewModel: ItemsViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(Color(0xFF040410), Color(0xFF0A0A1A), Color(0xFF0D1B2A))
                )
            )
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // ---- Top bar ------------------------------------------------
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
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "GASHAT SELECTION",
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 2.sp
                    )
                    Text(
                        text = "${state.items.size} available",
                        color = Color.White.copy(alpha = 0.4f),
                        fontSize = 11.sp
                    )
                }
            }

            // ---- Slot picker (when driver has multiple slots) ------------
            if ((state.driver?.insertSlots ?: 1) > 1) {
                SlotPicker(
                    slotCount = state.driver!!.insertSlots,
                    insertedItemIds = state.insertedItemIds,
                    items = state.items,
                    selectedSlotIndex = state.selectedSlotIndex,
                    onSlotSelected = viewModel::onSlotSelected,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }

            // ---- Item list -----------------------------------------------
            LazyColumn(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.weight(1f)
            ) {
                items(state.items, key = { it.id }) { item ->
                    val isSelected = item.id == state.selectedItemId
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        ItemCard(
                            item = item,
                            isSelected = isSelected,
                            onClick = { viewModel.onItemSelected(it) }
                        )

                        AnimatedVisibility(
                            visible = isSelected,
                            enter = fadeIn(tween(200)) + expandVertically(tween(200)),
                            exit  = fadeOut(tween(150)) + shrinkVertically(tween(150))
                        ) {
                            InsertPanel(
                                item = item,
                                slotCount = state.driver?.insertSlots ?: 1,
                                selectedSlotIndex = state.selectedSlotIndex,
                                insertedItemIds = state.insertedItemIds,
                                onSlotSelect = viewModel::onSlotSelected,
                                onInsert = {
                                    viewModel.onItemInserted(
                                        item,
                                        slotIndex = state.selectedSlotIndex
                                    ) { onItemInserted() }
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Sub-composables
// ---------------------------------------------------------------------------

@Composable
private fun SlotPicker(
    slotCount: Int,
    insertedItemIds: List<String?>,
    items: List<RiderItem>,
    selectedSlotIndex: Int,
    onSlotSelected: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFF0F1C36).copy(alpha = 0.8f))
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = "SELECT TARGET SLOT",
            color = Color(0xFF42A5F5),
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 2.sp
        )
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            repeat(slotCount) { index ->
                val insertedId = insertedItemIds.getOrNull(index)
                val insertedName = insertedId?.let { id ->
                    items.find { it.id == id }?.name?.take(10)
                }
                SlotButton(
                    index = index,
                    label = insertedName ?: "Empty",
                    isOccupied = insertedId != null,
                    isSelected = index == selectedSlotIndex,
                    onClick = { onSlotSelected(index) },
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun SlotButton(
    index: Int,
    label: String,
    isOccupied: Boolean,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val borderColor by animateColorAsState(
        targetValue = when {
            isSelected  -> Color(0xFFE91E63)
            isOccupied  -> Color(0xFF42A5F5)
            else        -> Color(0xFF3D5A80)
        },
        animationSpec = tween(200),
        label = "slot_border"
    )
    val shape = RoundedCornerShape(10.dp)
    Column(
        modifier = modifier
            .clip(shape)
            .background(
                if (isSelected) Color(0xFF200A14)
                else Color(0xFF0D0D1A)
            )
            .border(if (isSelected) 2.dp else 1.dp, borderColor, shape)
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = "SLOT ${index + 1}",
            color = borderColor,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp
        )
        Text(
            text = label,
            color = Color.White.copy(alpha = if (isOccupied) 0.9f else 0.4f),
            fontSize = 11.sp,
            maxLines = 1
        )
    }
}

@Composable
private fun InsertPanel(
    item: RiderItem,
    slotCount: Int,
    selectedSlotIndex: Int,
    insertedItemIds: List<String?>,
    onSlotSelect: (Int) -> Unit,
    onInsert: () -> Unit
) {
    val rarityColor = when (item.rarity) {
        ItemRarity.COMMON     -> Color(0xFFAAAAAA)
        ItemRarity.RARE       -> Color(0xFF42A5F5)
        ItemRarity.SUPER_RARE -> Color(0xFFAB47BC)
        ItemRarity.LEGENDARY  -> Color(0xFFFFD700)
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFF0F1C36).copy(alpha = 0.85f))
            .border(1.dp, rarityColor.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Item summary row
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(44.dp)
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
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(item.name, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(3.dp))
                RarityBadge(rarity = item.rarity)
            }
        }

        // Slot selector (only if multiple slots)
        if (slotCount > 1) {
            Text(
                text = "INSERT INTO SLOT:",
                color = Color.White.copy(alpha = 0.6f),
                fontSize = 11.sp,
                letterSpacing = 1.sp
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                repeat(slotCount) { index ->
                    val occupied = insertedItemIds.getOrNull(index) != null
                    val isChosen = index == selectedSlotIndex
                    val chipColor = when {
                        isChosen -> Color(0xFFE91E63)
                        occupied -> Color(0xFF42A5F5)
                        else     -> Color(0xFF3D5A80)
                    }
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(chipColor.copy(alpha = if (isChosen) 0.25f else 0.1f))
                            .border(1.dp, chipColor.copy(alpha = 0.7f), RoundedCornerShape(8.dp))
                            .clickable { onSlotSelect(index) }
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            if (isChosen) {
                                Icon(
                                    imageVector = Icons.Filled.CheckCircle,
                                    contentDescription = null,
                                    tint = chipColor,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                            Text(
                                text = "Slot ${index + 1}",
                                color = chipColor,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }
        }

        GameButton(
            text = "INSERT TO BELT",
            onClick = onInsert,
            modifier = Modifier.fillMaxWidth(),
            gradientColors = listOf(Color(0xFFE91E63), Color(0xFFFF5722))
        )
    }
}
