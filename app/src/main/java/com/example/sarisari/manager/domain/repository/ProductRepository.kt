package com.example.sarisari.manager.domain.repository

import com.example.sarisari.manager.domain.model.Product
import kotlinx.coroutines.flow.Flow

interface ProductRepository {
    fun observeProducts(query: String, categoryId: Long?, includeArchived: Boolean): Flow<List<Product>>

    suspend fun getProduct(id: Long): Product?

    /** Inserts when [Product.id] is 0, otherwise updates. An update never changes stock. Returns the product id. */
    suspend fun saveProduct(product: Product): Long

    suspend fun setArchived(id: Long, archived: Boolean)
}
