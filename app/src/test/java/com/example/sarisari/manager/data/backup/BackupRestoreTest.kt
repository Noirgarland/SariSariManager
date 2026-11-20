package com.example.sarisari.manager.data.backup

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.sarisari.manager.data.local.database.ALL_MIGRATIONS
import com.example.sarisari.manager.data.local.database.AppDatabase
import com.example.sarisari.manager.data.local.entity.ProductEntity
import com.example.sarisari.manager.domain.model.BackupError
import com.example.sarisari.manager.domain.model.BackupOutcome
import com.example.sarisari.manager.domain.model.RestoreResult
import com.example.sarisari.manager.testing.FakeClock
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.time.Instant
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.SQLiteMode

/** LEGACY SQLite mode: the native runtime loader fails when the user home path contains spaces. */
@RunWith(RobolectricTestRunner::class)
@SQLiteMode(SQLiteMode.Mode.LEGACY)
class BackupRestoreTest {
    private val context: Context = ApplicationProvider.getApplicationContext()
    private val dbName = "live-test.db"
    private val clock = FakeClock(Instant.parse("2025-11-18T02:00:00Z"))
    private lateinit var db: AppDatabase
    private lateinit var repo: BackupRepositoryImpl

    @Before
    fun setUp() {
        db = openLive()
        repo = BackupRepositoryImpl(context, db, clock, Dispatchers.Unconfined)
    }

    @After
    fun tearDown() {
        db.close()
        context.deleteDatabase(dbName)
        File(context.filesDir, "restore").deleteRecursively()
    }

    private fun openLive() = Room.databaseBuilder(context, AppDatabase::class.java, dbName)
        .addMigrations(*ALL_MIGRATIONS)
        .allowMainThreadQueries()
        .build()

    private suspend fun addProduct(database: AppDatabase, name: String) = database.productDao().save(
        ProductEntity(
            name = name, categoryId = null, baseUnitName = "piece", costCentavos = 100, sellingPriceCentavos = 200,
            stockQuantity = 0, lowStockThreshold = 1, isArchived = false, createdAt = 0, updatedAt = 0,
        ),
        emptyList(), now = 1,
    )

    private suspend fun productNames(database: AppDatabase) =
        database.productDao().observe("", null, true).first().map { it.product.name }

    private suspend fun backupBytes(): ByteArray = ByteArrayOutputStream().also { repo.writeBackupTo(it) }.toByteArray()

    /** Builds an archive by hand, for files a real backup would never produce. */
    private fun archive(database: ByteArray?, manifest: BackupManifest?): ByteArray {
        val out = ByteArrayOutputStream()
        ZipOutputStream(out).use { zip ->
            manifest?.let {
                zip.putNextEntry(ZipEntry(BackupArchive.MANIFEST_ENTRY))
                zip.write(it.toJson().toByteArray())
                zip.closeEntry()
            }
            database?.let {
                zip.putNextEntry(ZipEntry(BackupArchive.DATABASE_ENTRY))
                zip.write(it)
                zip.closeEntry()
            }
        }
        return out.toByteArray()
    }

    private fun manifestFor(bytes: ByteArray, schema: Int = AppDatabase.VERSION): BackupManifest {
        val temp = File.createTempFile("backup-db", ".bin").apply { writeBytes(bytes) }
        return BackupManifest(BackupManifest.FORMAT_VERSION, "1.0", schema, 123, BackupArchive.sha256(temp)).also { temp.delete() }
    }

    private fun extractDatabase(backup: ByteArray): ByteArray {
        val out = File.createTempFile("extract", ".db")
        BackupArchive.read(ByteArrayInputStream(backup), out)
        return out.readBytes().also { out.delete() }
    }

    private fun prepare(bytes: ByteArray) = repo.prepareRestoreFrom(ByteArrayInputStream(bytes))

    @Test
    fun validBackupHasManifestAndPassesValidation() = runTest {
        addProduct(db, "Coke")
        val bytes = backupBytes()
        val outcome = prepare(bytes) as BackupOutcome.Success
        assertEquals(AppDatabase.VERSION, outcome.info.schemaVersion)
        assertEquals(clock.millis(), outcome.info.createdAtMillis)
    }

    @Test
    fun truncatedFileIsRejected() = runTest {
        addProduct(db, "Coke")
        val bytes = backupBytes()
        assertEquals(BackupOutcome.Failure(BackupError.UNREADABLE), prepare(bytes.copyOf(bytes.size / 2)))
        assertFalse(repo.confirmRestore())
    }

    @Test
    fun corruptedArchiveBytesAreRejected() = runTest {
        val bytes = backupBytes()
        val corrupted = bytes.copyOf().also { for (i in it.size / 3 until it.size / 3 + 64) it[i] = 0x5A }
        assertTrue(prepare(corrupted) is BackupOutcome.Failure)
        assertFalse(repo.confirmRestore())
    }

