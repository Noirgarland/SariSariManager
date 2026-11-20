package com.example.sarisari.manager.feature.products.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.unit.dp
import com.example.sarisari.manager.R
import com.example.sarisari.manager.domain.model.Category

/** Lists categories, with a field to add one or rename the selected one. */
@Composable
fun CategoriesDialog(
    categories: List<Category>,
    errorRes: Int?,
    onAdd: (String) -> Unit,
    onRename: (id: Long, name: String) -> Unit,
    onErrorShown: () -> Unit,
    onDismiss: () -> Unit,
) {
    var text by remember { mutableStateOf("") }
    var renamingId by remember { mutableStateOf<Long?>(null) }

    AlertDialog(
        onDismissRequest = {
            onErrorShown()
            onDismiss()
        },
        title = { Text(stringResource(R.string.categories_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (categories.isEmpty()) {
                    Text(stringResource(R.string.categories_empty))
                } else {
                    LazyColumn(modifier = Modifier.heightIn(max = 240.dp)) {
                        items(categories, key = { it.id }) { category ->
                            Row(
                                modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                            ) {
                                Text(category.name, modifier = Modifier.weight(1f))
                                TextButton(onClick = {
                                    renamingId = category.id
                                    text = category.name
                                    onErrorShown()
                                }) { Text(stringResource(R.string.action_rename)) }
                            }
                        }
                    }
                }
                OutlinedTextField(
                    value = text,
                    onValueChange = {
                        text = it
                        onErrorShown()
                    },
                    label = { Text(stringResource(R.string.category_name_label)) },
                    singleLine = true,
                    isError = errorRes != null,
                    supportingText = errorRes?.let { { Text(stringResource(it), color = MaterialTheme.colorScheme.error) } },
                    modifier = Modifier.fillMaxWidth(),
                )
                Button(
                    onClick = {
                        val id = renamingId
                        if (id == null) onAdd(text) else onRename(id, text)
                        if (text.isNotBlank() && errorRes == null) {
                            text = ""
                            renamingId = null
                        }
                    },
                    modifier = Modifier.heightIn(min = 48.dp),
                ) {
                    Text(stringResource(if (renamingId == null) R.string.action_add else R.string.action_save))
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                onErrorShown()
                onDismiss()
            }) { Text(stringResource(R.string.action_close)) }
        },
    )
}
