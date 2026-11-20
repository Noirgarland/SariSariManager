package com.example.sarisari.manager.domain.model

/** What a backup file contains, read from its manifest. */
data class BackupInfo(
    val createdAtMillis: Long,
    val schemaVersion: Int,
    val appVersion: String,
)

enum class BackupError {
    /** The file is not a readable backup archive, or it is truncated. */
    UNREADABLE,
    MANIFEST_MISSING,
    CHECKSUM_MISMATCH,
    INTEGRITY_FAILED,
    MISSING_TABLES,
    NEWER_SCHEMA,
    TOO_LARGE,
    WRITE_FAILED,
}

sealed interface BackupOutcome {
    data class Success(val info: BackupInfo) : BackupOutcome
    data class Failure(val error: BackupError) : BackupOutcome
}

/** What happened to a restore that was applied at app startup. */
enum class RestoreResult { APPLIED, ROLLED_BACK }

sealed interface ExportOutcome {
    data class Success(val rowCount: Int) : ExportOutcome
    data object Failure : ExportOutcome
}
