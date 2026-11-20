package com.example.sarisari.manager.feature.more

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.sarisari.manager.R
import com.example.sarisari.manager.core.designsystem.SariSariManagerTheme

@Composable
fun MoreScreen(
    onOpenInventory: () -> Unit,
    onOpenSalesHistory: () -> Unit,
    onOpenExpenses: () -> Unit,
    onOpenSettings: () -> Unit,
    viewModel: MoreViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    MoreContent(uiState, onOpenInventory, onOpenSalesHistory, onOpenExpenses, onOpenSettings)
}

@Composable
internal fun MoreContent(
    uiState: MoreUiState,
    onOpenInventory: () -> Unit,
    onOpenSalesHistory: () -> Unit,
    onOpenExpenses: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(stringResource(uiState.titleRes), style = MaterialTheme.typography.headlineSmall)
        MoreRow(R.string.more_sales_title, R.string.more_sales_subtitle, onOpenSalesHistory)
        MoreRow(R.string.more_inventory_title, R.string.more_inventory_subtitle, onOpenInventory)
        MoreRow(R.string.more_expenses_title, R.string.more_expenses_subtitle, onOpenExpenses)
        MoreRow(R.string.more_settings_title, R.string.more_settings_subtitle, onOpenSettings)
    }
}

@Composable
private fun MoreRow(titleRes: Int, subtitleRes: Int, onClick: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).clickable(onClick = onClick)) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(stringResource(titleRes), style = MaterialTheme.typography.titleMedium)
            Text(stringResource(subtitleRes), style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun MoreContentPreview() {
    SariSariManagerTheme {
        MoreContent(MoreUiState(), onOpenInventory = {}, onOpenSalesHistory = {}, onOpenExpenses = {}, onOpenSettings = {})
    }
}
