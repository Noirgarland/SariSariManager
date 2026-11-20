package com.example.sarisari.manager.feature.products

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.example.sarisari.manager.core.navigation.ProductEditRoute
import com.example.sarisari.manager.core.navigation.ProductsRoute

fun NavGraphBuilder.productsScreen(onAddProduct: () -> Unit, onEditProduct: (Long) -> Unit) {
    composable<ProductsRoute> { ProductsScreen(onAddProduct = onAddProduct, onEditProduct = onEditProduct) }
}

fun NavGraphBuilder.productEditScreen(onBack: () -> Unit) {
    composable<ProductEditRoute> { ProductEditScreen(onBack = onBack) }
}
