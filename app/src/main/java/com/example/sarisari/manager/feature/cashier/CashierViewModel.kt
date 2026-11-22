package com.example.sarisari.manager.feature.cashier

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.sarisari.manager.R
import com.example.sarisari.manager.core.util.Money
import com.example.sarisari.manager.domain.model.CompletedSale
import com.example.sarisari.manager.domain.model.Product
import com.example.sarisari.manager.domain.model.StockShortage
import com.example.sarisari.manager.domain.repository.CategoryRepository
import com.example.sarisari.manager.domain.repository.ProductRepository
import com.example.sarisari.manager.domain.usecase.CheckoutError
import com.example.sarisari.manager.domain.usecase.CheckoutResult
import com.example.sarisari.manager.domain.usecase.CompleteSaleUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class CashierViewModel @Inject constructor(
    productRepository: ProductRepository,
    categoryRepository: CategoryRepository,
    private val completeSale: CompleteSaleUseCase,
) : ViewModel() {

    private data class Browse(val query: String, val categoryId: Long?)

    /** Everything except the product catalogue, which comes from the database. */
    private data class Local(
        val query: String = "",
        val categoryId: Long? = null,
        val cart: List<CartLine> = emptyList(),
        val stage: CashierStage = CashierStage.SELLING,
        val receivedText: String = "",
        val unitPickerProduct: Product? = null,
        val isSaving: Boolean = false,
        val errorRes: Int? = null,
        val stockShortages: List<StockShortage> = emptyList(),
        val completedSale: CompletedSale? = null,
    ) {
        fun withoutError() = copy(errorRes = null, stockShortages = emptyList())
    }

    private val local = MutableStateFlow(Local())

    private val products = local
        .map { Browse(it.query, it.categoryId) }
        .distinctUntilChanged()
        .flatMapLatest { productRepository.observeProducts(it.query, it.categoryId, includeArchived = false) }

    val uiState: StateFlow<CashierUiState> = combine(local, categoryRepository.observeCategories(), products) { l, categories, list ->
        CashierUiState(
            isLoading = false,
            query = l.query,
            categories = categories,
            selectedCategoryId = l.categoryId,
            products = list,
            cart = l.cart,
            stage = l.stage,
            receivedText = l.receivedText,
            unitPickerProduct = l.unitPickerProduct,
            isSaving = l.isSaving,
            errorRes = l.errorRes,
            stockShortages = l.stockShortages,
            completedSale = l.completedSale,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), CashierUiState())

    private fun update(change: (Local) -> Local) = local.update(change)

    fun onQueryChange(value: String) = update { it.copy(query = value) }
    fun onCategorySelected(id: Long?) = update { it.copy(categoryId = id) }

    fun onProductTapped(product: Product) {
        if (product.units.isEmpty()) {
            addLine(product, product.baseUnitName, 1, product.sellingPriceCentavos)
        } else {
            update { it.copy(unitPickerProduct = product) }
        }
    }

    fun onUnitPickerDismissed() = update { it.copy(unitPickerProduct = null) }

    fun onUnitChosen(product: Product, unitName: String, factor: Int, priceCentavos: Long) {
        addLine(product, unitName, factor, priceCentavos)
        update { it.copy(unitPickerProduct = null) }
    }

    private fun addLine(product: Product, unitName: String, factor: Int, priceCentavos: Long) = update { state ->
        val key = "${product.id}:$unitName"
        val existing = state.cart.firstOrNull { it.key == key }
        val cart = if (existing != null) {
            state.cart.map {
                if (it.key == key) it.copy(quantity = minOf(it.quantity + 1, MAX_QUANTITY)) else it
            }
        } else {
            state.cart + CartLine(
                productId = product.id,
                productName = product.name,
                unitName = unitName,
                unitFactor = factor,
                quantity = 1,
                unitPriceCentavos = priceCentavos,
                availableBaseStock = product.stockQuantity,
            )
        }
        state.copy(cart = cart).withoutError()
    }

    fun onIncrease(key: String) = update { state ->
        state.copy(cart = state.cart.map { if (it.key == key) it.copy(quantity = minOf(it.quantity + 1, MAX_QUANTITY)) else it })
    }

    fun onDecrease(key: String) = update { state ->
        state.copy(
            cart = state.cart.mapNotNull {
                when {
                    it.key != key -> it
                    it.quantity <= 1 -> null
                    else -> it.copy(quantity = it.quantity - 1)
                }
            }
        )
    }

    fun onRemove(key: String) = update { state -> state.copy(cart = state.cart.filterNot { it.key == key }) }

    fun onClearCart() = update { it.copy(cart = emptyList()).withoutError() }

    /** Returns true when [text] is a valid price and was applied. */
    fun onPriceChanged(key: String, text: String): Boolean {
        val price = Money.parse(text)?.takeIf { it in 1..CompleteSaleUseCase.MAX_UNIT_PRICE_CENTAVOS } ?: return false
        update { state -> state.copy(cart = state.cart.map { if (it.key == key) it.copy(unitPriceCentavos = price) else it }) }
        return true
    }

    fun onProceedToPayment() = update {
        if (it.cart.isEmpty()) it else it.copy(stage = CashierStage.PAYMENT, receivedText = "").withoutError()
    }

    fun onBackToCart() = update { it.copy(stage = CashierStage.SELLING).withoutError() }

    fun onReceivedChange(text: String) = update { it.copy(receivedText = text).withoutError() }

    fun onQuickCash(centavos: Long) = update { it.copy(receivedText = Money.toInputText(centavos)).withoutError() }

    fun onNewSale() = update { Local(query = it.query, categoryId = it.categoryId) }

    fun onCompleteSale() {
        val state = uiState.value
        val received = state.receivedCentavos
        if (!state.canComplete || received == null) return
        update { it.copy(isSaving = true).withoutError() }
        viewModelScope.launch {
            try {
                when (val result = completeSale(state.cart.map { it.toSaleLine() }, received)) {
                    is CheckoutResult.Completed -> update {
                        it.copy(
                            isSaving = false,
                            stage = CashierStage.COMPLETE,
                            cart = emptyList(),
                            completedSale = result.sale,
                        )
                    }
                    is CheckoutResult.Rejected -> update {
                        it.copy(isSaving = false, errorRes = result.error.messageRes())
                    }
                    // The cart is kept as it is. Only the stock shown on the affected lines is refreshed.
                    is CheckoutResult.InsufficientStock -> update { current ->
                        val available = result.shortages.associate { s -> s.productId to s.availableBaseQuantity }
                        current.copy(
                            isSaving = false,
                            errorRes = R.string.payment_error_insufficient_stock,
                            stockShortages = result.shortages,
                            cart = current.cart.map { line ->
                                available[line.productId]?.let { line.copy(availableBaseStock = it) } ?: line
                            },
                        )
                    }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                update { it.copy(isSaving = false, errorRes = R.string.payment_error_failed) }
            }
        }
    }

    private fun CheckoutError.messageRes(): Int = when (this) {
        CheckoutError.EMPTY_CART -> R.string.payment_error_empty
        CheckoutError.INVALID_QUANTITY -> R.string.payment_error_quantity
        CheckoutError.INVALID_PRICE -> R.string.payment_error_price
        CheckoutError.INSUFFICIENT_PAYMENT -> R.string.payment_not_enough
        CheckoutError.PRODUCT_UNAVAILABLE -> R.string.payment_error_unavailable
    }

    private companion object {
        const val STOP_TIMEOUT_MILLIS = 5_000L
        const val MAX_QUANTITY = CompleteSaleUseCase.MAX_QUANTITY
    }
}
