package com.example.sarisari.manager.feature.products

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.sarisari.manager.R
import com.example.sarisari.manager.core.designsystem.SariSariManagerTheme
import com.example.sarisari.manager.domain.usecase.ProductError

@Composable
fun ProductEditScreen(onBack: () -> Unit, viewModel: ProductEditViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    LaunchedEffect(uiState.isDone) {
        if (uiState.isDone) onBack()
    }
    ProductEditContent(
        uiState = uiState,
        onBack = onBack,
        onNameChange = viewModel::onNameChange,
        onCategoryChange = viewModel::onCategoryChange,
        onBaseUnitChange = viewModel::onBaseUnitChange,
        onCostChange = viewModel::onCostChange,
        onPriceChange = viewModel::onPriceChange,
        onThresholdChange = viewModel::onThresholdChange,
        onAddUnit = viewModel::onAddUnit,
        onRemoveUnit = viewModel::onRemoveUnit,
        onUnitChange = viewModel::onUnitChange,
        onSave = viewModel::onSave,
        onArchiveChange = viewModel::onArchiveChange,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ProductEditContent(
    uiState: ProductEditUiState,
    onBack: () -> Unit,
    onNameChange: (String) -> Unit,
    onCategoryChange: (Long?) -> Unit,
    onBaseUnitChange: (String) -> Unit,
    onCostChange: (String) -> Unit,
    onPriceChange: (String) -> Unit,
    onThresholdChange: (String) -> Unit,
    onAddUnit: () -> Unit,
    onRemoveUnit: (Int) -> Unit,
    onUnitChange: (Int, UnitForm) -> Unit,
    onSave: () -> Unit,
    onArchiveChange: (Boolean) -> Unit,
) {
    var confirmArchive by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        stringResource(
                            if (uiState.isEditing) R.string.product_form_edit_title else R.string.product_form_add_title
                        )
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(painterResource(R.drawable.ic_arrow_back), stringResource(R.string.action_back))
                    }
                },
            )
        },
    ) { padding ->
        if (uiState.isLoading) {
            Box(Modifier.padding(padding).fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            return@Scaffold
        }
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            FormField(
                value = uiState.name,
                onValueChange = onNameChange,
                label = R.string.product_form_name,
                error = errorText(uiState, ProductError.NAME_REQUIRED, R.string.product_error_name),
            )
            CategoryDropdown(uiState, onCategoryChange)
            FormField(
                value = uiState.baseUnit,
                onValueChange = onBaseUnitChange,
                label = R.string.product_form_base_unit,
                hint = R.string.product_form_base_unit_hint,
                error = errorText(uiState, ProductError.BASE_UNIT_REQUIRED, R.string.product_error_base_unit),
            )
            FormField(
                value = uiState.cost,
                onValueChange = onCostChange,
                label = R.string.product_form_cost,
                keyboardType = KeyboardType.Decimal,
                error = errorText(uiState, ProductError.COST_INVALID, R.string.product_error_cost),
            )
            FormField(
                value = uiState.price,
                onValueChange = onPriceChange,
                label = R.string.product_form_price,
                keyboardType = KeyboardType.Decimal,
                error = errorText(uiState, ProductError.PRICE_INVALID, R.string.product_error_price),
            )
            FormField(
                value = uiState.threshold,
                onValueChange = onThresholdChange,
                label = R.string.product_form_threshold,
                hint = R.string.product_form_threshold_hint,
                keyboardType = KeyboardType.Number,
                error = errorText(uiState, ProductError.THRESHOLD_INVALID, R.string.product_error_threshold),
            )
            if (!uiState.isEditing) {
                Text(
                    stringResource(R.string.product_form_stock_note),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            UnitsSection(uiState, onAddUnit, onRemoveUnit, onUnitChange)

            Button(
                onClick = onSave,
                enabled = !uiState.isSaving,
                modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
            ) { Text(stringResource(R.string.action_save)) }

            if (uiState.isEditing) {
                OutlinedButton(
                    onClick = { if (uiState.isArchived) onArchiveChange(false) else confirmArchive = true },
                    modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                ) {
                    Text(
                        stringResource(
                            if (uiState.isArchived) R.string.product_form_restore else R.string.product_form_archive
                        )
                    )
                }
            }
        }
    }

    if (confirmArchive) {
        AlertDialog(
            onDismissRequest = { confirmArchive = false },
            title = { Text(stringResource(R.string.product_archive_confirm_title)) },
            text = { Text(stringResource(R.string.product_archive_confirm_message)) },
            confirmButton = {
                TextButton(onClick = {
                    confirmArchive = false
                    onArchiveChange(true)
                }) { Text(stringResource(R.string.product_archive_confirm_action)) }
            },
            dismissButton = {
                TextButton(onClick = { confirmArchive = false }) { Text(stringResource(R.string.action_cancel)) }
            },
        )
    }
}

@Composable
private fun errorText(state: ProductEditUiState, error: ProductError, messageRes: Int): String? =
    if (error in state.errors) stringResource(messageRes) else null

@Composable
private fun FormField(
    value: String,
    onValueChange: (String) -> Unit,
    label: Int,
    modifier: Modifier = Modifier,
    hint: Int? = null,
    error: String? = null,
    keyboardType: KeyboardType = KeyboardType.Text,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(stringResource(label)) },
        singleLine = true,
        isError = error != null,
        supportingText = (error ?: hint?.let { stringResource(it) })?.let { { Text(it) } },
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        modifier = modifier.fillMaxWidth(),
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CategoryDropdown(uiState: ProductEditUiState, onCategoryChange: (Long?) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    val noCategory = stringResource(R.string.product_form_no_category)
    val selectedName = uiState.categories.firstOrNull { it.id == uiState.categoryId }?.name ?: noCategory
    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
        OutlinedTextField(
            value = selectedName,
            onValueChange = {},
            readOnly = true,
            label = { Text(stringResource(R.string.product_form_category)) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier.fillMaxWidth().menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable),
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            DropdownMenuItem(
                text = { Text(noCategory) },
                onClick = {
                    onCategoryChange(null)
                    expanded = false
                },
            )
            uiState.categories.forEach { category ->
                DropdownMenuItem(
                    text = { Text(category.name) },
                    onClick = {
                        onCategoryChange(category.id)
                        expanded = false
                    },
                )
            }
        }
    }
}

