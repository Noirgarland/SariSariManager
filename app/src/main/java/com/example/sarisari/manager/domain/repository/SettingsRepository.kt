package com.example.sarisari.manager.domain.repository

import com.example.sarisari.manager.domain.model.AppSetting
import kotlinx.coroutines.flow.Flow

interface SettingsRepository {
    suspend fun getSetting(key: String): AppSetting?

    fun observeSetting(key: String): Flow<AppSetting?>

    /** @throws IllegalArgumentException if key is not 1..128 chars or value exceeds 4096 chars. */
    suspend fun putSetting(key: String, value: String)
}
