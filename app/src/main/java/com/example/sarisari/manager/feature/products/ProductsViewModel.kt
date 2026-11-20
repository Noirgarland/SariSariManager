package com.example.sarisari.manager.feature.products

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.sarisari.manager.R
import com.example.sarisari.manager.domain.model.DuplicateNameException
import com.example.sarisari.manager.domain.repository.CategoryRepository
import com.example.sarisari.manager.domain.repository.ProductRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class ProductsViewModel @Inject constructor(
    private val productRepository: ProductRepository,
    private val categoryRepository: CategoryRepository,
) : ViewModel() {

    private data class Filters(val query: String, val categoryId: Long?, val showArchived: Boolean)

    private val query = MutableStateFlow("")
    private val selectedCategoryId = MutableStateFlow<Long?>(null)
    private val showArchived = MutableStateFlow(false)
    private val categoryError = MutableStateFlow<Int?>(null)

    private val filters = combine(query, selectedCategoryId, showArchived, ::Filters)

    val uiState: StateFlow<ProductsUiState> = combine(
        filters,
        categoryRepository.observeCategories(),
        filters.flatMapLatest { productRepository.observeProducts(it.query, it.categoryId, it.showArchived) },
        categoryError,
    ) { filters, categories, products, error ->
        val names = categories.associate { it.id to it.name }
        ProductsUiState(
            isLoading = false,
            query = filters.query,
            categories = categories,
            selectedCategoryId = filters.categoryId,
            showArchived = filters.showArchived,
            products = products.map {
                ProductListItem(
                    id = it.id,
                    name = it.name,
                    categoryName = it.categoryId?.let(names::get),
                    costCentavos = it.costCentavos,
                    sellingPriceCentavos = it.sellingPriceCentavos,
                    stockQuantity = it.stockQuantity,
                    baseUnitName = it.baseUnitName,
                    stockStatus = it.stockStatus,
                    isArchived = it.isArchived,
                )
            },
            categoryErrorRes = error,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), ProductsUiState())

    fun onQueryChange(value: String) {
        query.value = value
    }

    fun onCategorySelected(categoryId: Long?) {
        selectedCategoryId.value = categoryId
    }

    fun onShowArchivedChange(value: Boolean) {
        showArchived.value = value
    }

    fun onCategoryErrorShown() {
        categoryError.value = null
    }

    fun addCategory(name: String) = saveCategory(name) { categoryRepository.addCategory(it) }

    fun renameCategory(id: Long, name: String) = saveCategory(name) { categoryRepository.renameCategory(id, it) }

    private fun saveCategory(name: String, action: suspend (String) -> Unit) {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) {
            categoryError.value = R.string.category_error_blank
            return
        }
        viewModelScope.launch {
            try {
                action(trimmed)
                categoryError.value = null
            } catch (_: DuplicateNameException) {
                categoryError.value = R.string.category_error_exists
            }
        }
    }

    private companion object {
        const val STOP_TIMEOUT_MILLIS = 5_000L
    }
}
