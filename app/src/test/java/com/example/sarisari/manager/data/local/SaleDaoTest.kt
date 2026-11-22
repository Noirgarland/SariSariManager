package com.example.sarisari.manager.data.local

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.sarisari.manager.data.local.database.AppDatabase
import com.example.sarisari.manager.data.local.entity.ProductEntity
import com.example.sarisari.manager.data.repository.InventoryRepositoryImpl
import com.example.sarisari.manager.data.repository.SalesRepositoryImpl
import com.example.sarisari.manager.domain.model.ProductUnavailableException
import com.example.sarisari.manager.domain.model.SaleAlreadyVoidedException
import com.example.sarisari.manager.domain.model.SaleLineInput
import com.example.sarisari.manager.domain.model.SaleNotFoundException
import com.example.sarisari.manager.domain.model.StockMovementType
import com.example.sarisari.manager.domain.usecase.AdjustStockUseCase
import com.example.sarisari.manager.domain.usecase.AdjustmentKind
import com.example.sarisari.manager.domain.usecase.CheckoutError
import com.example.sarisari.manager.domain.usecase.CheckoutResult
import com.example.sarisari.manager.domain.usecase.CompleteSaleUseCase
import com.example.sarisari.manager.domain.usecase.ReceiveStockUseCase
import com.example.sarisari.manager.domain.usecase.VoidResult
import com.example.sarisari.manager.domain.usecase.VoidSaleUseCase
import com.example.sarisari.manager.testing.FakeClock
import com.example.sarisari.manager.testing.FakeSettingsRepository
import com.example.sarisari.manager.domain.model.QuantityLimits
import com.example.sarisari.manager.domain.model.StockShortage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.SQLiteMode

/** LEGACY SQLite mode: the native runtime loader fails when the user home path contains spaces. */
@RunWith(RobolectricTestRunner::class)
@SQLiteMode(SQLiteMode.Mode.LEGACY)
class SaleDaoTest {
    private lateinit var db: AppDatabase
    private val clock = FakeClock().also { it.setMillis(5_000) }
    private var coke = 0L
    private var chips = 0L

    private val sales get() = SalesRepositoryImpl(db.saleDao(), clock)
    private val inventory get() = InventoryRepositoryImpl(db.stockMovementDao(), clock)
    private val completeSale get() = CompleteSaleUseCase(sales)

