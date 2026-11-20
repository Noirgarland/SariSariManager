package com.example.sarisari.manager.data.local

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.sarisari.manager.data.local.database.AppDatabase
import com.example.sarisari.manager.data.local.entity.CategoryEntity
import com.example.sarisari.manager.data.local.entity.ProductEntity
import com.example.sarisari.manager.data.local.entity.ProductUnitEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.SQLiteMode

/** LEGACY SQLite mode: the native runtime loader fails when the user home path contains spaces. */
@RunWith(RobolectricTestRunner::class)
@SQLiteMode(SQLiteMode.Mode.LEGACY)
class ProductDaoTest {
    private lateinit var db: AppDatabase

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).allowMainThreadQueries().build()
    }

    @After
    fun tearDown() = db.close()

    private fun product(name: String, categoryId: Long? = null, stock: Int = 0) = ProductEntity(
        name = name, categoryId = categoryId, baseUnitName = "piece", costCentavos = 1000,
        sellingPriceCentavos = 1500, stockQuantity = stock, lowStockThreshold = 3,
        isArchived = false, createdAt = 0, updatedAt = 0,
    )

    private fun unit(name: String, qty: Int) = ProductUnitEntity(
        productId = 0, name = name, baseQuantity = qty, sellingPriceCentavos = 9000, createdAt = 0, updatedAt = 0,
    )

    private suspend fun observe(query: String = "", category: Long? = null, archived: Boolean = false) =
        db.productDao().observe(query, category, archived).first()

    @Test
    fun saveInsertsProductWithUnitsAndTimestamps() = runTest {
        val id = db.productDao().save(product("Coke"), listOf(unit("pack", 6)), now = 1000)
        val loaded = db.productDao().getWithUnits(id)!!
        assertEquals("Coke", loaded.product.name)
        assertEquals(1000L, loaded.product.createdAt)
        assertEquals(listOf("pack"), loaded.units.map { it.name })
        assertEquals(id, loaded.units.single().productId)
    }

    @Test
    fun updateKeepsStockAndCreatedAtAndReplacesUnits() = runTest {
        val dao = db.productDao()
        val id = dao.save(product("Coke"), listOf(unit("pack", 6)), now = 1000)
        db.openHelper.writableDatabase.execSQL("UPDATE products SET stock_quantity = 12 WHERE id = $id")
        dao.save(product("Coke Zero", stock = 999).copy(id = id), listOf(unit("box", 24)), now = 2000)
        val loaded = dao.getWithUnits(id)!!
        assertEquals("Coke Zero", loaded.product.name)
        assertEquals(12, loaded.product.stockQuantity)
        assertEquals(1000L, loaded.product.createdAt)
        assertEquals(2000L, loaded.product.updatedAt)
        assertEquals(listOf("box"), loaded.units.map { it.name })
        assertEquals(1, observe().size)
    }

    @Test
    fun searchIsCaseInsensitiveAndEscapesWildcards() = runTest {
        val dao = db.productDao()
        dao.save(product("Coke 1.5L"), emptyList(), 1)
        dao.save(product("Sprite 100% juice"), emptyList(), 1)
        dao.save(product("Royal"), emptyList(), 1)
        assertEquals(listOf("Coke 1.5L"), observe("coke").map { it.product.name })
        assertEquals(3, observe("").size)
        assertEquals(listOf("Sprite 100% juice"), observe("100\\%").map { it.product.name })
        assertTrue(observe("\\%").size == 1)
    }

    @Test
    fun filtersByCategoryAndArchivedFlag() = runTest {
        val categoryId = db.categoryDao().insert(CategoryEntity(name = "Drinks", createdAt = 1, updatedAt = 1))
        val dao = db.productDao()
        val cokeId = dao.save(product("Coke", categoryId), emptyList(), 1)
        dao.save(product("Piattos"), emptyList(), 1)
        assertEquals(listOf("Coke"), observe(category = categoryId).map { it.product.name })
        dao.setArchived(cokeId, true, 5)
        assertEquals(listOf("Piattos"), observe().map { it.product.name })
        assertEquals(2, observe(archived = true).size)
        assertTrue(dao.getWithUnits(cokeId)!!.product.isArchived)
    }

    @Test
    fun categoryNamesAreUniqueIgnoringCase() = runTest {
        val dao = db.categoryDao()
        dao.insert(CategoryEntity(name = "Drinks", createdAt = 1, updatedAt = 1))
        try {
            dao.insert(CategoryEntity(name = "dRINKS", createdAt = 1, updatedAt = 1))
            fail("Expected a unique constraint violation")
        } catch (e: android.database.sqlite.SQLiteConstraintException) {
            assertNotNull(e)
        }
        assertEquals(listOf("Drinks"), dao.observeAll().first().map { it.name })
    }
}
