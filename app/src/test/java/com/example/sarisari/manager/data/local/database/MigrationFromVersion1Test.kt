package com.example.sarisari.manager.data.local.database

import android.content.Context
import androidx.room.Room
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteOpenHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.SQLiteMode

/**
 * Builds a real version 1 database, then opens it with [AppDatabase] and [ALL_MIGRATIONS].
 * Room validates the migrated schema against the current schema on open, so a mismatch fails the test.
 *
 * LEGACY SQLite mode: the native runtime loader fails when the user home path contains spaces.
 */
@RunWith(RobolectricTestRunner::class)
@SQLiteMode(SQLiteMode.Mode.LEGACY)
class MigrationFromVersion1Test {
    private val context: Context = ApplicationProvider.getApplicationContext()
    private val name = "migration-test.db"

    @After
    fun tearDown() {
        context.deleteDatabase(name)
    }

    private fun createVersion1Database() {
        val callback = object : SupportSQLiteOpenHelper.Callback(1) {
            override fun onCreate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `app_settings` (`key` TEXT NOT NULL, `value` TEXT NOT NULL, " +
                        "`created_at` INTEGER NOT NULL, `updated_at` INTEGER NOT NULL, PRIMARY KEY(`key`))"
                )
                db.execSQL("INSERT INTO app_settings (`key`, value, created_at, updated_at) VALUES ('k', 'v', 1, 1)")
            }

            override fun onUpgrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) = Unit
        }
        val helper = FrameworkSQLiteOpenHelperFactory().create(
            SupportSQLiteOpenHelper.Configuration.builder(context).name(name).callback(callback).build()
        )
        helper.writableDatabase
        helper.close()
    }

    @Test
    fun migrationKeepsSettingsAndCreatesTablesMatchingTheCurrentSchema() = runTest {
        createVersion1Database()
        val db = Room.databaseBuilder(context, AppDatabase::class.java, name)
            .addMigrations(*ALL_MIGRATIONS)
            .allowMainThreadQueries()
            .build()
        try {
            assertEquals("v", db.appSettingDao().getByKey("k")?.value)
            listOf("categories", "products", "product_units", "stock_movements", "sales", "sale_items", "expenses", "store_sessions").forEach { table ->
                db.openHelper.readableDatabase.query("SELECT COUNT(*) FROM $table").use {
                    it.moveToFirst()
                    assertEquals(0, it.getInt(0))
                }
            }
        } finally {
            db.close()
        }
    }
}
