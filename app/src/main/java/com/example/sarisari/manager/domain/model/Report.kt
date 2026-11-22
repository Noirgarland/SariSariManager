package com.example.sarisari.manager.domain.model

/** Totals for a period. Voided sales are excluded. Money is in centavos. */
data class ReportSummary(
    val revenueCentavos: Long = 0,
    val costOfGoodsSoldCentavos: Long = 0,
    val expensesCentavos: Long = 0,
    val transactionCount: Int = 0,
    /** The part of [revenueCentavos] sold on utang. Utang sales count as sales on the day they are made. */
    val creditSalesCentavos: Long = 0,
) {
    /** The part of [revenueCentavos] paid in cash at the time of sale. */
    val cashSalesCentavos: Long get() = revenueCentavos - creditSalesCentavos
    val grossProfitCentavos: Long get() = revenueCentavos - costOfGoodsSoldCentavos
    val netProfitCentavos: Long get() = grossProfitCentavos - expensesCentavos
}

/** Base-unit quantity and revenue sold for one product in a period. */
data class TopProduct(
    val productId: Long,
    val productName: String,
    val baseUnitName: String,
    val baseQuantity: Int,
    val revenueCentavos: Long,
)
