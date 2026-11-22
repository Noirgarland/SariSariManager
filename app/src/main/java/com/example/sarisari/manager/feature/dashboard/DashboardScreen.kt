package com.example.sarisari.manager.feature.dashboard

import android.text.format.DateFormat
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimeInput
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TimePickerDialog
import androidx.compose.material3.TimePickerDialogDefaults
import androidx.compose.material3.TimePickerDisplayMode
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.sarisari.manager.R
import com.example.sarisari.manager.core.designsystem.SariSariManagerTheme
import com.example.sarisari.manager.core.designsystem.StatCard
import com.example.sarisari.manager.core.designsystem.TopProductRow
import com.example.sarisari.manager.core.util.Dates
import com.example.sarisari.manager.core.util.Money
import com.example.sarisari.manager.domain.model.ReportSummary
import com.example.sarisari.manager.domain.model.StoreSession
import com.example.sarisari.manager.domain.model.TopProduct
import java.time.LocalTime

@Composable
fun DashboardScreen(
    onNewSale: () -> Unit,
    onAddStock: () -> Unit,
    onAddExpense: () -> Unit,
    onSeeReports: () -> Unit,
    viewModel: DashboardViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    LifecycleResumeEffect(Unit) {
        viewModel.onResume()
        onPauseOrDispose { }
    }
    DashboardContent(
        uiState = uiState,
        actions = DashboardActions(
            onOpenStore = viewModel::onOpenStoreClicked,
            onCloseStore = viewModel::onCloseStoreClicked,
            onNewSale = onNewSale,
            onAddStock = onAddStock,
            onAddExpense = onAddExpense,
            onSeeReports = onSeeReports,
        ),
    )
    uiState.timeDialog?.let { dialog ->
        // Keyed by action so reopening the dialog resets the clock to its starting time.
        key(dialog.action) {
            StoreTimeDialog(
                dialog = dialog,
                session = uiState.session,
                sessionFromEarlierDay = uiState.sessionFromEarlierDay,
                currentTime = viewModel::now,
                onConfirm = viewModel::onTimeConfirmed,
                onDismiss = viewModel::onTimeDialogDismissed,
            )
        }
    }
}

internal data class DashboardActions(
    val onOpenStore: () -> Unit = {},
    val onCloseStore: () -> Unit = {},
    val onNewSale: () -> Unit = {},
    val onAddStock: () -> Unit = {},
    val onAddExpense: () -> Unit = {},
    val onSeeReports: () -> Unit = {},
)

@Composable
internal fun DashboardContent(uiState: DashboardUiState, actions: DashboardActions) {
    if (uiState.isLoading) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        return
    }
    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        StoreHeader(uiState, actions)
        Text(
            stringResource(R.string.dashboard_greeting, Dates.formatDate(uiState.todayStartMillis)),
            style = MaterialTheme.typography.bodyLarge,
        )
        StatCard(
            label = stringResource(R.string.dashboard_sales_today),
            value = Money.format(uiState.today.revenueCentavos),
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
            modifier = Modifier.fillMaxWidth(),
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            StatCard(
                stringResource(R.string.dashboard_transactions),
                uiState.today.transactionCount.toString(),
                Modifier.weight(1f),
            )
            StatCard(
                stringResource(R.string.dashboard_gross_profit),
                Money.format(uiState.today.grossProfitCentavos),
                Modifier.weight(1f),
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            StatCard(
                stringResource(R.string.dashboard_low_stock),
                uiState.lowStockCount.toString(),
                Modifier.weight(1f),
                containerColor = if (uiState.lowStockCount > 0) MaterialTheme.colorScheme.tertiaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                contentColor = if (uiState.lowStockCount > 0) MaterialTheme.colorScheme.onTertiaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
            )
            StatCard(
                stringResource(R.string.dashboard_expenses_today),
                Money.format(uiState.today.expensesCentavos),
                Modifier.weight(1f),
            )
        }

        Text(stringResource(R.string.dashboard_quick_actions), style = MaterialTheme.typography.titleMedium)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = actions.onNewSale, modifier = Modifier.weight(1f).heightIn(min = 56.dp)) {
                Text(stringResource(R.string.dashboard_new_sale))
            }
            FilledTonalButton(onClick = actions.onAddStock, modifier = Modifier.weight(1f).heightIn(min = 56.dp)) {
                Text(stringResource(R.string.dashboard_add_stock))
            }
        }
        OutlinedButton(onClick = actions.onAddExpense, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
            Text(stringResource(R.string.dashboard_add_expense))
        }

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(stringResource(R.string.dashboard_top_selling), style = MaterialTheme.typography.titleMedium)
                    TextButton(onClick = actions.onSeeReports) { Text(stringResource(R.string.dashboard_see_all)) }
                }
                if (uiState.topProducts.isEmpty()) {
                    Text(stringResource(R.string.dashboard_no_sales), style = MaterialTheme.typography.bodyMedium)
                }
                uiState.topProducts.forEachIndexed { index, product -> TopProductRow(index + 1, product) }
            }
        }
    }
}

