package com.example.sarisari.manager.domain.model

/** An additional way to sell a product, such as a pack, worth [baseQuantity] base units. */
data class ProductUnit(
    val name: String,
    val baseQuantity: Int,
    val sellingPriceCentavos: Long,
)
