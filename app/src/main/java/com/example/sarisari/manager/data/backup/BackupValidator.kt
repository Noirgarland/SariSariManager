package com.example.sarisari.manager.data.backup

import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteException
import com.example.sarisari.manager.domain.model.BackupError
import java.io.File

/** Checks an extracted backup database before it may replace live data. */
object BackupValidator {

    /** Tables each schema version must contain. A version includes every table of the versions before it. */
    private val tablesAddedInVersion: Map<Int, List<String>> = mapOf(
        1 to listOf("app_settings"),
        2 to listOf("categories", "products", "product_units"),
        3 to listOf("stock_movements"),
        4 to listOf("sales", "sale_items"),
        5 to listOf("expenses", "store_sessions"),
    )

    fun expectedTables(schemaVersion: Int): List<String> =
        tablesAddedInVersion.filterKeys { it <= schemaVersion }.values.flatten()

    /** Returns null when the database is valid, otherwise the first problem found. */
    fun validate(databaseFile: File, manifest: BackupManifest, supportedSchemaVersion: Int): BackupError? {
        if (manifest.schemaVersion > supportedSchemaVersion) return BackupError.NEWER_SCHEMA
        if (manifest.schemaVersion < 1) return BackupError.UNREADABLE
        if (BackupArchive.sha256(databaseFile) != manifest.databaseSha256) return BackupError.CHECKSUM_MISMATCH
        return try {
            SQLiteDatabase.openDatabase(databaseFile.path, null, SQLiteDatabase.OPEN_READONLY).use { db ->
                checkOpened(db, manifest.schemaVersion)
            }
        } catch (_: SQLiteException) {
            BackupError.INTEGRITY_FAILED
        }
    }

    private fun checkOpened(db: SQLiteDatabase, schemaVersion: Int): BackupError? {
        if (db.version != schemaVersion) return BackupError.INTEGRITY_FAILED
        val integrity = db.rawQuery("PRAGMA integrity_check", null).use { cursor ->
            if (cursor.moveToFirst()) cursor.getString(0) else null
        }
        if (integrity != "ok") return BackupError.INTEGRITY_FAILED
        val tables = db.rawQuery("SELECT name FROM sqlite_master WHERE type = 'table'", null).use { cursor ->
            buildSet { while (cursor.moveToNext()) add(cursor.getString(0)) }
        }
        return if (tables.containsAll(expectedTables(schemaVersion))) null else BackupError.MISSING_TABLES
    }
}
