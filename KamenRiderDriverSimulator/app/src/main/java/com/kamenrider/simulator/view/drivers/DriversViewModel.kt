package com.kamenrider.simulator.view.drivers

import androidx.lifecycle.ViewModel
import com.kamenrider.simulator.common.event.GameAction
import com.kamenrider.simulator.common.manager.ActionManager
import com.kamenrider.simulator.data.model.Driver
import com.kamenrider.simulator.data.model.TransformationForm
import com.kamenrider.simulator.data.repository.DriverRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject

data class DriversUiState(
    val drivers: List<Driver> = emptyList(),
    val selectedDriver: Driver? = null,
    val selectedDriverForms: List<TransformationForm> = emptyList()
)

@HiltViewModel
class DriversViewModel @Inject constructor(
    private val repository: DriverRepository,
    private val actionManager: ActionManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(DriversUiState())
    val uiState: StateFlow<DriversUiState> = _uiState.asStateFlow()

    init {
        _uiState.value = DriversUiState(drivers = repository.getAllDrivers())
    }

    fun onDriverSelected(driver: Driver, onNavigate: (driverId: String) -> Unit) {
        actionManager.dispatchAction(GameAction.PlaySound("menu_select"))
        onNavigate(driver.id)
    }

    fun onDriverHovered(driver: Driver) {
        _uiState.update {
            it.copy(
                selectedDriver = driver,
                selectedDriverForms = repository.getFormsByDriver(driver.id)
            )
        }
    }

    fun clearSelection() {
        _uiState.update { it.copy(selectedDriver = null, selectedDriverForms = emptyList()) }
    }
}
