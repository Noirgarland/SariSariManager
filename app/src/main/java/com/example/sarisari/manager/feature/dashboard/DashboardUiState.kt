package com.example.sarisari.manager.feature.dashboard

import androidx.annotation.StringRes
import com.example.sarisari.manager.domain.model.ReportSummary
import com.example.sarisari.manager.domain.model.StoreSession
import com.example.sarisari.manager.domain.model.TopProduct
import java.time.LocalTime

enum class StoreTimeAction { OPEN, CLOSE }

/** The clock dialog for choosing when the store opened or closed. */
data class StoreTimeDialogState(
    val action: StoreTimeAction,
    /** Where the clock hands start. */
    val initialTime: LocalTime,
    @param:StringRes val errorRes: Int? = null,
    val isSaving: Boolean = false,
)

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
    /** What all customers owe right now. */
    val outstandingUtangCentavos: Long = 0,
    val topProducts: List<TopProduct> = emptyList(),
    /** Null when no time dialog is shown. */
    val timeDialog: StoreTimeDialogState? = null,
)
