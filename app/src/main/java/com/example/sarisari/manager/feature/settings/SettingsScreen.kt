package com.example.sarisari.manager.feature.settings

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.sarisari.manager.R
import com.example.sarisari.manager.core.designsystem.BackTopBar
import com.example.sarisari.manager.core.designsystem.PeriodSelector
import com.example.sarisari.manager.core.designsystem.SariSariManagerTheme
import com.example.sarisari.manager.core.util.Dates
import com.example.sarisari.manager.core.util.restartApp
import com.example.sarisari.manager.domain.model.BackupError
import com.example.sarisari.manager.domain.model.CostingMethod
import com.example.sarisari.manager.domain.usecase.ReportPeriod
import java.time.YearMonth

@Composable
fun SettingsScreen(onBack: () -> Unit, viewModel: SettingsViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    val backupLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/zip")) { uri ->
        uri?.let { viewModel.onBackupDestination(it.toString()) }
    }
    val restoreLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let { viewModel.onRestoreSource(it.toString()) }
    }
    val salesLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/csv")) { uri ->
        uri?.let { viewModel.onSalesExportDestination(it.toString()) }
    }
    val productsLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/csv")) { uri ->
        uri?.let { viewModel.onProductsExportDestination(it.toString()) }
    }
    val utangBalancesLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/csv")) { uri ->
        uri?.let { viewModel.onUtangBalancesExportDestination(it.toString()) }
    }
    val utangHistoryLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/csv")) { uri ->
        uri?.let { viewModel.onUtangHistoryExportDestination(it.toString()) }
    }

    SettingsContent(
        uiState = uiState,
        actions = SettingsActions(
            onBack = onBack,
            onStoreNameChange = viewModel::onStoreNameChange,
            onSaveStoreName = viewModel::onSaveStoreName,
            onCostingMethodConfirmed = viewModel::onCostingMethodConfirmed,
            onCreateBackup = { backupLauncher.launch(viewModel.suggestedBackupName()) },
            onPickRestore = { restoreLauncher.launch(arrayOf("*/*")) },
            onRestoreConfirmed = viewModel::onRestoreConfirmed,
            onRestoreCancelled = viewModel::onRestoreCancelled,
            onRestart = { context.restartApp() },
            onPreviousMonth = viewModel::onPreviousMonth,
            onNextMonth = viewModel::onNextMonth,
            onExportSales = { salesLauncher.launch(viewModel.suggestedSalesExportName()) },
            onExportProducts = { productsLauncher.launch(viewModel.suggestedProductsExportName()) },
            onExportUtangBalances = { utangBalancesLauncher.launch(viewModel.suggestedUtangBalancesExportName()) },
            onExportUtangHistory = { utangHistoryLauncher.launch(viewModel.suggestedUtangHistoryExportName()) },
            onMessageShown = viewModel::onMessageShown,
        ),
    )
}

internal data class SettingsActions(
    val onBack: () -> Unit = {},
    val onStoreNameChange: (String) -> Unit = {},
    val onSaveStoreName: () -> Unit = {},
    val onCostingMethodConfirmed: (CostingMethod) -> Unit = {},
    val onCreateBackup: () -> Unit = {},
    val onPickRestore: () -> Unit = {},
    val onRestoreConfirmed: () -> Unit = {},
    val onRestoreCancelled: () -> Unit = {},
    val onRestart: () -> Unit = {},
    val onPreviousMonth: () -> Unit = {},
    val onNextMonth: () -> Unit = {},
    val onExportSales: () -> Unit = {},
    val onExportProducts: () -> Unit = {},
    val onExportUtangBalances: () -> Unit = {},
    val onExportUtangHistory: () -> Unit = {},
    val onMessageShown: () -> Unit = {},
)

