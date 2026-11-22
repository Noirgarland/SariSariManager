package com.example.sarisari.manager.domain.model

/** A customer who can buy on utang. [balanceCentavos] is what they owe now. */
data class Customer(
    val id: Long,
    val name: String,
    val balanceCentavos: Long,
    /** When they last bought on utang, or null if never. */
    val lastCreditAtMillis: Long?,
    /** When they last paid, or null if never. */
    val lastPaymentAtMillis: Long?,
)

enum class CreditEntryType {
    /** A sale on utang. Adds to the balance. */
    CREDIT_SALE,

    /** Money paid towards the balance. Reduces it. */
    PAYMENT,

    /** Reverses a voided utang sale. Reduces the balance. */
    SALE_VOID,

    /** Reverses a payment recorded by mistake. Adds back to the balance. */
    PAYMENT_VOID,
}

/** One line in a customer's ledger. [amountCentavos] is positive when the balance goes up. */
data class CreditEntry(
    val id: Long,
    val customerId: Long,
    val type: CreditEntryType,
    val amountCentavos: Long,
    val saleId: Long?,
    val note: String?,
    val occurredAtMillis: Long,
    val voidedAtMillis: Long?,
) {
    val isVoided: Boolean get() = voidedAtMillis != null
}

/** Utang totals for a report period. */
data class CreditSummary(
    /** Everything owed by all customers right now, regardless of period. */
    val outstandingCentavos: Long = 0,
    /** Payments received in the period, not counting voided ones. */
    val collectedCentavos: Long = 0,
    /** Customers who owe something, highest balance first. */
    val topDebtors: List<Customer> = emptyList(),
)

class CustomerNotFoundException(val customerId: Long) : Exception("Customer $customerId does not exist")

/** A payment larger than the customer's balance. Nothing was saved. */
class PaymentExceedsBalanceException(val balanceCentavos: Long, val amountCentavos: Long) :
    Exception("Payment $amountCentavos is more than the balance $balanceCentavos")

/** The entry is not a payment, or it is already voided. */
class CreditEntryNotVoidableException(val entryId: Long) : Exception("Entry $entryId cannot be voided")
