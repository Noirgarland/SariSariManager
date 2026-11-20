package com.example.sarisari.manager.feature.cashier

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.sarisari.manager.core.designsystem.PlaceholderContent
import com.example.sarisari.manager.core.designsystem.SariSariManagerTheme

@Composable
fun CashierScreen(viewModel: CashierViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    CashierContent(uiState)
}

@Composable
internal fun CashierContent(uiState: CashierUiState) {
    PlaceholderContent(title = stringResource(uiState.titleRes))
}

@Preview(showBackground = true)
@Composable
private fun CashierContentPreview() {
    SariSariManagerTheme {
        CashierContent(CashierUiState())
    }
}
