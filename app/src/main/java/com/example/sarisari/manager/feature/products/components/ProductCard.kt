package com.example.sarisari.manager.feature.products.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.sarisari.manager.R
import com.example.sarisari.manager.core.designsystem.Pill
import com.example.sarisari.manager.core.designsystem.ProductAvatar
import com.example.sarisari.manager.core.designsystem.SariSariManagerTheme
import com.example.sarisari.manager.core.designsystem.StockStatusPill
import com.example.sarisari.manager.core.designsystem.SurfaceCard
import com.example.sarisari.manager.core.util.Money
import com.example.sarisari.manager.domain.model.StockStatus
import com.example.sarisari.manager.feature.products.ProductListItem

@Composable
fun ProductCard(item: ProductListItem, onClick: () -> Unit, modifier: Modifier = Modifier) {
    SurfaceCard(modifier = modifier.fillMaxWidth(), onClick = onClick, contentPadding = 12.dp) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            ProductAvatar(item.name, size = 52.dp)
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
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = stringResource(R.string.product_stock, item.stockQuantity, item.baseUnitName),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                if (item.isArchived) {
                    Pill(
                        text = stringResource(R.string.product_archived_badge),
                        container = MaterialTheme.colorScheme.surfaceVariant,
                        content = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                StockStatusPill(item.stockStatus)
            }
        }
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
