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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.sarisari.manager.R
import com.example.sarisari.manager.core.designsystem.AppHeader
import com.example.sarisari.manager.core.designsystem.CardShape
import com.example.sarisari.manager.core.designsystem.MetricCard
import com.example.sarisari.manager.core.designsystem.Pill
import com.example.sarisari.manager.core.designsystem.ProductAvatar
import com.example.sarisari.manager.core.designsystem.SariSariManagerTheme
import com.example.sarisari.manager.core.designsystem.SectionHeader
import com.example.sarisari.manager.core.designsystem.SurfaceCard
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
    var renaming by remember { mutableStateOf(false) }
    DashboardContent(
        uiState = uiState,
        actions = DashboardActions(
            onOpenStore = viewModel::onOpenStoreClicked,
            onCloseStore = viewModel::onCloseStoreClicked,
            onEditStoreName = { renaming = true },
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
    if (renaming) {
        StoreNameDialog(
            initialName = uiState.storeName.orEmpty(),
            onSave = { name -> viewModel.onRenameStore(name).also { saved -> if (saved) renaming = false } },
            onDismiss = { renaming = false },
        )
    }
}

internal data class DashboardActions(
    val onOpenStore: () -> Unit = {},
    val onCloseStore: () -> Unit = {},
    val onEditStoreName: () -> Unit = {},
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
    Column(modifier = Modifier.fillMaxSize()) {
        StoreHeader(uiState, actions)
        Column(
            modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Column {
                Text(stringResource(R.string.dashboard_greeting_title), style = MaterialTheme.typography.headlineSmall)
                Text(
                    stringResource(R.string.dashboard_greeting, Dates.formatDate(uiState.todayStartMillis)),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            SalesTodayCard(uiState.today.revenueCentavos)
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                MetricCard(
                    iconRes = R.drawable.ic_cart,
                    value = uiState.today.transactionCount.toString(),
                    label = stringResource(R.string.dashboard_transactions),
                    modifier = Modifier.weight(1f),
                )
                MetricCard(
                    iconRes = R.drawable.ic_trending_up,
                    value = Money.format(uiState.today.grossProfitCentavos),
                    label = stringResource(R.string.dashboard_gross_profit),
                    modifier = Modifier.weight(1f),
                    iconTint = MaterialTheme.colorScheme.tertiary,
                    iconBackground = MaterialTheme.colorScheme.tertiaryContainer,
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                MetricCard(
                    iconRes = R.drawable.ic_receipt,
                    value = Money.format(uiState.today.expensesCentavos),
                    label = stringResource(R.string.dashboard_expenses_today),
                    modifier = Modifier.weight(1f),
                    onClick = actions.onAddExpense,
                )
                val warn = uiState.lowStockCount > 0
                MetricCard(
                    iconRes = R.drawable.ic_warning,
                    value = uiState.lowStockCount.toString(),
                    label = stringResource(R.string.dashboard_low_stock),
                    modifier = Modifier.weight(1f),
                    iconTint = if (warn) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                    iconBackground = if (warn) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.primaryContainer,
                    onClick = actions.onAddStock,
                )
            }
            SectionHeader(stringResource(R.string.dashboard_quick_actions))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                QuickActionButton(
                    iconRes = R.drawable.ic_cart,
                    label = stringResource(R.string.dashboard_new_sale),
                    onClick = actions.onNewSale,
                    primary = true,
                    modifier = Modifier.weight(1f),
                )
                QuickActionButton(
                    iconRes = R.drawable.ic_add_box,
                    label = stringResource(R.string.dashboard_add_stock),
                    onClick = actions.onAddStock,
                    primary = false,
                    modifier = Modifier.weight(1f),
                )
            }
            OutlinedButton(
                onClick = actions.onAddExpense,
                shape = CardShape,
                modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
            ) { Text(stringResource(R.string.dashboard_add_expense)) }
            SectionHeader(
                stringResource(R.string.dashboard_top_selling),
                actionLabel = stringResource(R.string.dashboard_see_all),
                onAction = actions.onSeeReports,
            )
            SurfaceCard(modifier = Modifier.fillMaxWidth()) {
                if (uiState.topProducts.isEmpty()) {
                    Text(
                        stringResource(R.string.dashboard_no_sales),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                uiState.topProducts.forEachIndexed { index, product ->
                    TopSellingRow(product, Modifier.padding(top = if (index == 0) 0.dp else 12.dp))
                }
            }
        }
    }
}

@Composable
private fun StoreHeader(uiState: DashboardUiState, actions: DashboardActions) {
    val session = uiState.session
    val status = when {
        session == null -> stringResource(R.string.dashboard_status_closed)
        session.isOpen -> stringResource(
            R.string.dashboard_status_open,
            if (uiState.sessionFromEarlierDay) Dates.formatDateTime(session.openedAtMillis)
            else Dates.formatTime(session.openedAtMillis),
        )
        else -> stringResource(R.string.dashboard_status_closed_at, Dates.formatDateTime(session.closedAtMillis!!))
    }
    AppHeader(
        title = uiState.storeName ?: stringResource(R.string.store_name_default),
        subtitle = status,
        actions = {
            IconButton(onClick = actions.onEditStoreName) {
                Icon(painterResource(R.drawable.ic_edit), stringResource(R.string.store_name_edit))
            }
            val isOpen = session?.isOpen == true
            Button(
                onClick = if (isOpen) actions.onCloseStore else actions.onOpenStore,
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isOpen) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.tertiaryContainer,
                    contentColor = if (isOpen) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onTertiaryContainer,
                ),
                modifier = Modifier.heightIn(min = 48.dp),
            ) {
                Text(stringResource(if (isOpen) R.string.dashboard_close_store else R.string.dashboard_open_store))
            }
        },
    )
}

@Composable
private fun SalesTodayCard(revenueCentavos: Long) {
    SurfaceCard(
        modifier = Modifier.fillMaxWidth(),
        containerColor = MaterialTheme.colorScheme.primary,
        contentColor = MaterialTheme.colorScheme.onPrimary,
        border = false,
        contentPadding = 20.dp,
    ) {
        Text(
            Money.format(revenueCentavos),
            style = MaterialTheme.typography.displaySmall,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(stringResource(R.string.dashboard_sales_today), style = MaterialTheme.typography.titleMedium)
    }
}

@Composable
private fun QuickActionButton(iconRes: Int, label: String, onClick: () -> Unit, primary: Boolean, modifier: Modifier = Modifier) {
    SurfaceCard(
        modifier = modifier.heightIn(min = 88.dp),
        onClick = onClick,
        containerColor = if (primary) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.primaryContainer,
        contentColor = if (primary) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onPrimaryContainer,
        border = false,
    ) {
        Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(painterResource(iconRes), contentDescription = null, modifier = Modifier.size(28.dp))
            Text(label, style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 6.dp))
        }
    }
}

@Composable
private fun TopSellingRow(product: TopProduct, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        ProductAvatar(product.productName)
        Column(modifier = Modifier.weight(1f)) {
            Text(product.productName, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(
                Money.format(product.revenueCentavos),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Pill(
            stringResource(R.string.top_product_sold, product.baseQuantity, product.baseUnitName),
            MaterialTheme.colorScheme.tertiaryContainer,
            MaterialTheme.colorScheme.onTertiaryContainer,
        )
    }
}

/** Edits the store name shown in the header. [onSave] returns false when the name is not valid. */
@Composable
private fun StoreNameDialog(initialName: String, onSave: (String) -> Boolean, onDismiss: () -> Unit) {
    var text by remember { mutableStateOf(initialName) }
    var invalid by remember { mutableStateOf(false) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.settings_store_name)) },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = {
                    text = it
                    invalid = false
                },
                singleLine = true,
                isError = invalid,
                supportingText = if (invalid) {
                    { Text(stringResource(R.string.settings_store_name_invalid)) }
                } else {
                    null
                },
                modifier = Modifier.fillMaxWidth(),
            )
        },
        confirmButton = {
            TextButton(onClick = { invalid = !onSave(text) }) { Text(stringResource(R.string.action_save)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) } },
    )
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
                today = ReportSummary(485_000, 393_000, 92_000, 48),
                lowStockCount = 6,
                topProducts = listOf(
                    TopProduct(1, "Coke 1.5L", "bottle", 12, 78_000),
                    TopProduct(2, "Piattos", "pack", 9, 18_000),
                ),
            ),
            DashboardActions(),
        )
    }
}
