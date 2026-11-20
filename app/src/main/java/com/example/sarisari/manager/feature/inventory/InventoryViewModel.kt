package com.example.sarisari.manager.feature.inventory

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

@HiltViewModel
class InventoryViewModel @Inject constructor() : ViewModel() {
    val uiState: StateFlow<InventoryUiState> = MutableStateFlow(InventoryUiState()).asStateFlow()
}
