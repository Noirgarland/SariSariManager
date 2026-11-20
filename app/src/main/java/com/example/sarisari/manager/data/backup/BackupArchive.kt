package com.example.sarisari.manager.data.backup

import com.example.sarisari.manager.domain.model.BackupError
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.security.MessageDigest
import java.util.zip.ZipEntry
import java.util.zip.ZipException
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

class InvalidBackupException(val error: BackupError) : Exception(error.name)

/** Reads and writes the backup archive: a zip holding [DATABASE_ENTRY] and [MANIFEST_ENTRY]. */
object BackupArchive {
    const val DATABASE_ENTRY = "sarisari.db"
    const val MANIFEST_ENTRY = "manifest.json"
    const val MAX_DATABASE_BYTES = 512L * 1024 * 1024
    private const val MAX_MANIFEST_BYTES = 64L * 1024

    fun write(output: OutputStream, databaseFile: File, manifest: BackupManifest) {
        ZipOutputStream(output.buffered()).use { zip ->
            zip.putNextEntry(ZipEntry(MANIFEST_ENTRY))
            zip.write(manifest.toJson().toByteArray(Charsets.UTF_8))
            zip.closeEntry()
            zip.putNextEntry(ZipEntry(DATABASE_ENTRY))
            databaseFile.inputStream().use { it.copyTo(zip) }
            zip.closeEntry()
        }
    }

    /**
     * Extracts the database entry into [databaseOut] and returns the manifest.
     * Only the two known entries are read. Their names are never used as file paths.
     * @throws InvalidBackupException when the archive is unreadable, truncated, too large, or incomplete.
     */
    fun read(input: InputStream, databaseOut: File): BackupManifest {
        var manifestText: String? = null
        var databaseFound = false
        try {
            ZipInputStream(input.buffered()).use { zip ->
                while (true) {
                    val entry = zip.nextEntry ?: break
                    when (entry.name) {
                        MANIFEST_ENTRY -> manifestText = String(readLimited(zip, MAX_MANIFEST_BYTES), Charsets.UTF_8)
                        DATABASE_ENTRY -> {
                            databaseOut.outputStream().use { copyLimited(zip, it, MAX_DATABASE_BYTES) }
                            databaseFound = true
                        }
                    }
                    zip.closeEntry()
                }
            }
        } catch (e: InvalidBackupException) {
            throw e
        } catch (_: ZipException) {
            throw InvalidBackupException(BackupError.UNREADABLE)
        } catch (_: IOException) {
            // EOFException from a truncated archive lands here too.
            throw InvalidBackupException(BackupError.UNREADABLE)
        }
        val text = manifestText ?: throw InvalidBackupException(BackupError.MANIFEST_MISSING)
        val manifest = BackupManifest.parse(text) ?: throw InvalidBackupException(BackupError.MANIFEST_MISSING)
        if (!databaseFound) throw InvalidBackupException(BackupError.UNREADABLE)
        return manifest
    }

    fun sha256(file: File): String {
        val digest = MessageDigest.getInstance("SHA-256")
        file.inputStream().use { input ->
            val buffer = ByteArray(BUFFER_SIZE)
            while (true) {
                val read = input.read(buffer)
                if (read < 0) break
                digest.update(buffer, 0, read)
            }
        }
        return digest.digest().joinToString("") { "%02x".format(it) }
    }

    private fun readLimited(input: InputStream, limit: Long): ByteArray {
        val out = java.io.ByteArrayOutputStream()
        copyLimited(input, out, limit)
        return out.toByteArray()
    }

    private fun copyLimited(input: InputStream, output: OutputStream, limit: Long) {
        val buffer = ByteArray(BUFFER_SIZE)
        var total = 0L
        while (true) {
            val read = input.read(buffer)
            if (read < 0) break
            total += read
            if (total > limit) throw InvalidBackupException(BackupError.TOO_LARGE)
            output.write(buffer, 0, read)
        }
    }

    private const val BUFFER_SIZE = 64 * 1024
}
