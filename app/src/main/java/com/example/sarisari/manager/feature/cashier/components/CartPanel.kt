package com.example.sarisari.manager.feature.cashier.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.sarisari.manager.R
import com.example.sarisari.manager.core.designsystem.CardShape
import com.example.sarisari.manager.core.designsystem.ProductAvatar
import com.example.sarisari.manager.core.designsystem.QuantityStepper
import com.example.sarisari.manager.core.designsystem.SariSariManagerTheme
import com.example.sarisari.manager.core.designsystem.SurfaceCard
import com.example.sarisari.manager.core.util.Money
import com.example.sarisari.manager.feature.cashier.CartLine

/** The cart summary shown under the product grid. */
@Composable
fun CartPanel(
    cart: List<CartLine>,
    itemCount: Int,
    totalCentavos: Long,
    onIncrease: (String) -> Unit,
    onDecrease: (String) -> Unit,
    onRemove: (String) -> Unit,
    onEditPrice: (CartLine) -> Unit,
    onClear: () -> Unit,
    onProceed: () -> Unit,
    modifier: Modifier = Modifier,
) {
    SurfaceCard(modifier = modifier.fillMaxWidth(), contentPadding = 12.dp) {
        if (cart.isEmpty()) {
            Text(
                stringResource(R.string.sell_cart_empty),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(vertical = 8.dp),
            )
            return@SurfaceCard
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(stringResource(R.string.sell_cart_title, itemCount), style = MaterialTheme.typography.titleMedium)
            TextButton(onClick = onClear) { Text(stringResource(R.string.sell_cart_clear)) }
        }
        LazyColumn(modifier = Modifier.heightIn(max = 220.dp)) {
            items(cart, key = { it.key }) { line ->
                CartLineRow(line, onIncrease, onDecrease, onRemove, onEditPrice)
            }
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(stringResource(R.string.sell_total), style = MaterialTheme.typography.titleLarge)
            Text(
                Money.format(totalCentavos),
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.primary,
            )
        }
        Button(onClick = onProceed, shape = CardShape, modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp)) {
            Text(stringResource(R.string.sell_proceed), style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.width(8.dp))
            Icon(painterResource(R.drawable.ic_arrow_forward), contentDescription = null, modifier = Modifier.size(20.dp))
        }
    }
}

@Composable
private fun CartLineRow(
    line: CartLine,
    onIncrease: (String) -> Unit,
    onDecrease: (String) -> Unit,
    onRemove: (String) -> Unit,
    onEditPrice: (CartLine) -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            ProductAvatar(line.productName, size = 40.dp)
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    if (line.unitFactor > 1) {
                        stringResource(R.string.sell_line_name_unit, line.productName, line.unitName)
                    } else {
                        line.productName
                    },
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                // Tapping the unit price opens the price editor.
                TextButton(
                    onClick = { onEditPrice(line) },
                    contentPadding = PaddingValues(horizontal = 0.dp),
                    modifier = Modifier.heightIn(min = 48.dp),
                ) {
                    Text(
                        stringResource(R.string.sell_line_each, Money.format(line.unitPriceCentavos)),
                        style = MaterialTheme.typography.labelLarge,
                    )
                    Icon(
                        painterResource(R.drawable.ic_edit),
                        contentDescription = stringResource(R.string.sell_edit_price),
                        modifier = Modifier.padding(start = 4.dp).size(14.dp),
                    )
                }
            }
            Text(Money.format(line.lineTotalCentavos), style = MaterialTheme.typography.titleSmall)
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            QuantityStepper(
                quantity = line.quantity,
                onDecrease = { onDecrease(line.key) },
                onIncrease = { onIncrease(line.key) },
                modifier = Modifier.padding(start = 50.dp),
            )
            if (line.exceedsStock) {
                Text(
                    stringResource(R.string.sell_line_low_stock, line.availableBaseStock),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(start = 8.dp).weight(1f),
                )
            } else {
                Spacer(Modifier.weight(1f))
            }
            IconButton(onClick = { onRemove(line.key) }) {
                Icon(
                    painterResource(R.drawable.ic_close),
                    stringResource(R.string.sell_remove_line),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun CartPanelPreview() {
    SariSariManagerTheme {
        CartPanel(
            cart = listOf(
                CartLine(1, "Coke 1.5L", "bottle", 1, 2, 6500, 1),
                CartLine(2, "Piattos", "pack", 6, 1, 12_000, 20),
            ),
            itemCount = 3, totalCentavos = 25_000,
            onIncrease = {}, onDecrease = {}, onRemove = {}, onEditPrice = {}, onClear = {}, onProceed = {},
        )
    }
}
