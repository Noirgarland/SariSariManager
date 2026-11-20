package com.example.sarisari.manager.data.repository

import com.example.sarisari.manager.data.local.entity.CategoryEntity
import com.example.sarisari.manager.data.local.entity.ProductEntity
import com.example.sarisari.manager.data.local.entity.ProductUnitEntity
import com.example.sarisari.manager.data.local.entity.ProductWithUnits
import com.example.sarisari.manager.domain.model.Category
import com.example.sarisari.manager.domain.model.Product
import com.example.sarisari.manager.domain.model.ProductUnit

fun CategoryEntity.toDomain() = Category(id = id, name = name)

fun ProductWithUnits.toDomain() = Product(
    id = product.id,
    name = product.name,
    categoryId = product.categoryId,
    baseUnitName = product.baseUnitName,
    costCentavos = product.costCentavos,
    sellingPriceCentavos = product.sellingPriceCentavos,
    stockQuantity = product.stockQuantity,
    lowStockThreshold = product.lowStockThreshold,
    isArchived = product.isArchived,
    units = units.sortedBy { it.baseQuantity }.map {
        ProductUnit(it.name, it.baseQuantity, it.sellingPriceCentavos)
    },
)

/** Timestamps are placeholders. [com.example.sarisari.manager.data.local.dao.ProductDao.save] sets the real ones. */
fun Product.toEntity() = ProductEntity(
    id = id,
    name = name,
    categoryId = categoryId,
    baseUnitName = baseUnitName,
    costCentavos = costCentavos,
    sellingPriceCentavos = sellingPriceCentavos,
    stockQuantity = stockQuantity,
    lowStockThreshold = lowStockThreshold,
    isArchived = isArchived,
    createdAt = 0,
    updatedAt = 0,
)

fun ProductUnit.toEntity(productId: Long) = ProductUnitEntity(
    productId = productId,
    name = name,
    baseQuantity = baseQuantity,
    sellingPriceCentavos = sellingPriceCentavos,
    createdAt = 0,
    updatedAt = 0,
)
