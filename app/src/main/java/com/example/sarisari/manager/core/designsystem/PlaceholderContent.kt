package com.example.sarisari.manager.core.designsystem

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.example.sarisari.manager.R

@Composable
fun PlaceholderContent(title: String, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        Text(text = title)
    }
}

@Preview(showBackground = true)
@Composable
private fun PlaceholderContentPreview() {
    SariSariManagerTheme {
        PlaceholderContent(title = stringResource(R.string.destination_dashboard))
    }
}
