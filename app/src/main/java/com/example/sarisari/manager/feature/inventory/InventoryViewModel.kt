package com.example.sarisari.manager.feature.inventory

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.sarisari.manager.domain.model.StockStatus
import com.example.sarisari.manager.domain.repository.ProductRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class InventoryViewModel @Inject constructor(
    productRepository: ProductRepository,
) : ViewModel() {

    private val query = MutableStateFlow("")
    private val lowStockOnly = MutableStateFlow(false)

    val uiState: StateFlow<InventoryUiState> = combine(
        query,
        lowStockOnly,
        query.flatMapLatest { productRepository.observeProducts(it, categoryId = null, includeArchived = false) },
    ) { query, lowOnly, products ->
        val low = products.filter { it.stockStatus != StockStatus.IN_STOCK }
        InventoryUiState(
            isLoading = false,
            query = query,
            lowStockOnly = lowOnly,
            lowStockCount = low.size,
            items = (if (lowOnly) low else products).map {
                InventoryItem(it.id, it.name, it.stockQuantity, it.baseUnitName, it.stockStatus)
            },
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), InventoryUiState())

    fun onQueryChange(value: String) {
        query.value = value
    }

    fun onLowStockOnlyChange(value: Boolean) {
        lowStockOnly.value = value
    }

    private companion object {
        const val STOP_TIMEOUT_MILLIS = 5_000L
    }
}
