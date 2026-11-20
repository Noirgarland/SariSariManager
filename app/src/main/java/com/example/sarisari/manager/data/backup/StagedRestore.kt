package com.example.sarisari.manager.data.backup

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import com.example.sarisari.manager.domain.model.RestoreResult
import java.io.File

/**
 * Applies a staged restore at app startup, before Room opens the database.
 * The current database is first saved as a safety copy. If the replacement fails, the safety copy is put back.
 */
object StagedRestore {
    private const val DIR = "restore"
    private const val STAGED = "staged.db"
    private const val RESULT = "result.txt"
    private const val SAFETY_DIR = "safety"
    private val SIDE_FILE_SUFFIXES = listOf("-wal", "-shm", "-journal")

    fun stagedFile(context: Context): File = File(File(context.filesDir, DIR), STAGED)

    private fun resultFile(context: Context) = File(File(context.filesDir, DIR), RESULT)

    /** Moves a validated database into place for the next start. */
    fun stage(context: Context, validatedDatabase: File) {
        val target = stagedFile(context)
        target.parentFile?.mkdirs()
        val temp = File(target.path + ".tmp")
        validatedDatabase.copyTo(temp, overwrite = true)
        if (!temp.renameTo(target)) {
            temp.delete()
            error("Could not stage the restore")
        }
    }

    fun applyIfPresent(context: Context, databaseName: String) {
        val staged = stagedFile(context)
        if (!staged.exists()) return
        val database = context.getDatabasePath(databaseName)
        database.parentFile?.mkdirs()
        val safetyDir = File(File(context.filesDir, DIR), SAFETY_DIR)
        val result = try {
            saveSafetyCopy(database, safetyDir)
            replace(database, staged)
            check(isHealthy(database)) { "Restored database failed its integrity check" }
            RestoreResult.APPLIED
        } catch (_: Exception) {
            putBackSafetyCopy(database, safetyDir)
            RestoreResult.ROLLED_BACK
        } finally {
            staged.delete()
        }
        resultFile(context).writeText(result.name)
    }

    fun takeResult(context: Context): RestoreResult? {
        val file = resultFile(context)
        if (!file.exists()) return null
        val result = RestoreResult.entries.firstOrNull { it.name == file.readText().trim() }
        file.delete()
        return result
    }

    private fun filesOf(database: File) = listOf(database) + SIDE_FILE_SUFFIXES.map { File(database.path + it) }

    private fun saveSafetyCopy(database: File, safetyDir: File) {
        safetyDir.deleteRecursively()
        safetyDir.mkdirs()
        filesOf(database).filter { it.exists() }.forEach { it.copyTo(File(safetyDir, it.name), overwrite = true) }
    }

    private fun replace(database: File, staged: File) {
        val incoming = File(database.path + ".restoring")
        staged.copyTo(incoming, overwrite = true)
        filesOf(database).forEach { it.delete() }
        check(incoming.renameTo(database)) { "Could not move the restored database into place" }
    }

    private fun putBackSafetyCopy(database: File, safetyDir: File) {
        filesOf(database).forEach { it.delete() }
        safetyDir.listFiles()?.forEach { it.copyTo(File(database.parentFile, it.name), overwrite = true) }
    }

    private fun isHealthy(database: File): Boolean =
        SQLiteDatabase.openDatabase(database.path, null, SQLiteDatabase.OPEN_READONLY).use { db ->
            db.rawQuery("PRAGMA quick_check", null).use { it.moveToFirst() && it.getString(0) == "ok" }
        }
}
