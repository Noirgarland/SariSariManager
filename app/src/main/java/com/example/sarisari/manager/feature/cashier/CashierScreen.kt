package com.example.sarisari.manager.feature.cashier

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.sarisari.manager.R
import com.example.sarisari.manager.core.designsystem.SariSariManagerTheme
import com.example.sarisari.manager.core.util.Money
import com.example.sarisari.manager.domain.model.CompletedSale
import com.example.sarisari.manager.domain.model.Product
import com.example.sarisari.manager.feature.cashier.components.CartPanel
import com.example.sarisari.manager.feature.cashier.components.PriceEditDialog
import com.example.sarisari.manager.feature.cashier.components.ProductTile
import com.example.sarisari.manager.feature.cashier.components.UnitPickerDialog

@Composable
fun CashierScreen(viewModel: CashierViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    BackHandler(enabled = uiState.stage == CashierStage.PAYMENT) { viewModel.onBackToCart() }
    CashierContent(
        uiState = uiState,
        actions = CashierActions(
            onQueryChange = viewModel::onQueryChange,
            onCategorySelected = viewModel::onCategorySelected,
            onProductTapped = viewModel::onProductTapped,
            onUnitPickerDismissed = viewModel::onUnitPickerDismissed,
            onUnitChosen = viewModel::onUnitChosen,
            onIncrease = viewModel::onIncrease,
            onDecrease = viewModel::onDecrease,
            onRemove = viewModel::onRemove,
            onClearCart = viewModel::onClearCart,
            onPriceChanged = viewModel::onPriceChanged,
            onProceedToPayment = viewModel::onProceedToPayment,
            onBackToCart = viewModel::onBackToCart,
            onReceivedChange = viewModel::onReceivedChange,
            onQuickCash = viewModel::onQuickCash,
            onCompleteSale = viewModel::onCompleteSale,
            onNewSale = viewModel::onNewSale,
        ),
    )
}

/** Everything the cashier screens can do, so the composables stay stateless. */
internal data class CashierActions(
    val onQueryChange: (String) -> Unit = {},
    val onCategorySelected: (Long?) -> Unit = {},
    val onProductTapped: (Product) -> Unit = {},
    val onUnitPickerDismissed: () -> Unit = {},
    val onUnitChosen: (Product, String, Int, Long) -> Unit = { _, _, _, _ -> },
    val onIncrease: (String) -> Unit = {},
    val onDecrease: (String) -> Unit = {},
    val onRemove: (String) -> Unit = {},
    val onClearCart: () -> Unit = {},
    val onPriceChanged: (String, String) -> Boolean = { _, _ -> true },
    val onProceedToPayment: () -> Unit = {},
    val onBackToCart: () -> Unit = {},
    val onReceivedChange: (String) -> Unit = {},
    val onQuickCash: (Long) -> Unit = {},
    val onCompleteSale: () -> Unit = {},
    val onNewSale: () -> Unit = {},
)

@Composable
internal fun CashierContent(uiState: CashierUiState, actions: CashierActions) {
    when (uiState.stage) {
        CashierStage.SELLING -> SellContent(uiState, actions)
        CashierStage.PAYMENT -> PaymentContent(uiState, actions)
        CashierStage.COMPLETE -> uiState.completedSale?.let { SaleCompleteContent(it, actions.onNewSale) }
    }
}

@Composable
private fun SellContent(uiState: CashierUiState, actions: CashierActions) {
    var priceEditKey by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(stringResource(R.string.sell_title), style = MaterialTheme.typography.headlineSmall)
        OutlinedTextField(
            value = uiState.query,
            onValueChange = actions.onQueryChange,
            label = { Text(stringResource(R.string.sell_search)) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            item {
                FilterChip(
                    selected = uiState.selectedCategoryId == null,
                    onClick = { actions.onCategorySelected(null) },
                    label = { Text(stringResource(R.string.products_filter_all)) },
                )
            }
            items(uiState.categories, key = { it.id }) { category ->
                FilterChip(
                    selected = uiState.selectedCategoryId == category.id,
                    onClick = { actions.onCategorySelected(category.id) },
                    label = { Text(category.name) },
                )
            }
        }
        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
            when {
                uiState.isLoading -> CircularProgressIndicator(Modifier.align(Alignment.Center))
                uiState.products.isEmpty() -> Text(
                    stringResource(if (uiState.isFiltering) R.string.sell_empty_filtered else R.string.sell_empty),
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.align(Alignment.Center),
                )
                else -> LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = 104.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(bottom = 8.dp),
                ) {
                    items(uiState.products, key = { it.id }) { product ->
                        ProductTile(product = product, onClick = { actions.onProductTapped(product) })
                    }
                }
            }
        }
        CartPanel(
            cart = uiState.cart,
            itemCount = uiState.itemCount,
            totalCentavos = uiState.totalCentavos,
            onIncrease = actions.onIncrease,
            onDecrease = actions.onDecrease,
            onRemove = actions.onRemove,
            onEditPrice = { priceEditKey = it.key },
            onClear = actions.onClearCart,
            onProceed = actions.onProceedToPayment,
        )
    }

    uiState.unitPickerProduct?.let { product ->
        UnitPickerDialog(
            product = product,
            onPick = { name, factor, price -> actions.onUnitChosen(product, name, factor, price) },
            onDismiss = actions.onUnitPickerDismissed,
        )
    }
    priceEditKey?.let { key ->
        uiState.cart.firstOrNull { it.key == key }?.let { line ->
            PriceEditDialog(
                unitName = line.unitName,
                initialCentavos = line.unitPriceCentavos,
                onConfirm = { actions.onPriceChanged(key, it) },
                onDismiss = { priceEditKey = null },
            )
        }
    }
}

