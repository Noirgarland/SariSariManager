package com.example.sarisari.manager.feature.products

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.sarisari.manager.core.designsystem.PlaceholderContent
import com.example.sarisari.manager.core.designsystem.SariSariManagerTheme

@Composable
fun ProductsScreen(viewModel: ProductsViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    ProductsContent(uiState)
}

@Composable
internal fun ProductsContent(uiState: ProductsUiState) {
    PlaceholderContent(title = stringResource(uiState.titleRes))
}

@Preview(showBackground = true)
@Composable
private fun ProductsContentPreview() {
    SariSariManagerTheme {
        ProductsContent(ProductsUiState())
    }
}
