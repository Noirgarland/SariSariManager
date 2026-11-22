package com.example.sarisari.manager.domain.usecase

import com.example.sarisari.manager.domain.model.CostingMethod
import java.math.BigInteger

/** Calculates a product's new cost per base unit, in centavos, after a stock receipt. */
fun interface CostingStrategy {
    /**
     * @param incomingTotalCostCentavos the full amount paid for [incomingQuantity] base units.
     * Rounds half-up once, at the end.
     */
    fun newUnitCost(
        currentQuantity: Int,
        currentCostCentavos: Long,
        incomingQuantity: Int,
        incomingTotalCostCentavos: Long,
    ): Long
}

/** (existingQty * existingCost + incomingTotal) / (existingQty + incomingQty). Falls back to the incoming cost when stock is zero or less. */
object WeightedAverageCostStrategy : CostingStrategy {
    override fun newUnitCost(
        currentQuantity: Int,
        currentCostCentavos: Long,
        incomingQuantity: Int,
        incomingTotalCostCentavos: Long,
    ): Long {
        require(incomingQuantity > 0) { "incomingQuantity must be positive" }
        if (currentQuantity <= 0) return divideRoundHalfUp(incomingTotalCostCentavos, incomingQuantity.toLong())
        // Large stock times a large cost can exceed Long, so the inventory value is summed as an exact BigInteger.
        // The result is a weighted average of the two unit costs, so it always fits back into a Long.
        val existingValue = BigInteger.valueOf(currentQuantity.toLong()) * BigInteger.valueOf(currentCostCentavos)
        val numerator = existingValue + BigInteger.valueOf(incomingTotalCostCentavos)
        val denominator = BigInteger.valueOf(currentQuantity.toLong() + incomingQuantity)
        val (quotient, remainder) = numerator.divideAndRemainder(denominator)
        val roundUp = remainder.shiftLeft(1) >= denominator
        return (if (roundUp) quotient + BigInteger.ONE else quotient).longValueExact()
    }
}

/** The cost of the most recent receipt. */
object LatestPurchaseCostStrategy : CostingStrategy {
    override fun newUnitCost(
        currentQuantity: Int,
        currentCostCentavos: Long,
        incomingQuantity: Int,
        incomingTotalCostCentavos: Long,
    ): Long {
        require(incomingQuantity > 0) { "incomingQuantity must be positive" }
        return divideRoundHalfUp(incomingTotalCostCentavos, incomingQuantity.toLong())
    }
}

fun CostingMethod.strategy(): CostingStrategy = when (this) {
    CostingMethod.WEIGHTED_AVERAGE -> WeightedAverageCostStrategy
    CostingMethod.LATEST_PURCHASE -> LatestPurchaseCostStrategy
}

/** Integer division rounding half up, for a non-negative numerator and positive denominator. */
internal fun divideRoundHalfUp(numerator: Long, denominator: Long): Long {
    require(numerator >= 0 && denominator > 0) { "numerator must be >= 0 and denominator > 0" }
    // Compares the remainder with what is left of the denominator, so no intermediate value is doubled.
    val quotient = numerator / denominator
    val remainder = numerator % denominator
    return if (remainder >= denominator - remainder) quotient + 1 else quotient
}
