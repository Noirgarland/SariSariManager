package com.example.sarisari.manager.feature.products

import androidx.lifecycle.SavedStateHandle
import com.example.sarisari.manager.domain.model.Product
import com.example.sarisari.manager.domain.model.ProductUnit
import com.example.sarisari.manager.domain.model.StockStatus
import com.example.sarisari.manager.domain.usecase.ProductError
import com.example.sarisari.manager.domain.usecase.SaveProductUseCase
import com.example.sarisari.manager.testing.FakeCategoryRepository
import com.example.sarisari.manager.testing.FakeProductRepository
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
class ProductEditViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val products = FakeProductRepository(
        listOf(
            Product(
                id = 7, name = "Coke", categoryId = null, baseUnitName = "bottle", costCentavos = 4500,
                sellingPriceCentavos = 6500, stockQuantity = 12, lowStockThreshold = 5,
                units = listOf(ProductUnit("pack", 6, 36_000)),
            )
        )
    )

    private fun viewModel(productId: Long?) = ProductEditViewModel(
        SavedStateHandle(mapOf("productId" to productId)),
        products,
        FakeCategoryRepository(),
        SaveProductUseCase(products),
    )

    @Test
    fun newProductStartsEmptyWithDefaultThreshold() {
        val state = viewModel(null).uiState.value
        assertFalse(state.isLoading)
        assertFalse(state.isEditing)
        assertEquals("", state.name)
        assertEquals("5", state.threshold)
    }

    private fun fillValidNewProduct(vm: ProductEditViewModel) {
        vm.onNameChange("Sprite")
        vm.onBaseUnitChange("bottle")
        vm.onCostChange("40")
        vm.onPriceChange("60")
    }

    @Test
    fun newProductSavesTheStockTypedAsOpeningStock() {
        val vm = viewModel(null)
        fillValidNewProduct(vm)
        vm.onThresholdChange("10")
        vm.onAddUnit()
        vm.onUnitChange(0, UnitForm("case", "24", "1300"))
        vm.onOpeningStockChange("120")
        vm.onSave()
        assertTrue(vm.uiState.value.isDone)
        val saved = products.products.value.single { it.name == "Sprite" }
        assertEquals(120, saved.stockQuantity)
        assertEquals(10, saved.lowStockThreshold)
        // The pack size is not stock, and 120 bottles is well above the warning level.
        assertEquals(24, saved.units.single().baseQuantity)
        assertEquals(StockStatus.IN_STOCK, saved.stockStatus)
    }

    @Test
    fun blankOpeningStockMeansZeroAndBadValuesAreRejected() {
        val vm = viewModel(null)
        fillValidNewProduct(vm)
        vm.onOpeningStockChange("12.5")
        vm.onSave()
        assertEquals(setOf(ProductError.OPENING_STOCK_INVALID), vm.uiState.value.errors)
        vm.onOpeningStockChange("-3")
        vm.onSave()
        assertEquals(setOf(ProductError.OPENING_STOCK_INVALID), vm.uiState.value.errors)
        vm.onOpeningStockChange(" ")
        vm.onSave()
        assertTrue(vm.uiState.value.isDone)
        assertEquals(0, products.products.value.single { it.name == "Sprite" }.stockQuantity)
    }

    @Test
    fun editingShowsStockOnHandAndKeepsIt() {
        val vm = viewModel(7)
        assertEquals(12, vm.uiState.value.stockOnHand)
        vm.onOpeningStockChange("999") // ignored for an existing product
        vm.onSave()
        assertTrue(vm.uiState.value.isDone)
        assertEquals(12, products.products.value.single { it.id == 7L }.stockQuantity)
    }

    @Test
    fun editLoadsExistingProductIntoTheForm() {
        val state = viewModel(7).uiState.value
        assertTrue(state.isEditing)
        assertEquals("Coke", state.name)
        assertEquals("45.00", state.cost)
        assertEquals("65.00", state.price)
        assertEquals(listOf(UnitForm("pack", "6", "360.00")), state.units)
    }

    @Test
    fun saveWithInvalidInputShowsAllErrorsAndSavesNothing() {
        val vm = viewModel(null)
        vm.onCostChange("abc")
        vm.onUnitChange(0.also { vm.onAddUnit() }, UnitForm("pack", "1", ""))
        vm.onSave()
        val state = vm.uiState.value
        assertEquals(
            setOf(
                ProductError.NAME_REQUIRED, ProductError.BASE_UNIT_REQUIRED, ProductError.COST_INVALID,
                ProductError.PRICE_INVALID, ProductError.UNIT_QUANTITY_INVALID, ProductError.UNIT_PRICE_INVALID,
            ),
            state.errors,
        )
        assertFalse(state.isDone)
        assertEquals(1, products.products.value.size)
    }

    @Test
    fun saveNewProductConvertsPesosToCentavosAndFinishes() {
        val vm = viewModel(null)
        vm.onNameChange("  Royal 1.5L ")
        vm.onBaseUnitChange("bottle")
        vm.onCostChange("45.5")
        vm.onPriceChange("65")
        vm.onAddUnit()
        vm.onUnitChange(0, UnitForm("pack", "6", "1,000.25"))
        vm.onSave()
        assertTrue(vm.uiState.value.isDone)
        val saved = products.products.value.first { it.name == "Royal 1.5L" }
        assertEquals(4550L, saved.costCentavos)
        assertEquals(6500L, saved.sellingPriceCentavos)
        assertEquals(listOf(ProductUnit("pack", 6, 100_025)), saved.units)
    }

    @Test
    fun editingKeepsExistingStockQuantity() {
        val vm = viewModel(7)
        vm.onPriceChange("70")
        vm.onSave()
        val saved = products.products.value.first { it.id == 7L }
        assertEquals(7000L, saved.sellingPriceCentavos)
        assertEquals(12, saved.stockQuantity)
    }

    @Test
    fun archiveAndRestoreUpdateTheProduct() {
        val vm = viewModel(7)
        vm.onArchiveChange(true)
        assertTrue(products.products.value.first { it.id == 7L }.isArchived)
        assertTrue(vm.uiState.value.isDone)
    }
}
