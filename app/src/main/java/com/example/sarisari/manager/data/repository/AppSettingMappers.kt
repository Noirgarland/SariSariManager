package com.example.sarisari.manager.data.repository

import com.example.sarisari.manager.data.local.entity.AppSettingEntity
import com.example.sarisari.manager.domain.model.AppSetting

internal fun AppSettingEntity.toDomain(): AppSetting =
    AppSetting(key = key, value = value, createdAt = createdAt, updatedAt = updatedAt)

internal fun AppSetting.toEntity(): AppSettingEntity =
    AppSettingEntity(key = key, value = value, createdAt = createdAt, updatedAt = updatedAt)
