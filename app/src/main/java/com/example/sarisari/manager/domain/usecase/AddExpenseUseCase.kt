package com.example.sarisari.manager.domain.usecase

import com.example.sarisari.manager.domain.model.ExpenseCategory
import com.example.sarisari.manager.domain.repository.ExpenseRepository
import java.time.Clock
import javax.inject.Inject

enum class ExpenseError { AMOUNT_INVALID, DESCRIPTION_TOO_LONG, DATE_IN_FUTURE }

sealed interface AddExpenseResult {
    data object Saved : AddExpenseResult
    data class Invalid(val error: ExpenseError) : AddExpenseResult
}

class AddExpenseUseCase @Inject constructor(
    private val expenses: ExpenseRepository,
    private val clock: Clock,
) {
    suspend operator fun invoke(
        category: ExpenseCategory,
        description: String?,
        amountCentavos: Long?,
        spentAtMillis: Long,
    ): AddExpenseResult {
        if (amountCentavos == null || amountCentavos <= 0 || amountCentavos > MAX_AMOUNT_CENTAVOS) {
            return AddExpenseResult.Invalid(ExpenseError.AMOUNT_INVALID)
        }
        val cleaned = description?.trim()?.ifEmpty { null }
        if (cleaned != null && cleaned.length > MAX_DESCRIPTION_LENGTH) {
            return AddExpenseResult.Invalid(ExpenseError.DESCRIPTION_TOO_LONG)
        }
        if (spentAtMillis > clock.millis()) return AddExpenseResult.Invalid(ExpenseError.DATE_IN_FUTURE)
        expenses.addExpense(category, cleaned, amountCentavos, spentAtMillis)
        return AddExpenseResult.Saved
    }

    companion object {
        const val MAX_AMOUNT_CENTAVOS = 100_000_000_00L
        const val MAX_DESCRIPTION_LENGTH = 120
    }
}
