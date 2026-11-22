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
    fun acceptsOnlyCorrectThousandsGrouping() {
        assertEquals(100_000L, Money.parse("1,000"))
        assertEquals(123_456_789_00L, Money.parse("123,456,789"))
        assertEquals(1_234_567_50L, Money.parse("\u20B11,234,567.50"))
        listOf("1,2", "12,34", "1,23", "1,2345", ",100", "100,", "1,,000", "0,100", "1,000,00", "1.000,50", "1,000.5.0")
            .forEach { assertNull("'$it' should be rejected", Money.parse(it)) }
    }

    @Test
    fun boundaryAmounts() {
        // Ten whole-peso digits is the most accepted, with or without grouping.
        assertEquals(999_999_999_999L, Money.parse("9999999999.99"))
        assertEquals(999_999_999_999L, Money.parse("9,999,999,999.99"))
        assertNull(Money.parse("10000000000"))
        assertNull(Money.parse("10,000,000,000"))
        assertNull(Money.parse("99999999999999999999999"))
        assertEquals(1L, Money.parse("0.01"))
        assertEquals(10L, Money.parse("0.1"))
    }

    @Test
    fun formatHandlesTheFullLongRange() {
        assertEquals("-\u20B192,233,720,368,547,758.08", Money.format(Long.MIN_VALUE))
        assertEquals("\u20B192,233,720,368,547,758.07", Money.format(Long.MAX_VALUE))
        assertEquals("-\u20B10.01", Money.format(-1))
    }

    @Test
    fun formatAndParseRoundTrip() {
        listOf(0L, 1L, 99L, 100L, 6500L, 123_456L, 999_999_999L).forEach {
            assertEquals(it, Money.parse(Money.toInputText(it)))
            assertEquals(it, Money.parse(Money.format(it)))
        }
    }
}
