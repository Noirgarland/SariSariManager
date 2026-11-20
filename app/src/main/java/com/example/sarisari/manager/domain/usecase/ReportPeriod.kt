package com.example.sarisari.manager.domain.usecase

import com.example.sarisari.manager.domain.model.StoreTime
import java.time.Clock
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId

/** A report period in store time, with its [startMillis, endMillis) range in UTC epoch milliseconds. */
sealed interface ReportPeriod {
    val startMillis: Long
    val endMillis: Long
    fun previous(): ReportPeriod
    fun next(): ReportPeriod

    data class Day(val date: LocalDate, val zone: ZoneId = StoreTime.ZONE) : ReportPeriod {
        override val startMillis: Long get() = date.atStartOfDay(zone).toInstant().toEpochMilli()
        override val endMillis: Long get() = date.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli()
        override fun previous() = copy(date = date.minusDays(1))
        override fun next() = copy(date = date.plusDays(1))
    }

    data class Month(val month: YearMonth, val zone: ZoneId = StoreTime.ZONE) : ReportPeriod {
        override val startMillis: Long get() = month.atDay(1).atStartOfDay(zone).toInstant().toEpochMilli()
        override val endMillis: Long get() = month.plusMonths(1).atDay(1).atStartOfDay(zone).toInstant().toEpochMilli()
        override fun previous() = copy(month = month.minusMonths(1))
        override fun next() = copy(month = month.plusMonths(1))
    }

    companion object {
        fun today(clock: Clock): Day = Day(clock.instant().atZone(StoreTime.ZONE).toLocalDate())
        fun thisMonth(clock: Clock): Month = Month(YearMonth.from(clock.instant().atZone(StoreTime.ZONE)))
    }
}

/** True when the period has not started yet, so the UI can disable "next". */
fun ReportPeriod.isAfterNow(clock: Clock): Boolean = startMillis > clock.millis()
