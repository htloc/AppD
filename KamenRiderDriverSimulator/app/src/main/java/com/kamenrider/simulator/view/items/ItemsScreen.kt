package com.kamenrider.simulator.view.items

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.kamenrider.simulator.view.components.GameButton
import com.kamenrider.simulator.view.components.ItemCard

/**
 * Items Screen – vertical list of collectible items for a given driver.
 *
 * Tapping "Use" inserts the item into slot 0 and navigates back.
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
                Brush.verticalGradient(listOf(Color(0xFF0A0A1A), Color(0xFF0D1B2A)))
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
                Text(
                    text = "ITEMS",
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 2.sp
                )
                androidx.compose.foundation.layout.Spacer(
                    Modifier.weight(1f)
                )
                Text(
                    text = "${state.items.size} items",
                    color = Color.White.copy(alpha = 0.5f),
                    fontSize = 12.sp
                )
            }

            // Item list
            LazyColumn(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.weight(1f)
            ) {
                items(state.items, key = { it.id }) { item ->
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        ItemCard(
                            item = item,
                            isSelected = item.id == state.selectedItemId,
                            onClick = { viewModel.onItemSelected(it) }
                        )

                        // Show "Use" button if this item is selected
                        if (item.id == state.selectedItemId) {
                            GameButton(
                                text = "INSERT TO BELT",
                                onClick = {
                                    viewModel.onItemInserted(item, slotIndex = 0) {
                                        onItemInserted()
                                    }
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 4.dp),
                                gradientColors = listOf(Color(0xFF1565C0), Color(0xFF42A5F5))
                            )
                        }
                    }
                }
            }
        }
    }
}
