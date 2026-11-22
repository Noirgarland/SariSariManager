package com.example.sarisari.manager.feature.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.sarisari.manager.R
import com.example.sarisari.manager.domain.model.StockStatus
import com.example.sarisari.manager.domain.model.StoreSettings
import com.example.sarisari.manager.domain.model.StoreTime
import com.example.sarisari.manager.domain.repository.ProductRepository
import com.example.sarisari.manager.domain.repository.ReportRepository
import com.example.sarisari.manager.domain.repository.SettingsRepository
import com.example.sarisari.manager.domain.repository.StoreSessionRepository
import com.example.sarisari.manager.domain.usecase.ReportPeriod
import com.example.sarisari.manager.domain.usecase.StoreSessionResult
import com.example.sarisari.manager.domain.usecase.StoreSessionUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.Clock
import java.time.LocalTime
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class DashboardViewModel @Inject constructor(
    reports: ReportRepository,
    products: ProductRepository,
    private val settings: SettingsRepository,
    sessions: StoreSessionRepository,
    private val storeSession: StoreSessionUseCase,
    private val clock: Clock,
) : ViewModel() {

    private val today = MutableStateFlow(ReportPeriod.today(clock))
    private val timeDialog = MutableStateFlow<StoreTimeDialogState?>(null)

    private val todayData = today.flatMapLatest { day ->
        combine(
            reports.observeSummary(day.startMillis, day.endMillis),
            reports.observeTopProducts(day.startMillis, day.endMillis, TOP_PRODUCT_LIMIT),
        ) { summary, top -> Triple(day, summary, top) }
    }

    private val lowStockCount = products.observeProducts("", categoryId = null, includeArchived = false)
        .map { list -> list.count { it.stockStatus != StockStatus.IN_STOCK } }

    val uiState: StateFlow<DashboardUiState> = combine(
        todayData,
        lowStockCount,
        settings.observeSetting(StoreSettings.STORE_NAME_KEY),
        sessions.observeLatestSession(),
        timeDialog,
    ) { (day, summary, top), lowStock, name, session, dialog ->
        DashboardUiState(
            isLoading = false,
            storeName = name?.value?.takeIf { it.isNotBlank() },
            session = session,
            sessionFromEarlierDay = session?.isOpen == true && session.openedAtMillis < day.startMillis,
            todayStartMillis = day.startMillis,
            today = summary,
            lowStockCount = lowStock,
            topProducts = top,
            timeDialog = dialog,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), DashboardUiState())

    /** Called when the screen is shown, so "today" moves forward after midnight. */
    fun onResume() = today.update { current -> ReportPeriod.today(clock).takeIf { it != current } ?: current }

    /**
     * Saves a new store name, using the same rules as Settings.
     * Returns false without saving when the name is blank or too long.
     */
    fun onRenameStore(input: String): Boolean {
        val name = StoreSettings.normalizeStoreName(input) ?: return false
        viewModelScope.launch { settings.putSetting(StoreSettings.STORE_NAME_KEY, name) }
        return true
    }

    /** Shows the clock set to now. */
    fun onOpenStoreClicked() = timeDialog.update { StoreTimeDialogState(StoreTimeAction.OPEN, now()) }

    /** Shows the clock set to now, or to 9:00 PM for a session left open since an earlier day. */
    fun onCloseStoreClicked() {
        val initial = if (uiState.value.sessionFromEarlierDay) LocalTime.of(DEFAULT_FORGOTTEN_CLOSE_HOUR, 0) else now()
        timeDialog.update { StoreTimeDialogState(StoreTimeAction.CLOSE, initial) }
    }

    fun onTimeDialogDismissed() = timeDialog.update { null }

    /** Saves the chosen opening or closing time. The dialog stays open with a message if the time is not valid. */
    fun onTimeConfirmed(time: LocalTime) {
        val dialog = timeDialog.value ?: return
        if (dialog.isSaving) return
        timeDialog.update { it?.copy(isSaving = true, errorRes = null, initialTime = time) }
        viewModelScope.launch {
            val result = try {
                when (dialog.action) {
                    StoreTimeAction.OPEN -> storeSession.openAt(time)
                    StoreTimeAction.CLOSE -> {
                        val session = uiState.value.session
                        if (session == null) StoreSessionResult.NOT_OPEN else storeSession.closeAt(session, time)
                    }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                null
            }
            timeDialog.update { current ->
                when (result) {
                    // ALREADY_OPEN and NOT_OPEN mean the status already changed; the header shows it.
                    StoreSessionResult.DONE, StoreSessionResult.ALREADY_OPEN, StoreSessionResult.NOT_OPEN -> null
                    else -> current?.copy(isSaving = false, errorRes = result.messageRes())
                }
            }
        }
    }

    private fun StoreSessionResult?.messageRes(): Int = when (this) {
        StoreSessionResult.TIME_IN_FUTURE -> R.string.store_time_error_future
        StoreSessionResult.BEFORE_OPENING -> R.string.store_time_error_before_open
        StoreSessionResult.BEFORE_LAST_CLOSE -> R.string.store_time_error_before_last_close
        else -> R.string.store_time_error_failed
    }

    /** The current store time to the minute, for the dialog's Now button. */
    fun now(): LocalTime = clock.instant().atZone(StoreTime.ZONE).toLocalTime().withSecond(0).withNano(0)

    private companion object {
        const val STOP_TIMEOUT_MILLIS = 5_000L
        const val TOP_PRODUCT_LIMIT = 3
        const val DEFAULT_FORGOTTEN_CLOSE_HOUR = 21
    }
}