    @Before
    fun setUp() = runTest {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).allowMainThreadQueries().build()
        coke = addProduct("Coke", price = 6500)
        chips = addProduct("Piattos", price = 2000)
        val receive = ReceiveStockUseCase(inventory, FakeSettingsRepository())
        receive(coke, unitFactor = 1, unitQuantity = 24, costPerUnitCentavos = 4500, note = null)
        receive(chips, unitFactor = 1, unitQuantity = 10, costPerUnitCentavos = 1200, note = null)
    }

    @After
    fun tearDown() = db.close()

    private suspend fun addProduct(name: String, price: Long) = db.productDao().save(
        ProductEntity(
            name = name, categoryId = null, baseUnitName = "piece", costCentavos = 0, sellingPriceCentavos = price,
            stockQuantity = 0, lowStockThreshold = 3, isArchived = false, createdAt = 0, updatedAt = 0,
        ),
        emptyList(), now = 1,
    )

    private suspend fun product(id: Long) = db.productDao().getWithUnits(id)!!.product

    private suspend fun assertCachedStockEqualsMovementSum(id: Long) =
        assertEquals(db.stockMovementDao().sumQuantity(id), product(id).stockQuantity)

    private fun line(productId: Long, qty: Int, price: Long, factor: Int = 1, unit: String = "piece") =
        SaleLineInput(productId, unit, factor, qty, price)

    @Test
    fun checkoutSavesSaleItemsSnapshotsAndDeductsStock() = runTest {
        val result = completeSale(listOf(line(coke, 2, 6500), line(chips, 3, 2000)), amountReceivedCentavos = 20_000)
        val sale = (result as CheckoutResult.Completed).sale
        assertEquals(19_000L, sale.totalCentavos)
        assertEquals(1_000L, sale.changeCentavos)

        val saved = sales.observeSale(sale.id).first()!!
        assertEquals(19_000L, saved.totalCentavos)
        assertEquals(2 * 4500L + 3 * 1200L, saved.totalCostCentavos)
        assertEquals(19_000L - 12_600L, saved.grossProfitCentavos)
        assertEquals(listOf("Coke", "Piattos"), saved.items.map { it.productName })
        assertEquals(4500L, saved.items[0].unitCostCentavos)
        assertEquals(9000L, saved.items[0].lineCostCentavos)
        assertEquals(22, product(coke).stockQuantity)
        assertEquals(7, product(chips).stockQuantity)
        assertCachedStockEqualsMovementSum(coke)
        assertCachedStockEqualsMovementSum(chips)
        val saleMovement = db.stockMovementDao().observe(coke, 10).first().first().movement
        assertEquals("SALE", saleMovement.type)
        assertEquals(-2, saleMovement.quantityChange)
        assertEquals("Sale #${sale.id}", saleMovement.reason)
    }

    @Test
    fun customPriceIsRecordedAsTheActualPrice() = runTest {
        val sale = (completeSale(listOf(line(coke, 1, 6000)), 6000) as CheckoutResult.Completed).sale
        val saved = sales.observeSale(sale.id).first()!!
        assertEquals(6000L, saved.items.single().unitPriceCentavos)
        assertEquals(6000L, saved.totalCentavos)
        assertEquals(6500L, db.productDao().getWithUnits(coke)!!.product.sellingPriceCentavos)
    }

    @Test
    fun packUnitsDeductBaseQuantityAndCostSnapshotUsesBaseUnits() = runTest {
        val sale = (completeSale(listOf(line(coke, 2, 36_000, factor = 6, unit = "pack")), 72_000) as CheckoutResult.Completed).sale
        val item = sales.observeSale(sale.id).first()!!.items.single()
        assertEquals(12, item.baseQuantity)
        assertEquals(12 * 4500L, item.lineCostCentavos)
        assertEquals(12, product(coke).stockQuantity)
    }

    @Test
    fun historicalSalesDoNotChangeWhenProductPriceOrCostChange() = runTest {
        val sale = (completeSale(listOf(line(coke, 1, 6500)), 6500) as CheckoutResult.Completed).sale
        val before = sales.observeSale(sale.id).first()!!
        ReceiveStockUseCase(inventory, FakeSettingsRepository(mapOf("costing_method" to "latest_purchase")))(coke, 1, 10, 9000, null)
        db.productDao().save(product(coke).copy(name = "Renamed", sellingPriceCentavos = 9999), emptyList(), now = 9)
        assertEquals(before, sales.observeSale(sale.id).first())
    }

    /** Puts [productId]'s stock at exactly [stock] through a recorded adjustment, keeping the invariant. */
    private suspend fun setStock(productId: Long, stock: Int) {
        val change = stock - product(productId).stockQuantity
        if (change != 0) inventory.adjustStock(productId, StockMovementType.ADJUSTMENT, change, "test setup")
        assertEquals(stock, product(productId).stockQuantity)
    }

    private suspend fun assertNothingSold(productId: Long, expectedStock: Int, movementsBefore: Int) {
        assertTrue(sales.observeSales().first().isEmpty())
        assertEquals(0, countRows("sale_items"))
        assertEquals(expectedStock, product(productId).stockQuantity)
        assertEquals(movementsBefore, db.stockMovementDao().observe(productId, 100).first().size)
        assertCachedStockEqualsMovementSum(productId)
    }

    private fun countRows(table: String): Int =
        db.openHelper.readableDatabase.query("SELECT COUNT(*) FROM $table").use { it.moveToFirst(); it.getInt(0) }

    @Test
    fun checkoutStockMatrix() = runTest {
        // available, requested, allowed
        val cases = listOf(Triple(10, 5, true), Triple(10, 10, true), Triple(10, 11, false), Triple(0, 1, false), Triple(1, 1, true))
        for ((available, requested, allowed) in cases) {
            setStock(chips, available)
            val result = completeSale(listOf(line(chips, requested, 2000)), requested * 2000L)
            val label = "available=$available requested=$requested"
            if (allowed) {
                assertTrue(label, result is CheckoutResult.Completed)
                assertEquals(label, available - requested, product(chips).stockQuantity)
            } else {
                val shortage = (result as CheckoutResult.InsufficientStock).shortages.single()
                assertEquals(label, StockShortage(chips, "Piattos", "piece", available, requested.toLong()), shortage)
                assertEquals(label, available, product(chips).stockQuantity)
            }
            assertTrue(label, product(chips).stockQuantity >= 0)
            assertCachedStockEqualsMovementSum(chips)
        }
    }

    @Test
    fun overSellingIsRejectedAndWritesNothing() = runTest {
        val movementsBefore = db.stockMovementDao().observe(chips, 100).first().size
        val result = completeSale(listOf(line(coke, 1, 6500), line(chips, 12, 2000)), 50_000)
        val shortage = (result as CheckoutResult.InsufficientStock).shortages.single()
        assertEquals(chips, shortage.productId)
        assertEquals(10, shortage.availableBaseQuantity)
        assertEquals(12L, shortage.requestedBaseQuantity)
        // The coke line was fine, but nothing from the sale may be written.
        assertNothingSold(chips, expectedStock = 10, movementsBefore = movementsBefore)
        assertEquals(24, product(coke).stockQuantity)
        assertEquals(1, db.stockMovementDao().observe(coke, 100).first().size)
    }

    @Test
    fun quantitiesOfTheSameProductAreSummedAcrossCartLines() = runTest {
        // 24 bottles in stock. 2 packs of 6 plus 13 bottles is 25 bottles: one more than available.
        val tooMuch = completeSale(listOf(line(coke, 2, 36_000, factor = 6, unit = "pack"), line(coke, 13, 6500)), 200_000)
        val shortage = (tooMuch as CheckoutResult.InsufficientStock).shortages.single()
        assertEquals(24, shortage.availableBaseQuantity)
        assertEquals(25L, shortage.requestedBaseQuantity)
        assertNothingSold(coke, expectedStock = 24, movementsBefore = 1)

        // 2 packs of 6 plus 12 bottles is exactly 24.
        val exact = completeSale(listOf(line(coke, 2, 36_000, factor = 6, unit = "pack"), line(coke, 12, 6500)), 200_000)
        assertTrue(exact is CheckoutResult.Completed)
        assertEquals(0, product(coke).stockQuantity)
        assertCachedStockEqualsMovementSum(coke)
    }

    @Test
    fun everyShortProductIsReported() = runTest {
        val result = completeSale(listOf(line(coke, 25, 6500), line(chips, 11, 2000)), 500_000)
        val shortages = (result as CheckoutResult.InsufficientStock).shortages
        assertEquals(listOf(coke to 25L, chips to 11L), shortages.map { it.productId to it.requestedBaseQuantity })
    }

    @Test
    fun archivedProductIsRejectedEvenWithStock() = runTest {
        db.productDao().setArchived(chips, true, 9)
        assertEquals(CheckoutResult.Rejected(CheckoutError.PRODUCT_UNAVAILABLE), completeSale(listOf(line(chips, 1, 2000)), 2000))
        assertNothingSold(chips, expectedStock = 10, movementsBefore = 1)
    }

    @Test
    fun concurrentCheckoutsCannotOversell() = runTest {
        // 10 in stock. Eight checkouts of 3 each race; at most three can succeed.
        val results = withContext(Dispatchers.IO) {
            (1..8).map { async { completeSale(listOf(line(chips, 3, 2000)), 6000) } }.awaitAll()
        }
        assertEquals(3, results.count { it is CheckoutResult.Completed })
        assertEquals(5, results.count { it is CheckoutResult.InsufficientStock })
        assertEquals(1, product(chips).stockQuantity)
        assertEquals(3, sales.observeSales().first().size)
        assertEquals(3, countRows("sale_items"))
        assertCachedStockEqualsMovementSum(chips)
    }

    @Test
    fun failureAfterTheFirstWriteRollsBackEverything() = runTest {
        // A line cost so large that multiplying it overflows. This fails after the sale row is inserted.
        db.openHelper.writableDatabase.execSQL("UPDATE products SET cost_centavos = ${Long.MAX_VALUE / 2} WHERE id = $chips")
        try {
            sales.completeSale(listOf(line(coke, 1, 6500), line(chips, 3, 2000)), 20_000)
            fail("Expected ArithmeticException")
        } catch (_: ArithmeticException) {
        }
        assertNothingSold(chips, expectedStock = 10, movementsBefore = 1)
        assertEquals(24, product(coke).stockQuantity)
        assertEquals(1, db.stockMovementDao().observe(coke, 100).first().size)
    }

    @Test
    fun saleItemsSnapshotTheBaseUnitAndSurviveProductEdits() = runTest {
        val sale = (completeSale(listOf(line(coke, 1, 36_000, factor = 6, unit = "pack")), 36_000) as CheckoutResult.Completed).sale
        val item = sales.observeSale(sale.id).first()!!.items.single()
        assertEquals("piece", item.baseUnitName)
        assertEquals("pack", item.unitName)
        assertEquals("Coke", item.productName)

        db.productDao().save(product(coke).copy(name = "Coca-Cola", baseUnitName = "bottle"), emptyList(), now = 9)
        val after = sales.observeSale(sale.id).first()!!.items.single()
        assertEquals(item, after)
    }

    @Test
    fun voidThatWouldExceedTheStockLimitIsRefusedAndWritesNothing() = runTest {
        val sale = (completeSale(listOf(line(chips, 5, 2000)), 10_000) as CheckoutResult.Completed).sale
        db.openHelper.writableDatabase.execSQL(
            "UPDATE products SET stock_quantity = ${QuantityLimits.MAX_STOCK_QUANTITY - 4} WHERE id = $chips"
        )
        assertEquals(VoidResult.STOCK_LIMIT_EXCEEDED, VoidSaleUseCase(sales)(sale.id, null))
        assertTrue(!sales.observeSale(sale.id).first()!!.isVoided)
        assertEquals(QuantityLimits.MAX_STOCK_QUANTITY - 4, product(chips).stockQuantity)
    }

    @Test
    fun unavailableProductRollsBackTheWholeSale() = runTest {
        db.productDao().setArchived(chips, true, 9)
        val result = completeSale(listOf(line(coke, 1, 6500), line(chips, 1, 2000)), 10_000)
        assertEquals(CheckoutResult.Rejected(CheckoutError.PRODUCT_UNAVAILABLE), result)
        assertTrue(sales.observeSales().first().isEmpty())
        assertEquals(24, product(coke).stockQuantity)
        assertEquals(1, db.stockMovementDao().observe(coke, 10).first().size)
        try {
            sales.completeSale(listOf(line(999, 1, 100)), 100)
            fail("Expected ProductUnavailableException")
        } catch (e: ProductUnavailableException) {
            assertEquals(999L, e.productId)
        }
    }

    @Test
    fun invalidCartsAreRejectedWithoutWriting() = runTest {
        assertEquals(CheckoutResult.Rejected(CheckoutError.EMPTY_CART), completeSale(emptyList(), 100))
        assertEquals(CheckoutResult.Rejected(CheckoutError.INVALID_QUANTITY), completeSale(listOf(line(coke, 0, 100)), 100))
        assertEquals(CheckoutResult.Rejected(CheckoutError.INVALID_PRICE), completeSale(listOf(line(coke, 1, 0)), 100))
        assertEquals(CheckoutResult.Rejected(CheckoutError.INSUFFICIENT_PAYMENT), completeSale(listOf(line(coke, 1, 6500)), 6499))
        assertTrue(sales.observeSales().first().isEmpty())
        assertEquals(24, product(coke).stockQuantity)
    }

    @Test
    fun voidKeepsTheSaleAndReturnsStockThroughCompensatingMovements() = runTest {
        val sale = (completeSale(listOf(line(coke, 5, 6500)), 32_500) as CheckoutResult.Completed).sale
        assertEquals(19, product(coke).stockQuantity)
        assertEquals(VoidResult.VOIDED, VoidSaleUseCase(sales)(sale.id, " customer returned "))
        val saved = sales.observeSale(sale.id).first()!!
        assertTrue(saved.isVoided)
        assertEquals("customer returned", saved.voidReason)
        assertEquals(32_500L, saved.totalCentavos)
        assertEquals(24, product(coke).stockQuantity)
        val types = db.stockMovementDao().observe(coke, 10).first().map { it.movement.type }
        assertEquals(listOf("SALE_VOID", "SALE", "RECEIPT"), types)
        assertCachedStockEqualsMovementSum(coke)
        assertEquals(VoidResult.ALREADY_VOIDED, VoidSaleUseCase(sales)(sale.id, null))
        assertEquals(24, product(coke).stockQuantity)
        assertEquals(VoidResult.NOT_FOUND, VoidSaleUseCase(sales)(12345, null))
    }

    @Test
    fun voidExceptionsAreTyped() = runTest {
        try {
            sales.voidSale(1, null)
            fail("Expected SaleNotFoundException")
        } catch (e: SaleNotFoundException) {
            assertNotNull(e)
        }
        val sale = (completeSale(listOf(line(coke, 1, 6500)), 6500) as CheckoutResult.Completed).sale
        sales.voidSale(sale.id, null)
        try {
            sales.voidSale(sale.id, null)
            fail("Expected SaleAlreadyVoidedException")
        } catch (e: SaleAlreadyVoidedException) {
            assertEquals(sale.id, e.saleId)
        }
    }

    @Test
    fun adjustmentsAfterSalesStillMatchTheMovementSum() = runTest {
        completeSale(listOf(line(coke, 4, 6500)), 26_000)
        AdjustStockUseCase(inventory)(coke, AdjustmentKind.DAMAGED, 2, null)
        assertEquals(18, product(coke).stockQuantity)
        assertCachedStockEqualsMovementSum(coke)
        assertEquals(StockMovementType.valueOf("SALE_VOID"), StockMovementType.SALE_VOID)
    }
}
