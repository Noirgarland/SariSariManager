package com.example.sarisari.manager.domain.usecase

import com.example.sarisari.manager.domain.model.InsufficientStockException
import com.example.sarisari.manager.domain.model.StockLimitExceededException
import com.example.sarisari.manager.domain.model.StockMovementType
import com.example.sarisari.manager.domain.repository.InventoryRepository
import javax.inject.Inject

enum class AdjustmentKind { DAMAGED, EXPIRED, MANUAL_INCREASE, MANUAL_DECREASE }

/** Records damaged or expired stock and manual corrections. Quantities are in the product's base unit. */
class AdjustStockUseCase @Inject constructor(
    private val inventory: InventoryRepository,
) {
    suspend operator fun invoke(
        productId: Long,
        kind: AdjustmentKind,
        quantity: Int,
        note: String?,
    ): StockEntryResult {
        if (quantity < 1 || quantity > ReceiveStockUseCase.MAX_BASE_QUANTITY) {
            return StockEntryResult.Invalid(StockEntryError.QUANTITY_INVALID)
        }
        val cleanNote = note?.trim()?.ifEmpty { null }
        val isManual = kind == AdjustmentKind.MANUAL_INCREASE || kind == AdjustmentKind.MANUAL_DECREASE
        if (isManual && cleanNote == null) return StockEntryResult.Invalid(StockEntryError.NOTE_REQUIRED)

        val (type, change) = when (kind) {
            AdjustmentKind.DAMAGED -> StockMovementType.DAMAGED to -quantity
            AdjustmentKind.EXPIRED -> StockMovementType.EXPIRED to -quantity
            AdjustmentKind.MANUAL_INCREASE -> StockMovementType.ADJUSTMENT to quantity
            AdjustmentKind.MANUAL_DECREASE -> StockMovementType.ADJUSTMENT to -quantity
        }
        return try {
            inventory.adjustStock(productId, type, change, cleanNote)
            StockEntryResult.Saved
        } catch (_: InsufficientStockException) {
            StockEntryResult.Invalid(StockEntryError.INSUFFICIENT_STOCK)
        } catch (_: StockLimitExceededException) {
            StockEntryResult.Invalid(StockEntryError.STOCK_LIMIT_EXCEEDED)
        }
    }
}
