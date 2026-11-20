package com.example.sarisari.manager.domain.usecase

import com.example.sarisari.manager.domain.model.ReportSummary
import com.example.sarisari.manager.testing.FakeClock
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ReportPeriodTest {
    @Test
    fun dayBoundariesUseManilaMidnight() {
        val day = ReportPeriod.Day(LocalDate.of(2025, 11, 18))
        // Manila midnight is 16:00 UTC the previous day
        assertEquals(Instant.parse("2025-11-17T16:00:00Z").toEpochMilli(), day.startMillis)
        assertEquals(Instant.parse("2025-11-18T16:00:00Z").toEpochMilli(), day.endMillis)
        assertEquals(LocalDate.of(2025, 11, 17), (day.previous() as ReportPeriod.Day).date)
    }

    @Test
    fun monthBoundariesUseManilaMidnightOnTheFirst() {
        val month = ReportPeriod.Month(YearMonth.of(2025, 12))
        assertEquals(Instant.parse("2025-11-30T16:00:00Z").toEpochMilli(), month.startMillis)
        assertEquals(Instant.parse("2025-12-31T16:00:00Z").toEpochMilli(), month.endMillis)
        assertEquals(YearMonth.of(2026, 1), (month.next() as ReportPeriod.Month).month)
    }

    @Test
    fun todayFollowsManilaNotUtc() {
        // 2025-11-18T17:00Z is already Nov 19 in Manila
        val clock = FakeClock(Instant.parse("2025-11-18T17:00:00Z"))
        assertEquals(LocalDate.of(2025, 11, 19), ReportPeriod.today(clock).date)
        assertEquals(YearMonth.of(2025, 11), ReportPeriod.thisMonth(clock).month)
        assertFalse(ReportPeriod.today(clock).isAfterNow(clock))
        assertTrue(ReportPeriod.today(clock).next().isAfterNow(clock))
    }

    @Test
    fun profitFormulas() {
        val summary = ReportSummary(revenueCentavos = 485_000, costOfGoodsSoldCentavos = 393_000, expensesCentavos = 50_000, transactionCount = 48)
        assertEquals(92_000L, summary.grossProfitCentavos)
        assertEquals(42_000L, summary.netProfitCentavos)
        assertEquals(-8_000L, summary.copy(expensesCentavos = 100_000).netProfitCentavos)
    }
}
