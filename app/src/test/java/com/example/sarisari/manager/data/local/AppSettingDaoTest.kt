package com.example.sarisari.manager.data.local

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.sarisari.manager.data.local.dao.AppSettingDao
import com.example.sarisari.manager.data.local.database.AppDatabase
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.SQLiteMode

/**
 * Validates: Requirements 9.4, 9.5
 *
 * LEGACY SQLite mode: the native runtime loader fails when the user home path contains spaces.
 */
@RunWith(RobolectricTestRunner::class)
@SQLiteMode(SQLiteMode.Mode.LEGACY)
class AppSettingDaoTest {

    private lateinit var db: AppDatabase
    private lateinit var dao: AppSettingDao

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        dao = db.appSettingDao()
    }

    @After
    fun tearDown() {
        db.close()
    }

    private fun rowCount(key: String): Int =
        db.openHelper.readableDatabase
            .query("SELECT COUNT(*) FROM app_settings WHERE `key` = ?", arrayOf<Any>(key))
            .use { c ->
                c.moveToFirst()
                c.getInt(0)
            }

    /** Validates: Requirements 9.4 */
    @Test
    fun write_then_read_by_key_returns_key_and_value() = runTest {
        dao.write("K", "V", 1_000L)

        val read = dao.getByKey("K")
        assertNotNull(read)
        assertEquals("K", read!!.key)
        assertEquals("V", read.value)
    }

    /** Validates: Requirements 9.5 */
    @Test
    fun overwrite_keeps_single_row_with_latest_value() = runTest {
        dao.write("K", "V1", 1_000L)
        dao.write("K", "V2", 2_000L)

        assertEquals(1, rowCount("K"))
        assertEquals("V2", dao.getByKey("K")!!.value)
    }
}
