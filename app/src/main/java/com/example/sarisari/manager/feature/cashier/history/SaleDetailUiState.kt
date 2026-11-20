package com.example.sarisari.manager.feature.cashier.history

import com.example.sarisari.manager.domain.model.Sale

data class SaleDetailUiState(
    val isLoading: Boolean = true,
    val sale: Sale? = null,
    val isVoiding: Boolean = false,
    val voidFailed: Boolean = false,
)
