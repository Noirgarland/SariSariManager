package com.example.sarisari.manager.feature.utang

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.sarisari.manager.R
import com.example.sarisari.manager.domain.repository.CustomerRepository
import com.example.sarisari.manager.domain.usecase.CustomerError
import com.example.sarisari.manager.domain.usecase.SaveCustomerResult
import com.example.sarisari.manager.domain.usecase.SaveCustomerUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class UtangViewModel @Inject constructor(
    customers: CustomerRepository,
    private val saveCustomer: SaveCustomerUseCase,
) : ViewModel() {

    private val query = MutableStateFlow("")
    private val newCustomer = MutableStateFlow<NameForm?>(null)

    val uiState: StateFlow<UtangUiState> = combine(
        query,
        query.flatMapLatest { customers.observeCustomers(it) },
        customers.observeOutstandingTotal(),
        newCustomer,
    ) { q, list, outstanding, form ->
        UtangUiState(isLoading = false, query = q, customers = list, outstandingCentavos = outstanding, newCustomer = form)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), UtangUiState())

    fun onQueryChange(value: String) = query.update { value }

    fun onNewCustomerClicked() = newCustomer.update { NameForm() }

    fun onNewCustomerDismissed() = newCustomer.update { null }

    fun onNewCustomerNameChange(value: String) = newCustomer.update { it?.copy(name = value, errorRes = null) }

    /** Saves the new customer. [onSaved] gets the new id, so the screen can open their ledger. */
    fun onSaveNewCustomer(onSaved: (Long) -> Unit = {}) {
        val form = newCustomer.value ?: return
        if (form.isSaving) return
        newCustomer.update { it?.copy(isSaving = true) }
        viewModelScope.launch {
            when (val result = saveCustomer(form.name)) {
                is SaveCustomerResult.Saved -> {
                    newCustomer.update { null }
                    onSaved(result.customerId)
                }
                is SaveCustomerResult.Invalid -> newCustomer.update {
                    it?.copy(isSaving = false, errorRes = result.error.messageRes())
                }
            }
        }
    }

    private companion object {
        const val STOP_TIMEOUT_MILLIS = 5_000L
    }
}

internal fun CustomerError.messageRes(): Int = when (this) {
    CustomerError.NAME_INVALID -> R.string.utang_error_name
    CustomerError.NAME_TAKEN -> R.string.utang_error_name_taken
}
