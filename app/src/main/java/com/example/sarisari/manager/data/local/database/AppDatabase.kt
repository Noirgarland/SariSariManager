package com.example.sarisari.manager.data.local.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.example.sarisari.manager.data.local.dao.AppSettingDao
import com.example.sarisari.manager.data.local.dao.CategoryDao
import com.example.sarisari.manager.data.local.dao.ExpenseDao
import com.example.sarisari.manager.data.local.dao.ExportDao
import com.example.sarisari.manager.data.local.dao.ReportDao
import com.example.sarisari.manager.data.local.dao.StoreSessionDao
import com.example.sarisari.manager.data.local.dao.ProductDao
import com.example.sarisari.manager.data.local.dao.SaleDao
import com.example.sarisari.manager.data.local.dao.StockMovementDao
import com.example.sarisari.manager.data.local.entity.AppSettingEntity
import com.example.sarisari.manager.data.local.entity.CategoryEntity
import com.example.sarisari.manager.data.local.entity.ExpenseEntity
import com.example.sarisari.manager.data.local.entity.StoreSessionEntity
import com.example.sarisari.manager.data.local.entity.ProductEntity
import com.example.sarisari.manager.data.local.entity.ProductUnitEntity
import com.example.sarisari.manager.data.local.entity.SaleEntity
import com.example.sarisari.manager.data.local.entity.SaleItemEntity
import com.example.sarisari.manager.data.local.entity.StockMovementEntity

@Database(
    entities = [
        AppSettingEntity::class,
        CategoryEntity::class,
        ProductEntity::class,
        ProductUnitEntity::class,
        StockMovementEntity::class,
        SaleEntity::class,
        SaleItemEntity::class,
        ExpenseEntity::class,
        StoreSessionEntity::class,
    ],
    version = AppDatabase.VERSION,
    exportSchema = true,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun appSettingDao(): AppSettingDao
    abstract fun categoryDao(): CategoryDao
    abstract fun productDao(): ProductDao
    abstract fun stockMovementDao(): StockMovementDao
    abstract fun saleDao(): SaleDao
    abstract fun reportDao(): ReportDao
    abstract fun expenseDao(): ExpenseDao
    abstract fun storeSessionDao(): StoreSessionDao
    abstract fun exportDao(): ExportDao

    companion object {
        const val VERSION = 6
    }
}