private val quickCashPesos = listOf(20L, 50L, 100L, 200L, 500L, 1000L)

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun PaymentContent(uiState: CashierUiState, actions: CashierActions) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .imePadding()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(stringResource(R.string.payment_title), style = MaterialTheme.typography.headlineSmall)
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(stringResource(R.string.payment_total_amount), style = MaterialTheme.typography.bodyMedium)
                Text(
                    Money.format(uiState.totalCentavos),
                    style = MaterialTheme.typography.displaySmall,
                    color = MaterialTheme.colorScheme.primary,
                )
                Text(stringResource(R.string.payment_method_cash), style = MaterialTheme.typography.bodyMedium)
            }
        }
        OutlinedTextField(
            value = uiState.receivedText,
            onValueChange = actions.onReceivedChange,
            label = { Text(stringResource(R.string.payment_received_label)) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            modifier = Modifier.fillMaxWidth(),
        )
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(
                onClick = { actions.onQuickCash(uiState.totalCentavos) },
                modifier = Modifier.heightIn(min = 48.dp),
            ) { Text(stringResource(R.string.payment_exact)) }
            quickCashPesos.filter { it * 100 >= uiState.totalCentavos }.take(4).forEach { pesos ->
                OutlinedButton(
                    onClick = { actions.onQuickCash(pesos * 100) },
                    modifier = Modifier.heightIn(min = 48.dp),
                ) { Text(stringResource(R.string.payment_quick_amount, Money.format(pesos * 100))) }
            }
        }
        val change = uiState.changeCentavos
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
        ) {
            Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(stringResource(R.string.payment_change), style = MaterialTheme.typography.bodyMedium)
                Text(
                    Money.format(change ?: 0),
                    style = MaterialTheme.typography.displaySmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                )
            }
        }
        if (uiState.receivedText.isNotBlank() && change == null) {
            Text(stringResource(R.string.payment_not_enough), color = MaterialTheme.colorScheme.error)
        }
        uiState.errorRes?.let { Text(stringResource(it), color = MaterialTheme.colorScheme.error) }
        Button(
            onClick = actions.onCompleteSale,
            enabled = uiState.canComplete,
            modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp),
        ) { Text(stringResource(R.string.payment_complete)) }
        TextButton(onClick = actions.onBackToCart, modifier = Modifier.heightIn(min = 48.dp)) {
            Text(stringResource(R.string.payment_back))
        }
    }
}

@Composable
private fun SaleCompleteContent(sale: CompletedSale, onNewSale: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(stringResource(R.string.sale_complete_title), style = MaterialTheme.typography.headlineMedium)
        Text(stringResource(R.string.sale_complete_total, Money.format(sale.totalCentavos)), style = MaterialTheme.typography.titleMedium)
        Text(stringResource(R.string.sale_complete_received, Money.format(sale.amountReceivedCentavos)), style = MaterialTheme.typography.titleMedium)
        Text(
            stringResource(R.string.sale_complete_change, Money.format(sale.changeCentavos)),
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.primary,
        )
        Button(onClick = onNewSale, modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp)) {
            Text(stringResource(R.string.sale_complete_new))
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun SellPreview() {
    SariSariManagerTheme {
        CashierContent(
            CashierUiState(
                isLoading = false,
                products = listOf(
                    Product(1, "Coke 1.5L", null, "bottle", 4500, 6500, stockQuantity = 24, lowStockThreshold = 5),
                    Product(2, "Piattos", null, "pack", 1200, 2000, stockQuantity = 2, lowStockThreshold = 5),
                ),
                cart = listOf(CartLine(1, "Coke 1.5L", "bottle", 1, 2, 6500, 24)),
            ),
            CashierActions(),
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun PaymentPreview() {
    SariSariManagerTheme {
        CashierContent(
            CashierUiState(
                isLoading = false,
                stage = CashierStage.PAYMENT,
                cart = listOf(CartLine(1, "Coke 1.5L", "bottle", 1, 2, 6500, 24)),
                receivedText = "200",
            ),
            CashierActions(),
        )
    }
}
