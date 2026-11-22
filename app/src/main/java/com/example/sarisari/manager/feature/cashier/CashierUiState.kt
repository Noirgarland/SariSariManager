package com.example.sarisari.manager.feature.cashier

import androidx.annotation.StringRes
import com.example.sarisari.manager.core.util.Money
import com.example.sarisari.manager.domain.model.Category
import com.example.sarisari.manager.domain.model.CompletedSale
import com.example.sarisari.manager.domain.model.Product
import com.example.sarisari.manager.domain.model.Customer
import com.example.sarisari.manager.domain.model.SaleLineInput
import com.example.sarisari.manager.domain.model.SalePayment
import com.example.sarisari.manager.domain.model.creditFor
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

enum class PaymentMethod { CASH, UTANG }

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
    val paymentMethod: PaymentMethod = PaymentMethod.CASH,
    /** The customer the utang goes to. Only used when [paymentMethod] is [PaymentMethod.UTANG]. */
    val customer: Customer? = null,
    val showCustomerPicker: Boolean = false,
    val customerQuery: String = "",
    val customers: List<Customer> = emptyList(),
    @param:StringRes val customerErrorRes: Int? = null,
) {
    val totalCentavos: Long get() = cart.sumOf { it.lineTotalCentavos }
    val itemCount: Int get() = cart.sumOf { it.quantity }
    val receivedCentavos: Long? get() = Money.parse(receivedText)
    val isUtang: Boolean get() = paymentMethod == PaymentMethod.UTANG

    /** On utang, blank cash means none was paid now. Null when the typed amount is not valid. */
    private val cashNowCentavos: Long? get() = if (isUtang && receivedText.isBlank()) 0L else receivedCentavos

    val changeCentavos: Long?
        get() = when {
            isUtang -> cashNowCentavos?.let { maxOf(0L, it - totalCentavos) }
            else -> receivedCentavos?.takeIf { it >= totalCentavos }?.let { it - totalCentavos }
        }

    /** What goes on the customer's utang, updated as the cash amount is typed. */
    val creditCentavos: Long get() = if (isUtang) cashNowCentavos?.let { creditFor(totalCentavos, it) } ?: 0L else 0L

    val canComplete: Boolean
        get() = !isSaving && cart.isNotEmpty() && when {
            isUtang -> customer != null && cashNowCentavos != null
            else -> changeCentavos != null
        }

    /** The payment to send to checkout, or null when it is not complete yet. */
    fun salePayment(): SalePayment? {
        if (!isUtang) return receivedCentavos?.let { SalePayment.Cash(it) }
        val chosen = customer ?: return null
        val cash = cashNowCentavos ?: return null
        return SalePayment.Utang(chosen.id, cash)
    }
    val isFiltering: Boolean get() = query.isNotBlank() || selectedCategoryId != null
}
