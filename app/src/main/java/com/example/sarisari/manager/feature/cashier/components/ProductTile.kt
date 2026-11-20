package com.example.sarisari.manager.feature.cashier.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.sarisari.manager.R
import com.example.sarisari.manager.core.designsystem.SariSariManagerTheme
import com.example.sarisari.manager.core.util.Money
import com.example.sarisari.manager.domain.model.Product
import com.example.sarisari.manager.domain.model.StockStatus

@Composable
fun ProductTile(product: Product, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val stockColor = when (product.stockStatus) {
        StockStatus.IN_STOCK -> MaterialTheme.colorScheme.onSurfaceVariant
        StockStatus.LOW_STOCK -> MaterialTheme.colorScheme.tertiary
        StockStatus.OUT_OF_STOCK -> MaterialTheme.colorScheme.error
    }
    Card(modifier = modifier.fillMaxWidth().heightIn(min = 96.dp).clickable(onClick = onClick)) {
        Column(modifier = Modifier.padding(10.dp)) {
            Text(
                product.name,
                style = MaterialTheme.typography.titleSmall,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                Money.format(product.sellingPriceCentavos),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
            )
            Text(
                stringResource(R.string.sell_tile_stock, product.stockQuantity),
                style = MaterialTheme.typography.bodySmall,
                color = stockColor,
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun ProductTilePreview() {
    SariSariManagerTheme {
        ProductTile(
            Product(1, "Coke 1.5L", null, "bottle", 4500, 6500, stockQuantity = 3, lowStockThreshold = 5),
            onClick = {},
        )
    }
}
