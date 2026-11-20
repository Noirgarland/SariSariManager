package com.example.sarisari.manager.testing

import java.time.Clock
import java.time.Instant
import java.time.ZoneId
import java.time.ZoneOffset

/** Hand-written [Clock] with a settable current instant. Always reports UTC. */
class FakeClock(initial: Instant = Instant.EPOCH) : Clock() {

    @Volatile
    private var current: Instant = initial

    fun set(instant: Instant) {
        current = instant
    }

    fun setMillis(epochMilli: Long) {
        current = Instant.ofEpochMilli(epochMilli)
    }

    override fun getZone(): ZoneId = ZoneOffset.UTC

    override fun withZone(zone: ZoneId): Clock = this

    override fun instant(): Instant = current
}
