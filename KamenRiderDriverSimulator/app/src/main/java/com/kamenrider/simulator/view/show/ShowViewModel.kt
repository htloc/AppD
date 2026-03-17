package com.kamenrider.simulator.view.show

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kamenrider.simulator.common.event.GameAction
import com.kamenrider.simulator.common.manager.ActionManager
import com.kamenrider.simulator.common.manager.TransformationManager
import com.kamenrider.simulator.common.manager.TransformationState
import com.kamenrider.simulator.common.manager.installShowScreenMappings
import com.kamenrider.simulator.data.model.Driver
import com.kamenrider.simulator.data.model.RiderItem
import com.kamenrider.simulator.data.model.TransformationForm
import com.kamenrider.simulator.data.repository.DriverRepository
import com.kamenrider.simulator.object3d.ModelLoader
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ShowUiState – everything the Show screen needs to render.
 */
data class ShowUiState(
    val driver: Driver? = null,
    val availableItems: List<RiderItem> = emptyList(),
    val insertedItemIds: List<String?> = emptyList(),   // null = empty slot
    val currentForm: TransformationForm? = null,
    val snackbarMessage: String? = null,
    val isLoading: Boolean = false
)

@HiltViewModel
class ShowViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repository: DriverRepository,
    private val actionManager: ActionManager,
    private val transformationManager: TransformationManager,
    private val modelLoader: ModelLoader
) : ViewModel() {

    private val driverId: String = checkNotNull(savedStateHandle["driverId"]) {
        "ShowScreen requires a driverId argument"
    }

    private val _uiState = MutableStateFlow(ShowUiState())
    val uiState: StateFlow<ShowUiState> = _uiState.asStateFlow()

    /**
     * Expose transformation state directly from the manager.
     */
    val transformationState: StateFlow<TransformationState> = transformationManager.state
        .stateIn(viewModelScope, SharingStarted.Eagerly, TransformationState.Idle)

    init {
        loadDriver()
        observeActionFlow()
        installBeltMappings()
        preloadAssets()
    }

    // ------------------------------------------------------------------
    // Setup
    // ------------------------------------------------------------------

    private fun loadDriver() {
        val driver = repository.getDriverById(driverId) ?: return
        val items  = repository.getItemsByDriver(driverId)
        _uiState.update {
            it.copy(
                driver = driver,
                availableItems = items,
                insertedItemIds = MutableList(driver.insertSlots) { null }
            )
        }
        // Reset transformation state
        transformationManager.clearAllItems()
    }

    private fun observeActionFlow() {
        viewModelScope.launch {
            actionManager.actionFlow.collect { action ->
                handleAction(action)
            }
        }
    }

    private fun installBeltMappings() {
        actionManager.installShowScreenMappings(
            currentFormId = ::resolveCurrentFormId,
            insertedItemIds = { transformationManager.getInsertedItemIds() }
        )
    }

    private fun preloadAssets() {
        // Model preloading is best-effort; it will silently skip
        // missing .sfb/.glb assets (placeholders not present).
        viewModelScope.launch {
            try {
                val forms = repository.getFormsByDriver(driverId)
                val modelIds = forms.map { it.modelAsset
                    .removeSuffix(".sfb")
                    .removeSuffix(".glb")
                    .removePrefix("object3d/") }
                modelLoader.preload(modelIds)
            } catch (_: Exception) { /* no 3D models – ignore */ }
        }
    }

    // ------------------------------------------------------------------
    // Public – called from UI
    // ------------------------------------------------------------------

    fun onItemInsertedToSlot(item: RiderItem, slotIndex: Int) {
        transformationManager.insertItem(item.id, slotIndex)
        _uiState.update { state ->
            val newSlots = state.insertedItemIds.toMutableList()
            if (slotIndex < newSlots.size) newSlots[slotIndex] = item.id
            state.copy(insertedItemIds = newSlots)
        }
        actionManager.dispatchAction(GameAction.PlaySound("gashat_insert"))
    }

    fun onItemRemovedFromSlot(slotIndex: Int) {
        transformationManager.removeItem(slotIndex)
        _uiState.update { state ->
            val newSlots = state.insertedItemIds.toMutableList()
            if (slotIndex < newSlots.size) newSlots[slotIndex] = null
            state.copy(insertedItemIds = newSlots, currentForm = null)
        }
    }

    /** Reset all slots, clear form and transformation state. */
    fun onResetAll() {
        transformationManager.clearAllItems()
        _uiState.update { state ->
            state.copy(
                insertedItemIds = MutableList(state.insertedItemIds.size) { null },
                currentForm = null
            )
        }
    }

    fun onHenshinPressed() {
        val formId = resolveCurrentFormId()
        if (formId != null) {
            actionManager.dispatchAction(GameAction.TriggerTransformation(formId))
        } else {
            showSnackbar("Insert a Gashat first!")
        }
    }

    fun onSnackbarDismissed() {
        _uiState.update { it.copy(snackbarMessage = null) }
    }

    // ------------------------------------------------------------------
    // Action handler
    // ------------------------------------------------------------------

    private fun handleAction(action: GameAction) {
        when (action) {
            is GameAction.TriggerTransformation -> {
                val form = repository.getFormById(action.formId) ?: return
                _uiState.update { it.copy(currentForm = form) }
                transformationManager.triggerTransformation(action.formId)
            }
            is GameAction.InsertItem -> {
                val item = repository.getItemById(action.itemId) ?: return
                onItemInsertedToSlot(item, action.slotIndex)
            }
            is GameAction.RemoveItem -> {
                onItemRemovedFromSlot(action.slotIndex)
            }
            is GameAction.ShowMessage -> {
                showSnackbar(action.message)
            }
            else -> { /* Other actions handled by SoundManager / AnimationManager */ }
        }
    }

    // ------------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------------

    private fun resolveCurrentFormId(): String? {
        val insertedIds = transformationManager.getInsertedItemIds()
        if (insertedIds.isEmpty()) return null
        return repository.getFormsByDriver(driverId)
            .firstOrNull { form ->
                form.requiredItemIds.all { it in insertedIds }
            }
            ?.id
    }

    private fun showSnackbar(message: String) {
        _uiState.update { it.copy(snackbarMessage = message) }
    }
}
