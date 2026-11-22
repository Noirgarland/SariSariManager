package com.example.sarisari.manager.domain.usecase

import com.example.sarisari.manager.domain.model.SaleLineInput
import com.example.sarisari.manager.domain.model.SalePayment
import com.example.sarisari.manager.domain.model.creditFor
import com.example.sarisari.manager.testing.FakeCustomerRepository
import com.example.sarisari.manager.testing.FakeSalesRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class UtangUseCasesTest {
    private val customers = FakeCustomerRepository()
    private val save = SaveCustomerUseCase(customers)

    @Test
    fun customerNamesAreTrimmedAndMustBeUnique() = runTest {
        val id = (save("  Juan   Dela  Cruz ") as SaveCustomerResult.Saved).customerId
        assertEquals("Juan Dela Cruz", customers.observeCustomer(id).first()!!.name)
        assertEquals(SaveCustomerResult.Invalid(CustomerError.NAME_TAKEN), save("juan dela cruz"))
        assertEquals(SaveCustomerResult.Invalid(CustomerError.NAME_INVALID), save("   "))
        assertEquals(SaveCustomerResult.Invalid(CustomerError.NAME_INVALID), save("x".repeat(61)))
        assertTrue(save("x".repeat(60)) is SaveCustomerResult.Saved)
    }

    @Test
    fun renamingKeepsTheSameCustomer() = runTest {
        val juan = (save("Juan") as SaveCustomerResult.Saved).customerId
        save("Maria")
        assertEquals(SaveCustomerResult.Saved(juan), save("Juan Dela Cruz", juan))
        assertEquals("Juan Dela Cruz", customers.observeCustomer(juan).first()!!.name)
        assertEquals(SaveCustomerResult.Invalid(CustomerError.NAME_TAKEN), save("maria", juan))
    }

    @Test
    fun creditIsWhatTheCashDoesNotCover() {
        assertEquals(10_000L, creditFor(10_000, 0))
        assertEquals(4_000L, creditFor(10_000, 6_000))
        assertEquals(0L, creditFor(10_000, 10_000))
        assertEquals(0L, creditFor(10_000, 20_000))
    }

    @Test
    fun utangCheckoutPassesTheCustomerAndAcceptsAnyCash() = runTest {
        val sales = FakeSalesRepository()
        val checkout = CompleteSaleUseCase(sales)
        val lines = listOf(SaleLineInput(1, "piece", 1, 2, 5_000))
        val noCash = checkout(lines, SalePayment.Utang(7, 0)) as CheckoutResult.Completed
        assertEquals(10_000L, noCash.sale.creditCentavos)
        val partial = checkout(lines, SalePayment.Utang(7, 2_500)) as CheckoutResult.Completed
        assertEquals(7_500L, partial.sale.creditCentavos)
        assertEquals(listOf<Long?>(7, 7), sales.creditCustomers)
        // A cash sale never carries a customer.
        checkout(lines, SalePayment.Cash(10_000))
        assertEquals(null, sales.creditCustomers.last())
    }
}
