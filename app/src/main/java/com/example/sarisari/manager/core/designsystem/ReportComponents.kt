package com.example.sarisari.manager.core.designsystem

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.sarisari.manager.R
import com.example.sarisari.manager.core.util.Money
import com.example.sarisari.manager.domain.model.TopProduct

/** "� label �" row for moving between report periods. */
@Composable
fun PeriodSelector(
    label: String,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    canGoNext: Boolean,
    modifier: Modifier = Modifier,
) {
    Row(modifier = modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = onPrevious) {
            Icon(painterResource(R.drawable.ic_chevron_left), stringResource(R.string.reports_previous))
        }
        Text(
            label,
            style = MaterialTheme.typography.titleMedium,
            textAlign = TextAlign.Center,
            modifier = Modifier.weight(1f),
        )
        IconButton(onClick = onNext, enabled = canGoNext) {
            Icon(painterResource(R.drawable.ic_chevron_right), stringResource(R.string.reports_next))
        }
    }
}

/** One ranked line in a top-products list. */
@Composable
fun TopProductRow(rank: Int, product: TopProduct) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(rank.toString(), style = MaterialTheme.typography.titleSmall)
        Column(modifier = Modifier.weight(1f)) {
            Text(product.productName, style = MaterialTheme.typography.titleSmall)
            Text(
                stringResource(R.string.top_product_sold, product.baseQuantity, product.baseUnitName),
                style = MaterialTheme.typography.bodySmall,
            )
        }
        Text(Money.format(product.revenueCentavos), style = MaterialTheme.typography.titleSmall)
    }
}
