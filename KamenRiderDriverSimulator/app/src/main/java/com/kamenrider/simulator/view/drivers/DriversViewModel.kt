package com.kamenrider.simulator.view.drivers

import androidx.lifecycle.ViewModel
import com.kamenrider.simulator.common.event.GameAction
import com.kamenrider.simulator.common.manager.ActionManager
import com.kamenrider.simulator.data.model.Driver
import com.kamenrider.simulator.data.repository.DriverRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

@HiltViewModel
class DriversViewModel @Inject constructor(
    private val repository: DriverRepository,
    private val actionManager: ActionManager
) : ViewModel() {

    private val _drivers = MutableStateFlow<List<Driver>>(emptyList())
    val drivers: StateFlow<List<Driver>> = _drivers.asStateFlow()

    init {
        _drivers.value = repository.getAllDrivers()
    }

    fun onDriverSelected(driver: Driver, onNavigate: (driverId: String) -> Unit) {
        actionManager.dispatchAction(GameAction.PlaySound("menu_select"))
        onNavigate(driver.id)
    }
}
