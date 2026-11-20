package com.example.sarisari.manager.feature.inventory

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
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
import com.example.sarisari.manager.domain.model.StockMovement
import com.example.sarisari.manager.domain.model.StockMovementType

@Composable
fun StockHistoryScreen(onBack: () -> Unit, viewModel: StockHistoryViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    StockHistoryContent(uiState, onBack)
}

@Composable
internal fun StockHistoryContent(uiState: StockHistoryUiState, onBack: () -> Unit) {
    Scaffold(topBar = { BackTopBar(stringResource(R.string.stock_history_title), onBack) }) { padding ->
        when {
            uiState.isLoading -> Box(Modifier.padding(padding).fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            uiState.movements.isEmpty() -> Box(Modifier.padding(padding).fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(stringResource(R.string.stock_history_empty), style = MaterialTheme.typography.bodyLarge)
            }
            else -> LazyColumn(
                modifier = Modifier.padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(uiState.movements, key = { it.id }) { MovementRow(it) }
            }
        }
    }
}

@Composable
private fun MovementRow(movement: StockMovement) {
    val typeLabel = when (movement.type) {
        StockMovementType.RECEIPT -> R.string.movement_type_receipt
        StockMovementType.DAMAGED -> R.string.movement_type_damaged
        StockMovementType.EXPIRED -> R.string.movement_type_expired
        StockMovementType.ADJUSTMENT -> R.string.movement_type_adjustment
        StockMovementType.SALE -> R.string.movement_type_sale
        StockMovementType.SALE_VOID -> R.string.movement_type_sale_void
    }
    val change = if (movement.quantityChange > 0) "+${movement.quantityChange}" else movement.quantityChange.toString()
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(movement.productName, style = MaterialTheme.typography.titleMedium)
                Text(stringResource(typeLabel), style = MaterialTheme.typography.bodyMedium)
                movement.unitCostCentavos?.let {
                    Text(stringResource(R.string.movement_cost_each, Money.format(it)), style = MaterialTheme.typography.bodySmall)
                }
                movement.reason?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
                Text(
                    Dates.formatDateTime(movement.createdAtMillis),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Text(
                stringResource(R.string.movement_quantity, change, movement.baseUnitName),
                style = MaterialTheme.typography.titleMedium,
                color = if (movement.quantityChange > 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun StockHistoryContentPreview() {
    SariSariManagerTheme {
        StockHistoryContent(
            StockHistoryUiState(
                isLoading = false,
                movements = listOf(
                    StockMovement(1, 1, "Coke 1.5L", "bottle", StockMovementType.RECEIPT, 12, 4500, 54000, "Delivery", 1_700_000_000_000),
                    StockMovement(2, 1, "Coke 1.5L", "bottle", StockMovementType.DAMAGED, -2, null, null, null, 1_700_000_100_000),
                ),
            ),
            onBack = {},
        )
    }
}
