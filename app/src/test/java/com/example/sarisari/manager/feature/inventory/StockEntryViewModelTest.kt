package com.example.sarisari.manager.feature.inventory

import androidx.lifecycle.SavedStateHandle
import com.example.sarisari.manager.domain.model.Product
import com.example.sarisari.manager.domain.model.ProductUnit
import com.example.sarisari.manager.domain.model.StockMovementType
import com.example.sarisari.manager.domain.usecase.AdjustStockUseCase
import com.example.sarisari.manager.domain.usecase.AdjustmentKind
import com.example.sarisari.manager.domain.usecase.ReceiveStockUseCase
import com.example.sarisari.manager.domain.usecase.StockEntryError
import com.example.sarisari.manager.testing.FakeInventoryRepository
import com.example.sarisari.manager.testing.FakeProductRepository
import com.example.sarisari.manager.testing.FakeSettingsRepository
import com.example.sarisari.manager.testing.MainDispatcherRule
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.SQLiteMode

/** Robolectric is needed because SavedStateHandle.toRoute reads an Android Bundle. */
@RunWith(RobolectricTestRunner::class)
@SQLiteMode(SQLiteMode.Mode.LEGACY)
class StockEntryViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val products = FakeProductRepository(
        listOf(
            Product(
                id = 1, name = "Coke", categoryId = null, baseUnitName = "bottle", costCentavos = 4500,
                sellingPriceCentavos = 6500, stockQuantity = 4, lowStockThreshold = 5,
                units = listOf(ProductUnit("case", 12, 70_000)),
            )
        )
    )
    private val inventory = FakeInventoryRepository(mapOf(1L to 4), mapOf(1L to 4500L))

    private fun viewModel(productId: Long = 1) = StockEntryViewModel(
        SavedStateHandle(mapOf("productId" to productId)),
        products,
        ReceiveStockUseCase(inventory, FakeSettingsRepository()),
        AdjustStockUseCase(inventory),
    )

    @Test
    fun loadsProductAndUnitOptions() {
        val state = viewModel().uiState.value
        assertFalse(state.isLoading)
        assertEquals("Coke", state.productName)
        assertEquals(4, state.currentStock)
        assertEquals(listOf(UnitOption("bottle", 1), UnitOption("case", 12)), state.unitOptions)
    }

    @Test
    fun missingProductClosesTheScreen() {
        assertTrue(viewModel(productId = 99).uiState.value.isDone)
    }

    @Test
    fun receivingInACaseAddsTwelveBaseUnitsAndUpdatesCost() {
        val vm = viewModel()
        vm.onUnitSelected(1)
        vm.onQuantityChange("1")
        vm.onCostChange("500")
        vm.onSave()
        assertTrue(vm.uiState.value.isDone)
        assertEquals(16, inventory.stock[1])
        // (4 * 4500 + 50000) / 16 = 4250
        assertEquals(4250L, inventory.cost[1])
    }

    @Test
    fun invalidInputShowsErrorsAndWritesNothing() {
        val vm = viewModel()
        vm.onSave()
        assertEquals(setOf(StockEntryError.QUANTITY_INVALID), vm.uiState.value.errors)
        vm.onQuantityChange("2")
        vm.onCostChange("abc")
        vm.onSave()
        assertEquals(setOf(StockEntryError.COST_INVALID), vm.uiState.value.errors)
        assertTrue(inventory.movements.isEmpty())
        vm.onCostChange("10")
        assertTrue(vm.uiState.value.errors.isEmpty())
    }

    @Test
    fun damagedAdjustmentReducesStockAndTooMuchIsRejected() {
        val vm = viewModel()
        vm.onModeChange(StockEntryMode.ADJUST)
        vm.onKindChange(AdjustmentKind.DAMAGED)
        vm.onQuantityChange("9")
        vm.onSave()
        assertEquals(setOf(StockEntryError.INSUFFICIENT_STOCK), vm.uiState.value.errors)
        vm.onQuantityChange("3")
        vm.onSave()
        assertTrue(vm.uiState.value.isDone)
        assertEquals(1, inventory.stock[1])
        assertEquals(StockMovementType.DAMAGED, inventory.movements.single().type)
    }

    @Test
    fun manualCorrectionRequiresAReason() {
        val vm = viewModel()
        vm.onModeChange(StockEntryMode.ADJUST)
        vm.onKindChange(AdjustmentKind.MANUAL_INCREASE)
        vm.onQuantityChange("2")
        vm.onSave()
        assertEquals(setOf(StockEntryError.NOTE_REQUIRED), vm.uiState.value.errors)
        vm.onNoteChange("found extra")
        vm.onSave()
        assertEquals(6, inventory.stock[1])
    }
}
