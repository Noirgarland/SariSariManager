package com.example.sarisari.manager.feature.inventory

import com.example.sarisari.manager.domain.model.StockStatus

data class InventoryItem(
    val id: Long,
    val name: String,
    val stockQuantity: Int,
    val baseUnitName: String,
    val stockStatus: StockStatus,
)

data class InventoryUiState(
    val isLoading: Boolean = true,
    val query: String = "",
    val lowStockOnly: Boolean = false,
    val lowStockCount: Int = 0,
    val items: List<InventoryItem> = emptyList(),
)
