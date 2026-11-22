package com.example.sarisari.manager.feature.utang

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.example.sarisari.manager.R
import com.example.sarisari.manager.core.navigation.CustomerLedgerRoute
import com.example.sarisari.manager.core.util.Money
import com.example.sarisari.manager.domain.model.CreditEntry
import com.example.sarisari.manager.domain.model.CreditEntryType
import com.example.sarisari.manager.domain.model.StoreTime
import com.example.sarisari.manager.domain.repository.CustomerRepository
import com.example.sarisari.manager.domain.usecase.RecordUtangPaymentUseCase
import com.example.sarisari.manager.domain.usecase.SaveCustomerResult
import com.example.sarisari.manager.domain.usecase.SaveCustomerUseCase
import com.example.sarisari.manager.domain.usecase.UtangPaymentError
import com.example.sarisari.manager.domain.usecase.UtangPaymentResult
import com.example.sarisari.manager.domain.usecase.VoidUtangPaymentUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.Clock
import java.time.Instant
import java.time.LocalTime
import java.time.ZoneOffset
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class CustomerLedgerViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    customers: CustomerRepository,
    private val recordPayment: RecordUtangPaymentUseCase,
    private val voidPayment: VoidUtangPaymentUseCase,
    private val saveCustomer: SaveCustomerUseCase,
    private val clock: Clock,
) : ViewModel() {

    private val customerId: Long = savedStateHandle.toRoute<CustomerLedgerRoute>().customerId

    private data class Local(
        val payment: PaymentForm? = null,
        val rename: NameForm? = null,
        val confirmVoidEntry: CreditEntry? = null,
        val messageRes: Int? = null,
    )

    private val local = MutableStateFlow(Local())

    val uiState: StateFlow<CustomerLedgerUiState> = combine(
        customers.observeCustomer(customerId),
        customers.observeEntries(customerId),
        local,
    ) { customer, entries, l ->
        CustomerLedgerUiState(
            isLoading = false,
            customer = customer,
            entries = entries,
            payment = l.payment,
            rename = l.rename,
            confirmVoidEntry = l.confirmVoidEntry,
            messageRes = l.messageRes,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), CustomerLedgerUiState())

    // Recording a payment

    fun onRecordPaymentClicked() = local.update { it.copy(payment = PaymentForm(), messageRes = null) }

    /** Fills the amount with the whole balance. */
    fun onPayFullBalance() {
        val balance = uiState.value.customer?.balanceCentavos ?: return
        if (balance > 0) local.update { it.copy(payment = it.payment?.copy(amount = Money.toInputText(balance), errorRes = null)) }
    }

    fun onPaymentDismissed() = local.update { it.copy(payment = null) }
    fun onPaymentAmountChange(value: String) = local.update { it.copy(payment = it.payment?.copy(amount = value, errorRes = null)) }
    fun onPaymentNoteChange(value: String) = local.update { it.copy(payment = it.payment?.copy(note = value, errorRes = null)) }
    fun onPaymentDatePicked(utcMidnightMillis: Long?) =
        local.update { it.copy(payment = it.payment?.copy(pickedDateUtcMillis = utcMidnightMillis, errorRes = null)) }

    fun onSavePayment() {
        val form = local.value.payment ?: return
        if (form.isSaving) return
        local.update { it.copy(payment = form.copy(isSaving = true)) }
        viewModelScope.launch {
            val result = recordPayment(customerId, Money.parse(form.amount), paidAtMillis(form.pickedDateUtcMillis), form.note)
            local.update {
                when (result) {
                    UtangPaymentResult.Saved -> it.copy(payment = null, messageRes = R.string.utang_payment_saved)
                    is UtangPaymentResult.Invalid -> it.copy(payment = form.copy(isSaving = false, errorRes = result.error.messageRes()))
                }
            }
        }
    }

    // Voiding a payment

    fun onVoidPaymentClicked(entry: CreditEntry) {
        if (entry.type == CreditEntryType.PAYMENT && !entry.isVoided) local.update { it.copy(confirmVoidEntry = entry) }
    }

    fun onVoidDismissed() = local.update { it.copy(confirmVoidEntry = null) }

    fun onVoidConfirmed() {
        val entry = local.value.confirmVoidEntry ?: return
        viewModelScope.launch {
            val voided = voidPayment(entry.id)
            local.update {
                it.copy(confirmVoidEntry = null, messageRes = if (voided) R.string.utang_payment_voided else R.string.utang_void_failed)
            }
        }
    }

    // Renaming

    fun onRenameClicked() = local.update { it.copy(rename = NameForm(name = uiState.value.customer?.name.orEmpty())) }
    fun onRenameDismissed() = local.update { it.copy(rename = null) }
    fun onRenameChange(value: String) = local.update { it.copy(rename = it.rename?.copy(name = value, errorRes = null)) }

    fun onSaveRename() {
        val form = local.value.rename ?: return
        if (form.isSaving) return
        local.update { it.copy(rename = form.copy(isSaving = true)) }
        viewModelScope.launch {
            when (val result = saveCustomer(form.name, customerId)) {
                is SaveCustomerResult.Saved -> local.update { it.copy(rename = null) }
                is SaveCustomerResult.Invalid -> local.update {
                    it.copy(rename = form.copy(isSaving = false, errorRes = result.error.messageRes()))
                }
            }
        }
    }

    fun onMessageShown() = local.update { it.copy(messageRes = null) }

    /** The date picker works in UTC midnights. */
    fun todayUtcMidnightMillis(): Long =
        clock.instant().atZone(StoreTime.ZONE).toLocalDate().atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()

    /** Today means now. An earlier day is recorded at noon store time, so it stays inside that day. */
    private fun paidAtMillis(pickedUtcMidnight: Long?): Long {
        val today = clock.instant().atZone(StoreTime.ZONE).toLocalDate()
        val picked = pickedUtcMidnight?.let { Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate() } ?: today
        return if (picked == today) clock.millis() else picked.atTime(LocalTime.NOON).atZone(StoreTime.ZONE).toInstant().toEpochMilli()
    }

    private fun UtangPaymentError.messageRes(): Int = when (this) {
        UtangPaymentError.AMOUNT_INVALID -> R.string.utang_error_amount
        UtangPaymentError.MORE_THAN_BALANCE -> R.string.utang_error_more_than_balance
        UtangPaymentError.DATE_IN_FUTURE -> R.string.utang_error_future
        UtangPaymentError.CUSTOMER_NOT_FOUND -> R.string.utang_error_customer
    }

    private companion object {
        const val STOP_TIMEOUT_MILLIS = 5_000L
    }
}
