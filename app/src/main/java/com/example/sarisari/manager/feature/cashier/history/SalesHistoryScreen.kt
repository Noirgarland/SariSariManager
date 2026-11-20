package com.example.sarisari.manager.feature.cashier.history

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.sarisari.manager.R
import com.example.sarisari.manager.core.designsystem.BackTopBar
import com.example.sarisari.manager.core.designsystem.SariSariManagerTheme
import com.example.sarisari.manager.core.util.Dates
import com.example.sarisari.manager.core.util.Money
import com.example.sarisari.manager.domain.model.Sale

@Composable
fun SalesHistoryScreen(
    onBack: () -> Unit,
    onOpenSale: (Long) -> Unit,
    viewModel: SalesHistoryViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    SalesHistoryContent(uiState, onBack, onOpenSale)
}

@Composable
internal fun SalesHistoryContent(uiState: SalesHistoryUiState, onBack: () -> Unit, onOpenSale: (Long) -> Unit) {
    Scaffold(topBar = { BackTopBar(stringResource(R.string.sales_history_title), onBack) }) { padding ->
        when {
            uiState.isLoading -> Box(Modifier.padding(padding).fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            uiState.sales.isEmpty() -> Box(Modifier.padding(padding).fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(stringResource(R.string.sales_history_empty), style = MaterialTheme.typography.bodyLarge)
            }
            else -> LazyColumn(
                modifier = Modifier.padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(uiState.sales, key = { it.id }) { sale -> SaleRow(sale, onClick = { onOpenSale(sale.id) }) }
            }
        }
    }
}

@Composable
private fun SaleRow(sale: Sale, onClick: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).clickable(onClick = onClick)) {
        Row(
            modifier = Modifier.padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(stringResource(R.string.sale_number, sale.id.toInt()), style = MaterialTheme.typography.titleMedium)
                Text(Dates.formatDateTime(sale.createdAtMillis), style = MaterialTheme.typography.bodySmall)
                Text(
                    stringResource(R.string.sale_items_count, sale.items.sumOf { it.quantity }),
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(Money.format(sale.totalCentavos), style = MaterialTheme.typography.titleMedium)
                if (sale.isVoided) {
                    Text(
                        stringResource(R.string.sale_voided_badge),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun SalesHistoryPreview() {
    SariSariManagerTheme {
        SalesHistoryContent(
            SalesHistoryUiState(
                isLoading = false,
                sales = listOf(Sale(12, 19_000, 20_000, 1_000, 12_600, 1_700_000_000_000, null, null, emptyList())),
            ),
            onBack = {}, onOpenSale = {},
        )
    }
}
