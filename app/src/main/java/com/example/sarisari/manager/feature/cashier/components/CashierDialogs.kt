package com.example.sarisari.manager.feature.cashier.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.sarisari.manager.R
import com.example.sarisari.manager.core.util.Money
import com.example.sarisari.manager.domain.model.Product

/** Lets the owner pick the base unit or one of the product's extra units. */
@Composable
fun UnitPickerDialog(
    product: Product,
    onPick: (unitName: String, factor: Int, priceCentavos: Long) -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.sell_unit_dialog_title)) },
        text = {
            Column {
                OutlinedButton(
                    onClick = { onPick(product.baseUnitName, 1, product.sellingPriceCentavos) },
                    modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                ) {
                    Text(
                        stringResource(
                            R.string.sell_unit_option_base,
                            product.baseUnitName, Money.format(product.sellingPriceCentavos),
                        )
                    )
                }
                product.units.forEach { unit ->
                    OutlinedButton(
                        onClick = { onPick(unit.name, unit.baseQuantity, unit.sellingPriceCentavos) },
                        modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                    ) {
                        Text(
                            stringResource(
                                R.string.sell_unit_option_pack,
                                unit.name, unit.baseQuantity, product.baseUnitName,
                                Money.format(unit.sellingPriceCentavos),
                            )
                        )
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) } },
    )
}

/** Edits a line's selling price. [onConfirm] returns true when the text was a valid price. */
@Composable
fun PriceEditDialog(
    unitName: String,
    initialCentavos: Long,
    onConfirm: (String) -> Boolean,
    onDismiss: () -> Unit,
) {
    var text by remember { mutableStateOf(Money.toInputText(initialCentavos)) }
    var invalid by remember { mutableStateOf(false) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.sell_price_dialog_title)) },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = {
                    text = it
                    invalid = false
                },
                label = { Text(stringResource(R.string.sell_price_dialog_label, unitName)) },
                singleLine = true,
                isError = invalid,
                supportingText = if (invalid) {
                    { Text(stringResource(R.string.sell_price_invalid)) }
                } else null,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth(),
            )
        },
        confirmButton = {
            TextButton(onClick = {
                if (onConfirm(text)) onDismiss() else invalid = true
            }) { Text(stringResource(R.string.action_save)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) } },
    )
}
