package com.example.sarisari.manager.feature.reports

import com.example.sarisari.manager.domain.model.ReportSummary
import com.example.sarisari.manager.domain.model.TopProduct
import com.example.sarisari.manager.domain.usecase.ReportPeriod

data class ReportsUiState(
    val isLoading: Boolean = true,
    val period: ReportPeriod? = null,
    val canGoNext: Boolean = false,
    val summary: ReportSummary = ReportSummary(),
    val topProducts: List<TopProduct> = emptyList(),
) {
    val isDaily: Boolean get() = period is ReportPeriod.Day
}
