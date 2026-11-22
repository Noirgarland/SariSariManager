package com.example.sarisari.manager.feature.products

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.example.sarisari.manager.core.navigation.ProductEditRoute
import com.example.sarisari.manager.core.util.Money
import com.example.sarisari.manager.domain.model.Product
import com.example.sarisari.manager.domain.model.ProductUnit
import com.example.sarisari.manager.domain.repository.CategoryRepository
import com.example.sarisari.manager.domain.repository.ProductRepository
import com.example.sarisari.manager.domain.usecase.ProductError
import com.example.sarisari.manager.domain.usecase.SaveProductResult
import com.example.sarisari.manager.domain.usecase.SaveProductUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class ProductEditViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val productRepository: ProductRepository,
    private val categoryRepository: CategoryRepository,
    private val saveProduct: SaveProductUseCase,
) : ViewModel() {

    private val productId: Long? = savedStateHandle.toRoute<ProductEditRoute>().productId
    private var loaded: Product? = null

    private val _uiState = MutableStateFlow(ProductEditUiState(isEditing = productId != null))
    val uiState: StateFlow<ProductEditUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            categoryRepository.observeCategories().collect { categories ->
                _uiState.update { it.copy(categories = categories) }
            }
        }
        viewModelScope.launch {
            val product = productId?.let { productRepository.getProduct(it) }
            loaded = product
            _uiState.update { state ->
                if (product == null) {
                    state.copy(isLoading = false)
                } else {
                    state.copy(
                        isLoading = false,
                        isArchived = product.isArchived,
                        name = product.name,
                        categoryId = product.categoryId,
                        baseUnit = product.baseUnitName,
                        cost = Money.toInputText(product.costCentavos),
                        price = Money.toInputText(product.sellingPriceCentavos),
                        threshold = product.lowStockThreshold.toString(),
                        stockOnHand = product.stockQuantity,
                        units = product.units.map {
                            UnitForm(it.name, it.baseQuantity.toString(), Money.toInputText(it.sellingPriceCentavos))
                        },
                    )
                }
            }
        }
    }

    fun onNameChange(value: String) = edit { it.copy(name = value) }
    fun onCategoryChange(value: Long?) = edit { it.copy(categoryId = value) }
    fun onBaseUnitChange(value: String) = edit { it.copy(baseUnit = value) }
    fun onCostChange(value: String) = edit { it.copy(cost = value) }
    fun onPriceChange(value: String) = edit { it.copy(price = value) }
    fun onThresholdChange(value: String) = edit { it.copy(threshold = value) }
    fun onOpeningStockChange(value: String) = edit { it.copy(openingStock = value) }
    fun onAddUnit() = edit { it.copy(units = it.units + UnitForm()) }
    fun onRemoveUnit(index: Int) = edit { it.copy(units = it.units.filterIndexed { i, _ -> i != index }) }
    fun onUnitChange(index: Int, unit: UnitForm) =
        edit { it.copy(units = it.units.mapIndexed { i, old -> if (i == index) unit else old }) }

    private fun edit(change: (ProductEditUiState) -> ProductEditUiState) {
        _uiState.update { change(it).copy(errors = emptySet()) }
    }

    fun onSave() {
        val state = _uiState.value
        if (state.isSaving) return
        val parseErrors = mutableSetOf<ProductError>()
        val cost = Money.parse(state.cost) ?: 0L.also { parseErrors += ProductError.COST_INVALID }
        val price = Money.parse(state.price) ?: 0L.also { parseErrors += ProductError.PRICE_INVALID }
        val threshold = state.threshold.trim().toIntOrNull()
            ?: 0.also { parseErrors += ProductError.THRESHOLD_INVALID }
        // Editing keeps the stock as it is; it only changes through Inventory.
        val openingStock = loaded?.stockQuantity
            ?: state.openingStock.trim().ifEmpty { "0" }.toIntOrNull()
            ?: 0.also { parseErrors += ProductError.OPENING_STOCK_INVALID }
        val units = state.units.map { form ->
            ProductUnit(
                name = form.name,
                baseQuantity = form.quantity.trim().toIntOrNull()
                    ?: 0.also { parseErrors += ProductError.UNIT_QUANTITY_INVALID },
                sellingPriceCentavos = Money.parse(form.price)
                    ?: 0L.also { parseErrors += ProductError.UNIT_PRICE_INVALID },
            )
        }
        val product = Product(
            id = productId ?: 0,
            name = state.name,
            categoryId = state.categoryId,
            baseUnitName = state.baseUnit,
            costCentavos = cost,
            sellingPriceCentavos = price,
            stockQuantity = openingStock,
            lowStockThreshold = threshold,
            isArchived = loaded?.isArchived ?: false,
            units = units,
        )
        // Validate the rest of the rules too, so one tap shows every problem at once.
        val errors = parseErrors + SaveProductUseCase.validate(product)
        if (errors.isNotEmpty()) {
            _uiState.update { it.copy(errors = errors) }
            return
        }
        _uiState.update { it.copy(isSaving = true) }
        viewModelScope.launch {
            when (val result = saveProduct(product)) {
                is SaveProductResult.Saved -> _uiState.update { it.copy(isSaving = false, isDone = true) }
                is SaveProductResult.Invalid -> _uiState.update { it.copy(isSaving = false, errors = result.errors) }
            }
        }
    }

    fun onArchiveChange(archived: Boolean) {
        val id = productId ?: return
        viewModelScope.launch {
            productRepository.setArchived(id, archived)
            _uiState.update { it.copy(isDone = true) }
        }
    }
}
