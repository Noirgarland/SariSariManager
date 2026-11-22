package com.example.sarisari.manager.feature.inventory

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.FlowRow
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.sarisari.manager.R
import com.example.sarisari.manager.core.designsystem.BackTopBar
import com.example.sarisari.manager.core.designsystem.SariSariManagerTheme
import com.example.sarisari.manager.core.util.Money
import com.example.sarisari.manager.domain.usecase.AdjustmentKind
import com.example.sarisari.manager.domain.usecase.StockEntryError

@Composable
fun StockEntryScreen(
    onBack: () -> Unit,
    onOpenHistory: () -> Unit,
    viewModel: StockEntryViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    LaunchedEffect(uiState.isDone) {
        if (uiState.isDone) onBack()
    }
    StockEntryContent(
        uiState = uiState,
        onBack = onBack,
        onOpenHistory = onOpenHistory,
        onModeChange = viewModel::onModeChange,
        onUnitSelected = viewModel::onUnitSelected,
        onQuantityChange = viewModel::onQuantityChange,
        onCostChange = viewModel::onCostChange,
        onKindChange = viewModel::onKindChange,
        onNoteChange = viewModel::onNoteChange,
        onSave = viewModel::onSave,
    )
}

@OptIn(ExperimentalMaterial3Api::class, androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
internal fun StockEntryContent(
    uiState: StockEntryUiState,
    onBack: () -> Unit,
    onOpenHistory: () -> Unit,
    onModeChange: (StockEntryMode) -> Unit,
    onUnitSelected: (Int) -> Unit,
    onQuantityChange: (String) -> Unit,
    onCostChange: (String) -> Unit,
    onKindChange: (AdjustmentKind) -> Unit,
    onNoteChange: (String) -> Unit,
    onSave: () -> Unit,
) {
    Scaffold(topBar = { BackTopBar(stringResource(R.string.stock_entry_title), onBack) }) { padding ->
        if (uiState.isLoading) {
            Box(Modifier.padding(padding).fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            return@Scaffold
        }
        val isReceive = uiState.mode == StockEntryMode.RECEIVE
        val selectedUnitName = uiState.selectedUnit?.name ?: uiState.baseUnitName
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(uiState.productName, style = MaterialTheme.typography.titleLarge)
            Text(
                stringResource(R.string.stock_entry_current, uiState.currentStock, uiState.baseUnitName),
                style = MaterialTheme.typography.bodyLarge,
            )
            Text(
                stringResource(
                    R.string.stock_entry_current_cost,
                    Money.format(uiState.currentCostCentavos),
                    uiState.baseUnitName,
                ),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = isReceive,
                    onClick = { onModeChange(StockEntryMode.RECEIVE) },
                    label = { Text(stringResource(R.string.stock_mode_receive)) },
                )
                FilterChip(
                    selected = !isReceive,
                    onClick = { onModeChange(StockEntryMode.ADJUST) },
                    label = { Text(stringResource(R.string.stock_mode_adjust)) },
                )
            }

            if (isReceive) {
                if (uiState.unitOptions.size > 1) {
                    Text(stringResource(R.string.stock_receive_unit), style = MaterialTheme.typography.titleSmall)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        uiState.unitOptions.forEachIndexed { index, option ->
                            FilterChip(
                                selected = index == uiState.selectedUnitIndex,
                                onClick = { onUnitSelected(index) },
                                label = {
                                    Text(
                                        if (option.factor == 1) {
                                            stringResource(R.string.stock_receive_unit_base, option.name)
                                        } else {
                                            stringResource(
                                                R.string.stock_receive_unit_pack,
                                                option.name, option.factor, uiState.baseUnitName,
                                            )
                                        }
                                    )
                                },
                            )
                        }
                    }
                }
                EntryField(
                    value = uiState.quantity,
                    onValueChange = onQuantityChange,
                    label = stringResource(R.string.stock_receive_quantity),
                    keyboardType = KeyboardType.Number,
                    error = errorText(uiState, StockEntryError.QUANTITY_INVALID, R.string.stock_error_quantity)
                        ?: errorText(uiState, StockEntryError.STOCK_LIMIT_EXCEEDED, R.string.stock_error_limit),
                )
                EntryField(
                    value = uiState.cost,
                    onValueChange = onCostChange,
                    label = stringResource(R.string.stock_receive_cost, selectedUnitName),
                    keyboardType = KeyboardType.Decimal,
                    error = errorText(uiState, StockEntryError.COST_INVALID, R.string.stock_error_cost),
                )
                EntryField(
                    value = uiState.note,
                    onValueChange = onNoteChange,
                    label = stringResource(R.string.stock_note_optional),
                )
            } else {
                Text(stringResource(R.string.stock_adjust_kind), style = MaterialTheme.typography.titleSmall)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    adjustmentKinds.forEach { (kind, labelRes) ->
                        FilterChip(
                            selected = uiState.kind == kind,
                            onClick = { onKindChange(kind) },
                            label = { Text(stringResource(labelRes)) },
                        )
                    }
                }
                EntryField(
                    value = uiState.quantity,
                    onValueChange = onQuantityChange,
                    label = stringResource(R.string.stock_adjust_quantity, uiState.baseUnitName),
                    keyboardType = KeyboardType.Number,
                    error = errorText(uiState, StockEntryError.QUANTITY_INVALID, R.string.stock_error_quantity)
                        ?: errorText(uiState, StockEntryError.INSUFFICIENT_STOCK, R.string.stock_error_insufficient)
                        ?: errorText(uiState, StockEntryError.STOCK_LIMIT_EXCEEDED, R.string.stock_error_limit),
                )
                val manual = uiState.kind == AdjustmentKind.MANUAL_INCREASE ||
                    uiState.kind == AdjustmentKind.MANUAL_DECREASE
                EntryField(
                    value = uiState.note,
                    onValueChange = onNoteChange,
                    label = stringResource(if (manual) R.string.stock_note_required else R.string.stock_note_optional),
                    error = errorText(uiState, StockEntryError.NOTE_REQUIRED, R.string.stock_error_note),
                )
            }

            Button(
                onClick = onSave,
                enabled = !uiState.isSaving,
                modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
            ) { Text(stringResource(R.string.action_save)) }
            TextButton(onClick = onOpenHistory, modifier = Modifier.heightIn(min = 48.dp)) {
                Text(stringResource(R.string.stock_view_history))
            }
        }
    }
}

