package com.example.sarisari.manager.feature.inventory

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.example.sarisari.manager.core.navigation.StockEntryRoute
import com.example.sarisari.manager.core.util.Money
import com.example.sarisari.manager.domain.repository.ProductRepository
import com.example.sarisari.manager.domain.usecase.AdjustStockUseCase
import com.example.sarisari.manager.domain.usecase.AdjustmentKind
import com.example.sarisari.manager.domain.usecase.ReceiveStockUseCase
import com.example.sarisari.manager.domain.usecase.StockEntryError
import com.example.sarisari.manager.domain.usecase.StockEntryResult
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class StockEntryViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val productRepository: ProductRepository,
    private val receiveStock: ReceiveStockUseCase,
    private val adjustStock: AdjustStockUseCase,
) : ViewModel() {

    private val productId: Long = savedStateHandle.toRoute<StockEntryRoute>().productId

    private val _uiState = MutableStateFlow(StockEntryUiState())
    val uiState: StateFlow<StockEntryUiState> = _uiState.asStateFlow()

    init {
        loadProduct()
    }

    private fun loadProduct() {
        viewModelScope.launch {
            val product = productRepository.getProduct(productId)
            if (product == null) {
                _uiState.update { it.copy(isLoading = false, isDone = true) }
                return@launch
            }
            _uiState.update {
                it.copy(
                    isLoading = false,
                    productName = product.name,
                    baseUnitName = product.baseUnitName,
                    currentStock = product.stockQuantity,
                    currentCostCentavos = product.costCentavos,
                    unitOptions = listOf(UnitOption(product.baseUnitName, 1)) +
                        product.units.map { unit -> UnitOption(unit.name, unit.baseQuantity) },
                )
            }
        }
    }

    fun onModeChange(mode: StockEntryMode) = edit { it.copy(mode = mode) }
    fun onUnitSelected(index: Int) = edit { it.copy(selectedUnitIndex = index) }
    fun onQuantityChange(value: String) = edit { it.copy(quantity = value) }
    fun onCostChange(value: String) = edit { it.copy(cost = value) }
    fun onKindChange(kind: AdjustmentKind) = edit { it.copy(kind = kind) }
    fun onNoteChange(value: String) = edit { it.copy(note = value) }

    private fun edit(change: (StockEntryUiState) -> StockEntryUiState) {
        _uiState.update { change(it).copy(errors = emptySet()) }
    }

    fun onSave() {
        val state = _uiState.value
        if (state.isSaving || state.isLoading) return
        val quantity = state.quantity.trim().toIntOrNull()
        if (quantity == null) {
            _uiState.update { it.copy(errors = setOf(StockEntryError.QUANTITY_INVALID)) }
            return
        }
        viewModelScope.launch {
            val result = when (state.mode) {
                StockEntryMode.RECEIVE -> {
                    val cost = Money.parse(state.cost)
                    if (cost == null) {
                        StockEntryResult.Invalid(StockEntryError.COST_INVALID)
                    } else {
                        _uiState.update { it.copy(isSaving = true) }
                        receiveStock(productId, state.selectedUnit?.factor ?: 1, quantity, cost, state.note)
                    }
                }
                StockEntryMode.ADJUST -> {
                    _uiState.update { it.copy(isSaving = true) }
                    adjustStock(productId, state.kind, quantity, state.note)
                }
            }
            _uiState.update {
                when (result) {
                    StockEntryResult.Saved -> it.copy(isSaving = false, isDone = true)
                    is StockEntryResult.Invalid -> it.copy(isSaving = false, errors = setOf(result.error))
                }
            }
        }
    }
}
