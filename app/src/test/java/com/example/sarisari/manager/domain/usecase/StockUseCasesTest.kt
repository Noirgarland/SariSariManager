package com.example.sarisari.manager.domain.usecase

import com.example.sarisari.manager.domain.model.CostingMethod
import com.example.sarisari.manager.domain.model.StockMovementType
import com.example.sarisari.manager.testing.FakeInventoryRepository
import com.example.sarisari.manager.testing.FakeSettingsRepository
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class StockUseCasesTest {

    private fun receive(
        inventory: FakeInventoryRepository,
        method: String? = null,
    ) = ReceiveStockUseCase(
        inventory,
        FakeSettingsRepository(method?.let { mapOf(CostingMethod.SETTING_KEY to it) } ?: emptyMap()),
    )

    @Test
    fun receiveConvertsUnitsAndUsesWeightedAverageByDefault() = runTest {
        val inventory = FakeInventoryRepository(mapOf(1L to 4), mapOf(1L to 4500L))
        // 1 box of 6 bottles at 30000 per box: 6 base units, total 30000
        val result = receive(inventory)(1, unitFactor = 6, unitQuantity = 1, costPerUnitCentavos = 30_000, note = " restock ")
        assertEquals(StockEntryResult.Saved, result)
        assertEquals(10, inventory.stock[1])
        assertEquals(4800L, inventory.cost[1]) // (4*4500 + 30000) / 10
        val movement = inventory.movements.single()
        assertEquals(StockMovementType.RECEIPT, movement.type)
        assertEquals(6, movement.quantityChange)
        assertEquals("restock", movement.reason)
    }

    @Test
    fun receiveUsesLatestPurchaseCostWhenSelected() = runTest {
        val inventory = FakeInventoryRepository(mapOf(1L to 4), mapOf(1L to 4500L))
        receive(inventory, "latest_purchase")(1, 1, 6, 5000, null)
        assertEquals(5000L, inventory.cost[1])
        assertEquals(10, inventory.stock[1])
        assertNull(inventory.movements.single().reason)
    }

    @Test
    fun receiveRejectsBadQuantityAndCostAndWritesNothing() = runTest {
        val inventory = FakeInventoryRepository()
        val useCase = receive(inventory)
        assertEquals(StockEntryResult.Invalid(StockEntryError.QUANTITY_INVALID), useCase(1, 1, 0, 100, null))
        assertEquals(StockEntryResult.Invalid(StockEntryError.QUANTITY_INVALID), useCase(1, 0, 1, 100, null))
        assertEquals(StockEntryResult.Invalid(StockEntryError.COST_INVALID), useCase(1, 1, 1, -1, null))
        assertEquals(StockEntryResult.Invalid(StockEntryError.QUANTITY_INVALID), useCase(1, 1000, 1001, 100, null))
        assertEquals(StockEntryResult.Invalid(StockEntryError.COST_INVALID), useCase(1, 1, 1000, 200_000_000L, null))
        assertTrue(inventory.movements.isEmpty())
    }

    @Test
    fun damagedAndExpiredReduceStock() = runTest {
        val inventory = FakeInventoryRepository(mapOf(1L to 10))
        val adjust = AdjustStockUseCase(inventory)
        assertEquals(StockEntryResult.Saved, adjust(1, AdjustmentKind.DAMAGED, 3, null))
        assertEquals(StockEntryResult.Saved, adjust(1, AdjustmentKind.EXPIRED, 2, "old batch"))
        assertEquals(5, inventory.stock[1])
        assertEquals(listOf(StockMovementType.DAMAGED, StockMovementType.EXPIRED), inventory.movements.map { it.type })
        assertEquals(listOf(-3, -2), inventory.movements.map { it.quantityChange })
    }

    @Test
    fun manualAdjustmentNeedsANoteAndCannotGoBelowZero() = runTest {
        val inventory = FakeInventoryRepository(mapOf(1L to 2))
        val adjust = AdjustStockUseCase(inventory)
        assertEquals(StockEntryResult.Invalid(StockEntryError.NOTE_REQUIRED), adjust(1, AdjustmentKind.MANUAL_INCREASE, 1, "  "))
        assertEquals(StockEntryResult.Invalid(StockEntryError.INSUFFICIENT_STOCK), adjust(1, AdjustmentKind.MANUAL_DECREASE, 3, "count"))
        assertEquals(StockEntryResult.Invalid(StockEntryError.INSUFFICIENT_STOCK), adjust(1, AdjustmentKind.DAMAGED, 3, null))
        assertEquals(StockEntryResult.Invalid(StockEntryError.QUANTITY_INVALID), adjust(1, AdjustmentKind.DAMAGED, 0, null))
        assertEquals(2, inventory.stock[1])
        assertTrue(inventory.movements.isEmpty())
        assertEquals(StockEntryResult.Saved, adjust(1, AdjustmentKind.MANUAL_INCREASE, 4, "found extra"))
        assertEquals(6, inventory.stock[1])
    }
}
