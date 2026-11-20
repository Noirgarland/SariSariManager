package com.example.sarisari.manager.feature.cashier.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import com.example.sarisari.manager.core.designsystem.SariSariManagerTheme
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
    Column(modifier = modifier.fillMaxWidth().padding(top = 4.dp)) {
        HorizontalDivider()
        if (cart.isEmpty()) {
            Text(
                stringResource(R.string.sell_cart_empty),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(vertical = 16.dp),
            )
            return
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(stringResource(R.string.sell_cart_title, itemCount), style = MaterialTheme.typography.titleMedium)
            TextButton(onClick = onClear) { Text(stringResource(R.string.sell_cart_clear)) }
        }
        LazyColumn(modifier = Modifier.heightIn(max = 200.dp)) {
            items(cart, key = { it.key }) { line ->
                CartLineRow(line, onIncrease, onDecrease, onRemove, onEditPrice)
            }
        }
        HorizontalDivider()
        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(stringResource(R.string.sell_total), style = MaterialTheme.typography.titleLarge)
            Text(Money.format(totalCentavos), style = MaterialTheme.typography.titleLarge)
        }
        Button(onClick = onProceed, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
            Text(stringResource(R.string.sell_proceed))
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
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                if (line.unitFactor > 1) {
                    stringResource(R.string.sell_line_name_unit, line.productName, line.unitName)
                } else {
                    line.productName
                },
                style = MaterialTheme.typography.titleSmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            Text(Money.format(line.lineTotalCentavos), style = MaterialTheme.typography.titleSmall)
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = { onDecrease(line.key) }) {
                Icon(painterResource(R.drawable.ic_remove), stringResource(R.string.sell_quantity_decrease))
            }
            Text(line.quantity.toString(), style = MaterialTheme.typography.titleMedium, modifier = Modifier.sizeIn(minWidth = 28.dp))
            IconButton(onClick = { onIncrease(line.key) }) {
                Icon(painterResource(R.drawable.ic_add), stringResource(R.string.sell_quantity_increase))
            }
            OutlinedButton(onClick = { onEditPrice(line) }, modifier = Modifier.heightIn(min = 48.dp)) {
                Text(stringResource(R.string.sell_line_each, Money.format(line.unitPriceCentavos)))
            }
            IconButton(onClick = { onRemove(line.key) }) {
                Icon(painterResource(R.drawable.ic_close), stringResource(R.string.sell_remove_line))
            }
        }
        if (line.exceedsStock) {
            Text(
                stringResource(R.string.sell_line_low_stock, line.availableBaseStock),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
            )
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
