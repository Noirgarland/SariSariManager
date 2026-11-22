package com.example.sarisari.manager.data.local

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.sarisari.manager.data.export.ExportRepositoryImpl
import com.example.sarisari.manager.data.local.database.AppDatabase
import com.example.sarisari.manager.data.local.entity.ProductEntity
import com.example.sarisari.manager.data.repository.CustomerRepositoryImpl
import com.example.sarisari.manager.data.repository.InventoryRepositoryImpl
import com.example.sarisari.manager.data.repository.ReportRepositoryImpl
import com.example.sarisari.manager.data.repository.SalesRepositoryImpl
import com.example.sarisari.manager.domain.model.CreditEntryType
import com.example.sarisari.manager.domain.model.SaleLineInput
import com.example.sarisari.manager.domain.model.SalePayment
import com.example.sarisari.manager.domain.usecase.CheckoutError
import com.example.sarisari.manager.domain.usecase.CheckoutResult
import com.example.sarisari.manager.domain.usecase.CompleteSaleUseCase
import com.example.sarisari.manager.domain.usecase.ReceiveStockUseCase
import com.example.sarisari.manager.domain.usecase.RecordUtangPaymentUseCase
import com.example.sarisari.manager.domain.usecase.UtangPaymentError
import com.example.sarisari.manager.domain.usecase.UtangPaymentResult
import com.example.sarisari.manager.domain.usecase.VoidResult
import com.example.sarisari.manager.domain.usecase.VoidSaleUseCase
import com.example.sarisari.manager.domain.usecase.VoidUtangPaymentUseCase
import com.example.sarisari.manager.testing.FakeClock
import com.example.sarisari.manager.testing.FakeSettingsRepository
import java.io.StringWriter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.SQLiteMode

/** LEGACY SQLite mode: the native runtime loader fails when the user home path contains spaces. */
@RunWith(RobolectricTestRunner::class)
@SQLiteMode(SQLiteMode.Mode.LEGACY)
class UtangDaoTest {
    private lateinit var db: AppDatabase
    private val clock = FakeClock().also { it.setMillis(100_000) }
    private var coke = 0L
    private var juan = 0L
    private val sales get() = SalesRepositoryImpl(db.saleDao(), clock)
    private val customers get() = CustomerRepositoryImpl(db.customerDao(), clock)
    private val completeSale get() = CompleteSaleUseCase(sales)
    private val pay get() = RecordUtangPaymentUseCase(customers, clock)