private val adjustmentKinds = listOf(
    AdjustmentKind.DAMAGED to R.string.stock_kind_damaged,
    AdjustmentKind.EXPIRED to R.string.stock_kind_expired,
    AdjustmentKind.MANUAL_INCREASE to R.string.stock_kind_increase,
    AdjustmentKind.MANUAL_DECREASE to R.string.stock_kind_decrease,
)

@Composable
private fun errorText(state: StockEntryUiState, error: StockEntryError, messageRes: Int): String? =
    if (error in state.errors) stringResource(messageRes) else null

@Composable
private fun EntryField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    keyboardType: KeyboardType = KeyboardType.Text,
    error: String? = null,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        singleLine = true,
        isError = error != null,
        supportingText = error?.let { { Text(it) } },
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        modifier = Modifier.fillMaxWidth(),
    )
}

@Preview(showBackground = true)
@Composable
private fun StockEntryContentPreview() {
    SariSariManagerTheme {
        StockEntryContent(
            uiState = StockEntryUiState(
                isLoading = false, productName = "Coke 1.5L", baseUnitName = "bottle", currentStock = 4,
                currentCostCentavos = 4500,
                unitOptions = listOf(UnitOption("bottle", 1), UnitOption("case", 12)),
            ),
            onBack = {}, onOpenHistory = {}, onModeChange = {}, onUnitSelected = {}, onQuantityChange = {},
            onCostChange = {}, onKindChange = {}, onNoteChange = {}, onSave = {},
        )
    }
}