@Composable
private fun UnitsSection(
    uiState: ProductEditUiState,
    onAddUnit: () -> Unit,
    onRemoveUnit: (Int) -> Unit,
    onUnitChange: (Int, UnitForm) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(stringResource(R.string.product_form_units_title), style = MaterialTheme.typography.titleMedium)
        Text(
            stringResource(R.string.product_form_units_hint),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        uiState.units.forEachIndexed { index, unit ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.Top) {
                FormField(
                    value = unit.name,
                    onValueChange = { onUnitChange(index, unit.copy(name = it)) },
                    label = R.string.product_form_unit_name,
                    modifier = Modifier.weight(1.2f),
                )
                FormField(
                    value = unit.quantity,
                    onValueChange = { onUnitChange(index, unit.copy(quantity = it)) },
                    label = R.string.product_form_unit_quantity,
                    keyboardType = KeyboardType.Number,
                    modifier = Modifier.weight(1f),
                )
                FormField(
                    value = unit.price,
                    onValueChange = { onUnitChange(index, unit.copy(price = it)) },
                    label = R.string.product_form_unit_price,
                    keyboardType = KeyboardType.Decimal,
                    modifier = Modifier.weight(1f),
                )
                IconButton(onClick = { onRemoveUnit(index) }, modifier = Modifier.padding(top = 4.dp)) {
                    Icon(painterResource(R.drawable.ic_close), stringResource(R.string.product_form_unit_remove))
                }
            }
        }
        unitErrors.forEach { (error, messageRes) ->
            if (error in uiState.errors) {
                Text(stringResource(messageRes), color = MaterialTheme.colorScheme.error)
            }
        }
        OutlinedButton(onClick = onAddUnit, modifier = Modifier.heightIn(min = 48.dp)) {
            Text(stringResource(R.string.product_form_unit_add))
        }
    }
}

private val unitErrors = listOf(
    ProductError.UNIT_NAME_INVALID to R.string.product_error_unit_name,
    ProductError.UNIT_NAME_DUPLICATE to R.string.product_error_unit_duplicate,
    ProductError.UNIT_QUANTITY_INVALID to R.string.product_error_unit_quantity,
    ProductError.UNIT_PRICE_INVALID to R.string.product_error_unit_price,
)

@Preview(showBackground = true)
@Composable
private fun ProductEditContentPreview() {
    SariSariManagerTheme {
        ProductEditContent(
            uiState = ProductEditUiState(
                isLoading = false, isEditing = true, name = "Coke 1.5L", baseUnit = "bottle",
                cost = "45.00", price = "65.00", units = listOf(UnitForm("pack", "6", "360.00")),
            ),
            onBack = {}, onNameChange = {}, onCategoryChange = {}, onBaseUnitChange = {}, onCostChange = {},
            onPriceChange = {}, onThresholdChange = {}, onAddUnit = {}, onRemoveUnit = {},
            onUnitChange = { _, _ -> }, onSave = {}, onArchiveChange = {},
        )
    }
}
