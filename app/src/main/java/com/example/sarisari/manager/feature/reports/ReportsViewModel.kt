package com.example.sarisari.manager.feature.reports

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

@HiltViewModel
class ReportsViewModel @Inject constructor() : ViewModel() {
    val uiState: StateFlow<ReportsUiState> = MutableStateFlow(ReportsUiState()).asStateFlow()
}
