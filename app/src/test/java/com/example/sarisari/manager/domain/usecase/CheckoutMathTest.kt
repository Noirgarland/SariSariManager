package com.example.sarisari.manager.domain.usecase

import org.junit.Assert.assertEquals
import org.junit.Test

class CheckoutMathTest {
    @Test
    fun changeIsReceivedMinusTotalAndNeverNegative() {
        assertEquals(9500L, changeCentavos(10_500, 20_000))
        assertEquals(0L, changeCentavos(10_500, 10_500))
        assertEquals(0L, changeCentavos(10_500, 5_000))
    }
}
