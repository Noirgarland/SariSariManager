package com.example.sarisari.manager.feature.cashier.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.sarisari.manager.R
import com.example.sarisari.manager.core.designsystem.ProductAvatar
import com.example.sarisari.manager.core.designsystem.SariSariManagerTheme
import com.example.sarisari.manager.core.designsystem.SurfaceCard
import com.example.sarisari.manager.core.util.Money
import com.example.sarisari.manager.domain.model.Product
import com.example.sarisari.manager.domain.model.StockStatus

/** One product in the Sell grid. Tapping it adds it to the cart. */
@Composable
fun ProductTile(product: Product, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val stockColor = when (product.stockStatus) {
        StockStatus.IN_STOCK -> MaterialTheme.colorScheme.onSurfaceVariant
        StockStatus.LOW_STOCK -> MaterialTheme.colorScheme.tertiary
        StockStatus.OUT_OF_STOCK -> MaterialTheme.colorScheme.error
    }
    SurfaceCard(modifier = modifier.fillMaxWidth().heightIn(min = 128.dp), onClick = onClick, contentPadding = 10.dp) {
        ProductAvatar(product.name, size = 56.dp, modifier = Modifier.align(Alignment.CenterHorizontally))
        Text(
            product.name,
            style = MaterialTheme.typography.titleSmall,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth().padding(top = 6.dp),
        )
        Text(
            Money.format(product.sellingPriceCentavos),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
        Text(
            stringResource(R.string.sell_tile_stock, product.stockQuantity),
            style = MaterialTheme.typography.labelMedium,
            color = stockColor,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
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
