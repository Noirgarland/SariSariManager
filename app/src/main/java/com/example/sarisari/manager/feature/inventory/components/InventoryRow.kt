package com.example.sarisari.manager.feature.inventory.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.sarisari.manager.R
import com.example.sarisari.manager.core.designsystem.SariSariManagerTheme
import com.example.sarisari.manager.domain.model.StockStatus
import com.example.sarisari.manager.feature.inventory.InventoryItem

@Composable
fun InventoryRow(item: InventoryItem, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val colors = MaterialTheme.colorScheme
    val (label, container, content) = when (item.stockStatus) {
        StockStatus.IN_STOCK -> Triple(R.string.stock_status_in, colors.primaryContainer, colors.onPrimaryContainer)
        StockStatus.LOW_STOCK -> Triple(R.string.stock_status_low, colors.tertiaryContainer, colors.onTertiaryContainer)
        StockStatus.OUT_OF_STOCK -> Triple(R.string.stock_status_out, colors.errorContainer, colors.onErrorContainer)
    }
    Card(modifier = modifier.fillMaxWidth().heightIn(min = 48.dp).clickable(onClick = onClick)) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(item.name, style = MaterialTheme.typography.titleMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Text(
                    stringResource(R.string.inventory_stock_line, item.stockQuantity, item.baseUnitName),
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
            Surface(color = container, contentColor = content, shape = RoundedCornerShape(50)) {
                Text(
                    stringResource(label),
                    style = MaterialTheme.typography.labelMedium,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun InventoryRowPreview() {
    SariSariManagerTheme {
        InventoryRow(InventoryItem(1, "Royal 1.5L", 3, "bottle", StockStatus.LOW_STOCK), onClick = {})
    }
}
