package com.example.sarisari.manager.feature.dashboard

import com.example.sarisari.manager.domain.model.ReportSummary
import com.example.sarisari.manager.domain.model.StoreSession
import com.example.sarisari.manager.domain.model.TopProduct

data class DashboardUiState(
    val isLoading: Boolean = true,
    /** Null until the owner sets a name in Settings. */
    val storeName: String? = null,
    val session: StoreSession? = null,
    /** True when the open session started on an earlier day than today. */
    val sessionFromEarlierDay: Boolean = false,
    val todayStartMillis: Long = 0,
    val today: ReportSummary = ReportSummary(),
    val lowStockCount: Int = 0,
    val topProducts: List<TopProduct> = emptyList(),
    val showCloseDialog: Boolean = false,
    val closeTimeInvalid: Boolean = false,
)
