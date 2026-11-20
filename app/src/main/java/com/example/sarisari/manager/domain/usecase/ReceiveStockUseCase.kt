package com.example.sarisari.manager.domain.usecase

import com.example.sarisari.manager.domain.model.CostingMethod
import com.example.sarisari.manager.domain.repository.InventoryRepository
import com.example.sarisari.manager.domain.repository.SettingsRepository
import javax.inject.Inject

enum class StockEntryError { QUANTITY_INVALID, COST_INVALID, NOTE_REQUIRED, INSUFFICIENT_STOCK }

sealed interface StockEntryResult {
    data object Saved : StockEntryResult
    data class Invalid(val error: StockEntryError) : StockEntryResult
}

/**
 * Records incoming stock. The quantity and cost are given per selling unit (for example per box),
 * where one unit holds [unitFactor] base units. The product's new cost follows the owner's costing method.
 */
class ReceiveStockUseCase @Inject constructor(
    private val inventory: InventoryRepository,
    private val settings: SettingsRepository,
) {
    suspend operator fun invoke(
        productId: Long,
        unitFactor: Int,
        unitQuantity: Int,
        costPerUnitCentavos: Long,
        note: String?,
    ): StockEntryResult {
        if (unitFactor < 1 || unitQuantity < 1) return StockEntryResult.Invalid(StockEntryError.QUANTITY_INVALID)
        if (costPerUnitCentavos < 0) return StockEntryResult.Invalid(StockEntryError.COST_INVALID)
        val baseQuantity = unitQuantity.toLong() * unitFactor
        val totalCost = unitQuantity.toLong() * costPerUnitCentavos
        if (baseQuantity > MAX_BASE_QUANTITY) return StockEntryResult.Invalid(StockEntryError.QUANTITY_INVALID)
        if (totalCost > MAX_TOTAL_COST_CENTAVOS) return StockEntryResult.Invalid(StockEntryError.COST_INVALID)

        val method = CostingMethod.fromStorage(settings.getSetting(CostingMethod.SETTING_KEY)?.value)
        val strategy = method.strategy()
        inventory.receiveStock(
            productId = productId,
            baseQuantity = baseQuantity.toInt(),
            totalCostCentavos = totalCost,
            reason = note?.trim()?.ifEmpty { null },
        ) { currentQuantity, currentCost ->
            strategy.newUnitCost(currentQuantity, currentCost, baseQuantity.toInt(), totalCost)
        }
        return StockEntryResult.Saved
    }

    companion object {
        const val MAX_BASE_QUANTITY = 1_000_000L
        const val MAX_TOTAL_COST_CENTAVOS = 100_000_000_000L
    }
}
