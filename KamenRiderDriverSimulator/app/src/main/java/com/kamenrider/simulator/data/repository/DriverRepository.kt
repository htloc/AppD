package com.kamenrider.simulator.data.repository

import com.kamenrider.simulator.data.config.GameConfig
import com.kamenrider.simulator.data.model.Driver
import com.kamenrider.simulator.data.model.RiderItem
import com.kamenrider.simulator.data.model.TransformationForm
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Single source of truth for all game data.
 * Currently backed by in-memory [GameConfig]; swap with Room or remote source
 * without changing callers.
 */
@Singleton
class DriverRepository @Inject constructor() {

    fun getAllDrivers(): List<Driver> = GameConfig.drivers

    fun getDriverById(id: String): Driver? =
        GameConfig.drivers.find { it.id == id }

    fun getAllItems(): List<RiderItem> = GameConfig.items

    fun getItemById(id: String): RiderItem? =
        GameConfig.items.find { it.id == id }

    fun getItemsByDriver(driverId: String): List<RiderItem> {
        val driver = getDriverById(driverId) ?: return emptyList()
        val supportedFormIds = driver.supportedForms.toSet()
        return GameConfig.items.filter { it.linkedFormId in supportedFormIds }
    }

    fun getAllForms(): List<TransformationForm> = GameConfig.forms

    fun getFormById(id: String): TransformationForm? =
        GameConfig.forms.find { it.id == id }

    fun getFormsByDriver(driverId: String): List<TransformationForm> =
        GameConfig.forms.filter { it.driverId == driverId }
}
