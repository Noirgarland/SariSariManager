package com.example.sarisari.manager.feature.utang

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.sarisari.manager.R
import com.example.sarisari.manager.core.designsystem.AppHeader
import com.example.sarisari.manager.core.designsystem.CardShape
import com.example.sarisari.manager.core.designsystem.Pill
import com.example.sarisari.manager.core.designsystem.SariSariManagerTheme
import com.example.sarisari.manager.core.designsystem.SectionHeader
import com.example.sarisari.manager.core.designsystem.SurfaceCard
import com.example.sarisari.manager.core.util.Dates
import com.example.sarisari.manager.core.util.Money
import com.example.sarisari.manager.domain.model.CreditEntry
import com.example.sarisari.manager.domain.model.CreditEntryType
import com.example.sarisari.manager.domain.model.Customer
import com.example.sarisari.manager.domain.model.StoreTime
import java.time.Instant
import java.time.ZoneOffset
import kotlin.math.abs

@Composable
fun CustomerLedgerScreen(onBack: () -> Unit, viewModel: CustomerLedgerViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    CustomerLedgerContent(
        uiState = uiState,
        todayUtcMidnightMillis = viewModel.todayUtcMidnightMillis(),
        actions = LedgerActions(
            onBack = onBack,
            onRecordPayment = viewModel::onRecordPaymentClicked,
            onPayFull = viewModel::onPayFullBalance,
            onPaymentAmountChange = viewModel::onPaymentAmountChange,
            onPaymentNoteChange = viewModel::onPaymentNoteChange,
            onPaymentDatePicked = viewModel::onPaymentDatePicked,
            onSavePayment = viewModel::onSavePayment,
            onPaymentDismissed = viewModel::onPaymentDismissed,
            onVoidPayment = viewModel::onVoidPaymentClicked,
            onVoidConfirmed = viewModel::onVoidConfirmed,
            onVoidDismissed = viewModel::onVoidDismissed,
            onRename = viewModel::onRenameClicked,
            onRenameChange = viewModel::onRenameChange,
            onSaveRename = viewModel::onSaveRename,
            onRenameDismissed = viewModel::onRenameDismissed,
            onMessageShown = viewModel::onMessageShown,
        ),
    )
}

internal data class LedgerActions(
    val onBack: () -> Unit = {},
    val onRecordPayment: () -> Unit = {},
    val onPayFull: () -> Unit = {},
    val onPaymentAmountChange: (String) -> Unit = {},
    val onPaymentNoteChange: (String) -> Unit = {},
    val onPaymentDatePicked: (Long?) -> Unit = {},
    val onSavePayment: () -> Unit = {},
    val onPaymentDismissed: () -> Unit = {},
    val onVoidPayment: (CreditEntry) -> Unit = {},
    val onVoidConfirmed: () -> Unit = {},
    val onVoidDismissed: () -> Unit = {},
    val onRename: () -> Unit = {},
    val onRenameChange: (String) -> Unit = {},
    val onSaveRename: () -> Unit = {},
    val onRenameDismissed: () -> Unit = {},
    val onMessageShown: () -> Unit = {},
)