    @Before
    fun setUp() = runTest {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).allowMainThreadQueries().build()
        coke = db.productDao().save(
            ProductEntity(
                name = "Coke", categoryId = null, baseUnitName = "bottle", costCentavos = 0, sellingPriceCentavos = 6500,
                stockQuantity = 0, lowStockThreshold = 3, isArchived = false, createdAt = 0, updatedAt = 0,
            ),
            emptyList(), now = 1,
        )
        ReceiveStockUseCase(InventoryRepositoryImpl(db.stockMovementDao(), clock), FakeSettingsRepository())(coke, 1, 50, 4500, null)
        juan = customers.addCustomer("Juan Dela Cruz")
    }

    @After
    fun tearDown() = db.close()

    private fun coke(qty: Int) = listOf(SaleLineInput(coke, "bottle", 1, qty, 6500))
    private suspend fun balance(id: Long = juan) = customers.observeCustomer(id).first()!!.balanceCentavos
    private suspend fun utangSale(qty: Int, cash: Long, customer: Long = juan) =
        (completeSale(coke(qty), SalePayment.Utang(customer, cash)) as CheckoutResult.Completed).sale

    @Test
    fun utangSaleWithNoCashPutsTheWholeTotalOnTheBalance() = runTest {
        val sale = utangSale(qty = 2, cash = 0)
        assertEquals(13_000L, sale.creditCentavos)
        assertEquals("Juan Dela Cruz", sale.customerName)
        assertEquals(13_000L, balance())
        val saved = sales.observeSale(sale.id).first()!!
        assertEquals(juan, saved.customerId)
        assertEquals(13_000L, saved.creditCentavos)
        assertEquals(0L, saved.changeCentavos)
        val entry = customers.observeEntries(juan).first().single()
        assertEquals(CreditEntryType.CREDIT_SALE, entry.type)
        assertEquals(sale.id, entry.saleId)
        assertEquals(clock.millis(), entry.occurredAtMillis)
        // Stock is deducted like any sale.
        assertEquals(48, db.productDao().getWithUnits(coke)!!.product.stockQuantity)
    }

    @Test
    fun partialCashLeavesOnlyTheRestAsUtang() = runTest {
        val sale = utangSale(qty = 2, cash = 5_000)
        assertEquals(8_000L, sale.creditCentavos)
        assertEquals(8_000L, balance())
    }

    @Test
    fun cashCoveringTheTotalOnUtangIsAPlainCashSale() = runTest {
        val sale = utangSale(qty = 1, cash = 10_000)
        assertEquals(0L, sale.creditCentavos)
        assertEquals(3_500L, sale.changeCentavos)
        assertNull(sales.observeSale(sale.id).first()!!.customerId)
        assertEquals(0L, balance())
        assertTrue(customers.observeEntries(juan).first().isEmpty())
    }

    @Test
    fun unknownCustomerIsRejectedAndNothingIsWritten() = runTest {
        assertEquals(
            CheckoutResult.Rejected(CheckoutError.CUSTOMER_UNAVAILABLE),
            completeSale(coke(1), SalePayment.Utang(999, 0)),
        )
        assertTrue(sales.observeSales().first().isEmpty())
        assertEquals(50, db.productDao().getWithUnits(coke)!!.product.stockQuantity)
    }

    @Test
    fun cashSaleStillNeedsTheFullAmount() = runTest {
        assertEquals(CheckoutResult.Rejected(CheckoutError.INSUFFICIENT_PAYMENT), completeSale(coke(1), SalePayment.Cash(6_499)))
        assertEquals(CheckoutResult.Rejected(CheckoutError.INSUFFICIENT_PAYMENT), completeSale(coke(1), SalePayment.Utang(juan, -1)))
    }

    @Test
    fun voidingAnUtangSaleTakesItOffTheBalance() = runTest {
        val sale = utangSale(qty = 3, cash = 0)
        utangSale(qty = 1, cash = 0)
        assertEquals(26_000L, balance())
        assertEquals(VoidResult.VOIDED, VoidSaleUseCase(sales)(sale.id, null))
        assertEquals(6_500L, balance())
        val voidEntry = customers.observeEntries(juan).first().first()
        assertEquals(CreditEntryType.SALE_VOID, voidEntry.type)
        assertEquals(-19_500L, voidEntry.amountCentavos)
        assertEquals(sale.id, voidEntry.saleId)
    }

    @Test
    fun partialPaymentsAreRecordedAndOverpayingIsRefused() = runTest {
        utangSale(qty = 2, cash = 0) // owes 130.00
        clock.setMillis(200_000)
        assertEquals(UtangPaymentResult.Saved, pay(juan, 5_000, 150_000, " partial "))
        assertEquals(8_000L, balance())
        val customer = customers.observeCustomer(juan).first()!!
        assertEquals(150_000L, customer.lastPaymentAtMillis)
        assertEquals(100_000L, customer.lastCreditAtMillis)
        assertEquals("partial", customers.observeEntries(juan).first().first().note)

        assertEquals(UtangPaymentResult.Invalid(UtangPaymentError.MORE_THAN_BALANCE), pay(juan, 8_001, 200_000, null))
        assertEquals(UtangPaymentResult.Invalid(UtangPaymentError.AMOUNT_INVALID), pay(juan, 0, 200_000, null))
        assertEquals(UtangPaymentResult.Invalid(UtangPaymentError.AMOUNT_INVALID), pay(juan, null, 200_000, null))
        assertEquals(UtangPaymentResult.Invalid(UtangPaymentError.DATE_IN_FUTURE), pay(juan, 100, 200_001, null))
        assertEquals(UtangPaymentResult.Invalid(UtangPaymentError.CUSTOMER_NOT_FOUND), pay(999, 100, 200_000, null))
        assertEquals(8_000L, balance())

        assertEquals(UtangPaymentResult.Saved, pay(juan, 8_000, 200_000, null))
        assertEquals(0L, balance())
    }

    @Test
    fun voidingAPaymentPutsTheAmountBackOnce() = runTest {
        utangSale(qty = 2, cash = 0)
        pay(juan, 5_000, clock.millis(), null)
        val payment = customers.observeEntries(juan).first().first { it.type == CreditEntryType.PAYMENT }
        val voidPayment = VoidUtangPaymentUseCase(customers)
        assertTrue(voidPayment(payment.id))
        assertEquals(13_000L, balance())
        assertFalse(voidPayment(payment.id))
        assertEquals(13_000L, balance())
        val credit = customers.observeEntries(juan).first().first { it.type == CreditEntryType.CREDIT_SALE }
        assertFalse(voidPayment(credit.id))
        assertTrue(customers.observeEntries(juan).first().first { it.id == payment.id }.isVoided)
        assertNull(customers.observeCustomer(juan).first()!!.lastPaymentAtMillis)
    }

    @Test
    fun outstandingTotalAndCollectionsForReports() = runTest {
        val maria = customers.addCustomer("Maria")
        utangSale(qty = 2, cash = 0) // Juan owes 130.00
        utangSale(qty = 1, cash = 1_500, customer = maria) // Maria owes 50.00
        completeSale(coke(1), SalePayment.Cash(6_500))
        clock.setMillis(200_000)
        pay(maria, 5_000, 120_000, null)
        pay(juan, 3_000, 50_000, null)

        assertEquals(10_000L, customers.observeOutstandingTotal().first())
        assertEquals(5_000L, customers.observeCollected(100_000, 200_000).first())
        assertEquals(8_000L, customers.observeCollected(0, 200_000).first())

        val summary = ReportRepositoryImpl(db.reportDao()).observeSummary(0, Long.MAX_VALUE).first()
        assertEquals(26_000L, summary.revenueCentavos)
        assertEquals(18_000L, summary.creditSalesCentavos)
        assertEquals(8_000L, summary.cashSalesCentavos)
        assertEquals(3, summary.transactionCount)
        // Highest balance first, and Maria has paid in full.
        assertEquals(listOf("Juan Dela Cruz" to 10_000L, "Maria" to 0L), customers.observeCustomers().first().map { it.name to it.balanceCentavos })
    }

    @Test
    fun customerNamesAreUniqueIgnoringCase() = runTest {
        try {
            customers.addCustomer("juan dela cruz")
            throw AssertionError("Expected DuplicateNameException")
        } catch (_: com.example.sarisari.manager.domain.model.DuplicateNameException) {
        }
        assertEquals(listOf("Juan Dela Cruz"), customers.observeCustomers("dela").first().map { it.name })
        assertTrue(customers.observeCustomers("100%").first().isEmpty())
    }

    @Test
    fun utangCsvExportsBalancesAndHistory() = runTest {
        utangSale(qty = 2, cash = 0)
        pay(juan, 5_000, clock.millis(), null)
        customers.addCustomer("=Maria")
        val exporter = ExportRepositoryImpl(ApplicationProvider.getApplicationContext(), db.exportDao(), Dispatchers.Unconfined)

        val balances = StringWriter().also { assertEquals(2, exporter.writeUtangBalances(it)) }.toString()
            .removePrefix("\uFEFF").split("\r\n").filter { it.isNotEmpty() }
        assertEquals("Customer,Balance,Status,Last utang date,Last payment date", balances[0])
        assertEquals("Juan Dela Cruz,80.00,Unpaid,1970-01-01,1970-01-01", balances[1])
        assertEquals("'=Maria,0.00,Paid,,", balances[2])

        val history = StringWriter().also { assertEquals(2, exporter.writeUtangHistory(it)) }.toString()
            .removePrefix("\uFEFF").split("\r\n").filter { it.isNotEmpty() }
        assertTrue(history[1].contains(",Utang sale,130.00,130.00,1,"))
        assertTrue(history[2].contains(",Payment,-50.00,80.00,,"))
    }
}
