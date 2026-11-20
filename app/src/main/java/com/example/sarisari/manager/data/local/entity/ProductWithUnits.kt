package com.example.sarisari.manager.data.local.entity

import androidx.room.Embedded
import androidx.room.Relation

data class ProductWithUnits(
    @Embedded val product: ProductEntity,
    @Relation(parentColumn = "id", entityColumn = "product_id") val units: List<ProductUnitEntity>,
)
