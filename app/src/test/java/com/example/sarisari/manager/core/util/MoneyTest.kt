package com.example.sarisari.manager.core.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class MoneyTest {
    @Test
    fun formatsWithPesoSignGroupingAndTwoDecimals() {
        assertEquals("\u20B10.00", Money.format(0))
        assertEquals("\u20B10.05", Money.format(5))
        assertEquals("\u20B165.00", Money.format(6500))
        assertEquals("\u20B11,234.50", Money.format(123_450))
        assertEquals("\u20B11,000,000.00", Money.format(100_000_000))
        assertEquals("-\u20B12.50", Money.format(-250))
    }

    @Test
    fun parsesWholeAndDecimalAmounts() {
        assertEquals(1200L, Money.parse("12"))
        assertEquals(1250L, Money.parse("12.5"))
        assertEquals(1205L, Money.parse("12.05"))
        assertEquals(120_075L, Money.parse("1,200.75"))
        assertEquals(500L, Money.parse("\u20B15"))
        assertEquals(500L, Money.parse("  5.00 "))
        assertEquals(0L, Money.parse("0"))
    }

    @Test
    fun rejectsInvalidInput() {
        listOf("", " ", "abc", "-5", "1.234", "1.", ".5", "1e3", "5 0", "12345678901").forEach {
            assertNull("'$it' should be rejected", Money.parse(it))
        }
    }

    @Test
    fun formatAndParseRoundTrip() {
        listOf(0L, 1L, 99L, 100L, 6500L, 123_456L, 999_999_999L).forEach {
            assertEquals(it, Money.parse(Money.toInputText(it)))
            assertEquals(it, Money.parse(Money.format(it)))
        }
    }
}
