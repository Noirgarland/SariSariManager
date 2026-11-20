package com.example.sarisari.manager.feature

import com.example.sarisari.manager.R
import com.example.sarisari.manager.feature.more.MoreUiState
import com.example.sarisari.manager.feature.more.MoreViewModel
import com.example.sarisari.manager.testing.MainDispatcherRule
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class PlaceholderViewModelsTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun moreInitialState() {
        val state = MoreViewModel().uiState.value
        assertEquals(MoreUiState(), state)
        assertEquals(R.string.destination_more, state.titleRes)
    }
}
