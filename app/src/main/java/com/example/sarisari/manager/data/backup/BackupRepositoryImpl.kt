package com.example.sarisari.manager.data.backup

import android.content.Context
import android.net.Uri
import androidx.room.withTransaction
import com.example.sarisari.manager.data.local.database.AppDatabase
import com.example.sarisari.manager.di.IoDispatcher
import com.example.sarisari.manager.domain.model.BackupError
import com.example.sarisari.manager.domain.model.BackupInfo
import com.example.sarisari.manager.domain.model.BackupOutcome
import com.example.sarisari.manager.domain.model.RestoreResult
import com.example.sarisari.manager.domain.repository.BackupRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.time.Clock
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext

@Singleton
class BackupRepositoryImpl @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val database: AppDatabase,
    private val clock: Clock,
    @param:IoDispatcher private val io: CoroutineDispatcher,
) : BackupRepository {

    private val workDir get() = File(context.cacheDir, "backup").apply { mkdirs() }
    private val pendingRestore get() = File(workDir, "pending-restore.db")

    override suspend fun createBackup(destinationUri: String): BackupOutcome = withContext(io) {
        val archive = File(workDir, "backup.zip")
        try {
            val info = writeBackup(archive)
            val output = context.contentResolver.openOutputStream(Uri.parse(destinationUri), "wt")
                ?: return@withContext BackupOutcome.Failure(BackupError.WRITE_FAILED)
            output.use { out -> archive.inputStream().use { it.copyTo(out) } }
            BackupOutcome.Success(info)
        } catch (_: IOException) {
            BackupOutcome.Failure(BackupError.WRITE_FAILED)
        } catch (_: SecurityException) {
            BackupOutcome.Failure(BackupError.WRITE_FAILED)
        } finally {
            archive.delete()
        }
    }

    /** Writes a complete backup archive to [archive]. Visible for tests. */
    internal suspend fun writeBackup(archive: File): BackupInfo {
        val snapshot = File(workDir, "snapshot.db")
        try {
            // Move everything from the write-ahead log into the main file, then copy it while holding
            // a write transaction so no other write or checkpoint can change the file mid-copy.
            database.openHelper.writableDatabase.query("PRAGMA wal_checkpoint(TRUNCATE)").use { it.moveToFirst() }
            database.withTransaction {
                File(database.openHelper.writableDatabase.path!!).copyTo(snapshot, overwrite = true)
            }
            val manifest = BackupManifest(
                formatVersion = BackupManifest.FORMAT_VERSION,
                appVersion = appVersion(),
                schemaVersion = AppDatabase.VERSION,
                createdAtMillis = clock.millis(),
                databaseSha256 = BackupArchive.sha256(snapshot),
            )
            archive.outputStream().use { BackupArchive.write(it, snapshot, manifest) }
            return BackupInfo(manifest.createdAtMillis, manifest.schemaVersion, manifest.appVersion)
        } finally {
            snapshot.delete()
        }
    }

    override suspend fun prepareRestore(sourceUri: String): BackupOutcome = withContext(io) {
        try {
            val input = context.contentResolver.openInputStream(Uri.parse(sourceUri))
                ?: return@withContext BackupOutcome.Failure(BackupError.UNREADABLE)
            input.use { prepareRestoreFrom(it) }
        } catch (_: SecurityException) {
            BackupOutcome.Failure(BackupError.UNREADABLE)
        } catch (_: IOException) {
            BackupOutcome.Failure(BackupError.UNREADABLE)
        }
    }

    /** Extracts and validates a backup into the pending slot. Live data is never touched. Visible for tests. */
    internal fun prepareRestoreFrom(input: InputStream): BackupOutcome {
        val candidate = File(workDir, "candidate.db")
        pendingRestore.delete()
        return try {
            val manifest = BackupArchive.read(input, candidate)
            val error = BackupValidator.validate(candidate, manifest, AppDatabase.VERSION)
            if (error != null) {
                BackupOutcome.Failure(error)
            } else {
                check(candidate.renameTo(pendingRestore)) { "Could not keep the validated backup" }
                BackupOutcome.Success(BackupInfo(manifest.createdAtMillis, manifest.schemaVersion, manifest.appVersion))
            }
        } catch (e: InvalidBackupException) {
            BackupOutcome.Failure(e.error)
        } finally {
            candidate.delete()
        }
    }

    override suspend fun confirmRestore(): Boolean = withContext(io) {
        val pending = pendingRestore
        if (!pending.exists()) return@withContext false
        StagedRestore.stage(context, pending)
        pending.delete()
        true
    }

    override suspend fun discardPendingRestore() {
        withContext(io) { pendingRestore.delete() }
    }

    override suspend fun takeLastRestoreResult(): RestoreResult? = withContext(io) { StagedRestore.takeResult(context) }

    @Suppress("DEPRECATION")
    private fun appVersion(): String =
        context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: "unknown"

    /** Used by tests to write an archive to any stream. */
    internal suspend fun writeBackupTo(output: OutputStream): BackupInfo {
        val archive = File(workDir, "backup-test.zip")
        try {
            val info = writeBackup(archive)
            archive.inputStream().use { it.copyTo(output) }
            return info
        } finally {
            archive.delete()
        }
    }
}
