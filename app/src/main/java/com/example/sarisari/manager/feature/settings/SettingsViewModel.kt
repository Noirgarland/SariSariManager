package com.example.sarisari.manager.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.sarisari.manager.core.util.Dates
import com.example.sarisari.manager.domain.model.BackupError
import com.example.sarisari.manager.domain.model.BackupOutcome
import com.example.sarisari.manager.domain.model.CostingMethod
import com.example.sarisari.manager.domain.model.ExportOutcome
import com.example.sarisari.manager.domain.model.RestoreResult
import com.example.sarisari.manager.domain.model.StoreSettings
import com.example.sarisari.manager.domain.repository.BackupRepository
import com.example.sarisari.manager.domain.repository.ExportRepository
import com.example.sarisari.manager.domain.repository.SettingsRepository
import com.example.sarisari.manager.domain.usecase.ReportPeriod
import com.example.sarisari.manager.domain.usecase.isAfterNow
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.Clock
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settings: SettingsRepository,
    private val backups: BackupRepository,
    private val exports: ExportRepository,
    private val clock: Clock,
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val name = settings.getSetting(StoreSettings.STORE_NAME_KEY)?.value.orEmpty()
            val method = CostingMethod.fromStorage(settings.getSetting(CostingMethod.SETTING_KEY)?.value)
            val restoreMessage = when (backups.takeLastRestoreResult()) {
                RestoreResult.APPLIED -> SettingsMessage.RestoreApplied
                RestoreResult.ROLLED_BACK -> SettingsMessage.RestoreRolledBack
                null -> null
            }
            val month = ReportPeriod.thisMonth(clock)
            _uiState.update {
                it.copy(
                    isLoading = false,
                    storeNameInput = name,
                    costingMethod = method,
                    message = restoreMessage,
                    exportMonth = month,
                    canGoNextMonth = !month.next().isAfterNow(clock),
                )
            }
        }
    }

    fun onMessageShown() = _uiState.update { it.copy(message = null) }

    fun onStoreNameChange(value: String) = _uiState.update { it.copy(storeNameInput = value) }

    fun onSaveStoreName() {
        val name = StoreSettings.normalizeStoreName(_uiState.value.storeNameInput)
        if (name == null) {
            _uiState.update { it.copy(message = SettingsMessage.StoreNameInvalid) }
            return
        }
        viewModelScope.launch {
            settings.putSetting(StoreSettings.STORE_NAME_KEY, name)
            _uiState.update { it.copy(storeNameInput = name, message = SettingsMessage.StoreNameSaved) }
        }
    }

    /** Called after the owner confirms that the change affects only future receipts and sales. */
    fun onCostingMethodConfirmed(method: CostingMethod) {
        viewModelScope.launch {
            settings.putSetting(CostingMethod.SETTING_KEY, method.storageValue)
            _uiState.update { it.copy(costingMethod = method, message = SettingsMessage.CostingChanged) }
        }
    }

    fun suggestedBackupName(): String = "sarisari-backup-${Dates.fileStamp(clock.millis())}.zip"

    fun onBackupDestination(uri: String) = runTask(SettingsTask.BACKUP) {
        when (backups.createBackup(uri)) {
            is BackupOutcome.Success -> it.copy(message = SettingsMessage.BackupSaved)
            is BackupOutcome.Failure -> it.copy(message = SettingsMessage.BackupFailed)
        }
    }

    fun onRestoreSource(uri: String) = runTask(SettingsTask.CHECK_RESTORE) {
        when (val outcome = backups.prepareRestore(uri)) {
            is BackupOutcome.Success -> it.copy(pendingRestore = outcome.info)
            is BackupOutcome.Failure -> it.copy(message = SettingsMessage.RestoreRejected(outcome.error))
        }
    }

    fun onRestoreConfirmed() {
        viewModelScope.launch {
            val staged = backups.confirmRestore()
            _uiState.update {
                it.copy(
                    pendingRestore = null,
                    restoreStaged = staged,
                    message = if (staged) null else SettingsMessage.RestoreRolledBack,
                )
            }
        }
    }

    fun onRestoreCancelled() {
        viewModelScope.launch {
            backups.discardPendingRestore()
            _uiState.update { it.copy(pendingRestore = null) }
        }
    }

    fun onPreviousMonth() = _uiState.update { state ->
        val month = state.exportMonth?.previous() as? ReportPeriod.Month ?: return@update state
        state.copy(exportMonth = month, canGoNextMonth = true)
    }

    fun onNextMonth() = _uiState.update { state ->
        val month = state.exportMonth?.next() as? ReportPeriod.Month ?: return@update state
        if (month.isAfterNow(clock)) state else state.copy(exportMonth = month, canGoNextMonth = !month.next().isAfterNow(clock))
    }

    fun suggestedSalesExportName(): String {
        val month = _uiState.value.exportMonth?.month ?: ReportPeriod.thisMonth(clock).month
        return "sales-$month.csv"
    }

    fun suggestedProductsExportName(): String =
        "products-inventory-${Dates.fileStamp(clock.millis())}.csv"

    fun onSalesExportDestination(uri: String) {
        val month = _uiState.value.exportMonth ?: return
        runTask(SettingsTask.EXPORT) { state ->
            state.copy(message = exportMessage(exports.exportSales(uri, month.startMillis, month.endMillis)))
        }
    }

    fun onProductsExportDestination(uri: String) = runTask(SettingsTask.EXPORT) {
        it.copy(message = exportMessage(exports.exportProducts(uri)))
    }

    fun suggestedUtangBalancesExportName(): String = "utang-balances-${Dates.fileStamp(clock.millis())}.csv"

    fun suggestedUtangHistoryExportName(): String = "utang-history-${Dates.fileStamp(clock.millis())}.csv"

    fun onUtangBalancesExportDestination(uri: String) = runTask(SettingsTask.EXPORT) {
        it.copy(message = exportMessage(exports.exportUtangBalances(uri)))
    }

    fun onUtangHistoryExportDestination(uri: String) = runTask(SettingsTask.EXPORT) {
        it.copy(message = exportMessage(exports.exportUtangHistory(uri)))
    }

    private fun exportMessage(outcome: ExportOutcome) = when (outcome) {
        is ExportOutcome.Success -> SettingsMessage.ExportDone(outcome.rowCount)
        ExportOutcome.Failure -> SettingsMessage.ExportFailed
    }

    /** Runs one long task at a time and shows progress while it runs. */
    private fun runTask(task: SettingsTask, work: suspend (SettingsUiState) -> SettingsUiState) {
        if (_uiState.value.isBusy) return
        _uiState.update { it.copy(task = task, message = null) }
        viewModelScope.launch {
            val result = try {
                work(_uiState.value)
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                _uiState.value.copy(message = failureMessage(task))
            }
            _uiState.value = result.copy(task = null)
        }
    }

    private fun failureMessage(task: SettingsTask): SettingsMessage = when (task) {
        SettingsTask.BACKUP -> SettingsMessage.BackupFailed
        SettingsTask.CHECK_RESTORE -> SettingsMessage.RestoreRejected(BackupError.UNREADABLE)
        SettingsTask.EXPORT -> SettingsMessage.ExportFailed
    }
}
