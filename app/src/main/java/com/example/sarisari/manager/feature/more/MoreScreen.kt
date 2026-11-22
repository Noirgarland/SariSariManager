package com.example.sarisari.manager.feature.more

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.sarisari.manager.R
import com.example.sarisari.manager.core.designsystem.AppHeader
import com.example.sarisari.manager.core.designsystem.SariSariManagerTheme
import com.example.sarisari.manager.core.designsystem.SurfaceCard

@Composable
fun MoreScreen(
    onOpenUtang: () -> Unit,
    onOpenInventory: () -> Unit,
    onOpenSalesHistory: () -> Unit,
    onOpenExpenses: () -> Unit,
    onOpenSettings: () -> Unit,
    viewModel: MoreViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    MoreContent(uiState, onOpenUtang, onOpenInventory, onOpenSalesHistory, onOpenExpenses, onOpenSettings)
}

@Composable
internal fun MoreContent(
    uiState: MoreUiState,
    onOpenUtang: () -> Unit,
    onOpenInventory: () -> Unit,
    onOpenSalesHistory: () -> Unit,
    onOpenExpenses: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    Column(modifier = Modifier.fillMaxSize()) {
        AppHeader(title = stringResource(uiState.titleRes))
        Column(
            modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            MoreRow(R.drawable.ic_people, R.string.more_utang_title, R.string.more_utang_subtitle, onOpenUtang)
            MoreRow(R.drawable.ic_receipt, R.string.more_sales_title, R.string.more_sales_subtitle, onOpenSalesHistory)
            MoreRow(R.drawable.ic_add_box, R.string.more_inventory_title, R.string.more_inventory_subtitle, onOpenInventory)
            MoreRow(R.drawable.ic_money, R.string.more_expenses_title, R.string.more_expenses_subtitle, onOpenExpenses)
            MoreRow(R.drawable.ic_nav_more, R.string.more_settings_title, R.string.more_settings_subtitle, onOpenSettings)
        }
    }
}

@Composable
private fun MoreRow(iconRes: Int, titleRes: Int, subtitleRes: Int, onClick: () -> Unit) {
    SurfaceCard(modifier = Modifier.fillMaxWidth(), onClick = onClick, contentPadding = 14.dp) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            Box(
                modifier = Modifier.size(44.dp).clip(RoundedCornerShape(12.dp)).background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center,
            ) {
                Icon(painterResource(iconRes), contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(stringResource(titleRes), style = MaterialTheme.typography.titleMedium)
                Text(
                    stringResource(subtitleRes),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Icon(painterResource(R.drawable.ic_chevron_right), contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun MoreContentPreview() {
    SariSariManagerTheme {
        MoreContent(
            MoreUiState(), onOpenUtang = {}, onOpenInventory = {}, onOpenSalesHistory = {}, onOpenExpenses = {}, onOpenSettings = {},
        )
    }
}
