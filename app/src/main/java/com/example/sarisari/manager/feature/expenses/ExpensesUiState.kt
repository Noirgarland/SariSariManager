package com.example.sarisari.manager.feature.expenses

import androidx.annotation.StringRes
import com.example.sarisari.manager.R
import com.example.sarisari.manager.domain.model.Expense
import com.example.sarisari.manager.domain.model.ExpenseCategory
import com.example.sarisari.manager.domain.usecase.ExpenseError
import com.example.sarisari.manager.domain.usecase.ReportPeriod

data class ExpenseForm(
    val category: ExpenseCategory = ExpenseCategory.ELECTRICITY,
    val amount: String = "",
    val description: String = "",
    /** The chosen day as UTC epoch milliseconds at midnight, as the date picker uses. Null means today. */
    val pickedDateUtcMillis: Long? = null,
    val error: ExpenseError? = null,
    val isSaving: Boolean = false,
)

data class ExpensesUiState(
    val isLoading: Boolean = true,
    val period: ReportPeriod.Month? = null,
    val canGoNext: Boolean = false,
    val expenses: List<Expense> = emptyList(),
    val form: ExpenseForm? = null,
) {
    val totalCentavos: Long get() = expenses.sumOf { it.amountCentavos }
}

@StringRes
fun ExpenseCategory.labelRes(): Int = when (this) {
    ExpenseCategory.ELECTRICITY -> R.string.expense_category_electricity
    ExpenseCategory.WATER -> R.string.expense_category_water
    ExpenseCategory.RENT -> R.string.expense_category_rent
    ExpenseCategory.TRANSPORTATION -> R.string.expense_category_transportation
    ExpenseCategory.SUPPLIES -> R.string.expense_category_supplies
    ExpenseCategory.SALARIES -> R.string.expense_category_salaries
    ExpenseCategory.OTHER -> R.string.expense_category_other
}
