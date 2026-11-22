package com.example.sarisari.manager.feature.utang

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.sarisari.manager.R
import com.example.sarisari.manager.core.designsystem.AppHeader
import com.example.sarisari.manager.core.designsystem.CardShape
import com.example.sarisari.manager.core.designsystem.ProductAvatar
import com.example.sarisari.manager.core.designsystem.SariSariManagerTheme
import com.example.sarisari.manager.core.designsystem.SearchField
import com.example.sarisari.manager.core.designsystem.SurfaceCard
import com.example.sarisari.manager.core.util.Dates
import com.example.sarisari.manager.core.util.Money
import com.example.sarisari.manager.domain.model.Customer

@Composable
fun UtangScreen(
    onBack: () -> Unit,
    onOpenCustomer: (Long) -> Unit,
    viewModel: UtangViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    UtangContent(
        uiState = uiState,
        actions = UtangActions(
            onBack = onBack,
            onOpenCustomer = onOpenCustomer,
            onQueryChange = viewModel::onQueryChange,
            onNewCustomer = viewModel::onNewCustomerClicked,
            onNewCustomerNameChange = viewModel::onNewCustomerNameChange,
            onSaveNewCustomer = { viewModel.onSaveNewCustomer(onOpenCustomer) },
            onNewCustomerDismissed = viewModel::onNewCustomerDismissed,
        ),
    )
}

internal data class UtangActions(
    val onBack: () -> Unit = {},
    val onOpenCustomer: (Long) -> Unit = {},
    val onQueryChange: (String) -> Unit = {},
    val onNewCustomer: () -> Unit = {},
    val onNewCustomerNameChange: (String) -> Unit = {},
    val onSaveNewCustomer: () -> Unit = {},
    val onNewCustomerDismissed: () -> Unit = {},
)

@Composable
internal fun UtangContent(uiState: UtangUiState, actions: UtangActions) {
    Column(modifier = Modifier.fillMaxSize()) {
        AppHeader(title = stringResource(R.string.utang_title), onBack = actions.onBack)
        Column(
            modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            OutstandingCard(uiState.outstandingCentavos)
            Button(
                onClick = actions.onNewCustomer,
                shape = CardShape,
                modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
            ) {
                Icon(painterResource(R.drawable.ic_add), contentDescription = null)
                Text(stringResource(R.string.utang_new_customer), modifier = Modifier.padding(start = 8.dp))
            }
            SearchField(uiState.query, actions.onQueryChange, stringResource(R.string.utang_search))
            when {
                uiState.isLoading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
                uiState.customers.isEmpty() -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        stringResource(if (uiState.query.isBlank()) R.string.utang_empty else R.string.utang_no_match),
                        style = MaterialTheme.typography.bodyLarge,
                        textAlign = TextAlign.Center,
                    )
                }
                else -> LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp), contentPadding = PaddingValues(bottom = 16.dp)) {
                    items(uiState.customers, key = { it.id }) { customer ->
                        CustomerRow(customer, onClick = { actions.onOpenCustomer(customer.id) })
                    }
                }
            }
        }
    }
    uiState.newCustomer?.let { form ->
        CustomerNameDialog(
            title = stringResource(R.string.utang_new_customer),
            form = form,
            onNameChange = actions.onNewCustomerNameChange,
            onSave = actions.onSaveNewCustomer,
            onDismiss = actions.onNewCustomerDismissed,
        )
    }
}

@Composable
private fun OutstandingCard(outstandingCentavos: Long) {
    SurfaceCard(
        modifier = Modifier.fillMaxWidth(),
        containerColor = MaterialTheme.colorScheme.errorContainer,
        contentColor = MaterialTheme.colorScheme.onErrorContainer,
        border = false,
    ) {
        Text(stringResource(R.string.utang_total_outstanding), style = MaterialTheme.typography.titleSmall)
        Text(Money.format(outstandingCentavos), style = MaterialTheme.typography.headlineMedium)
    }
}

@Composable
private fun CustomerRow(customer: Customer, onClick: () -> Unit) {
    SurfaceCard(modifier = Modifier.fillMaxWidth(), onClick = onClick, contentPadding = 12.dp) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            ProductAvatar(customer.name, size = 44.dp)
            Column(modifier = Modifier.weight(1f)) {
                Text(customer.name, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                customer.lastCreditAtMillis?.let {
                    Text(
                        stringResource(R.string.utang_last_credit, Dates.formatDate(it)),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                customer.lastPaymentAtMillis?.let {
                    Text(
                        stringResource(R.string.utang_last_payment, Dates.formatDate(it)),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Text(
                Money.format(customer.balanceCentavos),
                style = MaterialTheme.typography.titleMedium,
                color = if (customer.balanceCentavos > 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
            )
        }
    }
}

/** Shared by "New customer" and "Rename customer". */
@Composable
internal fun CustomerNameDialog(
    title: String,
    form: NameForm,
    onNameChange: (String) -> Unit,
    onSave: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            OutlinedTextField(
                value = form.name,
                onValueChange = onNameChange,
                label = { Text(stringResource(R.string.utang_customer_name)) },
                singleLine = true,
                isError = form.errorRes != null,
                supportingText = form.errorRes?.let { { Text(stringResource(it)) } },
                modifier = Modifier.fillMaxWidth(),
            )
        },
        confirmButton = {
            TextButton(onClick = onSave, enabled = !form.isSaving) { Text(stringResource(R.string.action_save)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) } },
    )
}

@Preview(showBackground = true)
@Composable
private fun UtangPreview() {
    SariSariManagerTheme {
        UtangContent(
            UtangUiState(
                isLoading = false,
                outstandingCentavos = 92_000,
                customers = listOf(
                    Customer(1, "Juan Dela Cruz", 35_000, 1_763_420_000_000, null),
                    Customer(2, "Maria Santos", 12_000, 1_763_300_000_000, 1_763_400_000_000),
                ),
            ),
            UtangActions(),
        )
    }
}
