package com.example.sarisari.manager.feature.utang

import androidx.annotation.StringRes
import com.example.sarisari.manager.domain.model.CreditEntry
import com.example.sarisari.manager.domain.model.Customer

/** The list of customers with their balances. */
data class UtangUiState(
    val isLoading: Boolean = true,
    val query: String = "",
    val customers: List<Customer> = emptyList(),
    /** What all customers owe right now. */
    val outstandingCentavos: Long = 0,
    /** Non-null while the "New customer" dialog is open. */
    val newCustomer: NameForm? = null,
)

/** Text typed into a customer-name dialog. */
data class NameForm(
    val name: String = "",
    @param:StringRes val errorRes: Int? = null,
    val isSaving: Boolean = false,
)

/** The form for recording a payment. Dates are UTC midnights, as the date picker uses them. */
data class PaymentForm(
    val amount: String = "",
    val note: String = "",
    val pickedDateUtcMillis: Long? = null,
    @param:StringRes val errorRes: Int? = null,
    val isSaving: Boolean = false,
)

/** One customer's balance and ledger. */
data class CustomerLedgerUiState(
    val isLoading: Boolean = true,
    /** Null after loading means the customer does not exist. */
    val customer: Customer? = null,
    val entries: List<CreditEntry> = emptyList(),
    val payment: PaymentForm? = null,
    val rename: NameForm? = null,
    /** The payment waiting for the owner to confirm its void. */
    val confirmVoidEntry: CreditEntry? = null,
    @param:StringRes val messageRes: Int? = null,
)
