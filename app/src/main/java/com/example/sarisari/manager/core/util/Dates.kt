package com.example.sarisari.manager.core.util

import com.example.sarisari.manager.domain.model.StoreTime
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/** Dates are stored as UTC epoch milliseconds and shown, and grouped into days, in Philippine time. */
object Dates {
    val STORE_ZONE: ZoneId = StoreTime.ZONE

    private val dateTimeFormatter: DateTimeFormatter =
        DateTimeFormatter.ofPattern("MMM d, yyyy h:mm a", Locale.ENGLISH).withZone(STORE_ZONE)

    private val dateFormatter: DateTimeFormatter =
        DateTimeFormatter.ofPattern("EEE, MMM d, yyyy", Locale.ENGLISH).withZone(STORE_ZONE)
    private val monthFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("MMMM yyyy", Locale.ENGLISH)
    private val timeFormatter: DateTimeFormatter =
        DateTimeFormatter.ofPattern("h:mm a", Locale.ENGLISH).withZone(STORE_ZONE)

    fun formatDateTime(epochMillis: Long): String = dateTimeFormatter.format(Instant.ofEpochMilli(epochMillis))

    fun formatDate(epochMillis: Long): String = dateFormatter.format(Instant.ofEpochMilli(epochMillis))

    fun formatTime(epochMillis: Long): String = timeFormatter.format(Instant.ofEpochMilli(epochMillis))

    fun formatMonth(month: java.time.YearMonth): String = monthFormatter.format(month)

    private val fileStampFormatter: DateTimeFormatter =
        DateTimeFormatter.ofPattern("yyyyMMdd-HHmm", Locale.ENGLISH).withZone(STORE_ZONE)

    /** A sortable stamp for file names, such as 20251118-1030. */
    fun fileStamp(epochMillis: Long): String = fileStampFormatter.format(Instant.ofEpochMilli(epochMillis))
}