@Composable
internal fun CustomerLedgerContent(uiState: CustomerLedgerUiState, todayUtcMidnightMillis: Long, actions: LedgerActions) {
    val snackbar = remember { SnackbarHostState() }
    val message = uiState.messageRes?.let { stringResource(it) }
    LaunchedEffect(message) {
        if (message != null) {
            snackbar.showSnackbar(message)
            actions.onMessageShown()
        }
    }
    val customer = uiState.customer
    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            AppHeader(
                title = customer?.name ?: stringResource(R.string.utang_title),
                onBack = actions.onBack,
                actions = {
                    if (customer != null) {
                        IconButton(onClick = actions.onRename) {
                            Icon(painterResource(R.drawable.ic_edit), stringResource(R.string.utang_rename))
                        }
                    }
                },
            )
            when {
                uiState.isLoading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
                customer == null -> Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
                    Text(stringResource(R.string.utang_error_customer), textAlign = TextAlign.Center)
                }
                else -> LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    item { BalanceCard(customer, actions.onRecordPayment) }
                    item { SectionHeader(stringResource(R.string.utang_history)) }
                    if (uiState.entries.isEmpty()) {
                        item {
                            Text(
                                stringResource(R.string.utang_history_empty),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                    items(uiState.entries, key = { it.id }) { entry -> EntryRow(entry, actions.onVoidPayment) }
                }
            }
        }
        SnackbarHost(snackbar, modifier = Modifier.align(Alignment.BottomCenter))
    }
    uiState.payment?.let { form ->
        if (customer != null) PaymentDialog(customer, form, todayUtcMidnightMillis, actions)
    }
    uiState.rename?.let { form ->
        CustomerNameDialog(
            title = stringResource(R.string.utang_rename),
            form = form,
            onNameChange = actions.onRenameChange,
            onSave = actions.onSaveRename,
            onDismiss = actions.onRenameDismissed,
        )
    }
    uiState.confirmVoidEntry?.let { entry ->
        AlertDialog(
            onDismissRequest = actions.onVoidDismissed,
            title = { Text(stringResource(R.string.utang_void_title)) },
            text = { Text(stringResource(R.string.utang_void_message, Money.format(-entry.amountCentavos))) },
            confirmButton = {
                TextButton(onClick = actions.onVoidConfirmed) { Text(stringResource(R.string.utang_void_confirm)) }
            },
            dismissButton = { TextButton(onClick = actions.onVoidDismissed) { Text(stringResource(R.string.action_cancel)) } },
        )
    }
}

@Composable
private fun BalanceCard(customer: Customer, onRecordPayment: () -> Unit) {
    val owes = customer.balanceCentavos > 0
    SurfaceCard(modifier = Modifier.fillMaxWidth()) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text(stringResource(R.string.utang_outstanding_balance), style = MaterialTheme.typography.titleSmall)
            if (owes) {
                Pill(stringResource(R.string.utang_unpaid), MaterialTheme.colorScheme.error, MaterialTheme.colorScheme.onError)
            } else {
                Pill(stringResource(R.string.utang_paid), MaterialTheme.colorScheme.primaryContainer, MaterialTheme.colorScheme.onPrimaryContainer)
            }
        }
        Text(
            Money.format(customer.balanceCentavos),
            style = MaterialTheme.typography.displaySmall,
            color = if (owes) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(vertical = 4.dp),
        )
        Text(
            stringResource(
                R.string.utang_last_credit,
                customer.lastCreditAtMillis?.let { Dates.formatDate(it) } ?: stringResource(R.string.utang_never),
            ),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            stringResource(
                R.string.utang_last_payment,
                customer.lastPaymentAtMillis?.let { Dates.formatDate(it) } ?: stringResource(R.string.utang_never),
            ),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Button(
            onClick = onRecordPayment,
            enabled = owes,
            shape = CardShape,
            modifier = Modifier.fillMaxWidth().padding(top = 12.dp).heightIn(min = 52.dp),
        ) { Text(stringResource(R.string.utang_record_payment), style = MaterialTheme.typography.titleMedium) }
    }
}

@Composable
private fun EntryRow(entry: CreditEntry, onVoidPayment: (CreditEntry) -> Unit) {
    val increases = entry.amountCentavos > 0
    SurfaceCard(modifier = Modifier.fillMaxWidth(), contentPadding = 12.dp) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(stringResource(entry.type.labelRes()), style = MaterialTheme.typography.titleSmall)
                Text(
                    Dates.formatDateTime(entry.occurredAtMillis),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                entry.note?.let {
                    Text(it, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                if (entry.isVoided) {
                    Text(stringResource(R.string.utang_voided), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.error)
                }
            }
            Text(
                stringResource(
                    if (increases) R.string.utang_amount_up else R.string.utang_amount_down,
                    Money.format(abs(entry.amountCentavos)),
                ),
                style = MaterialTheme.typography.titleMedium,
                color = if (increases) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
            )
        }
        if (entry.type == CreditEntryType.PAYMENT && !entry.isVoided) {
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant, modifier = Modifier.padding(vertical = 6.dp))
            TextButton(
                onClick = { onVoidPayment(entry) },
                colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
                modifier = Modifier.heightIn(min = 48.dp),
            ) { Text(stringResource(R.string.utang_void_payment)) }
        }
    }
}

