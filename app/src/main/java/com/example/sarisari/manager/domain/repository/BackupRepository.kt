package com.example.sarisari.manager.domain.repository

import com.example.sarisari.manager.domain.model.BackupOutcome
import com.example.sarisari.manager.domain.model.ExportOutcome
import com.example.sarisari.manager.domain.model.RestoreResult

/** Locations are Storage Access Framework document URIs, passed as strings to keep this layer Android-free. */
interface BackupRepository {
    suspend fun createBackup(destinationUri: String): BackupOutcome

    /** Copies and fully validates a backup without touching live data. On success it is kept as the pending restore. */
    suspend fun prepareRestore(sourceUri: String): BackupOutcome

    /** Stages the pending restore so it replaces the database at the next app start. Returns false if none is pending. */
    suspend fun confirmRestore(): Boolean

    suspend fun discardPendingRestore()

    /** The outcome of the last restore applied at startup, reported once. */
    suspend fun takeLastRestoreResult(): RestoreResult?
}

interface ExportRepository {
    /** One row per sale item in [startMillis, endMillis). */
    suspend fun exportSales(destinationUri: String, startMillis: Long, endMillis: Long): ExportOutcome
    suspend fun exportProducts(destinationUri: String): ExportOutcome
}
