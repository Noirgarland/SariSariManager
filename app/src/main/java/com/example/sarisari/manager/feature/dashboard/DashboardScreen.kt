package com.example.sarisari.manager.feature.dashboard

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.sarisari.manager.core.designsystem.PlaceholderContent
import com.example.sarisari.manager.core.designsystem.SariSariManagerTheme

@Composable
fun DashboardScreen(viewModel: DashboardViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    DashboardContent(uiState)
}

@Composable
internal fun DashboardContent(uiState: DashboardUiState) {
    PlaceholderContent(title = stringResource(uiState.titleRes))
}

@Preview(showBackground = true)
@Composable
private fun DashboardContentPreview() {
    SariSariManagerTheme {
        DashboardContent(DashboardUiState())
    }
}
