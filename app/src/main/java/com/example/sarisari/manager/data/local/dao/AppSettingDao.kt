package com.example.sarisari.manager.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import com.example.sarisari.manager.data.local.entity.AppSettingEntity
import kotlinx.coroutines.flow.Flow

@Dao
abstract class AppSettingDao {
    @Query("SELECT * FROM app_settings WHERE `key` = :key")
    abstract suspend fun getByKey(key: String): AppSettingEntity?

    @Query("SELECT * FROM app_settings WHERE `key` = :key")
    abstract fun observeByKey(key: String): Flow<AppSettingEntity?>

    @Upsert
    protected abstract suspend fun upsert(entity: AppSettingEntity)

    /** Writes a setting, preserving `createdAt` when the key already exists. */
    @Transaction
    open suspend fun write(key: String, value: String, nowMillis: Long) {
        val existing = getByKey(key)
        upsert(AppSettingEntity(key, value, existing?.createdAt ?: nowMillis, nowMillis))
    }
}
