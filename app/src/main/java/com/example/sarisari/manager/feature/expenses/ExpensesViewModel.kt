package com.example.sarisari.manager.feature.expenses

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.sarisari.manager.core.util.Money
import com.example.sarisari.manager.domain.model.ExpenseCategory
import com.example.sarisari.manager.domain.model.StoreTime
import com.example.sarisari.manager.domain.repository.ExpenseRepository
import com.example.sarisari.manager.domain.usecase.AddExpenseResult
import com.example.sarisari.manager.domain.usecase.AddExpenseUseCase
import com.example.sarisari.manager.domain.usecase.ReportPeriod
import com.example.sarisari.manager.domain.usecase.isAfterNow
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.Clock
import java.time.Instant
import java.time.LocalTime
import java.time.ZoneOffset
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
class ExpensesViewModel @Inject constructor(
    private val expenses: ExpenseRepository,
    private val addExpense: AddExpenseUseCase,
    private val clock: Clock,
) : ViewModel() {

    private val period = MutableStateFlow(ReportPeriod.thisMonth(clock))
    private val form = MutableStateFlow<ExpenseForm?>(null)

    val uiState: StateFlow<ExpensesUiState> = combine(
        period,
        period.flatMapLatest { expenses.observeExpenses(it.startMillis, it.endMillis) },
        form,
    ) { p, list, f ->
        ExpensesUiState(
            isLoading = false,
            period = p,
            canGoNext = !p.next().isAfterNow(clock),
            expenses = list,
            form = f,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), ExpensesUiState())

    fun onPrevious() = period.update { it.previous() }
    fun onNext() = period.update { current -> current.next().takeUnless { it.isAfterNow(clock) } ?: current }

    fun onAddClicked() = form.update { ExpenseForm() }
    fun onFormDismissed() = form.update { null }
    fun onCategoryChange(value: ExpenseCategory) = form.update { it?.copy(category = value, error = null) }
    fun onAmountChange(value: String) = form.update { it?.copy(amount = value, error = null) }
    fun onDescriptionChange(value: String) = form.update { it?.copy(description = value, error = null) }
    fun onDatePicked(utcMidnightMillis: Long?) = form.update { it?.copy(pickedDateUtcMillis = utcMidnightMillis, error = null) }

    fun onSave() {
        val current = form.value ?: return
        if (current.isSaving) return
        form.update { it?.copy(isSaving = true) }
        viewModelScope.launch {
            val result = addExpense(
                current.category,
                current.description,
                Money.parse(current.amount),
                spentAtMillis(current.pickedDateUtcMillis),
            )
            form.update {
                when (result) {
                    AddExpenseResult.Saved -> null
                    is AddExpenseResult.Invalid -> it?.copy(isSaving = false, error = result.error)
                }
            }
        }
    }

    fun onDelete(id: Long) {
        viewModelScope.launch { expenses.deleteExpense(id) }
    }

    /** Today means now. An earlier day is recorded at noon store time on that day. */
    private fun spentAtMillis(pickedUtcMidnight: Long?): Long {
        val today = clock.instant().atZone(StoreTime.ZONE).toLocalDate()
        val picked = pickedUtcMidnight?.let { Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate() } ?: today
        return if (picked == today) clock.millis() else picked.atTime(LocalTime.NOON).atZone(StoreTime.ZONE).toInstant().toEpochMilli()
    }

    /** The date picker works in UTC midnights. */
    fun todayUtcMidnightMillis(): Long =
        clock.instant().atZone(StoreTime.ZONE).toLocalDate().atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()

    private companion object {
        const val STOP_TIMEOUT_MILLIS = 5_000L
    }
}
