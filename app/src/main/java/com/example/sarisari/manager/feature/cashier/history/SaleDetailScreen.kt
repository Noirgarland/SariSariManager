package com.example.sarisari.manager.feature.cashier.history

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import com.example.sarisari.manager.domain.model.SaleItem

@Composable
fun SaleDetailScreen(onBack: () -> Unit, viewModel: SaleDetailViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    SaleDetailContent(uiState, onBack, viewModel::onVoid)
}

@Composable
internal fun SaleDetailContent(uiState: SaleDetailUiState, onBack: () -> Unit, onVoid: (String) -> Unit) {
    var confirmVoid by remember { mutableStateOf(false) }
    var reason by remember { mutableStateOf("") }
    val sale = uiState.sale
    val title = if (sale != null) stringResource(R.string.sale_number, sale.id.toInt()) else stringResource(R.string.sales_history_title)

    Scaffold(topBar = { BackTopBar(title, onBack) }) { padding ->
        when {
            uiState.isLoading -> Box(Modifier.padding(padding).fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            sale == null -> Box(Modifier.padding(padding).fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(stringResource(R.string.sale_detail_not_found))
            }
            else -> Column(
                modifier = Modifier.padding(padding).fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(Dates.formatDateTime(sale.createdAtMillis), style = MaterialTheme.typography.bodyMedium)
                if (sale.isVoided) VoidedBanner(sale)
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        sale.items.forEach { ItemRow(it) }
                        HorizontalDivider()
                        AmountRow(R.string.sale_detail_total, sale.totalCentavos, bold = true)
                        AmountRow(R.string.sale_detail_received, sale.amountReceivedCentavos)
                        AmountRow(R.string.sale_detail_change, sale.changeCentavos)
                        AmountRow(R.string.sale_detail_profit, sale.grossProfitCentavos)
                    }
                }
                if (uiState.voidFailed) {
                    Text(stringResource(R.string.sale_void_error), color = MaterialTheme.colorScheme.error)
                }
                if (!sale.isVoided) {
                    OutlinedButton(
                        onClick = { confirmVoid = true },
                        enabled = !uiState.isVoiding,
                        modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                    ) { Text(stringResource(R.string.sale_void_action)) }
                }
            }
        }
    }

    if (confirmVoid) {
        AlertDialog(
            onDismissRequest = { confirmVoid = false },
            title = { Text(stringResource(R.string.sale_void_dialog_title)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(stringResource(R.string.sale_void_dialog_message))
                    OutlinedTextField(
                        value = reason,
                        onValueChange = { reason = it },
                        label = { Text(stringResource(R.string.sale_void_reason_label)) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    confirmVoid = false
                    onVoid(reason)
                }) { Text(stringResource(R.string.sale_void_confirm)) }
            },
            dismissButton = { TextButton(onClick = { confirmVoid = false }) { Text(stringResource(R.string.action_cancel)) } },
        )
    }
}

@Composable
private fun VoidedBanner(sale: Sale) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(stringResource(R.string.sale_voided_badge), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.error)
            sale.voidedAtMillis?.let {
                Text(stringResource(R.string.sale_detail_voided_on, Dates.formatDateTime(it)), style = MaterialTheme.typography.bodyMedium)
            }
            sale.voidReason?.let { Text(stringResource(R.string.sale_detail_void_reason, it), style = MaterialTheme.typography.bodyMedium) }
        }
    }
}

@Composable
private fun ItemRow(item: SaleItem) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                if (item.unitFactor > 1) stringResource(R.string.sell_line_name_unit, item.productName, item.unitName) else item.productName,
                style = MaterialTheme.typography.titleSmall,
            )
            Text(
                stringResource(R.string.sale_detail_line, item.unitName, item.quantity, Money.format(item.unitPriceCentavos)),
                style = MaterialTheme.typography.bodySmall,
            )
        }
        Text(Money.format(item.lineTotalCentavos), style = MaterialTheme.typography.titleSmall)
    }
}

@Composable
private fun AmountRow(labelRes: Int, centavos: Long, bold: Boolean = false) {
    val style = if (bold) MaterialTheme.typography.titleMedium else MaterialTheme.typography.bodyMedium
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(stringResource(labelRes), style = style)
        Text(Money.format(centavos), style = style)
    }
}

@Preview(showBackground = true)
@Composable
private fun SaleDetailPreview() {
    SariSariManagerTheme {
        SaleDetailContent(
            SaleDetailUiState(
                isLoading = false,
                sale = Sale(
                    12, 19_000, 20_000, 1_000, 12_600, 1_700_000_000_000, null, null,
                    listOf(SaleItem(1, 1, "Coke 1.5L", "bottle", 1, 2, 2, 6500, 13_000, 4500, 9000)),
                ),
            ),
            onBack = {}, onVoid = {},
        )
    }
}
