package com.example.sarisari.manager.data.repository

import android.database.sqlite.SQLiteConstraintException
import com.example.sarisari.manager.data.local.dao.CategoryDao
import com.example.sarisari.manager.data.local.entity.CategoryEntity
import com.example.sarisari.manager.domain.model.Category
import com.example.sarisari.manager.domain.model.DuplicateNameException
import com.example.sarisari.manager.domain.repository.CategoryRepository
import java.time.Clock
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class CategoryRepositoryImpl @Inject constructor(
    private val dao: CategoryDao,
    private val clock: Clock,
) : CategoryRepository {

    override fun observeCategories(): Flow<List<Category>> =
        dao.observeAll().map { list -> list.map { it.toDomain() } }

    override suspend fun addCategory(name: String): Long {
        val now = clock.millis()
        return try {
            dao.insert(CategoryEntity(name = name, createdAt = now, updatedAt = now))
        } catch (_: SQLiteConstraintException) {
            throw DuplicateNameException(name)
        }
    }

    override suspend fun renameCategory(id: Long, name: String) {
        try {
            dao.rename(id, name, clock.millis())
        } catch (_: SQLiteConstraintException) {
            throw DuplicateNameException(name)
        }
    }
}
