package com.example.sarisari.manager.feature.products.components

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
import com.example.sarisari.manager.core.util.Money
import com.example.sarisari.manager.domain.model.StockStatus
import com.example.sarisari.manager.feature.products.ProductListItem

@Composable
fun ProductCard(item: ProductListItem, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Card(modifier = modifier.fillMaxWidth().heightIn(min = 48.dp).clickable(onClick = onClick)) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.name,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = stringResource(
                        R.string.product_price_cost,
                        Money.format(item.sellingPriceCentavos),
                        Money.format(item.costCentavos),
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                )
                Text(
                    text = stringResource(R.string.product_stock, item.stockQuantity, item.baseUnitName),
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
            Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                if (item.isArchived) {
                    StatusBadge(
                        text = stringResource(R.string.product_archived_badge),
                        container = MaterialTheme.colorScheme.surfaceVariant,
                        content = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                StockBadge(item.stockStatus)
            }
        }
    }
}

@Composable
private fun StockBadge(status: StockStatus) {
    val colors = MaterialTheme.colorScheme
    when (status) {
        StockStatus.IN_STOCK -> StatusBadge(
            stringResource(R.string.stock_status_in), colors.primaryContainer, colors.onPrimaryContainer,
        )
        StockStatus.LOW_STOCK -> StatusBadge(
            stringResource(R.string.stock_status_low), colors.tertiaryContainer, colors.onTertiaryContainer,
        )
        StockStatus.OUT_OF_STOCK -> StatusBadge(
            stringResource(R.string.stock_status_out), colors.errorContainer, colors.onErrorContainer,
        )
    }
}

@Composable
private fun StatusBadge(
    text: String,
    container: androidx.compose.ui.graphics.Color,
    content: androidx.compose.ui.graphics.Color,
) {
    Surface(color = container, contentColor = content, shape = RoundedCornerShape(50)) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun ProductCardPreview() {
    SariSariManagerTheme {
        ProductCard(
            item = ProductListItem(
                id = 1, name = "Royal 1.5L", categoryName = "Drinks", costCentavos = 4500,
                sellingPriceCentavos = 6500, stockQuantity = 8, baseUnitName = "bottle",
                stockStatus = StockStatus.LOW_STOCK, isArchived = false,
            ),
            onClick = {},
        )
    }
}
