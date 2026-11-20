package com.example.sarisari.manager.data.repository

import com.example.sarisari.manager.data.local.dao.ProductDao
import com.example.sarisari.manager.domain.model.Product
import com.example.sarisari.manager.domain.repository.ProductRepository
import java.time.Clock
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class ProductRepositoryImpl @Inject constructor(
    private val dao: ProductDao,
    private val clock: Clock,
) : ProductRepository {

    override fun observeProducts(query: String, categoryId: Long?, includeArchived: Boolean): Flow<List<Product>> =
        dao.observe(escapeLike(query.trim()), categoryId, includeArchived)
            .map { list -> list.map { it.toDomain() } }

    override suspend fun getProduct(id: Long): Product? = dao.getWithUnits(id)?.toDomain()

    override suspend fun saveProduct(product: Product): Long =
        dao.save(product.toEntity(), product.units.map { it.toEntity(product.id) }, clock.millis())

    override suspend fun setArchived(id: Long, archived: Boolean) = dao.setArchived(id, archived, clock.millis())

    private fun escapeLike(text: String): String =
        text.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_")
}
