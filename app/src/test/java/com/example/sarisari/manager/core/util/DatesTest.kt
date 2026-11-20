package com.example.sarisari.manager.core.util

import org.junit.Assert.assertEquals
import org.junit.Test

class DatesTest {
    @Test
    fun formatsInPhilippineTimeRegardlessOfDeviceZone() {
        // 2025-11-18T16:30:00Z is 2025-11-19 00:30 in Manila (UTC+8)
        assertEquals("Nov 19, 2025 12:30 AM", Dates.formatDateTime(1_763_483_400_000))
        assertEquals("Jan 1, 1970 8:00 AM", Dates.formatDateTime(0))
    }
}
