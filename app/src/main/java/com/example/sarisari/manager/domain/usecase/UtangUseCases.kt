package com.example.sarisari.manager.domain.usecase

import com.example.sarisari.manager.domain.model.CreditEntryNotVoidableException
import com.example.sarisari.manager.domain.model.CustomerNotFoundException
import com.example.sarisari.manager.domain.model.DuplicateNameException
import com.example.sarisari.manager.domain.model.PaymentExceedsBalanceException
import com.example.sarisari.manager.domain.repository.CustomerRepository
import java.time.Clock
import javax.inject.Inject

enum class CustomerError { NAME_INVALID, NAME_TAKEN }

sealed interface SaveCustomerResult {
    data class Saved(val customerId: Long) : SaveCustomerResult
    data class Invalid(val error: CustomerError) : SaveCustomerResult
}

/** Adds a customer, or renames one when [customerId] is given. Names are trimmed and unique, ignoring case. */
class SaveCustomerUseCase @Inject constructor(
    private val customers: CustomerRepository,
) {
    suspend operator fun invoke(name: String, customerId: Long? = null): SaveCustomerResult {
        val cleaned = name.trim().replace(Regex("\\s+"), " ")
        if (cleaned.isEmpty() || cleaned.length > MAX_NAME_LENGTH) return SaveCustomerResult.Invalid(CustomerError.NAME_INVALID)
        return try {
            if (customerId == null) {
                SaveCustomerResult.Saved(customers.addCustomer(cleaned))
            } else {
                customers.renameCustomer(customerId, cleaned)
                SaveCustomerResult.Saved(customerId)
            }
        } catch (_: DuplicateNameException) {
            SaveCustomerResult.Invalid(CustomerError.NAME_TAKEN)
        }
    }

    companion object {
        const val MAX_NAME_LENGTH = 60
    }
}

enum class UtangPaymentError { AMOUNT_INVALID, MORE_THAN_BALANCE, DATE_IN_FUTURE, CUSTOMER_NOT_FOUND }

sealed interface UtangPaymentResult {
    data object Saved : UtangPaymentResult
    data class Invalid(val error: UtangPaymentError) : UtangPaymentResult
}

/**
 * Records money paid towards a customer's utang. Any amount up to the balance is accepted,
 * so a partial payment leaves the rest owing. A payment larger than the balance is refused.
 */
class RecordUtangPaymentUseCase @Inject constructor(
    private val customers: CustomerRepository,
    private val clock: Clock,
) {
    suspend operator fun invoke(customerId: Long, amountCentavos: Long?, paidAtMillis: Long, note: String?): UtangPaymentResult {
        if (amountCentavos == null || amountCentavos <= 0 || amountCentavos > MAX_PAYMENT_CENTAVOS) {
            return UtangPaymentResult.Invalid(UtangPaymentError.AMOUNT_INVALID)
        }
        if (paidAtMillis > clock.millis()) return UtangPaymentResult.Invalid(UtangPaymentError.DATE_IN_FUTURE)
        return try {
            customers.recordPayment(customerId, amountCentavos, paidAtMillis, note?.trim()?.ifEmpty { null })
            UtangPaymentResult.Saved
        } catch (_: PaymentExceedsBalanceException) {
            UtangPaymentResult.Invalid(UtangPaymentError.MORE_THAN_BALANCE)
        } catch (_: CustomerNotFoundException) {
            UtangPaymentResult.Invalid(UtangPaymentError.CUSTOMER_NOT_FOUND)
        }
    }

    companion object {
        const val MAX_PAYMENT_CENTAVOS = 100_000_000_000L
    }
}

/** Voids a payment recorded by mistake. The payment stays in the ledger, marked voided, and its amount is owed again. */
class VoidUtangPaymentUseCase @Inject constructor(
    private val customers: CustomerRepository,
) {
    /** Returns false if the entry is not a payment or is already voided. */
    suspend operator fun invoke(entryId: Long): Boolean = try {
        customers.voidPayment(entryId)
        true
    } catch (_: CreditEntryNotVoidableException) {
        false
    }
}
