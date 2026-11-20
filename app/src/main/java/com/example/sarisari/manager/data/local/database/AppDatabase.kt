package com.example.sarisari.manager.data.local.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.example.sarisari.manager.data.local.dao.AppSettingDao
import com.example.sarisari.manager.data.local.entity.AppSettingEntity

@Database(entities = [AppSettingEntity::class], version = AppDatabase.VERSION, exportSchema = true)
abstract class AppDatabase : RoomDatabase() {
    abstract fun appSettingDao(): AppSettingDao

    companion object {
        const val VERSION = 1
    }
}