private fun CreditEntryType.labelRes(): Int = when (this) {
    CreditEntryType.CREDIT_SALE -> R.string.utang_entry_credit_sale
    CreditEntryType.PAYMENT -> R.string.utang_entry_payment
    CreditEntryType.SALE_VOID -> R.string.utang_entry_sale_void
    CreditEntryType.PAYMENT_VOID -> R.string.utang_entry_payment_void
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PaymentDialog(customer: Customer, form: PaymentForm, todayUtcMidnightMillis: Long, actions: LedgerActions) {
    var pickingDate by remember { mutableStateOf(false) }
    val dateMillis = form.pickedDateUtcMillis ?: todayUtcMidnightMillis
    val dateLabel = Dates.formatDate(
        Instant.ofEpochMilli(dateMillis).atZone(ZoneOffset.UTC).toLocalDate()
            .atTime(12, 0).atZone(StoreTime.ZONE).toInstant().toEpochMilli()
    )
    AlertDialog(
        onDismissRequest = actions.onPaymentDismissed,
        title = { Text(stringResource(R.string.utang_record_payment)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(stringResource(R.string.utang_current_balance, Money.format(customer.balanceCentavos)))
                OutlinedTextField(
                    value = form.amount,
                    onValueChange = actions.onPaymentAmountChange,
                    label = { Text(stringResource(R.string.utang_payment_amount)) },
                    singleLine = true,
                    isError = form.errorRes != null,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth(),
                )
                TextButton(onClick = actions.onPayFull, modifier = Modifier.heightIn(min = 48.dp)) {
                    Text(stringResource(R.string.utang_pay_full))
                }
                OutlinedButton(onClick = { pickingDate = true }, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
                    Text(stringResource(R.string.utang_payment_date, dateLabel))
                }
                OutlinedTextField(
                    value = form.note,
                    onValueChange = actions.onPaymentNoteChange,
                    label = { Text(stringResource(R.string.stock_note_optional)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Text(
                    stringResource(R.string.utang_partial_hint),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                form.errorRes?.let { Text(stringResource(it), color = MaterialTheme.colorScheme.error) }
            }
        },
        confirmButton = {
            TextButton(onClick = actions.onSavePayment, enabled = !form.isSaving) { Text(stringResource(R.string.action_save)) }
        },
        dismissButton = { TextButton(onClick = actions.onPaymentDismissed) { Text(stringResource(R.string.action_cancel)) } },
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
                    actions.onPaymentDatePicked(state.selectedDateMillis)
                    pickingDate = false
                }) { Text(stringResource(R.string.action_save)) }
            },
            dismissButton = { TextButton(onClick = { pickingDate = false }) { Text(stringResource(R.string.action_cancel)) } },
        ) { DatePicker(state = state) }
    }
}

@Preview(showBackground = true)
@Composable
private fun LedgerPreview() {
    SariSariManagerTheme {
        CustomerLedgerContent(
            CustomerLedgerUiState(
                isLoading = false,
                customer = Customer(1, "Juan Dela Cruz", 35_000, 1_763_420_000_000, 1_763_300_000_000),
                entries = listOf(
                    CreditEntry(2, 1, CreditEntryType.PAYMENT, -10_000, null, null, 1_763_420_000_000, null),
                    CreditEntry(1, 1, CreditEntryType.CREDIT_SALE, 45_000, 3, "Sale #3", 1_763_300_000_000, null),
                ),
            ),
            todayUtcMidnightMillis = 1_763_337_600_000,
            actions = LedgerActions(),
        )
    }
}
