package com.example.sarisari.manager.feature.cashier

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.sarisari.manager.R
import com.example.sarisari.manager.core.designsystem.AppHeader
import com.example.sarisari.manager.core.designsystem.CardShape
import com.example.sarisari.manager.core.designsystem.CategoryChipRow
import com.example.sarisari.manager.core.designsystem.SariSariManagerTheme
import com.example.sarisari.manager.core.designsystem.SearchField
import com.example.sarisari.manager.core.designsystem.SurfaceCard
import com.example.sarisari.manager.core.util.Money
import com.example.sarisari.manager.domain.model.CompletedSale
import com.example.sarisari.manager.domain.model.Customer
import com.example.sarisari.manager.domain.model.Product
import com.example.sarisari.manager.feature.cashier.components.CustomerPickerDialog
import com.example.sarisari.manager.feature.cashier.components.PaymentMethodSelector
import com.example.sarisari.manager.feature.cashier.components.UtangCustomerCard
import com.example.sarisari.manager.feature.cashier.components.AmountKeypad
import com.example.sarisari.manager.feature.cashier.components.AmountKeypadInput
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
            onPaymentMethodSelected = viewModel::onPaymentMethodSelected,
            onCustomerPickerOpened = viewModel::onCustomerPickerOpened,
            onCustomerPickerDismissed = viewModel::onCustomerPickerDismissed,
            onCustomerQueryChange = viewModel::onCustomerQueryChange,
            onCustomerChosen = viewModel::onCustomerChosen,
            onAddCustomer = viewModel::onAddCustomer,
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
    val onPaymentMethodSelected: (PaymentMethod) -> Unit = {},
    val onCustomerPickerOpened: () -> Unit = {},
    val onCustomerPickerDismissed: () -> Unit = {},
    val onCustomerQueryChange: (String) -> Unit = {},
    val onCustomerChosen: (Customer) -> Unit = {},
    val onAddCustomer: (String) -> Unit = {},
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
    Column(modifier = Modifier.fillMaxSize()) {
        AppHeader(title = stringResource(R.string.sell_title))
        Column(
            modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            SearchField(uiState.query, actions.onQueryChange, stringResource(R.string.sell_search))
            CategoryChipRow(uiState.categories, uiState.selectedCategoryId, actions.onCategorySelected)
            Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                when {
                    uiState.isLoading -> CircularProgressIndicator(Modifier.align(Alignment.Center))
                    uiState.products.isEmpty() -> Text(
                        stringResource(if (uiState.isFiltering) R.string.sell_empty_filtered else R.string.sell_empty),
                        style = MaterialTheme.typography.bodyLarge,
                        textAlign = TextAlign.Center,
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

@Composable
private fun PaymentContent(uiState: CashierUiState, actions: CashierActions) {
    Column(modifier = Modifier.fillMaxSize()) {
        AppHeader(title = stringResource(R.string.payment_title), onBack = actions.onBackToCart)
        Column(
            modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            SurfaceCard(modifier = Modifier.fillMaxWidth()) {
                Text(
                    stringResource(R.string.payment_total_amount),
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )
                Text(
                    Money.format(uiState.totalCentavos),
                    style = MaterialTheme.typography.displaySmall,
                    color = MaterialTheme.colorScheme.primary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            Text(stringResource(R.string.payment_method_title), style = MaterialTheme.typography.titleMedium)
            PaymentMethodSelector(uiState.paymentMethod, actions.onPaymentMethodSelected)
            if (uiState.isUtang) {
                UtangCustomerCard(uiState.customer, actions.onCustomerPickerOpened)
            }
            Text(
                stringResource(if (uiState.isUtang) R.string.payment_cash_now_label else R.string.payment_received_label),
                style = MaterialTheme.typography.titleMedium,
            )
            ReceivedAmountDisplay(uiState.receivedText)
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                item {
                    OutlinedButton(
                        onClick = { actions.onQuickCash(uiState.totalCentavos) },
                        shape = CardShape,
                        modifier = Modifier.heightIn(min = 48.dp),
                    ) { Text(stringResource(R.string.payment_exact)) }
                }
                // On utang the owner may take part of the total now, so smaller notes are offered too.
                val quick = if (uiState.isUtang) quickCashPesos else quickCashPesos.filter { it * 100 >= uiState.totalCentavos }
                items(quick.take(if (uiState.isUtang) quickCashPesos.size else 4)) { pesos ->
                    OutlinedButton(
                        onClick = { actions.onQuickCash(pesos * 100) },
                        shape = CardShape,
                        modifier = Modifier.heightIn(min = 48.dp),
                    ) { Text(stringResource(R.string.payment_quick_amount, Money.format(pesos * 100))) }
                }
            }
            AmountKeypad(onKey = { key -> actions.onReceivedChange(AmountKeypadInput.press(uiState.receivedText, key)) })
            if (uiState.isUtang) {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    UtangAmountCard(uiState.creditCentavos, Modifier.weight(1f))
                    ChangeCard(uiState.changeCentavos, Modifier.weight(1f))
                }
            } else {
                ChangeCard(uiState.changeCentavos)
            }
            if (!uiState.isUtang && uiState.receivedText.isNotBlank() && uiState.changeCentavos == null) {
                Text(stringResource(R.string.payment_not_enough), color = MaterialTheme.colorScheme.error)
            }
            uiState.errorRes?.let { Text(stringResource(it), color = MaterialTheme.colorScheme.error) }
            uiState.stockShortages.forEach { shortage ->
                Text(
                    stringResource(
                        R.string.payment_stock_shortage_line,
                        shortage.productName,
                        shortage.availableBaseQuantity,
                        shortage.requestedBaseQuantity,
                        shortage.baseUnitName,
                    ),
                    color = MaterialTheme.colorScheme.error,
                )
            }
            Button(
                onClick = actions.onCompleteSale,
                enabled = uiState.canComplete,
                shape = CardShape,
                modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp),
            ) { Text(stringResource(R.string.payment_complete), style = MaterialTheme.typography.titleMedium) }
        }
    }
    if (uiState.showCustomerPicker) {
        CustomerPickerDialog(
            query = uiState.customerQuery,
            customers = uiState.customers,
            errorRes = uiState.customerErrorRes,
            onQueryChange = actions.onCustomerQueryChange,
            onChoose = actions.onCustomerChosen,
            onAdd = actions.onAddCustomer,
            onDismiss = actions.onCustomerPickerDismissed,
        )
    }
}

/** What goes on the customer's utang. Amber, to set it apart from cash. */
@Composable
private fun UtangAmountCard(creditCentavos: Long, modifier: Modifier = Modifier) {
    SurfaceCard(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.tertiaryContainer,
        contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
        border = false,
    ) {
        Text(
            stringResource(R.string.payment_utang_amount),
            style = MaterialTheme.typography.titleSmall,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
        Text(
            Money.format(creditCentavos),
            style = MaterialTheme.typography.headlineSmall,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

/** Shows the typed amount. Typing happens on the keypad below. */
@Composable
private fun ReceivedAmountDisplay(receivedText: String) {
    SurfaceCard(modifier = Modifier.fillMaxWidth(), contentPadding = 14.dp) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                stringResource(R.string.payment_peso_sign),
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                receivedText.ifEmpty { stringResource(R.string.payment_amount_placeholder) },
                style = MaterialTheme.typography.headlineSmall,
                color = if (receivedText.isEmpty()) MaterialTheme.colorScheme.outline else MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(start = 8.dp),
            )
        }
    }
}

@Composable
private fun ChangeCard(changeCentavos: Long?, modifier: Modifier = Modifier) {
    SurfaceCard(
        modifier = modifier.fillMaxWidth(),
        containerColor = MaterialTheme.colorScheme.primaryContainer,
        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        border = false,
    ) {
        Text(
            stringResource(R.string.payment_change),
            style = MaterialTheme.typography.titleSmall,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
        Text(
            Money.format(changeCentavos ?: 0),
            style = MaterialTheme.typography.displaySmall,
            color = MaterialTheme.colorScheme.primary,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun SaleCompleteContent(sale: CompletedSale, onNewSale: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(
            painterResource(R.drawable.ic_check_circle),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(72.dp),
        )
        Text(stringResource(R.string.sale_complete_title), style = MaterialTheme.typography.headlineMedium)
        SurfaceCard(modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.sale_complete_total, Money.format(sale.totalCentavos)), style = MaterialTheme.typography.titleMedium)
            Text(stringResource(R.string.sale_complete_received, Money.format(sale.amountReceivedCentavos)), style = MaterialTheme.typography.titleMedium)
            if (sale.creditCentavos > 0) {
                Text(
                    stringResource(R.string.sale_complete_utang, Money.format(sale.creditCentavos), sale.customerName.orEmpty()),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.tertiary,
                )
            }
        }
        ChangeCard(sale.changeCentavos)
        Button(onClick = onNewSale, shape = CardShape, modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp)) {
            Text(stringResource(R.string.sale_complete_new), style = MaterialTheme.typography.titleMedium)
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

@Preview(showBackground = true, heightDp = 1100)
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