@Composable
private fun StoreHeader(uiState: DashboardUiState, actions: DashboardActions) {
    val session = uiState.session
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        ),
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    uiState.storeName ?: stringResource(R.string.store_name_default),
                    style = MaterialTheme.typography.titleLarge,
                )
                Text(
                    when {
                        session == null -> stringResource(R.string.dashboard_status_closed)
                        session.isOpen -> stringResource(
                            R.string.dashboard_status_open,
                            if (uiState.sessionFromEarlierDay) Dates.formatDateTime(session.openedAtMillis)
                            else Dates.formatTime(session.openedAtMillis),
                        )
                        else -> stringResource(R.string.dashboard_status_closed_at, Dates.formatDateTime(session.closedAtMillis!!))
                    },
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
            if (session?.isOpen == true) {
                OutlinedButton(onClick = actions.onCloseStore, modifier = Modifier.heightIn(min = 48.dp)) {
                    Text(stringResource(R.string.dashboard_close_store))
                }
            } else {
                Button(onClick = actions.onOpenStore, modifier = Modifier.heightIn(min = 48.dp)) {
                    Text(stringResource(R.string.dashboard_open_store))
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun StoreTimeDialog(
    dialog: StoreTimeDialogState,
    session: StoreSession?,
    sessionFromEarlierDay: Boolean,
    currentTime: () -> LocalTime,
    onConfirm: (LocalTime) -> Unit,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    val density = LocalDensity.current
    val screenHeight = with(density) { LocalWindowInfo.current.containerSize.height.toDp() }
    val state = rememberTimePickerState(
        initialHour = dialog.initialTime.hour,
        initialMinute = dialog.initialTime.minute,
        is24Hour = DateFormat.is24HourFormat(context),
    )
    // The clock face needs room. On short screens start with the keyboard entry instead.
    var displayMode by remember {
        mutableStateOf(
            if (screenHeight < TimePickerDialogDefaults.MinHeightForTimePicker) TimePickerDisplayMode.Input
            else TimePickerDisplayMode.Picker
        )
    }
    val isOpen = dialog.action == StoreTimeAction.OPEN
    TimePickerDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(stringResource(if (isOpen) R.string.store_time_open_title else R.string.close_store_title))
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(LocalTime.of(state.hour, state.minute)) }, enabled = !dialog.isSaving) {
                Text(stringResource(if (isOpen) R.string.dashboard_open_store else R.string.close_store_confirm))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) }
        },
        modeToggleButton = {
            TimePickerDialogDefaults.DisplayModeToggle(
                onDisplayModeChange = {
                    displayMode = if (displayMode == TimePickerDisplayMode.Picker) {
                        TimePickerDisplayMode.Input
                    } else {
                        TimePickerDisplayMode.Picker
                    }
                },
                displayMode = displayMode,
            )
        },
    ) {
        Text(
            when {
                isOpen -> stringResource(R.string.store_time_open_message)
                sessionFromEarlierDay && session != null ->
                    stringResource(R.string.close_store_forgot, Dates.formatDate(session.openedAtMillis))
                else -> stringResource(R.string.close_store_message)
            },
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(bottom = 12.dp),
        )
        if (displayMode == TimePickerDisplayMode.Picker) TimePicker(state = state) else TimeInput(state = state)
        TextButton(
            onClick = {
                val now = currentTime()
                state.hour = now.hour
                state.minute = now.minute
            },
            modifier = Modifier.heightIn(min = 48.dp),
        ) { Text(stringResource(R.string.store_time_now)) }
        dialog.errorRes?.let {
            Text(stringResource(it), color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
        }
    }
}
@Preview(showBackground = true)
@Composable
private fun DashboardPreview() {
    SariSariManagerTheme {
        DashboardContent(
            DashboardUiState(
                isLoading = false,
                storeName = "Aling Rosa Store",
                session = StoreSession(1, 1_763_420_000_000, null),
                todayStartMillis = 1_763_395_200_000,
                today = ReportSummary(485_000, 393_000, 0, 48),
                lowStockCount = 6,
                topProducts = listOf(TopProduct(1, "Coke 1.5L", "bottle", 12, 78_000)),
            ),
            DashboardActions(),
        )
    }
}
