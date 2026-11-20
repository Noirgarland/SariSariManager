package com.example.sarisari.manager.feature.products

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.example.sarisari.manager.core.navigation.ProductsRoute

fun NavGraphBuilder.productsScreen() {
    composable<ProductsRoute> { ProductsScreen() }
}
