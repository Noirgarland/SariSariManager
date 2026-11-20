package com.example.sarisari.manager.core.designsystem

import org.junit.Assert.assertTrue
import org.junit.Test

/** Validates: Requirements 6.6, 7.4, 7.5, 9.7 */
class TypographyTest {

    @Test
    fun bodyStylesAreAtLeast16sp() {
        listOf(Typography.bodyLarge, Typography.bodyMedium, Typography.bodySmall).forEach {
            assertTrue(it.fontSize.isSp)
            assertTrue(it.fontSize.value >= 16f)
        }
    }
}
