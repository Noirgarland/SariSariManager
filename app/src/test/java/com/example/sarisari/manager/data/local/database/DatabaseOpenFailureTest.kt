package com.example.sarisari.manager.data.local.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.test.core.app.ApplicationProvider
import com.example.sarisari.manager.data.local.entity.AppSettingEntity
import org.junit.After
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.SQLiteMode

/** Same schema as [AppDatabase] but declared one version higher, with no migration. */
@Database(entities = [AppSettingEntity::class], version = AppDatabase.VERSION + 1, exportSchema = false)
abstract class HigherVersionDatabase : RoomDatabase()

/**
 * Validates: Requirements 5.11
 *
 * LEGACY SQLite mode: the native runtime loader fails when the user home path contains spaces.
 */
@RunWith(RobolectricTestRunner::class)
@SQLiteMode(SQLiteMode.Mode.LEGACY)
class DatabaseOpenFailureTest {

    private val context: Context = ApplicationProvider.getApplicationContext()
    private val name = "open-failure-test.db"

    @After
    fun tearDown() {
        context.deleteDatabase(name)
    }

    @Test
    fun openingWithHigherVersionAndNoMigrationFailsAndLeavesFileUnchanged() {
        val v1 = Room.databaseBuilder(context, AppDatabase::class.java, name).build()
        v1.openHelper.writableDatabase
        v1.close()

        val file = context.getDatabasePath(name)
        assertTrue(file.exists())
        val before = file.readBytes()

        val v2 = Room.databaseBuilder(context, HigherVersionDatabase::class.java, name).build()
        try {
            v2.openHelper.writableDatabase
            fail("Expected opening a higher version without a migration to throw")
        } catch (_: Exception) {
            // expected
        } finally {
            v2.close()
        }

        assertArrayEquals(before, file.readBytes())
    }
}
