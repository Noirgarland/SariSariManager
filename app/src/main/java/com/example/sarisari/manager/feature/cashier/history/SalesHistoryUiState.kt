package com.example.sarisari.manager.feature.cashier.history

import com.example.sarisari.manager.domain.model.Sale

data class SalesHistoryUiState(
    val isLoading: Boolean = true,
    val sales: List<Sale> = emptyList(),
)
