package com.example.sarisari.manager.feature.reports

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.sarisari.manager.R
import com.example.sarisari.manager.core.designsystem.PeriodSelector
import com.example.sarisari.manager.core.designsystem.SariSariManagerTheme
import com.example.sarisari.manager.core.designsystem.StatCard
import com.example.sarisari.manager.core.designsystem.TopProductRow
import com.example.sarisari.manager.core.util.Dates
import com.example.sarisari.manager.core.util.Money
import com.example.sarisari.manager.domain.model.ReportSummary
import com.example.sarisari.manager.domain.model.TopProduct
import com.example.sarisari.manager.domain.usecase.ReportPeriod
import java.time.LocalDate

@Composable
fun ReportsScreen(viewModel: ReportsViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    ReportsContent(
        uiState = uiState,
        onDailySelected = viewModel::onDailySelected,
        onMonthlySelected = viewModel::onMonthlySelected,
        onPrevious = viewModel::onPrevious,
        onNext = viewModel::onNext,
    )
}

@Composable
internal fun ReportsContent(
    uiState: ReportsUiState,
    onDailySelected: () -> Unit,
    onMonthlySelected: () -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(stringResource(R.string.reports_title), style = MaterialTheme.typography.headlineSmall)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(selected = uiState.isDaily, onClick = onDailySelected, label = { Text(stringResource(R.string.reports_daily)) })
            FilterChip(selected = !uiState.isDaily, onClick = onMonthlySelected, label = { Text(stringResource(R.string.reports_monthly)) })
        }
        val period = uiState.period
        if (uiState.isLoading || period == null) {
            Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            return@Column
        }
        PeriodSelector(label = periodLabel(period), onPrevious = onPrevious, onNext = onNext, canGoNext = uiState.canGoNext)
        SummarySection(uiState.summary)
        TopProductsSection(uiState.topProducts)
        Text(
            stringResource(R.string.reports_note),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

private fun periodLabel(period: ReportPeriod): String = when (period) {
    is ReportPeriod.Day -> Dates.formatDate(period.startMillis)
    is ReportPeriod.Month -> Dates.formatMonth(period.month)
}

@Composable
private fun SummarySection(summary: ReportSummary) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        StatCard(
            label = stringResource(R.string.reports_total_sales),
            value = Money.format(summary.revenueCentavos),
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
            modifier = Modifier.weight(1f),
        )
        StatCard(
            label = stringResource(R.string.reports_transactions),
            value = summary.transactionCount.toString(),
            modifier = Modifier.weight(1f),
        )
    }
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            AmountLine(R.string.reports_total_sales, summary.revenueCentavos)
            AmountLine(R.string.reports_cogs, -summary.costOfGoodsSoldCentavos)
            HorizontalDivider()
            AmountLine(R.string.reports_gross_profit, summary.grossProfitCentavos, bold = true)
            AmountLine(R.string.reports_expenses, -summary.expensesCentavos)
            HorizontalDivider()
            AmountLine(R.string.reports_net_profit, summary.netProfitCentavos, bold = true)
        }
    }
}

@Composable
private fun AmountLine(labelRes: Int, centavos: Long, bold: Boolean = false) {
    val style = if (bold) MaterialTheme.typography.titleMedium else MaterialTheme.typography.bodyLarge
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(stringResource(labelRes), style = style)
        Text(
            Money.format(centavos),
            style = style,
            fontWeight = if (bold) FontWeight.Bold else null,
            color = if (centavos < 0 && bold) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
        )
    }
}

@Composable
private fun TopProductsSection(products: List<TopProduct>) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(stringResource(R.string.reports_top_products), style = MaterialTheme.typography.titleMedium)
            if (products.isEmpty()) {
                Text(stringResource(R.string.reports_no_sales), style = MaterialTheme.typography.bodyMedium)
            }
            products.forEachIndexed { index, product ->
                TopProductRow(index + 1, product)
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun ReportsPreview() {
    SariSariManagerTheme {
        ReportsContent(
            ReportsUiState(
                isLoading = false,
                period = ReportPeriod.Day(LocalDate.of(2025, 11, 18)),
                summary = ReportSummary(485_000, 393_000, 50_000, 48),
                topProducts = listOf(TopProduct(1, "Coke 1.5L", "bottle", 32, 208_000)),
            ),
            onDailySelected = {}, onMonthlySelected = {}, onPrevious = {}, onNext = {},
        )
    }
}
