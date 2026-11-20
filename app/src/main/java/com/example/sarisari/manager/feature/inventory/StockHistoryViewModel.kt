package com.example.sarisari.manager.feature.inventory

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.example.sarisari.manager.core.navigation.StockHistoryRoute
import com.example.sarisari.manager.domain.repository.InventoryRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

@HiltViewModel
class StockHistoryViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    inventoryRepository: InventoryRepository,
) : ViewModel() {

    private val productId: Long? = savedStateHandle.toRoute<StockHistoryRoute>().productId

    val uiState: StateFlow<StockHistoryUiState> = inventoryRepository.observeMovements(productId)
        .map { StockHistoryUiState(isLoading = false, movements = it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), StockHistoryUiState())

    private companion object {
        const val STOP_TIMEOUT_MILLIS = 5_000L
    }
}