@Composable
internal fun SettingsContent(uiState: SettingsUiState, actions: SettingsActions) {
    val snackbar = remember { SnackbarHostState() }
    val messageText = uiState.message?.let { messageText(it) }
    LaunchedEffect(uiState.message) {
        if (messageText != null) {
            snackbar.showSnackbar(messageText)
            actions.onMessageShown()
        }
    }
    var pendingMethod by remember { mutableStateOf<CostingMethod?>(null) }

    Scaffold(
        topBar = { BackTopBar(stringResource(R.string.settings_title), actions.onBack) },
        snackbarHost = { SnackbarHost(snackbar) },
    ) { padding ->
        if (uiState.isLoading) {
            Box(Modifier.padding(padding).fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            return@Scaffold
        }
        Column(
            modifier = Modifier.padding(padding).fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Section(R.string.settings_store_section) {
                OutlinedTextField(
                    value = uiState.storeNameInput,
                    onValueChange = actions.onStoreNameChange,
                    label = { Text(stringResource(R.string.settings_store_name)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Button(onClick = actions.onSaveStoreName, modifier = Modifier.heightIn(min = 48.dp)) {
                    Text(stringResource(R.string.action_save))
                }
            }

            Section(R.string.settings_costing_section) {
                costingOptions.forEach { (method, title, description) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 48.dp)
                            .selectable(
                                selected = uiState.costingMethod == method,
                                role = Role.RadioButton,
                                onClick = { if (uiState.costingMethod != method) pendingMethod = method },
                            ),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(selected = uiState.costingMethod == method, onClick = null)
                        Column(modifier = Modifier.padding(start = 8.dp)) {
                            Text(stringResource(title), style = MaterialTheme.typography.titleSmall)
                            Text(stringResource(description), style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }

            Section(R.string.settings_backup_section) {
                Text(
                    stringResource(R.string.settings_backup_notice),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                TaskProgress(uiState.task, SettingsTask.BACKUP, R.string.settings_backup_working)
                TaskProgress(uiState.task, SettingsTask.CHECK_RESTORE, R.string.settings_restore_checking)
                Button(
                    onClick = actions.onCreateBackup,
                    enabled = !uiState.isBusy,
                    modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                ) { Text(stringResource(R.string.settings_backup_create)) }
                OutlinedButton(
                    onClick = actions.onPickRestore,
                    enabled = !uiState.isBusy,
                    modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                ) { Text(stringResource(R.string.settings_backup_restore)) }
            }

            Section(R.string.settings_export_section) {
                Text(stringResource(R.string.settings_export_month), style = MaterialTheme.typography.titleSmall)
                uiState.exportMonth?.let { month ->
                    PeriodSelector(
                        label = Dates.formatMonth(month.month),
                        onPrevious = actions.onPreviousMonth,
                        onNext = actions.onNextMonth,
                        canGoNext = uiState.canGoNextMonth,
                    )
                }
                TaskProgress(uiState.task, SettingsTask.EXPORT, R.string.settings_export_working)
                Button(
                    onClick = actions.onExportSales,
                    enabled = !uiState.isBusy,
                    modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                ) { Text(stringResource(R.string.settings_export_sales)) }
                OutlinedButton(
                    onClick = actions.onExportProducts,
                    enabled = !uiState.isBusy,
                    modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                ) { Text(stringResource(R.string.settings_export_products)) }
                OutlinedButton(
                    onClick = actions.onExportUtangBalances,
                    enabled = !uiState.isBusy,
                    modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                ) { Text(stringResource(R.string.settings_export_utang_balances)) }
                OutlinedButton(
                    onClick = actions.onExportUtangHistory,
                    enabled = !uiState.isBusy,
                    modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                ) { Text(stringResource(R.string.settings_export_utang_history)) }
            }
        }
    }

    pendingMethod?.let { method ->
        AlertDialog(
            onDismissRequest = { pendingMethod = null },
            title = { Text(stringResource(R.string.settings_costing_confirm_title)) },
            text = { Text(stringResource(R.string.settings_costing_confirm_message)) },
            confirmButton = {
                TextButton(onClick = {
                    actions.onCostingMethodConfirmed(method)
                    pendingMethod = null
                }) { Text(stringResource(R.string.settings_costing_confirm)) }
            },
            dismissButton = { TextButton(onClick = { pendingMethod = null }) { Text(stringResource(R.string.action_cancel)) } },
        )
    }

    uiState.pendingRestore?.let { info ->
        AlertDialog(
            onDismissRequest = actions.onRestoreCancelled,
            title = { Text(stringResource(R.string.settings_restore_confirm_title)) },
            text = { Text(stringResource(R.string.settings_restore_confirm_message, Dates.formatDateTime(info.createdAtMillis))) },
            confirmButton = {
                TextButton(onClick = actions.onRestoreConfirmed) { Text(stringResource(R.string.settings_restore_confirm)) }
            },
            dismissButton = { TextButton(onClick = actions.onRestoreCancelled) { Text(stringResource(R.string.action_cancel)) } },
        )
    }

    if (uiState.restoreStaged) {
        AlertDialog(
            onDismissRequest = {},
            title = { Text(stringResource(R.string.settings_restore_restart_title)) },
            text = { Text(stringResource(R.string.settings_restore_restart_message)) },
            confirmButton = { TextButton(onClick = actions.onRestart) { Text(stringResource(R.string.settings_restore_restart)) } },
        )
    }
}

private data class CostingOption(val method: CostingMethod, val title: Int, val description: Int)

private val costingOptions = listOf(
    CostingOption(CostingMethod.WEIGHTED_AVERAGE, R.string.settings_costing_weighted, R.string.settings_costing_weighted_desc),
    CostingOption(CostingMethod.LATEST_PURCHASE, R.string.settings_costing_latest, R.string.settings_costing_latest_desc),
)

@Composable
private fun Section(titleRes: Int, content: @Composable () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(stringResource(titleRes), style = MaterialTheme.typography.titleMedium)
            content()
        }
    }
}

@Composable
private fun TaskProgress(current: SettingsTask?, task: SettingsTask, labelRes: Int) {
    if (current != task) return
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(stringResource(labelRes), style = MaterialTheme.typography.bodyMedium)
        LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
    }
}

@Composable
private fun messageText(message: SettingsMessage): String = when (message) {
    SettingsMessage.StoreNameSaved -> stringResource(R.string.settings_store_name_saved)
    SettingsMessage.StoreNameInvalid -> stringResource(R.string.settings_store_name_invalid)
    SettingsMessage.CostingChanged -> stringResource(R.string.settings_costing_changed)
    SettingsMessage.BackupSaved -> stringResource(R.string.settings_backup_saved)
    SettingsMessage.BackupFailed -> stringResource(R.string.settings_backup_failed)
    SettingsMessage.RestoreApplied -> stringResource(R.string.settings_restore_applied)
    SettingsMessage.RestoreRolledBack -> stringResource(R.string.settings_restore_rolled_back)
    SettingsMessage.ExportFailed -> stringResource(R.string.settings_export_failed)
    is SettingsMessage.ExportDone -> stringResource(R.string.settings_export_done, message.rows)
    is SettingsMessage.RestoreRejected -> stringResource(
        when (message.error) {
            BackupError.UNREADABLE, BackupError.WRITE_FAILED -> R.string.settings_restore_error_unreadable
            BackupError.MANIFEST_MISSING -> R.string.settings_restore_error_manifest
            BackupError.CHECKSUM_MISMATCH -> R.string.settings_restore_error_checksum
            BackupError.INTEGRITY_FAILED -> R.string.settings_restore_error_integrity
            BackupError.MISSING_TABLES -> R.string.settings_restore_error_tables
            BackupError.NEWER_SCHEMA -> R.string.settings_restore_error_newer
            BackupError.TOO_LARGE -> R.string.settings_restore_error_too_large
        }
    )
}

@Preview(showBackground = true)
@Composable
private fun SettingsPreview() {
    SariSariManagerTheme {
        SettingsContent(
            SettingsUiState(isLoading = false, storeNameInput = "Aling Rosa Store", exportMonth = ReportPeriod.Month(YearMonth.of(2025, 11))),
            SettingsActions(),
        )
    }
}
