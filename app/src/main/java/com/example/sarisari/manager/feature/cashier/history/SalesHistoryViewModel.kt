package com.example.sarisari.manager.feature.cashier.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.sarisari.manager.domain.repository.SalesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

@HiltViewModel
class SalesHistoryViewModel @Inject constructor(
    salesRepository: SalesRepository,
) : ViewModel() {
    val uiState: StateFlow<SalesHistoryUiState> = salesRepository.observeSales()
        .map { SalesHistoryUiState(isLoading = false, sales = it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), SalesHistoryUiState())

    private companion object {
        const val STOP_TIMEOUT_MILLIS = 5_000L
    }
}
