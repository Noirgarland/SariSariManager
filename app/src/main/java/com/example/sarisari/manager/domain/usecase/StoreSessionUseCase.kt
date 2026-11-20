package com.example.sarisari.manager.domain.usecase

import com.example.sarisari.manager.domain.model.StoreAlreadyOpenException
import com.example.sarisari.manager.domain.model.StoreSession
import com.example.sarisari.manager.domain.repository.StoreSessionRepository
import java.time.Clock
import javax.inject.Inject

enum class StoreSessionResult { DONE, ALREADY_OPEN, INVALID_TIME }

/** Opening and closing only record times. They never block sales. */
class StoreSessionUseCase @Inject constructor(
    private val sessions: StoreSessionRepository,
    private val clock: Clock,
) {
    suspend fun open(): StoreSessionResult = try {
        sessions.open(clock.millis())
        StoreSessionResult.DONE
    } catch (_: StoreAlreadyOpenException) {
        StoreSessionResult.ALREADY_OPEN
    }

    /** [closedAtMillis] defaults to now. It may be set earlier for a session the owner forgot to close. */
    suspend fun close(session: StoreSession, closedAtMillis: Long = clock.millis()): StoreSessionResult {
        if (!session.isOpen || closedAtMillis < session.openedAtMillis || closedAtMillis > clock.millis()) {
            return StoreSessionResult.INVALID_TIME
        }
        sessions.close(session.id, closedAtMillis)
        return StoreSessionResult.DONE
    }
}
