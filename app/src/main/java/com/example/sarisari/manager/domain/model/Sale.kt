package com.example.sarisari.manager.domain.model

/**
 * One cart line handed to checkout. [unitFactor] is how many base units one sold unit holds.
 * [unitPriceCentavos] is the price actually charged, which the owner may have changed.
 */
data class SaleLineInput(
    val productId: Long,
    val unitName: String,
    val unitFactor: Int,
    val quantity: Int,
    val unitPriceCentavos: Long,
) {
    val baseQuantity: Int get() = quantity * unitFactor
    val lineTotalCentavos: Long get() = quantity.toLong() * unitPriceCentavos
}

fun List<SaleLineInput>.totalCentavos(): Long = sumOf { it.lineTotalCentavos }

/** A saved sale line. Name, unit, price, and cost are snapshots taken when the sale was made. */
data class SaleItem(
    val id: Long,
    val productId: Long,
    val productName: String,
    val unitName: String,
    val unitFactor: Int,
    val quantity: Int,
    val baseQuantity: Int,
    val unitPriceCentavos: Long,
    val lineTotalCentavos: Long,
    val unitCostCentavos: Long,
    val lineCostCentavos: Long,
)

data class Sale(
    val id: Long,
    val totalCentavos: Long,
    val amountReceivedCentavos: Long,
    val changeCentavos: Long,
    val totalCostCentavos: Long,
    val createdAtMillis: Long,
    val voidedAtMillis: Long?,
    val voidReason: String?,
    val items: List<SaleItem>,
) {
    val isVoided: Boolean get() = voidedAtMillis != null
    val grossProfitCentavos: Long get() = totalCentavos - totalCostCentavos
}

data class CompletedSale(
    val id: Long,
    val totalCentavos: Long,
    val amountReceivedCentavos: Long,
    val changeCentavos: Long,
    val createdAtMillis: Long,
)
