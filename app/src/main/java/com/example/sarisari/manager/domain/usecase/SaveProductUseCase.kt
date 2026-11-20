package com.example.sarisari.manager.domain.usecase

import com.example.sarisari.manager.domain.model.Product
import com.example.sarisari.manager.domain.repository.ProductRepository
import javax.inject.Inject

enum class ProductError {
    NAME_REQUIRED,
    BASE_UNIT_REQUIRED,
    COST_INVALID,
    PRICE_INVALID,
    THRESHOLD_INVALID,
    UNIT_NAME_INVALID,
    UNIT_NAME_DUPLICATE,
    UNIT_QUANTITY_INVALID,
    UNIT_PRICE_INVALID,
}

sealed interface SaveProductResult {
    data class Saved(val productId: Long) : SaveProductResult
    data class Invalid(val errors: Set<ProductError>) : SaveProductResult
}

/** Validates the product rules, then saves. Names are trimmed. */
class SaveProductUseCase @Inject constructor(
    private val repository: ProductRepository,
) {
    suspend operator fun invoke(product: Product): SaveProductResult {
        val cleaned = product.copy(
            name = product.name.trim(),
            baseUnitName = product.baseUnitName.trim(),
            units = product.units.map { it.copy(name = it.name.trim()) },
        )
        val errors = validate(cleaned)
        if (errors.isNotEmpty()) return SaveProductResult.Invalid(errors)
        return SaveProductResult.Saved(repository.saveProduct(cleaned))
    }

    companion object {
        const val MAX_NAME_LENGTH = 80
        const val MIN_UNIT_QUANTITY = 2

        fun validate(product: Product): Set<ProductError> = buildSet {
            if (product.name.isBlank() || product.name.length > MAX_NAME_LENGTH) add(ProductError.NAME_REQUIRED)
            if (product.baseUnitName.isBlank() || product.baseUnitName.length > MAX_NAME_LENGTH) {
                add(ProductError.BASE_UNIT_REQUIRED)
            }
            if (product.costCentavos < 0) add(ProductError.COST_INVALID)
            if (product.sellingPriceCentavos <= 0) add(ProductError.PRICE_INVALID)
            if (product.lowStockThreshold < 0) add(ProductError.THRESHOLD_INVALID)
            val seen = mutableSetOf(product.baseUnitName.lowercase())
            product.units.forEach { unit ->
                if (unit.name.isBlank() || unit.name.length > MAX_NAME_LENGTH) {
                    add(ProductError.UNIT_NAME_INVALID)
                } else if (!seen.add(unit.name.lowercase())) {
                    add(ProductError.UNIT_NAME_DUPLICATE)
                }
                if (unit.baseQuantity < MIN_UNIT_QUANTITY) add(ProductError.UNIT_QUANTITY_INVALID)
                if (unit.sellingPriceCentavos <= 0) add(ProductError.UNIT_PRICE_INVALID)
            }
        }
    }
}
