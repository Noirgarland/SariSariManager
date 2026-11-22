package com.example.sarisari.manager.feature.cashier

import com.example.sarisari.manager.core.util.Money
import com.example.sarisari.manager.feature.cashier.components.AmountKeypadInput
import com.example.sarisari.manager.feature.cashier.components.KeypadKey
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class AmountKeypadInputTest {

    private fun type(vararg keys: KeypadKey, start: String = ""): String =
        keys.fold(start) { text, key -> AmountKeypadInput.press(text, key) }

    private fun d(digits: String) = KeypadKey.Digits(digits)

    @Test
    fun digitsAndDoubleZeroBuildTheAmount() {
        assertEquals("200", type(d("2"), d("00")))
        assertEquals("1500", type(d("1"), d("5"), d("00")))
        assertEquals(20_000L, Money.parse(type(d("2"), d("00"))))
    }

    @Test
    fun noLeadingZeros() {
        assertEquals("0", type(d("0"), d("0")))
        assertEquals("0", type(d("00")))
        assertEquals("5", type(d("0"), d("5")))
    }

    @Test
    fun decimalPointOnceAndAtMostTwoDecimals() {
        assertEquals("0.", type(KeypadKey.Decimal))
        assertEquals("12.5", type(d("1"), d("2"), KeypadKey.Decimal, d("5"), KeypadKey.Decimal))
        assertEquals("12.50", type(d("1"), d("2"), KeypadKey.Decimal, d("5"), d("0"), d("9")))
        assertEquals("1.20", type(d("1"), KeypadKey.Decimal, d("2"), d("00")))
        assertEquals(1_250L, Money.parse("12.50"))
    }

    @Test
    fun wholeDigitsStopAtTen() {
        val ten = type(*Array(12) { d("9") })
        assertEquals("9999999999", ten)
        assertNotNull(Money.parse(ten))
    }

    @Test
    fun backspaceAndClear() {
        assertEquals("12", type(KeypadKey.Backspace, start = "125"))
        assertEquals("", type(KeypadKey.Backspace, start = ""))
        assertEquals("", type(KeypadKey.Clear, start = "125.50"))
    }

    @Test
    fun everyResultIsParseableOrBlank() {
        val keys = listOf(d("0"), d("00"), d("7"), KeypadKey.Decimal, KeypadKey.Backspace)
        var text = ""
        repeat(200) { i ->
            text = AmountKeypadInput.press(text, keys[(i * 7 + i / 3) % keys.size])
            if (text.isNotEmpty() && !text.endsWith(".")) assertNotNull("'$text' should parse", Money.parse(text))
        }
    }
}
