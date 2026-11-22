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
    /** Computed in Long so a large quantity times a large factor cannot overflow. */
    val baseQuantity: Long get() = quantity.toLong() * unitFactor

    /** @throws ArithmeticException if the result does not fit in a Long. */
    val lineTotalCentavos: Long get() = Math.multiplyExact(quantity.toLong(), unitPriceCentavos)
}

/** @throws ArithmeticException on overflow, instead of wrapping to a wrong total. */
fun List<SaleLineInput>.totalCentavos(): Long = fold(0L) { total, line -> Math.addExact(total, line.lineTotalCentavos) }

/** A saved sale line. Name, units, price, and cost are snapshots taken when the sale was made. */
data class SaleItem(
    val id: Long,
    val productId: Long,
    val productName: String,
    val unitName: String,
    /** The product's base unit at sale time. [baseQuantity] is counted in this unit. */
    val baseUnitName: String,
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
    /** The part of the total put on the customer's utang. 0 for a cash sale. */
    val creditCentavos: Long = 0,
    val customerId: Long? = null,
    val customerName: String? = null,
) {
    val isOnCredit: Boolean get() = creditCentavos > 0
    val isVoided: Boolean get() = voidedAtMillis != null
    val grossProfitCentavos: Long get() = totalCentavos - totalCostCentavos
}

data class CompletedSale(
    val id: Long,
    val totalCentavos: Long,
    val amountReceivedCentavos: Long,
    val changeCentavos: Long,
    val createdAtMillis: Long,
    val creditCentavos: Long = 0,
    val customerName: String? = null,
)

/**
 * How a sale is paid. [Cash] needs at least the total. [Utang] takes any cash now, including none,
 * and puts the rest on the customer's balance.
 */
sealed interface SalePayment {
    val amountReceivedCentavos: Long

    data class Cash(override val amountReceivedCentavos: Long) : SalePayment
    data class Utang(val customerId: Long, override val amountReceivedCentavos: Long) : SalePayment
}

/** The utang part of a sale: what the cash does not cover. Never negative. */
fun creditFor(totalCentavos: Long, amountReceivedCentavos: Long): Long = maxOf(0L, totalCentavos - amountReceivedCentavos)
