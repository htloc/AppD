package com.kamenrider.simulator.view.home

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
import javax.inject.Inject

data class HomeUiState(
    val drivers: List<Driver> = emptyList(),
    val items: List<RiderItem> = emptyList()
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val actionManager: ActionManager,
    private val repository: DriverRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        _uiState.value = HomeUiState(
            drivers = repository.getAllDrivers(),
            items   = repository.getAllItems()
        )
    }

    fun onStartClicked(navigateToDrivers: () -> Unit) {
        actionManager.dispatchAction(GameAction.PlaySound("menu_select"))
        navigateToDrivers()
    }

    fun onExitClicked(finish: () -> Unit) {
        actionManager.dispatchAction(GameAction.PlaySound("menu_back"))
        finish()
    }
}
