package com.example.sarisari.manager.data.repository

import com.example.sarisari.manager.data.local.dao.AppSettingDao
import com.example.sarisari.manager.domain.model.AppSetting
import com.example.sarisari.manager.domain.repository.SettingsRepository
import java.time.Clock
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class SettingsRepositoryImpl @Inject constructor(
    private val dao: AppSettingDao,
    private val clock: Clock,
) : SettingsRepository {

    override suspend fun getSetting(key: String): AppSetting? =
        dao.getByKey(key)?.toDomain()

    override fun observeSetting(key: String): Flow<AppSetting?> =
        dao.observeByKey(key).map { it?.toDomain() }

    override suspend fun putSetting(key: String, value: String) {
        require(key.length in 1..MAX_KEY_LENGTH) {
            "Setting key length must be in 1..$MAX_KEY_LENGTH but was ${key.length}"
        }
        require(value.length <= MAX_VALUE_LENGTH) {
            "Setting value length must be at most $MAX_VALUE_LENGTH but was ${value.length}"
        }
        dao.write(key, value, clock.millis())
    }

    private companion object {
        const val MAX_KEY_LENGTH = 128
        const val MAX_VALUE_LENGTH = 4096
    }
}
