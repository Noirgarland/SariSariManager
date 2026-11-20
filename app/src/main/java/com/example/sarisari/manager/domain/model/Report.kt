package com.example.sarisari.manager.domain.model

/** Totals for a period. Voided sales are excluded. Money is in centavos. */
data class ReportSummary(
    val revenueCentavos: Long = 0,
    val costOfGoodsSoldCentavos: Long = 0,
    val expensesCentavos: Long = 0,
    val transactionCount: Int = 0,
) {
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
