package com.example.sarisari.manager.domain.usecase

import com.example.sarisari.manager.domain.model.Product
import com.example.sarisari.manager.domain.model.ProductUnit
import com.example.sarisari.manager.domain.repository.ProductRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SaveProductUseCaseTest {

    private class FakeProductRepository : ProductRepository {
        val saved = mutableListOf<Product>()
        override fun observeProducts(query: String, categoryId: Long?, includeArchived: Boolean): Flow<List<Product>> =
            flowOf(saved)
        override suspend fun getProduct(id: Long): Product? = saved.firstOrNull { it.id == id }
        override suspend fun saveProduct(product: Product): Long {
            saved += product
            return 42
        }
        override suspend fun setArchived(id: Long, archived: Boolean) = Unit
    }

    private val valid = Product(
        name = "Coke 1.5L", categoryId = null, baseUnitName = "bottle",
        costCentavos = 4500, sellingPriceCentavos = 6500, lowStockThreshold = 5,
    )

    private fun errorsOf(product: Product) = SaveProductUseCase.validate(product)

    @Test
    fun savesValidProductAndTrimsNames() = runTest {
        val repo = FakeProductRepository()
        val result = SaveProductUseCase(repo)(valid.copy(name = "  Coke 1.5L  ", baseUnitName = " bottle "))
        assertEquals(SaveProductResult.Saved(42), result)
        assertEquals("Coke 1.5L", repo.saved.single().name)
        assertEquals("bottle", repo.saved.single().baseUnitName)
    }

    @Test
    fun invalidProductIsNotSaved() = runTest {
        val repo = FakeProductRepository()
        val result = SaveProductUseCase(repo)(valid.copy(name = " "))
        assertEquals(SaveProductResult.Invalid(setOf(ProductError.NAME_REQUIRED)), result)
        assertTrue(repo.saved.isEmpty())
    }

    @Test
    fun validatesBasicFields() {
        assertEquals(emptySet<ProductError>(), errorsOf(valid))
        assertEquals(setOf(ProductError.BASE_UNIT_REQUIRED), errorsOf(valid.copy(baseUnitName = "")))
        assertEquals(setOf(ProductError.COST_INVALID), errorsOf(valid.copy(costCentavos = -1)))
        assertEquals(emptySet<ProductError>(), errorsOf(valid.copy(costCentavos = 0)))
        assertEquals(setOf(ProductError.PRICE_INVALID), errorsOf(valid.copy(sellingPriceCentavos = 0)))
        assertEquals(setOf(ProductError.THRESHOLD_INVALID), errorsOf(valid.copy(lowStockThreshold = -1)))
        assertEquals(setOf(ProductError.NAME_REQUIRED), errorsOf(valid.copy(name = "x".repeat(81))))
    }

    @Test
    fun validatesExtraUnits() {
        val pack = ProductUnit("pack", 6, 36_000)
        assertEquals(emptySet<ProductError>(), errorsOf(valid.copy(units = listOf(pack))))
        assertEquals(setOf(ProductError.UNIT_NAME_INVALID), errorsOf(valid.copy(units = listOf(pack.copy(name = "")))))
        assertEquals(setOf(ProductError.UNIT_QUANTITY_INVALID), errorsOf(valid.copy(units = listOf(pack.copy(baseQuantity = 1)))))
        assertEquals(setOf(ProductError.UNIT_PRICE_INVALID), errorsOf(valid.copy(units = listOf(pack.copy(sellingPriceCentavos = 0)))))
        assertEquals(
            setOf(ProductError.UNIT_NAME_DUPLICATE),
            errorsOf(valid.copy(units = listOf(pack, pack.copy(name = "PACK", baseQuantity = 12)))),
        )
        assertEquals(
            setOf(ProductError.UNIT_NAME_DUPLICATE),
            errorsOf(valid.copy(units = listOf(pack.copy(name = "Bottle")))),
        )
    }
}
