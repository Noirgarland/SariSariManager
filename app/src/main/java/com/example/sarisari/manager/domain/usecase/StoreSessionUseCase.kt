package com.example.sarisari.manager.domain.usecase

import com.example.sarisari.manager.domain.model.StoreAlreadyOpenException
import com.example.sarisari.manager.domain.model.StoreSession
import com.example.sarisari.manager.domain.model.StoreTime
import com.example.sarisari.manager.domain.repository.StoreSessionRepository
import java.time.Clock
import java.time.Instant
import java.time.LocalTime
import java.time.ZonedDateTime
import javax.inject.Inject
import kotlinx.coroutines.flow.first

enum class StoreSessionResult {
    DONE,
    ALREADY_OPEN,

    /** The session is already closed. */
    NOT_OPEN,

    /** The chosen time has not happened yet. */
    TIME_IN_FUTURE,

    /** A closing time before the session's opening time. */
    BEFORE_OPENING,

    /** An opening time before the previous session closed. */
    BEFORE_LAST_CLOSE,
}

/**
 * Opening and closing only record times. They never block sales.
 * Times picked on a clock face are turned into exact moments in store time (Asia/Manila).
 */
class StoreSessionUseCase @Inject constructor(
    private val sessions: StoreSessionRepository,
    private val clock: Clock,
) {

    /** Opens the store now. */
    suspend fun open(): StoreSessionResult = open(clock.millis())

    /** Opens the store at [time] today, in store time. A time later than now is rejected. */
    suspend fun openAt(time: LocalTime): StoreSessionResult {
        val today = clock.instant().atZone(StoreTime.ZONE).toLocalDate()
        return open(today.atTime(time).atZone(StoreTime.ZONE).millis())
    }

    private suspend fun open(openedAtMillis: Long): StoreSessionResult {
        if (openedAtMillis > clock.millis()) return StoreSessionResult.TIME_IN_FUTURE
        val latest = sessions.observeLatestSession().first()
        if (latest?.isOpen == true) return StoreSessionResult.ALREADY_OPEN
        val lastClosed = latest?.closedAtMillis
        var openedAt = openedAtMillis
        if (lastClosed != null && openedAt < lastClosed) {
            // A picked time has no seconds. Reopening in the same minute the store closed is allowed.
            if (lastClosed - openedAt >= ONE_MINUTE_MILLIS) return StoreSessionResult.BEFORE_LAST_CLOSE
            openedAt = lastClosed
        }
        return try {
            sessions.open(openedAt)
            StoreSessionResult.DONE
        } catch (_: StoreAlreadyOpenException) {
            StoreSessionResult.ALREADY_OPEN
        }
    }

    /**
     * Closes [session] at [time] on a clock face: the first moment with that time after the store opened.
     * Opened 8:00 PM and closed 1:00 AM means the next day. For a session left open for days,
     * this is the closing time on the day it was opened.
     */
    suspend fun closeAt(session: StoreSession, time: LocalTime): StoreSessionResult =
        close(session, firstAtOrAfter(time, session.openedAtMillis))

    /** [closedAtMillis] defaults to now. */
    suspend fun close(session: StoreSession, closedAtMillis: Long = clock.millis()): StoreSessionResult {
        if (!session.isOpen) return StoreSessionResult.NOT_OPEN
        if (closedAtMillis < session.openedAtMillis) return StoreSessionResult.BEFORE_OPENING
        if (closedAtMillis > clock.millis()) return StoreSessionResult.TIME_IN_FUTURE
        sessions.close(session.id, closedAtMillis)
        return StoreSessionResult.DONE
    }

    private fun firstAtOrAfter(time: LocalTime, referenceMillis: Long): Long {
        val reference = Instant.ofEpochMilli(referenceMillis).atZone(StoreTime.ZONE)
        // Picked times have no seconds, so compare at minute precision: the opening minute itself counts.
        val referenceMinute = reference.withSecond(0).withNano(0)
        val sameDay = reference.toLocalDate().atTime(time).atZone(StoreTime.ZONE)
        val result = if (sameDay.isBefore(referenceMinute)) sameDay.plusDays(1) else sameDay
        return maxOf(result.millis(), referenceMillis)
    }

    private fun ZonedDateTime.millis(): Long = toInstant().toEpochMilli()

    private companion object {
        const val ONE_MINUTE_MILLIS = 60_000L
    }
}
