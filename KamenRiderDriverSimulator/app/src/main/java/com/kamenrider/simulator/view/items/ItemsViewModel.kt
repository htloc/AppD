package com.kamenrider.simulator.view.items

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import com.kamenrider.simulator.common.event.GameAction
import com.kamenrider.simulator.common.manager.ActionManager
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
    val selectedItemId: String? = null
)

@HiltViewModel
class ItemsViewModel @Inject constructor(
    private val repository: DriverRepository,
    private val actionManager: ActionManager,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    // Optional: filter by driver id if navigated from a driver detail
    private val driverId: String? = savedStateHandle["driverId"]

    private val _uiState = MutableStateFlow(ItemsUiState())
    val uiState: StateFlow<ItemsUiState> = _uiState.asStateFlow()

    init {
        val items = if (driverId != null) {
            repository.getItemsByDriver(driverId)
        } else {
            repository.getAllItems()
        }
        _uiState.update { it.copy(items = items) }
    }

    fun onItemSelected(item: RiderItem) {
        _uiState.update { it.copy(selectedItemId = item.id) }
        actionManager.dispatchAction(GameAction.PlaySound("menu_select"))
    }

    fun onItemInserted(item: RiderItem, slotIndex: Int, onNavigate: () -> Unit) {
        actionManager.dispatchAction(GameAction.InsertItem(item.id, slotIndex))
        actionManager.dispatchAction(GameAction.PlaySound("gashat_insert"))
        onNavigate()
    }
}
