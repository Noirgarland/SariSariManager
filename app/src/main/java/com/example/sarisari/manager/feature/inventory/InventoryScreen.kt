package com.example.sarisari.manager.feature.inventory

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.foundation.layout.Row
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
import com.example.sarisari.manager.domain.model.StockStatus
import com.example.sarisari.manager.feature.inventory.components.InventoryRow

@Composable
fun InventoryScreen(
    onBack: () -> Unit,
    onOpenProduct: (Long) -> Unit,
    onOpenHistory: () -> Unit,
    viewModel: InventoryViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    InventoryContent(
        uiState = uiState,
        onBack = onBack,
        onQueryChange = viewModel::onQueryChange,
        onLowStockOnlyChange = viewModel::onLowStockOnlyChange,
        onOpenProduct = onOpenProduct,
        onOpenHistory = onOpenHistory,
    )
}

@Composable
internal fun InventoryContent(
    uiState: InventoryUiState,
    onBack: () -> Unit,
    onQueryChange: (String) -> Unit,
    onLowStockOnlyChange: (Boolean) -> Unit,
    onOpenProduct: (Long) -> Unit,
    onOpenHistory: () -> Unit,
) {
    Scaffold(
        topBar = {
            BackTopBar(
                title = stringResource(R.string.inventory_title),
                onBack = onBack,
                actions = { TextButton(onClick = onOpenHistory) { Text(stringResource(R.string.inventory_history)) } },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier.padding(padding).fillMaxSize().padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            OutlinedTextField(
                value = uiState.query,
                onValueChange = onQueryChange,
                label = { Text(stringResource(R.string.inventory_search)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = !uiState.lowStockOnly,
                    onClick = { onLowStockOnlyChange(false) },
                    label = { Text(stringResource(R.string.inventory_filter_all)) },
                )
                FilterChip(
                    selected = uiState.lowStockOnly,
                    onClick = { onLowStockOnlyChange(true) },
                    label = { Text(stringResource(R.string.inventory_filter_low, uiState.lowStockCount)) },
                )
            }
            when {
                uiState.isLoading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
                uiState.items.isEmpty() -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        stringResource(
                            if (uiState.query.isNotBlank() || uiState.lowStockOnly) R.string.inventory_empty_filtered
                            else R.string.inventory_empty
                        ),
                        style = MaterialTheme.typography.bodyLarge,
                    )
                }
                else -> LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = 16.dp),
                ) {
                    items(uiState.items, key = { it.id }) { item ->
                        InventoryRow(item = item, onClick = { onOpenProduct(item.id) })
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun InventoryContentPreview() {
    SariSariManagerTheme {
        InventoryContent(
            uiState = InventoryUiState(
                isLoading = false,
                lowStockCount = 1,
                items = listOf(
                    InventoryItem(1, "Coke 1.5L", 24, "bottle", StockStatus.IN_STOCK),
                    InventoryItem(2, "Royal 1.5L", 3, "bottle", StockStatus.LOW_STOCK),
                ),
            ),
            onBack = {}, onQueryChange = {}, onLowStockOnlyChange = {}, onOpenProduct = {}, onOpenHistory = {},
        )
    }
}
