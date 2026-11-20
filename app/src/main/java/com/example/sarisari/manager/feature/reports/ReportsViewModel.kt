package com.example.sarisari.manager.feature.reports

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.sarisari.manager.domain.repository.ReportRepository
import com.example.sarisari.manager.domain.usecase.ReportPeriod
import com.example.sarisari.manager.domain.usecase.isAfterNow
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.Clock
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class ReportsViewModel @Inject constructor(
    private val reports: ReportRepository,
    private val clock: Clock,
) : ViewModel() {

    private val period = MutableStateFlow<ReportPeriod>(ReportPeriod.today(clock))

    val uiState: StateFlow<ReportsUiState> = period.flatMapLatest { p ->
        combine(
            reports.observeSummary(p.startMillis, p.endMillis),
            reports.observeTopProducts(p.startMillis, p.endMillis, TOP_PRODUCT_LIMIT),
        ) { summary, top ->
            ReportsUiState(
                isLoading = false,
                period = p,
                canGoNext = !p.next().isAfterNow(clock),
                summary = summary,
                topProducts = top,
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), ReportsUiState())

    fun onDailySelected() = period.update { if (it is ReportPeriod.Day) it else ReportPeriod.today(clock) }
    fun onMonthlySelected() = period.update { if (it is ReportPeriod.Month) it else ReportPeriod.thisMonth(clock) }
    fun onPrevious() = period.update { it.previous() }
    fun onNext() = period.update { next -> next.next().takeUnless { it.isAfterNow(clock) } ?: next }

    private companion object {
        const val STOP_TIMEOUT_MILLIS = 5_000L
        const val TOP_PRODUCT_LIMIT = 5
    }
}
