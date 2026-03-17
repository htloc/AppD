package com.kamenrider.simulator.view.home

import androidx.lifecycle.ViewModel
import com.kamenrider.simulator.common.manager.ActionManager
import com.kamenrider.simulator.common.event.GameAction
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val actionManager: ActionManager
) : ViewModel() {

    fun onStartClicked(navigateToDrivers: () -> Unit) {
        actionManager.dispatchAction(GameAction.PlaySound("menu_select"))
        navigateToDrivers()
    }

    fun onExitClicked(finish: () -> Unit) {
        actionManager.dispatchAction(GameAction.PlaySound("menu_back"))
        finish()
    }
}
