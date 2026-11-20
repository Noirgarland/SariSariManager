package com.example.sarisari.manager.feature.settings

import com.example.sarisari.manager.domain.model.BackupError
import com.example.sarisari.manager.domain.model.BackupInfo
import com.example.sarisari.manager.domain.model.BackupOutcome
import com.example.sarisari.manager.domain.model.CostingMethod
import com.example.sarisari.manager.domain.model.ExportOutcome
import com.example.sarisari.manager.domain.model.RestoreResult
import com.example.sarisari.manager.domain.model.StoreSettings
import com.example.sarisari.manager.domain.repository.BackupRepository
import com.example.sarisari.manager.domain.repository.ExportRepository
import com.example.sarisari.manager.domain.usecase.ReportPeriod
import com.example.sarisari.manager.testing.FakeClock
import com.example.sarisari.manager.testing.FakeSettingsRepository
import com.example.sarisari.manager.testing.MainDispatcherRule
import java.time.Instant
import java.time.YearMonth
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class SettingsViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val clock = FakeClock(Instant.parse("2025-11-18T02:00:00Z"))
    private val info = BackupInfo(createdAtMillis = 1_700_000_000_000, schemaVersion = 5, appVersion = "1.0")

    private class FakeBackups : BackupRepository {
        var createOutcome: BackupOutcome = BackupOutcome.Success(BackupInfo(1, 5, "1.0"))
        var prepareOutcome: BackupOutcome = BackupOutcome.Failure(BackupError.UNREADABLE)
        var lastResult: RestoreResult? = null
        var throwOnCreate = false
        var pending = false
        var staged = false
        override suspend fun createBackup(destinationUri: String): BackupOutcome {
            if (throwOnCreate) error("disk full")
            return createOutcome
        }
        override suspend fun prepareRestore(sourceUri: String): BackupOutcome =
            prepareOutcome.also { pending = it is BackupOutcome.Success }
        override suspend fun confirmRestore(): Boolean = pending.also { staged = it; pending = false }
        override suspend fun discardPendingRestore() { pending = false }
        override suspend fun takeLastRestoreResult(): RestoreResult? = lastResult.also { lastResult = null }
    }

    private class FakeExports : ExportRepository {
        val salesRanges = mutableListOf<Pair<Long, Long>>()
        override suspend fun exportSales(destinationUri: String, startMillis: Long, endMillis: Long): ExportOutcome {
            salesRanges += startMillis to endMillis
            return ExportOutcome.Success(42)
        }
        override suspend fun exportProducts(destinationUri: String): ExportOutcome = ExportOutcome.Failure
    }

    private val settings = FakeSettingsRepository(
        mapOf(StoreSettings.STORE_NAME_KEY to "Aling Rosa", CostingMethod.SETTING_KEY to "latest_purchase")
    )
    private val backups = FakeBackups()
    private val exports = FakeExports()
    private fun viewModel() = SettingsViewModel(settings, backups, exports, clock)

    @Test
    fun loadsSavedSettingsAndReportsTheLastRestoreOnce() = runTest {
        backups.lastResult = RestoreResult.ROLLED_BACK
        val state = viewModel().uiState.value
        assertFalse(state.isLoading)
        assertEquals("Aling Rosa", state.storeNameInput)
        assertEquals(CostingMethod.LATEST_PURCHASE, state.costingMethod)
        assertEquals(SettingsMessage.RestoreRolledBack, state.message)
        assertEquals(ReportPeriod.Month(YearMonth.of(2025, 11)), state.exportMonth)
        assertNull(viewModel().uiState.value.message)
    }

    @Test
    fun storeNameIsTrimmedAndValidated() = runTest {
        val vm = viewModel()
        vm.onStoreNameChange("   ")
        vm.onSaveStoreName()
        assertEquals(SettingsMessage.StoreNameInvalid, vm.uiState.value.message)
        vm.onStoreNameChange("  Tindahan ni Aling Nena  ")
        vm.onSaveStoreName()
        assertEquals("Tindahan ni Aling Nena", settings.getSetting(StoreSettings.STORE_NAME_KEY)?.value)
        assertEquals(SettingsMessage.StoreNameSaved, vm.uiState.value.message)
        vm.onMessageShown()
        assertNull(vm.uiState.value.message)
    }

    @Test
    fun costingMethodChangeIsSaved() = runTest {
        val vm = viewModel()
        vm.onCostingMethodConfirmed(CostingMethod.WEIGHTED_AVERAGE)
        assertEquals("weighted_average", settings.getSetting(CostingMethod.SETTING_KEY)?.value)
        assertEquals(CostingMethod.WEIGHTED_AVERAGE, vm.uiState.value.costingMethod)
    }

    @Test
    fun backupReportsSuccessFailureAndUnexpectedErrors() = runTest {
        val vm = viewModel()
        vm.onBackupDestination("content://x")
        assertEquals(SettingsMessage.BackupSaved, vm.uiState.value.message)
        assertFalse(vm.uiState.value.isBusy)
        backups.createOutcome = BackupOutcome.Failure(BackupError.WRITE_FAILED)
        vm.onBackupDestination("content://x")
        assertEquals(SettingsMessage.BackupFailed, vm.uiState.value.message)
        backups.throwOnCreate = true
        vm.onBackupDestination("content://x")
        assertEquals(SettingsMessage.BackupFailed, vm.uiState.value.message)
        assertFalse(vm.uiState.value.isBusy)
        assertTrue(vm.suggestedBackupName().startsWith("sarisari-backup-20251118-1000"))
    }

    @Test
    fun restoreNeedsConfirmationBeforeItIsStaged() = runTest {
        val vm = viewModel()
        vm.onRestoreSource("content://bad")
        assertEquals(SettingsMessage.RestoreRejected(BackupError.UNREADABLE), vm.uiState.value.message)
        assertNull(vm.uiState.value.pendingRestore)

        backups.prepareOutcome = BackupOutcome.Success(info)
        vm.onRestoreSource("content://good")
        assertEquals(info, vm.uiState.value.pendingRestore)
        assertFalse(backups.staged)
        vm.onRestoreCancelled()
        assertNull(vm.uiState.value.pendingRestore)
        assertFalse(backups.pending)

        vm.onRestoreSource("content://good")
        vm.onRestoreConfirmed()
        assertTrue(backups.staged)
        assertTrue(vm.uiState.value.restoreStaged)
        assertNull(vm.uiState.value.pendingRestore)
    }

    @Test
    fun exportsUseTheChosenMonthAndCannotPickAFutureMonth() = runTest {
        val vm = viewModel()
        assertFalse(vm.uiState.value.canGoNextMonth)
        vm.onNextMonth()
        assertEquals(ReportPeriod.Month(YearMonth.of(2025, 11)), vm.uiState.value.exportMonth)
        vm.onPreviousMonth()
        assertEquals("sales-2025-10.csv", vm.suggestedSalesExportName())
        vm.onSalesExportDestination("content://csv")
        val october = ReportPeriod.Month(YearMonth.of(2025, 10))
        assertEquals(listOf(october.startMillis to october.endMillis), exports.salesRanges)
        assertEquals(SettingsMessage.ExportDone(42), vm.uiState.value.message)
        vm.onProductsExportDestination("content://csv")
        assertEquals(SettingsMessage.ExportFailed, vm.uiState.value.message)
    }
}
