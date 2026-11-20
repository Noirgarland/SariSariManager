package com.example.sarisari.manager.domain.repository

import com.example.sarisari.manager.domain.model.Category
import kotlinx.coroutines.flow.Flow

interface CategoryRepository {
    fun observeCategories(): Flow<List<Category>>

    /** @throws com.example.sarisari.manager.domain.model.DuplicateNameException if the name is taken (case-insensitive). */
    suspend fun addCategory(name: String): Long

    /** @throws com.example.sarisari.manager.domain.model.DuplicateNameException if the name is taken (case-insensitive). */
    suspend fun renameCategory(id: Long, name: String)
}
