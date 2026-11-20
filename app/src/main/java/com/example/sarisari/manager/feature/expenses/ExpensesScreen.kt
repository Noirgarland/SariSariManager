package com.example.sarisari.manager.feature.expenses

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.sarisari.manager.R
import com.example.sarisari.manager.core.designsystem.BackTopBar
import com.example.sarisari.manager.core.designsystem.PeriodSelector
import com.example.sarisari.manager.core.designsystem.SariSariManagerTheme
import com.example.sarisari.manager.core.util.Dates
import com.example.sarisari.manager.core.util.Money
import com.example.sarisari.manager.domain.model.Expense
import com.example.sarisari.manager.domain.model.ExpenseCategory
import com.example.sarisari.manager.domain.model.StoreTime
import com.example.sarisari.manager.domain.usecase.ExpenseError
import com.example.sarisari.manager.domain.usecase.ReportPeriod
import java.time.Instant
import java.time.YearMonth
import java.time.ZoneOffset

@Composable
fun ExpensesScreen(onBack: () -> Unit, viewModel: ExpensesViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    ExpensesContent(
        uiState = uiState,
        todayUtcMidnightMillis = viewModel.todayUtcMidnightMillis(),
        onBack = onBack,
        onPrevious = viewModel::onPrevious,
        onNext = viewModel::onNext,
        onAdd = viewModel::onAddClicked,
        onDelete = viewModel::onDelete,
        formActions = ExpenseFormActions(
            onCategoryChange = viewModel::onCategoryChange,
            onAmountChange = viewModel::onAmountChange,
            onDescriptionChange = viewModel::onDescriptionChange,
            onDatePicked = viewModel::onDatePicked,
            onSave = viewModel::onSave,
            onDismiss = viewModel::onFormDismissed,
        ),
    )
}

internal data class ExpenseFormActions(
    val onCategoryChange: (ExpenseCategory) -> Unit = {},
    val onAmountChange: (String) -> Unit = {},
    val onDescriptionChange: (String) -> Unit = {},
    val onDatePicked: (Long?) -> Unit = {},
    val onSave: () -> Unit = {},
    val onDismiss: () -> Unit = {},
)

@Composable
internal fun ExpensesContent(
    uiState: ExpensesUiState,
    todayUtcMidnightMillis: Long,
    onBack: () -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onAdd: () -> Unit,
    onDelete: (Long) -> Unit,
    formActions: ExpenseFormActions,
) {
    var deleteId by remember { mutableStateOf<Long?>(null) }
    Scaffold(
        topBar = { BackTopBar(stringResource(R.string.expenses_title), onBack) },
        floatingActionButton = {
            FloatingActionButton(onClick = onAdd) {
                Icon(painterResource(R.drawable.ic_add), stringResource(R.string.expenses_add))
            }
        },
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize().padding(horizontal = 16.dp)) {
            val period = uiState.period
            if (uiState.isLoading || period == null) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
                return@Column
            }
            PeriodSelector(Dates.formatMonth(period.month), onPrevious, onNext, uiState.canGoNext)
            Text(
                stringResource(R.string.expenses_total, Money.format(uiState.totalCentavos)),
                style = MaterialTheme.typography.titleLarge,
            )
            if (uiState.expenses.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(stringResource(R.string.expenses_empty), style = MaterialTheme.typography.bodyLarge)
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(top = 8.dp, bottom = 88.dp),
                ) {
                    items(uiState.expenses, key = { it.id }) { expense ->
                        ExpenseRow(expense, onDelete = { deleteId = expense.id })
                    }
                }
            }
        }
    }

    uiState.form?.let { ExpenseFormDialog(it, todayUtcMidnightMillis, formActions) }

    deleteId?.let { id ->
        AlertDialog(
            onDismissRequest = { deleteId = null },
            title = { Text(stringResource(R.string.expenses_delete_title)) },
            text = { Text(stringResource(R.string.expenses_delete_message)) },
            confirmButton = {
                TextButton(onClick = {
                    onDelete(id)
                    deleteId = null
                }) { Text(stringResource(R.string.expenses_delete_confirm)) }
            },
            dismissButton = { TextButton(onClick = { deleteId = null }) { Text(stringResource(R.string.action_cancel)) } },
        )
    }
}

