package com.example.sarisari.manager.feature.inventory

import com.example.sarisari.manager.domain.usecase.AdjustmentKind
import com.example.sarisari.manager.domain.usecase.StockEntryError

enum class StockEntryMode { RECEIVE, ADJUST }

/** A unit stock can be received in. [factor] is how many base units it holds (1 for the base unit). */
data class UnitOption(val name: String, val factor: Int)

data class StockEntryUiState(
    val isLoading: Boolean = true,
    val productName: String = "",
    val baseUnitName: String = "",
    val currentStock: Int = 0,
    val currentCostCentavos: Long = 0,
    val unitOptions: List<UnitOption> = emptyList(),
    val mode: StockEntryMode = StockEntryMode.RECEIVE,
    val selectedUnitIndex: Int = 0,
    val quantity: String = "",
    val cost: String = "",
    val kind: AdjustmentKind = AdjustmentKind.DAMAGED,
    val note: String = "",
    val errors: Set<StockEntryError> = emptySet(),
    val isSaving: Boolean = false,
    val isDone: Boolean = false,
) {
    val selectedUnit: UnitOption? get() = unitOptions.getOrNull(selectedUnitIndex)
}
