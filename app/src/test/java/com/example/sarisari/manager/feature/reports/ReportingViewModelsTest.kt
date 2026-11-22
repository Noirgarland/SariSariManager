package com.example.sarisari.manager.feature.reports

import com.example.sarisari.manager.domain.model.ExpenseCategory
import com.example.sarisari.manager.domain.model.ReportSummary
import com.example.sarisari.manager.domain.model.StoreSettings
import com.example.sarisari.manager.domain.usecase.AddExpenseUseCase
import com.example.sarisari.manager.domain.usecase.ExpenseError
import com.example.sarisari.manager.domain.usecase.ReportPeriod
import com.example.sarisari.manager.domain.usecase.StoreSessionUseCase
import com.example.sarisari.manager.R
import com.example.sarisari.manager.feature.dashboard.DashboardViewModel
import com.example.sarisari.manager.feature.dashboard.StoreTimeAction
import com.example.sarisari.manager.feature.dashboard.StoreTimeDialogState
import com.example.sarisari.manager.feature.expenses.ExpensesViewModel
import com.example.sarisari.manager.testing.FakeClock
import com.example.sarisari.manager.testing.FakeCustomerRepository
import com.example.sarisari.manager.testing.FakeExpenseRepository
import com.example.sarisari.manager.testing.FakeProductRepository
import com.example.sarisari.manager.testing.FakeReportRepository
import com.example.sarisari.manager.testing.FakeSettingsRepository
import com.example.sarisari.manager.testing.FakeStoreSessionRepository
import com.example.sarisari.manager.testing.MainDispatcherRule
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.YearMonth
import java.time.ZoneOffset
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/** Covers the Reports, Expenses, and Dashboard ViewModels. The clock is 10:00 AM, Nov 18 2025, Manila. */
@OptIn(ExperimentalCoroutinesApi::class)
class ReportingViewModelsTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val clock = FakeClock(Instant.parse("2025-11-18T02:00:00Z"))
    private val todayDate = LocalDate.of(2025, 11, 18)

    @Test
    fun reportsStartOnTodayAndCannotMoveIntoTheFuture() = runTest {
        val reports = FakeReportRepository()
        val today = ReportPeriod.Day(todayDate)
        reports.summaries.value = mapOf(today.startMillis to ReportSummary(10_000, 6_000, 1_000, 3))
        val vm = ReportsViewModel(reports, FakeCustomerRepository(), clock)
        vm.uiState.launchIn(backgroundScope)
        advanceUntilIdle()
        assertEquals(today, vm.uiState.value.period)
        assertEquals(4_000L, vm.uiState.value.summary.grossProfitCentavos)
        assertEquals(3_000L, vm.uiState.value.summary.netProfitCentavos)
        assertFalse(vm.uiState.value.canGoNext)

        vm.onNext()
        advanceUntilIdle()
        assertEquals(today, vm.uiState.value.period)
        vm.onPrevious()
        advanceUntilIdle()
        assertEquals(ReportPeriod.Day(todayDate.minusDays(1)), vm.uiState.value.period)
        assertTrue(vm.uiState.value.canGoNext)
        assertEquals(ReportSummary(), vm.uiState.value.summary)

        vm.onMonthlySelected()
        advanceUntilIdle()
        assertEquals(ReportPeriod.Month(YearMonth.of(2025, 11)), vm.uiState.value.period)
        vm.onDailySelected()
        advanceUntilIdle()
        assertEquals(today, vm.uiState.value.period)
    }

    @Test
    fun expensesAreValidatedSavedAndDeleted() = runTest {
        val repo = FakeExpenseRepository()
        val vm = ExpensesViewModel(repo, AddExpenseUseCase(repo, clock), clock)
        vm.uiState.launchIn(backgroundScope)
        advanceUntilIdle()
        vm.onAddClicked()
        vm.onSave()
        advanceUntilIdle()
        assertEquals(ExpenseError.AMOUNT_INVALID, vm.uiState.value.form?.error)

        vm.onCategoryChange(ExpenseCategory.RENT)
        vm.onAmountChange("1,500.50")
        vm.onSave()
        advanceUntilIdle()
        assertNull(vm.uiState.value.form)
        val saved = repo.expenses.value.single()
        assertEquals(150_050L, saved.amountCentavos)
        assertEquals(clock.millis(), saved.spentAtMillis)
        assertEquals(150_050L, vm.uiState.value.totalCentavos)

        vm.onAddClicked()
        vm.onAmountChange("100")
        vm.onDatePicked(LocalDate.of(2025, 11, 2).atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli())
        vm.onSave()
        advanceUntilIdle()
        // Earlier days are recorded at noon Manila time, which is 04:00 UTC.
        assertEquals(Instant.parse("2025-11-02T04:00:00Z").toEpochMilli(), repo.expenses.value.last().spentAtMillis)

        vm.onDelete(saved.id)
        advanceUntilIdle()
        assertEquals(1, vm.uiState.value.expenses.size)
    }

    private fun dashboard(
        sessions: FakeStoreSessionRepository,
        settings: FakeSettingsRepository = FakeSettingsRepository(),
    ) = DashboardViewModel(
        FakeReportRepository(), FakeProductRepository(), settings, sessions, FakeCustomerRepository(),
        StoreSessionUseCase(sessions, clock), clock,
    )

    private fun TestScope.start(vm: DashboardViewModel) {
        vm.uiState.launchIn(backgroundScope)
        advanceUntilIdle()
    }

    @Test
    fun dashboardOpensAndClosesTheStoreWithTheClockDialog() = runTest {
        val sessions = FakeStoreSessionRepository()
        val vm = dashboard(sessions, FakeSettingsRepository(mapOf(StoreSettings.STORE_NAME_KEY to "Aling Rosa")))
        start(vm)
        assertEquals("Aling Rosa", vm.uiState.value.storeName)
        assertNull(vm.uiState.value.session)

        // 10:00 AM Manila. The clock starts at now.
        vm.onOpenStoreClicked()
        advanceUntilIdle()
        assertEquals(StoreTimeDialogState(StoreTimeAction.OPEN, LocalTime.of(10, 0)), vm.uiState.value.timeDialog)
        vm.onTimeConfirmed(LocalTime.of(10, 30)) // later than now
        advanceUntilIdle()
        assertEquals(R.string.store_time_error_future, vm.uiState.value.timeDialog?.errorRes)
        assertNull(vm.uiState.value.session)
        vm.onTimeConfirmed(LocalTime.of(9, 15))
        advanceUntilIdle()
        assertNull(vm.uiState.value.timeDialog)
        assertEquals(Instant.parse("2025-11-18T01:15:00Z").toEpochMilli(), vm.uiState.value.session!!.openedAtMillis)
        assertFalse(vm.uiState.value.sessionFromEarlierDay)

        clock.set(Instant.parse("2025-11-18T12:00:00Z")) // 8:00 PM Manila
        vm.onCloseStoreClicked()
        advanceUntilIdle()
        assertEquals(LocalTime.of(20, 0), vm.uiState.value.timeDialog?.initialTime)
        vm.onTimeConfirmed(LocalTime.of(21, 0)) // in the future
        advanceUntilIdle()
        assertEquals(R.string.store_time_error_future, vm.uiState.value.timeDialog?.errorRes)
        assertTrue(vm.uiState.value.session!!.isOpen)
        vm.onTimeConfirmed(LocalTime.of(19, 30))
        advanceUntilIdle()
        assertNull(vm.uiState.value.timeDialog)
        assertEquals(Instant.parse("2025-11-18T11:30:00Z").toEpochMilli(), vm.uiState.value.session!!.closedAtMillis)

        // Reopening before the last close is refused with a message; after it, it works.
        vm.onOpenStoreClicked()
        vm.onTimeConfirmed(LocalTime.of(19, 0))
        advanceUntilIdle()
        assertEquals(R.string.store_time_error_before_last_close, vm.uiState.value.timeDialog?.errorRes)
        vm.onTimeConfirmed(LocalTime.of(20, 0))
        advanceUntilIdle()
        assertNull(vm.uiState.value.timeDialog)
        assertTrue(vm.uiState.value.session!!.isOpen)
    }

    @Test
    fun cancellingTheDialogChangesNothing() = runTest {
        val sessions = FakeStoreSessionRepository()
        val vm = dashboard(sessions)
        start(vm)
        vm.onOpenStoreClicked()
        vm.onTimeDialogDismissed()
        advanceUntilIdle()
        assertNull(vm.uiState.value.timeDialog)
        assertNull(vm.uiState.value.session)
    }

    @Test
    fun closingAfterMidnightIsRecordedOnTheNextDay() = runTest {
        val sessions = FakeStoreSessionRepository()
        sessions.open(Instant.parse("2025-11-18T12:00:00Z").toEpochMilli()) // 8:00 PM Nov 18 Manila
        clock.set(Instant.parse("2025-11-18T17:30:00Z")) // 1:30 AM Nov 19 Manila
        val vm = dashboard(sessions)
        start(vm)
        vm.onCloseStoreClicked()
        vm.onTimeConfirmed(LocalTime.of(1, 0))
        advanceUntilIdle()
        assertNull(vm.uiState.value.timeDialog)
        assertEquals(Instant.parse("2025-11-18T17:00:00Z").toEpochMilli(), vm.uiState.value.session!!.closedAtMillis)
    }

    @Test
    fun forgottenSessionIsDetectedAndClosedOnItsOwnDay() = runTest {
        val sessions = FakeStoreSessionRepository()
        sessions.open(Instant.parse("2025-11-16T23:00:00Z").toEpochMilli()) // 7:00 AM Nov 17 Manila
        val vm = dashboard(sessions)
        start(vm)
        assertNull(vm.uiState.value.storeName)
        assertTrue(vm.uiState.value.sessionFromEarlierDay)
        vm.onCloseStoreClicked()
        advanceUntilIdle()
        assertEquals(LocalTime.of(21, 0), vm.uiState.value.timeDialog?.initialTime)
        vm.onTimeConfirmed(LocalTime.of(21, 0))
        advanceUntilIdle()
        assertEquals(Instant.parse("2025-11-17T13:00:00Z").toEpochMilli(), vm.uiState.value.session!!.closedAtMillis)
    }
}