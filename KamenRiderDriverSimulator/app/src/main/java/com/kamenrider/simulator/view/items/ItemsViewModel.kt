package com.kamenrider.simulator.view.items

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import com.kamenrider.simulator.common.event.GameAction
import com.kamenrider.simulator.common.manager.ActionManager
import com.kamenrider.simulator.data.model.Driver
import com.kamenrider.simulator.data.model.RiderItem
import com.kamenrider.simulator.data.repository.DriverRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject

data class ItemsUiState(
    val items: List<RiderItem> = emptyList(),
    val selectedItemId: String? = null,
    val selectedSlotIndex: Int = 0,
    val driver: Driver? = null,
    val insertedItemIds: List<String?> = emptyList()
)

@HiltViewModel
class ItemsViewModel @Inject constructor(
    private val repository: DriverRepository,
    private val actionManager: ActionManager,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val driverId: String? = savedStateHandle["driverId"]

    private val _uiState = MutableStateFlow(ItemsUiState())
    val uiState: StateFlow<ItemsUiState> = _uiState.asStateFlow()

    init {
        val items = if (driverId != null) {
            repository.getItemsByDriver(driverId)
        } else {
            repository.getAllItems()
        }
        val driver = driverId?.let { repository.getDriverById(it) }
        _uiState.update {
            it.copy(
                items = items,
                driver = driver,
                insertedItemIds = MutableList(driver?.insertSlots ?: 1) { null }
            )
        }
    }

    fun onItemSelected(item: RiderItem) {
        _uiState.update { it.copy(selectedItemId = item.id) }
        actionManager.dispatchAction(GameAction.PlaySound("menu_select"))
    }

    fun onSlotSelected(slotIndex: Int) {
        _uiState.update { it.copy(selectedSlotIndex = slotIndex) }
    }

    fun onItemInserted(item: RiderItem, slotIndex: Int, onNavigate: () -> Unit) {
        _uiState.update { state ->
            val newSlots = state.insertedItemIds.toMutableList()
            if (slotIndex < newSlots.size) newSlots[slotIndex] = item.id
            state.copy(insertedItemIds = newSlots)
        }
        actionManager.dispatchAction(GameAction.InsertItem(item.id, slotIndex))
        actionManager.dispatchAction(GameAction.PlaySound("gashat_insert"))
        onNavigate()
    }
}
