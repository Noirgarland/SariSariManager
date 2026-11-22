package com.example.sarisari.manager.feature.cashier

import androidx.annotation.StringRes
import com.example.sarisari.manager.core.util.Money
import com.example.sarisari.manager.domain.model.Category
import com.example.sarisari.manager.domain.model.CompletedSale
import com.example.sarisari.manager.domain.model.Product
import com.example.sarisari.manager.domain.model.SaleLineInput
import com.example.sarisari.manager.domain.model.StockShortage

/** One line in the cart. [availableBaseStock] is the product's stock when the line was added. */
data class CartLine(
    val productId: Long,
    val productName: String,
    val unitName: String,
    val unitFactor: Int,
    val quantity: Int,
    val unitPriceCentavos: Long,
    val availableBaseStock: Int,
) {
    val key: String get() = "$productId:$unitName"
    val lineTotalCentavos: Long get() = quantity.toLong() * unitPriceCentavos
    val exceedsStock: Boolean get() = quantity.toLong() * unitFactor > availableBaseStock

    fun toSaleLine() = SaleLineInput(productId, unitName, unitFactor, quantity, unitPriceCentavos)
}

enum class CashierStage { SELLING, PAYMENT, COMPLETE }

data class CashierUiState(
    val isLoading: Boolean = true,
    val query: String = "",
    val categories: List<Category> = emptyList(),
    val selectedCategoryId: Long? = null,
    val products: List<Product> = emptyList(),
    val cart: List<CartLine> = emptyList(),
    val stage: CashierStage = CashierStage.SELLING,
    val receivedText: String = "",
    val unitPickerProduct: Product? = null,
    val isSaving: Boolean = false,
    @param:StringRes val errorRes: Int? = null,
    /** Products the last checkout attempt could not cover, as read inside the checkout transaction. */
    val stockShortages: List<StockShortage> = emptyList(),
    val completedSale: CompletedSale? = null,
) {
    val totalCentavos: Long get() = cart.sumOf { it.lineTotalCentavos }
    val itemCount: Int get() = cart.sumOf { it.quantity }
    val receivedCentavos: Long? get() = Money.parse(receivedText)
    val changeCentavos: Long? get() = receivedCentavos?.takeIf { it >= totalCentavos }?.let { it - totalCentavos }
    val canComplete: Boolean get() = !isSaving && cart.isNotEmpty() && changeCentavos != null
    val isFiltering: Boolean get() = query.isNotBlank() || selectedCategoryId != null
}
