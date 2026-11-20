package com.example.sarisari.manager.feature.inventory

import com.example.sarisari.manager.domain.model.StockMovement

data class StockHistoryUiState(
    val isLoading: Boolean = true,
    val movements: List<StockMovement> = emptyList(),
)
