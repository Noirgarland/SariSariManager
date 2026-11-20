package com.example.sarisari.manager.feature.cashier

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

@HiltViewModel
class CashierViewModel @Inject constructor() : ViewModel() {
    val uiState: StateFlow<CashierUiState> = MutableStateFlow(CashierUiState()).asStateFlow()
}