    @Test
    fun corruptedDatabaseWithMatchingChecksumFailsTheIntegrityCheck() = runTest {
        val garbage = ByteArray(8192) { (it * 31).toByte() }
        assertEquals(BackupOutcome.Failure(BackupError.INTEGRITY_FAILED), prepare(archive(garbage, manifestFor(garbage))))
    }

    @Test
    fun wrongChecksumIsRejected() = runTest {
        val database = extractDatabase(backupBytes())
        val wrong = manifestFor(database).copy(databaseSha256 = "0".repeat(64))
        assertEquals(BackupOutcome.Failure(BackupError.CHECKSUM_MISMATCH), prepare(archive(database, wrong)))
    }

    @Test
    fun newerSchemaIsRejected() = runTest {
        val database = extractDatabase(backupBytes())
        val newer = manifestFor(database, schema = AppDatabase.VERSION + 1)
        assertEquals(BackupOutcome.Failure(BackupError.NEWER_SCHEMA), prepare(archive(database, newer)))
    }

    @Test
    fun missingManifestAndNonZipFilesAreRejected() = runTest {
        val database = extractDatabase(backupBytes())
        assertEquals(BackupOutcome.Failure(BackupError.MANIFEST_MISSING), prepare(archive(database, null)))
        assertEquals(BackupOutcome.Failure(BackupError.MANIFEST_MISSING), prepare("not a backup".toByteArray()))
        assertEquals(BackupOutcome.Failure(BackupError.UNREADABLE), prepare(archive(null, manifestFor(database))))
    }

    @Test
    fun databaseWithoutExpectedTablesIsRejected() = runTest {
        val file = File(context.cacheDir, "partial.db").apply { delete() }
        SQLiteDatabase.openOrCreateDatabase(file, null).use {
            it.execSQL("CREATE TABLE app_settings (`key` TEXT PRIMARY KEY NOT NULL, value TEXT NOT NULL)")
            it.version = AppDatabase.VERSION
        }
        val bytes = file.readBytes().also { file.delete() }
        assertEquals(BackupOutcome.Failure(BackupError.MISSING_TABLES), prepare(archive(bytes, manifestFor(bytes))))
    }

    @Test
    fun confirmedRestoreReplacesDataAtNextStart() = runTest {
        addProduct(db, "Coke")
        val bytes = backupBytes()
        addProduct(db, "Sprite")
        assertTrue(prepare(bytes) is BackupOutcome.Success)
        assertTrue(repo.confirmRestore())
        // Live data is untouched until the next start.
        assertEquals(listOf("Coke", "Sprite"), productNames(db))

        db.close()
        StagedRestore.applyIfPresent(context, dbName)
        db = openLive()
        assertEquals(listOf("Coke"), productNames(db))
        assertEquals(RestoreResult.APPLIED, StagedRestore.takeResult(context))
        assertNull(StagedRestore.takeResult(context))
        assertFalse(StagedRestore.stagedFile(context).exists())
    }

    @Test
    fun failedReplacementPutsTheSafetyCopyBack() = runTest {
        addProduct(db, "Coke")
        db.close()
        StagedRestore.stagedFile(context).apply { parentFile?.mkdirs() }.writeBytes(ByteArray(4096) { 7 })
        StagedRestore.applyIfPresent(context, dbName)
        db = openLive()
        assertEquals(listOf("Coke"), productNames(db))
        assertEquals(RestoreResult.ROLLED_BACK, StagedRestore.takeResult(context))
    }

    @Test
    fun olderSchemaBackupIsMigratedOnOpen() = runTest {
        val file = File(context.cacheDir, "v1.db").apply { delete() }
        SQLiteDatabase.openOrCreateDatabase(file, null).use {
            it.execSQL(
                "CREATE TABLE IF NOT EXISTS `app_settings` (`key` TEXT NOT NULL, `value` TEXT NOT NULL, " +
                    "`created_at` INTEGER NOT NULL, `updated_at` INTEGER NOT NULL, PRIMARY KEY(`key`))"
            )
            it.execSQL("INSERT INTO app_settings VALUES ('store_name', 'Old Store', 1, 1)")
            it.version = 1
        }
        val bytes = file.readBytes().also { file.delete() }
        assertTrue(prepare(archive(bytes, manifestFor(bytes, schema = 1))) is BackupOutcome.Success)
        assertTrue(repo.confirmRestore())
        db.close()
        StagedRestore.applyIfPresent(context, dbName)
        db = openLive()
        assertEquals("Old Store", db.appSettingDao().getByKey("store_name")?.value)
        assertTrue(productNames(db).isEmpty())
    }
}
