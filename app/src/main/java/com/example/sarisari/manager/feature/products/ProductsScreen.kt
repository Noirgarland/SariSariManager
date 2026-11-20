package com.example.sarisari.manager.feature.products

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.sarisari.manager.R
import com.example.sarisari.manager.core.designsystem.SariSariManagerTheme
import com.example.sarisari.manager.domain.model.StockStatus
import com.example.sarisari.manager.feature.products.components.CategoriesDialog
import com.example.sarisari.manager.feature.products.components.ProductCard

@Composable
fun ProductsScreen(
    onAddProduct: () -> Unit,
    onEditProduct: (Long) -> Unit,
    viewModel: ProductsViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    ProductsContent(
        uiState = uiState,
        onQueryChange = viewModel::onQueryChange,
        onCategorySelected = viewModel::onCategorySelected,
        onShowArchivedChange = viewModel::onShowArchivedChange,
        onAddProduct = onAddProduct,
        onEditProduct = onEditProduct,
        onAddCategory = viewModel::addCategory,
        onRenameCategory = viewModel::renameCategory,
        onCategoryErrorShown = viewModel::onCategoryErrorShown,
    )
}

@Composable
internal fun ProductsContent(
    uiState: ProductsUiState,
    onQueryChange: (String) -> Unit,
    onCategorySelected: (Long?) -> Unit,
    onShowArchivedChange: (Boolean) -> Unit,
    onAddProduct: () -> Unit,
    onEditProduct: (Long) -> Unit,
    onAddCategory: (String) -> Unit,
    onRenameCategory: (Long, String) -> Unit,
    onCategoryErrorShown: () -> Unit,
) {
    var showCategories by remember { mutableStateOf(false) }

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        floatingActionButton = {
            FloatingActionButton(onClick = onAddProduct) {
                Icon(painterResource(R.drawable.ic_add), contentDescription = stringResource(R.string.products_add))
            }
        },
    ) { padding ->
        Column(
            modifier = Modifier.padding(padding).fillMaxSize().padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(stringResource(R.string.products_title), style = MaterialTheme.typography.headlineSmall)
                TextButton(onClick = { showCategories = true }) {
                    Text(stringResource(R.string.products_manage_categories))
                }
            }
            OutlinedTextField(
                value = uiState.query,
                onValueChange = onQueryChange,
                label = { Text(stringResource(R.string.products_search)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                item {
                    FilterChip(
                        selected = uiState.selectedCategoryId == null,
                        onClick = { onCategorySelected(null) },
                        label = { Text(stringResource(R.string.products_filter_all)) },
                    )
                }
                items(uiState.categories, key = { it.id }) { category ->
                    FilterChip(
                        selected = uiState.selectedCategoryId == category.id,
                        onClick = { onCategorySelected(category.id) },
                        label = { Text(category.name) },
                    )
                }
                item {
                    FilterChip(
                        selected = uiState.showArchived,
                        onClick = { onShowArchivedChange(!uiState.showArchived) },
                        label = { Text(stringResource(R.string.products_filter_archived)) },
                    )
                }
            }
            when {
                uiState.isLoading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
                uiState.products.isEmpty() -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        text = stringResource(
                            if (uiState.isFiltering) R.string.products_empty_filtered else R.string.products_empty
                        ),
                        style = MaterialTheme.typography.bodyLarge,
                    )
                }
                else -> LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = 88.dp),
                ) {
                    items(uiState.products, key = { it.id }) { item ->
                        ProductCard(item = item, onClick = { onEditProduct(item.id) })
                    }
                }
            }
        }
    }

    if (showCategories) {
        CategoriesDialog(
            categories = uiState.categories,
            errorRes = uiState.categoryErrorRes,
            onAdd = onAddCategory,
            onRename = onRenameCategory,
            onErrorShown = onCategoryErrorShown,
            onDismiss = { showCategories = false },
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun ProductsContentPreview() {
    SariSariManagerTheme {
        ProductsContent(
            uiState = ProductsUiState(
                isLoading = false,
                products = listOf(
                    ProductListItem(1, "Coke 1.5L", "Drinks", 4500, 6500, 24, "bottle", StockStatus.IN_STOCK, false),
                    ProductListItem(2, "Royal 1.5L", "Drinks", 4500, 6500, 3, "bottle", StockStatus.LOW_STOCK, false),
                ),
            ),
            onQueryChange = {}, onCategorySelected = {}, onShowArchivedChange = {}, onAddProduct = {},
            onEditProduct = {}, onAddCategory = {}, onRenameCategory = { _, _ -> }, onCategoryErrorShown = {},
        )
    }
}
