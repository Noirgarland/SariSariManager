package com.example.sarisari.manager.feature.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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
import java.time.Instant
import java.time.LocalTime
import javax.inject.Inject
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
    settings: SettingsRepository,
    sessions: StoreSessionRepository,
    private val storeSession: StoreSessionUseCase,
    private val clock: Clock,
) : ViewModel() {

    private data class Dialog(val show: Boolean = false, val invalid: Boolean = false)

    private val today = MutableStateFlow(ReportPeriod.today(clock))
    private val dialog = MutableStateFlow(Dialog())

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
        dialog,
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
            showCloseDialog = dialog.show,
            closeTimeInvalid = dialog.invalid,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), DashboardUiState())

    /** Called when the screen is shown, so "today" moves forward after midnight. */
    fun onResume() = today.update { current -> ReportPeriod.today(clock).takeIf { it != current } ?: current }

    fun onOpenStore() {
        viewModelScope.launch { storeSession.open() }
    }

    fun onCloseStoreClicked() = dialog.update { Dialog(show = true) }
    fun onCloseDialogDismissed() = dialog.update { Dialog() }

    /** Default for the time picker: now if the store opened today, otherwise 9:00 PM on the opening day. */
    fun defaultCloseTime(): LocalTime =
        if (uiState.value.sessionFromEarlierDay) {
            LocalTime.of(DEFAULT_FORGOTTEN_CLOSE_HOUR, 0)
        } else {
            clock.instant().atZone(StoreTime.ZONE).toLocalTime().withSecond(0).withNano(0)
        }

    /** Closes at [time] on the day the session was opened, in store time. */
    fun onCloseStoreConfirmed(time: LocalTime) {
        val session = uiState.value.session ?: return
        val openedDate = Instant.ofEpochMilli(session.openedAtMillis).atZone(StoreTime.ZONE).toLocalDate()
        val closedAt = openedDate.atTime(time).atZone(StoreTime.ZONE).toInstant().toEpochMilli()
        viewModelScope.launch {
            when (storeSession.close(session, closedAt)) {
                StoreSessionResult.DONE -> dialog.update { Dialog() }
                else -> dialog.update { Dialog(show = true, invalid = true) }
            }
        }
    }

    private companion object {
        const val STOP_TIMEOUT_MILLIS = 5_000L
        const val TOP_PRODUCT_LIMIT = 3
        const val DEFAULT_FORGOTTEN_CLOSE_HOUR = 21
    }
}
