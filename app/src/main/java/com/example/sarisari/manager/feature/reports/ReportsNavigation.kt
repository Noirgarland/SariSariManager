package com.example.sarisari.manager.feature.reports

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.example.sarisari.manager.core.navigation.ReportsRoute

fun NavGraphBuilder.reportsScreen() {
    composable<ReportsRoute> { ReportsScreen() }
}