@Composable
private fun ExpenseRow(expense: Expense, onDelete: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(modifier = Modifier.padding(start = 12.dp, top = 8.dp, bottom = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(stringResource(expense.category.labelRes()), style = MaterialTheme.typography.titleMedium)
                expense.description?.let { Text(it, style = MaterialTheme.typography.bodyMedium) }
                Text(Dates.formatDateTime(expense.spentAtMillis), style = MaterialTheme.typography.bodySmall)
            }
            Text(Money.format(expense.amountCentavos), style = MaterialTheme.typography.titleMedium)
            IconButton(onClick = onDelete) {
                Icon(painterResource(R.drawable.ic_delete), stringResource(R.string.expenses_delete))
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun ExpenseFormDialog(form: ExpenseForm, todayUtcMidnightMillis: Long, actions: ExpenseFormActions) {
    var pickingDate by remember { mutableStateOf(false) }
    val dateMillis = form.pickedDateUtcMillis ?: todayUtcMidnightMillis
    val dateLabelMillis = Instant.ofEpochMilli(dateMillis).atZone(ZoneOffset.UTC).toLocalDate()
        .atStartOfDay(StoreTime.ZONE).toInstant().toEpochMilli()

    AlertDialog(
        onDismissRequest = actions.onDismiss,
        title = { Text(stringResource(R.string.expenses_add)) },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(stringResource(R.string.expense_form_category), style = MaterialTheme.typography.titleSmall)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ExpenseCategory.entries.forEach { category ->
                        FilterChip(
                            selected = form.category == category,
                            onClick = { actions.onCategoryChange(category) },
                            label = { Text(stringResource(category.labelRes())) },
                        )
                    }
                }
                OutlinedTextField(
                    value = form.amount,
                    onValueChange = actions.onAmountChange,
                    label = { Text(stringResource(R.string.expense_form_amount)) },
                    singleLine = true,
                    isError = form.error == ExpenseError.AMOUNT_INVALID,
                    supportingText = if (form.error == ExpenseError.AMOUNT_INVALID) {
                        { Text(stringResource(R.string.expense_error_amount)) }
                    } else null,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = form.description,
                    onValueChange = actions.onDescriptionChange,
                    label = { Text(stringResource(R.string.expense_form_description)) },
                    singleLine = true,
                    isError = form.error == ExpenseError.DESCRIPTION_TOO_LONG,
                    supportingText = if (form.error == ExpenseError.DESCRIPTION_TOO_LONG) {
                        { Text(stringResource(R.string.expense_error_description)) }
                    } else null,
                    modifier = Modifier.fillMaxWidth(),
                )
                Text(stringResource(R.string.expense_form_date, Dates.formatDate(dateLabelMillis)), style = MaterialTheme.typography.bodyLarge)
                TextButton(onClick = { pickingDate = true }) { Text(stringResource(R.string.expense_form_change_date)) }
                if (form.error == ExpenseError.DATE_IN_FUTURE) {
                    Text(stringResource(R.string.expense_error_date), color = MaterialTheme.colorScheme.error)
                }
                Text(
                    stringResource(R.string.expense_form_not_stock),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
        confirmButton = {
            TextButton(onClick = actions.onSave, enabled = !form.isSaving) { Text(stringResource(R.string.action_save)) }
        },
        dismissButton = { TextButton(onClick = actions.onDismiss) { Text(stringResource(R.string.action_cancel)) } },
    )

    if (pickingDate) {
        val state = rememberDatePickerState(
            initialSelectedDateMillis = dateMillis,
            selectableDates = object : SelectableDates {
                override fun isSelectableDate(utcTimeMillis: Long) = utcTimeMillis <= todayUtcMidnightMillis
            },
        )
        DatePickerDialog(
            onDismissRequest = { pickingDate = false },
            confirmButton = {
                TextButton(onClick = {
                    actions.onDatePicked(state.selectedDateMillis)
                    pickingDate = false
                }) { Text(stringResource(R.string.action_ok)) }
            },
            dismissButton = { TextButton(onClick = { pickingDate = false }) { Text(stringResource(R.string.action_cancel)) } },
        ) { DatePicker(state = state) }
    }
}

@Preview(showBackground = true)
@Composable
private fun ExpensesPreview() {
    SariSariManagerTheme {
        ExpensesContent(
            uiState = ExpensesUiState(
                isLoading = false,
                period = ReportPeriod.Month(YearMonth.of(2025, 11)),
                expenses = listOf(Expense(1, ExpenseCategory.ELECTRICITY, "Meralco", 150_000, 1_763_000_000_000)),
            ),
            todayUtcMidnightMillis = 0,
            onBack = {}, onPrevious = {}, onNext = {}, onAdd = {}, onDelete = {},
            formActions = ExpenseFormActions(),
        )
    }
}
