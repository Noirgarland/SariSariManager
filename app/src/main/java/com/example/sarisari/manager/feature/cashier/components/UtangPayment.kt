package com.example.sarisari.manager.feature.cashier.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.sarisari.manager.R
import com.example.sarisari.manager.core.designsystem.ProductAvatar
import com.example.sarisari.manager.core.designsystem.SearchField
import com.example.sarisari.manager.core.designsystem.SurfaceCard
import com.example.sarisari.manager.core.util.Money
import com.example.sarisari.manager.domain.model.Customer
import com.example.sarisari.manager.feature.cashier.PaymentMethod

/** Two big tiles: Cash and Utang. The selected one is solid green with a check mark. */
@Composable
fun PaymentMethodSelector(selected: PaymentMethod, onSelect: (PaymentMethod) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        MethodTile(
            iconRes = R.drawable.ic_money,
            label = stringResource(R.string.payment_method_cash),
            caption = stringResource(R.string.payment_method_cash_caption),
            selected = selected == PaymentMethod.CASH,
            onClick = { onSelect(PaymentMethod.CASH) },
            modifier = Modifier.weight(1f),
        )
        MethodTile(
            iconRes = R.drawable.ic_people,
            label = stringResource(R.string.payment_method_utang),
            caption = stringResource(R.string.payment_method_utang_caption),
            selected = selected == PaymentMethod.UTANG,
            onClick = { onSelect(PaymentMethod.UTANG) },
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun MethodTile(
    iconRes: Int,
    label: String,
    caption: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    SurfaceCard(
        modifier = modifier.heightIn(min = 88.dp),
        onClick = onClick,
        containerColor = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainerLowest,
        contentColor = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
        border = !selected,
        contentPadding = 12.dp,
    ) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Icon(painterResource(iconRes), contentDescription = null)
            if (selected) Icon(painterResource(R.drawable.ic_check_circle), contentDescription = null)
        }
        Text(label, style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 6.dp))
        Text(caption, style = MaterialTheme.typography.labelMedium)
    }
}

/** The customer the utang goes to, with their current balance. Tap to change. */
@Composable
fun UtangCustomerCard(customer: Customer?, onChange: () -> Unit) {
    SurfaceCard(modifier = Modifier.fillMaxWidth(), onClick = onChange, contentPadding = 12.dp) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            if (customer == null) {
                Icon(painterResource(R.drawable.ic_people), contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Text(
                    stringResource(R.string.payment_choose_customer),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.weight(1f),
                )
            } else {
                ProductAvatar(customer.name, size = 40.dp)
                Column(modifier = Modifier.weight(1f)) {
                    Text(customer.name, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(
                        stringResource(R.string.utang_current_balance, Money.format(customer.balanceCentavos)),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Text(stringResource(R.string.payment_change_customer), color = MaterialTheme.colorScheme.primary)
            }
        }
    }
}

/**
 * Search existing customers or add a new one with the typed name.
 * [errorRes] explains why a new name was not accepted.
 */
@Composable
fun CustomerPickerDialog(
    query: String,
    customers: List<Customer>,
    errorRes: Int?,
    onQueryChange: (String) -> Unit,
    onChoose: (Customer) -> Unit,
    onAdd: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    val typed = query.trim()
    val exactMatch = customers.any { it.name.equals(typed, ignoreCase = true) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.payment_choose_customer)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                SearchField(query, onQueryChange, stringResource(R.string.utang_search))
                if (typed.isNotEmpty() && !exactMatch) {
                    TextButton(onClick = { onAdd(typed) }, modifier = Modifier.heightIn(min = 48.dp)) {
                        Icon(painterResource(R.drawable.ic_add), contentDescription = null)
                        Text(stringResource(R.string.utang_add_named, typed), modifier = Modifier.padding(start = 8.dp))
                    }
                }
                errorRes?.let { Text(stringResource(it), color = MaterialTheme.colorScheme.error) }
                if (customers.isEmpty()) {
                    Text(
                        stringResource(if (typed.isEmpty()) R.string.utang_picker_empty else R.string.utang_no_match),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                LazyColumn(modifier = Modifier.heightIn(max = 320.dp)) {
                    items(customers, key = { it.id }) { customer ->
                        TextButton(
                            onClick = { onChoose(customer) },
                            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                        ) {
                            Text(
                                customer.name,
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f),
                            )
                            Text(
                                Money.format(customer.balanceCentavos),
                                style = MaterialTheme.typography.titleSmall,
                                color = if (customer.balanceCentavos > 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) } },
    )
}
