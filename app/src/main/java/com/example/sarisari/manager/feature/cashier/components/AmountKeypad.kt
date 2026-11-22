package com.example.sarisari.manager.feature.cashier.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.sarisari.manager.R
import com.example.sarisari.manager.core.designsystem.CardShape

/** A key on the cash amount pad. */
sealed interface KeypadKey {
    data class Digits(val digits: String) : KeypadKey
    data object Decimal : KeypadKey
    data object Backspace : KeypadKey
    data object Clear : KeypadKey
}

/**
 * Applies one key press to the typed amount. The result is always something [com.example.sarisari.manager.core.util.Money.parse]
 * accepts, or blank: at most [MAX_WHOLE_DIGITS] whole-peso digits, one decimal point, and two decimals.
 */
object AmountKeypadInput {
    private const val MAX_WHOLE_DIGITS = 10
    private const val MAX_DECIMALS = 2

    fun press(current: String, key: KeypadKey): String = when (key) {
        KeypadKey.Clear -> ""
        KeypadKey.Backspace -> current.dropLast(1)
        KeypadKey.Decimal -> when {
            current.contains('.') -> current
            current.isEmpty() -> "0."
            else -> "$current."
        }
        is KeypadKey.Digits -> key.digits.fold(current, ::appendDigit)
    }

    private fun appendDigit(current: String, digit: Char): String {
        val point = current.indexOf('.')
        return when {
            point >= 0 -> if (current.length - point - 1 >= MAX_DECIMALS) current else current + digit
            // No leading zeros: "0" then "5" is "5", and "0" then "0" stays "0".
            current == "0" -> digit.toString()
            current.length >= MAX_WHOLE_DIGITS -> current
            else -> current + digit
        }
    }
}

private val digitRows = listOf(listOf("1", "2", "3"), listOf("4", "5", "6"), listOf("7", "8", "9"))

/** A large number pad for typing the cash received: 1–9, 0, 00, decimal point, backspace and clear. */
@Composable
fun AmountKeypad(onKey: (KeypadKey) -> Unit, modifier: Modifier = Modifier) {
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        digitRows.forEachIndexed { index, row ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                row.forEach { digit -> TextKey(digit) { onKey(KeypadKey.Digits(digit)) } }
                when (index) {
                    0 -> IconKey(R.drawable.ic_backspace, stringResource(R.string.keypad_backspace)) { onKey(KeypadKey.Backspace) }
                    1 -> TextKey(stringResource(R.string.keypad_clear), accent = true) { onKey(KeypadKey.Clear) }
                    else -> TextKey(".") { onKey(KeypadKey.Decimal) }
                }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TextKey("0", weight = 2f) { onKey(KeypadKey.Digits("0")) }
            TextKey("00", weight = 2f) { onKey(KeypadKey.Digits("00")) }
        }
    }
}

@Composable
private fun RowScope.TextKey(label: String, weight: Float = 1f, accent: Boolean = false, onClick: () -> Unit) {
    FilledTonalButton(
        onClick = onClick,
        shape = CardShape,
        colors = keyColors(accent),
        modifier = Modifier.weight(weight).height(56.dp),
    ) { Text(label, style = MaterialTheme.typography.titleLarge) }
}

@Composable
private fun RowScope.IconKey(iconRes: Int, description: String, onClick: () -> Unit) {
    FilledTonalButton(
        onClick = onClick,
        shape = CardShape,
        colors = keyColors(accent = true),
        modifier = Modifier.weight(1f).height(56.dp),
    ) { Icon(painterResource(iconRes), contentDescription = description) }
}

@Composable
private fun keyColors(accent: Boolean) = ButtonDefaults.filledTonalButtonColors(
    containerColor = if (accent) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
    contentColor = if (accent) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface,
)
