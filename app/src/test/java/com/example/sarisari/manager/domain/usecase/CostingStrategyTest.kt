package com.example.sarisari.manager.domain.usecase

import com.example.sarisari.manager.domain.model.CostingMethod
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class CostingStrategyTest {

    private val weighted = WeightedAverageCostStrategy
    private val latest = LatestPurchaseCostStrategy

    @Test
    fun weightedAverageBlendsExistingAndIncomingValue() {
        // 10 on hand at 100, receive 10 for a total of 200 (20 each): (1000 + 200) / 20 = 60
        assertEquals(60L, weighted.newUnitCost(10, 100, 10, 200))
        // 4 at 4500, receive 6 at 5000 each (30000): (18000 + 30000) / 10 = 4800
        assertEquals(4800L, weighted.newUnitCost(4, 4500, 6, 30_000))
    }

    @Test
    fun weightedAverageUsesIncomingCostWhenNoStock() {
        assertEquals(500L, weighted.newUnitCost(0, 9999, 6, 3000))
        assertEquals(500L, weighted.newUnitCost(-3, 9999, 6, 3000))
    }

    @Test
    fun weightedAverageRoundsHalfUpOnlyOnce() {
        // (1 * 100 + 101) / 2 = 100.5 -> 101
        assertEquals(101L, weighted.newUnitCost(1, 100, 1, 101))
        // (1 * 100 + 100) / 2 = 100 exactly
        assertEquals(100L, weighted.newUnitCost(1, 100, 1, 100))
        // (2 * 100 + 100) / 3 = 100 ; (1 * 100 + 102) / 3 = 67.33 -> 67 ; (1 * 100 + 103) / 3 = 67.67 -> 68
        assertEquals(67L, weighted.newUnitCost(1, 100, 2, 102))
        assertEquals(68L, weighted.newUnitCost(1, 100, 2, 103))
    }

    @Test
    fun weightedAverageDoesNotRoundIncomingUnitCostFirst() {
        // Buying 3 for a total of 100 centavos (33.33 each). Rounding the unit cost first would give 33 and a result of 33.
        // Exact: (3 * 50 + 100) / 6 = 41.67 -> 42. Rounding 33 first gives (150 + 99) / 6 = 41.5 -> 42 as well,
        // so use a case where they differ: existing 1 at 0, incoming 3 for 100: exact 100/4 = 25, rounded-first 99/4 = 24.75 -> 25.
        assertEquals(25L, weighted.newUnitCost(1, 0, 3, 100))
        // existing 1 at 0, incoming 3 for 101 (33.67): exact 101/4 = 25.25 -> 25
        assertEquals(25L, weighted.newUnitCost(1, 0, 3, 101))
        // existing 1 at 0, incoming 3 for 102: exact 25.5 -> 26 (rounded-first 34 * 3 = 102 gives the same here)
        assertEquals(26L, weighted.newUnitCost(1, 0, 3, 102))
    }

    @Test
    fun latestPurchaseCostIgnoresExistingStock() {
        assertEquals(20L, latest.newUnitCost(10, 100, 10, 200))
        assertEquals(20L, latest.newUnitCost(0, 100, 10, 200))
        assertEquals(34L, latest.newUnitCost(5, 1, 3, 101)) // 33.67 -> 34
        assertEquals(33L, latest.newUnitCost(5, 1, 3, 100)) // 33.33 -> 33
    }

    @Test
    fun methodsMapToStrategies() {
        assertEquals(WeightedAverageCostStrategy, CostingMethod.WEIGHTED_AVERAGE.strategy())
        assertEquals(LatestPurchaseCostStrategy, CostingMethod.LATEST_PURCHASE.strategy())
    }

    @Test
    fun unknownOrMissingSettingFallsBackToWeightedAverage() {
        assertEquals(CostingMethod.WEIGHTED_AVERAGE, CostingMethod.fromStorage(null))
        assertEquals(CostingMethod.WEIGHTED_AVERAGE, CostingMethod.fromStorage("garbage"))
        assertEquals(CostingMethod.LATEST_PURCHASE, CostingMethod.fromStorage("latest_purchase"))
    }

    @Test
    fun rejectsNonPositiveIncomingQuantity() {
        assertThrows(IllegalArgumentException::class.java) { weighted.newUnitCost(1, 1, 0, 1) }
        assertThrows(IllegalArgumentException::class.java) { latest.newUnitCost(1, 1, 0, 1) }
    }
}
