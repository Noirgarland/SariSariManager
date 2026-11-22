package com.example.sarisari.manager.data.local.database

import androidx.room.migration.Migration
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** Validates: Requirements 5.10, 9.6 */
class MigrationsTest {

    @Test
    fun databaseVersionIsSeven() {
        assertEquals(7, AppDatabase.VERSION)
    }

    @Test
    fun migrationChainIsContiguousFromOneToCurrentVersion() {
        assertChain(ALL_MIGRATIONS.toList(), AppDatabase.VERSION)
    }

    @Test
    fun chainAssertionAcceptsValidChainAndRejectsBrokenOnes() {
        assertChain(listOf(migration(1, 2), migration(2, 3)), 3)
        assertFails { assertChain(listOf(migration(2, 3)), 3) }
        assertFails { assertChain(listOf(migration(1, 2), migration(3, 4)), 4) }
        assertFails { assertChain(listOf(migration(1, 2)), 3) }
    }

    @Test
    fun sourcesContainNoDestructiveFallback() {
        val roots = listOf(File("src/main"), File("app/src/main")).filter { it.isDirectory }
        assertTrue("src/main not found", roots.isNotEmpty())
        val offenders = roots.flatMap { root ->
            root.walkTopDown()
                .filter { it.isFile && it.extension in setOf("kt", "java") }
                .filter { it.readText().contains("fallbackToDestructiveMigration") }
                .map { it.path }
                .toList()
        }
        assertTrue("Destructive fallback found in $offenders", offenders.isEmpty())
    }

    private fun assertChain(migrations: List<Migration>, version: Int) {
        if (migrations.isEmpty()) {
            assertEquals("No migrations requires version 1", 1, version)
            return
        }
        assertEquals(1, migrations.first().startVersion)
        migrations.zipWithNext { a, b -> assertEquals(a.endVersion, b.startVersion) }
        migrations.forEach { assertEquals(it.startVersion + 1, it.endVersion) }
        assertEquals(version, migrations.last().endVersion)
    }

    private fun assertFails(block: () -> Unit) {
        val failed = try {
            block()
            false
        } catch (_: AssertionError) {
            true
        }
        assertTrue("Expected assertion failure", failed)
    }

    private fun migration(start: Int, end: Int): Migration = object : Migration(start, end) {
        override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) = Unit
    }
}
