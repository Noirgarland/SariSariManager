package com.example.sarisari.manager.feature.products

import androidx.annotation.StringRes
import com.example.sarisari.manager.domain.model.Category
import com.example.sarisari.manager.domain.model.StockStatus

data class ProductListItem(
    val id: Long,
    val name: String,
    val categoryName: String?,
    val costCentavos: Long,
    val sellingPriceCentavos: Long,
    val stockQuantity: Int,
    val baseUnitName: String,
    val stockStatus: StockStatus,
    val isArchived: Boolean,
)

data class ProductsUiState(
    val isLoading: Boolean = true,
    val query: String = "",
    val categories: List<Category> = emptyList(),
    val selectedCategoryId: Long? = null,
    val showArchived: Boolean = false,
    val products: List<ProductListItem> = emptyList(),
    @param:StringRes val categoryErrorRes: Int? = null,
) {
    val isFiltering: Boolean get() = query.isNotBlank() || selectedCategoryId != null || showArchived
}
