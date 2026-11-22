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
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.sarisari.manager.R
import com.example.sarisari.manager.core.designsystem.AppHeader
import com.example.sarisari.manager.core.designsystem.BrandFilterChip
import com.example.sarisari.manager.core.designsystem.MetricCard
import com.example.sarisari.manager.core.designsystem.PeriodSelector
import com.example.sarisari.manager.core.designsystem.ProductAvatar
import com.example.sarisari.manager.core.designsystem.SariSariManagerTheme
import com.example.sarisari.manager.core.designsystem.SectionHeader
import com.example.sarisari.manager.core.designsystem.SurfaceCard
import com.example.sarisari.manager.core.designsystem.TopProductRow
import com.example.sarisari.manager.core.util.Dates
import com.example.sarisari.manager.core.util.Money
import com.example.sarisari.manager.domain.model.CreditSummary
import com.example.sarisari.manager.domain.model.Customer
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
    Column(modifier = Modifier.fillMaxSize()) {
        AppHeader(title = stringResource(R.string.reports_title))
        Column(
            modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                BrandFilterChip(uiState.isDaily, stringResource(R.string.reports_daily), onDailySelected)
                BrandFilterChip(!uiState.isDaily, stringResource(R.string.reports_monthly), onMonthlySelected)
            }
            val period = uiState.period
            if (uiState.isLoading || period == null) {
                Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
                return@Column
            }
            PeriodSelector(label = periodLabel(period), onPrevious = onPrevious, onNext = onNext, canGoNext = uiState.canGoNext)
            SalesHero(uiState.summary)
            SalesBreakdown(uiState.summary)
            ProfitSection(uiState.summary)
            UtangSection(uiState.summary, uiState.utang)
            TopProductsSection(uiState.topProducts)
            Text(
                stringResource(R.string.reports_note),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

private fun periodLabel(period: ReportPeriod): String = when (period) {
    is ReportPeriod.Day -> Dates.formatDate(period.startMillis)
    is ReportPeriod.Month -> Dates.formatMonth(period.month)
}

@Composable
private fun SalesHero(summary: ReportSummary) {
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        SurfaceCard(
            modifier = Modifier.weight(1.3f),
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
            border = false,
        ) {
            Text(Money.format(summary.revenueCentavos), style = MaterialTheme.typography.headlineSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(stringResource(R.string.reports_total_sales), style = MaterialTheme.typography.labelLarge)
        }
        MetricCard(
            iconRes = R.drawable.ic_cart,
            value = summary.transactionCount.toString(),
            label = stringResource(R.string.reports_transactions),
            modifier = Modifier.weight(1f),
        )
    }
}

/** Total sales split into what was paid in cash and what was sold on utang. */
@Composable
private fun SalesBreakdown(summary: ReportSummary) {
    SurfaceCard(modifier = Modifier.fillMaxWidth()) {
        Text(stringResource(R.string.reports_sales_breakdown), style = MaterialTheme.typography.titleMedium)
        Column(modifier = Modifier.padding(top = 8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            AmountLine(R.string.reports_cash_sales, summary.cashSalesCentavos)
            AmountLine(R.string.reports_utang_sales, summary.creditSalesCentavos, color = MaterialTheme.colorScheme.tertiary)
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            AmountLine(R.string.reports_total_sales, summary.revenueCentavos, bold = true)
        }
    }
}

@Composable
private fun ProfitSection(summary: ReportSummary) {
    SurfaceCard(modifier = Modifier.fillMaxWidth()) {
        Text(stringResource(R.string.reports_profit_title), style = MaterialTheme.typography.titleMedium)
        Column(modifier = Modifier.padding(top = 8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            AmountLine(R.string.reports_total_sales, summary.revenueCentavos)
            AmountLine(R.string.reports_cogs, -summary.costOfGoodsSoldCentavos)
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            AmountLine(R.string.reports_gross_profit, summary.grossProfitCentavos, bold = true)
            AmountLine(R.string.reports_expenses, -summary.expensesCentavos)
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            AmountLine(R.string.reports_net_profit, summary.netProfitCentavos, bold = true)
        }
    }
}

/** The separate utang report: utang given and collected in the period, and who owes what now. */
@Composable
private fun UtangSection(summary: ReportSummary, utang: CreditSummary) {
    SectionHeader(stringResource(R.string.reports_utang_title))
    SurfaceCard(modifier = Modifier.fillMaxWidth()) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            AmountLine(R.string.reports_utang_given, summary.creditSalesCentavos)
            AmountLine(R.string.reports_utang_collected, utang.collectedCentavos, color = MaterialTheme.colorScheme.primary)
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            AmountLine(R.string.reports_utang_outstanding, utang.outstandingCentavos, bold = true, color = MaterialTheme.colorScheme.error)
        }
    }
    SurfaceCard(modifier = Modifier.fillMaxWidth()) {
        Text(stringResource(R.string.reports_utang_debtors), style = MaterialTheme.typography.titleMedium)
        if (utang.topDebtors.isEmpty()) {
            Text(
                stringResource(R.string.reports_utang_none),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 8.dp),
            )
        }
        utang.topDebtors.forEach { DebtorRow(it) }
    }
}

@Composable
private fun DebtorRow(customer: Customer) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(top = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        ProductAvatar(customer.name, size = 36.dp)
        Column(modifier = Modifier.weight(1f)) {
            Text(customer.name, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
            customer.lastCreditAtMillis?.let {
                Text(
                    stringResource(R.string.utang_last_credit, Dates.formatDate(it)),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            customer.lastPaymentAtMillis?.let {
                Text(
                    stringResource(R.string.utang_last_payment, Dates.formatDate(it)),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Text(Money.format(customer.balanceCentavos), style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.error)
    }
}

@Composable
private fun AmountLine(labelRes: Int, centavos: Long, bold: Boolean = false, color: Color = Color.Unspecified) {
    val style = if (bold) MaterialTheme.typography.titleMedium else MaterialTheme.typography.bodyLarge
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(stringResource(labelRes), style = style)
        Text(
            Money.format(centavos),
            style = style,
            fontWeight = if (bold) FontWeight.Bold else null,
            color = when {
                color != Color.Unspecified -> color
                centavos < 0 && bold -> MaterialTheme.colorScheme.error
                else -> MaterialTheme.colorScheme.onSurface
            },
        )
    }
}

@Composable
private fun TopProductsSection(products: List<TopProduct>) {
    SurfaceCard(modifier = Modifier.fillMaxWidth()) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(stringResource(R.string.reports_top_products), style = MaterialTheme.typography.titleMedium)
            if (products.isEmpty()) {
                Text(stringResource(R.string.reports_no_sales), style = MaterialTheme.typography.bodyMedium)
            }
            products.forEachIndexed { index, product -> TopProductRow(index + 1, product) }
        }
    }
}

@Preview(showBackground = true, heightDp = 1400)
@Composable
private fun ReportsPreview() {
    SariSariManagerTheme {
        ReportsContent(
            ReportsUiState(
                isLoading = false,
                period = ReportPeriod.Day(LocalDate.of(2025, 11, 18)),
                summary = ReportSummary(485_000, 393_000, 50_000, 48, creditSalesCentavos = 35_000),
                topProducts = listOf(TopProduct(1, "Coke 1.5L", "bottle", 32, 208_000)),
                utang = CreditSummary(
                    outstandingCentavos = 92_000,
                    collectedCentavos = 15_000,
                    topDebtors = listOf(Customer(1, "Juan Dela Cruz", 35_000, 1_763_420_000_000, null)),
                ),
            ),
            onDailySelected = {}, onMonthlySelected = {}, onPrevious = {}, onNext = {},
        )
    }
}
