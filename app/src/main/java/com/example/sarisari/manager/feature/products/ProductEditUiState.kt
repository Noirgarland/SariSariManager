package com.example.sarisari.manager.feature.products

import com.example.sarisari.manager.domain.model.Category
import com.example.sarisari.manager.domain.usecase.ProductError

/** Text typed for one extra selling unit. */
data class UnitForm(
    val name: String = "",
    val quantity: String = "",
    val price: String = "",
)

data class ProductEditUiState(
    val isLoading: Boolean = true,
    val isEditing: Boolean = false,
    val isArchived: Boolean = false,
    val name: String = "",
    val categoryId: Long? = null,
    val categories: List<Category> = emptyList(),
    val baseUnit: String = "",
    val cost: String = "",
    val price: String = "",
    val threshold: String = DEFAULT_THRESHOLD,
    /** New products only: how many base units the owner has now. Blank means 0. */
    val openingStock: String = "",
    /** Existing products only: the current stock, shown read-only. */
    val stockOnHand: Int = 0,
    val units: List<UnitForm> = emptyList(),
    val errors: Set<ProductError> = emptySet(),
    val isSaving: Boolean = false,
    val isDone: Boolean = false,
) {
    companion object {
        const val DEFAULT_THRESHOLD = "5"
    }
}
