package com.example.sarisari.manager.testing

import com.example.sarisari.manager.domain.model.Category
import com.example.sarisari.manager.domain.model.DuplicateNameException
import com.example.sarisari.manager.domain.model.Product
import com.example.sarisari.manager.domain.repository.CategoryRepository
import com.example.sarisari.manager.domain.repository.ProductRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

class FakeProductRepository(initial: List<Product> = emptyList()) : ProductRepository {
    val products = MutableStateFlow(initial)
    private var nextId = (initial.maxOfOrNull { it.id } ?: 0) + 1

    override fun observeProducts(query: String, categoryId: Long?, includeArchived: Boolean): Flow<List<Product>> =
        products.map { list ->
            list.filter {
                (includeArchived || !it.isArchived) &&
                    (categoryId == null || it.categoryId == categoryId) &&
                    it.name.contains(query, ignoreCase = true)
            }
        }

    override suspend fun getProduct(id: Long): Product? = products.value.firstOrNull { it.id == id }

    override suspend fun saveProduct(product: Product): Long {
        val id = if (product.id == 0L) nextId++ else product.id
        val saved = product.copy(id = id)
        products.value = products.value.filter { it.id != id } + saved
        return id
    }

    override suspend fun setArchived(id: Long, archived: Boolean) {
        products.value = products.value.map { if (it.id == id) it.copy(isArchived = archived) else it }
    }
}

class FakeCategoryRepository(initial: List<Category> = emptyList()) : CategoryRepository {
    val categories = MutableStateFlow(initial)
    private var nextId = (initial.maxOfOrNull { it.id } ?: 0) + 1

    override fun observeCategories(): Flow<List<Category>> = categories

    override suspend fun addCategory(name: String): Long {
        if (categories.value.any { it.name.equals(name, ignoreCase = true) }) throw DuplicateNameException(name)
        val id = nextId++
        categories.value = categories.value + Category(id, name)
        return id
    }

    override suspend fun renameCategory(id: Long, name: String) {
        if (categories.value.any { it.id != id && it.name.equals(name, ignoreCase = true) }) {
            throw DuplicateNameException(name)
        }
        categories.value = categories.value.map { if (it.id == id) it.copy(name = name) else it }
    }
}
