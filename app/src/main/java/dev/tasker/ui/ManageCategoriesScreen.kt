package dev.tasker.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.tasker.domain.Category

private sealed interface Dialog {
    data object Add : Dialog
    data class Rename(val category: Category) : Dialog
    data class Delete(val category: Category) : Dialog
}

/** Create, rename, delete and reorder (up/down) categories. Order here is the tab order. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManageCategoriesScreen(
    categories: List<Category>,
    onAdd: (String) -> Unit,
    onRename: (Long, String) -> Unit,
    onDelete: (Long) -> Unit,
    onMove: (Long, Int) -> Unit,
    onBack: () -> Unit,
) {
    var dialog by remember { mutableStateOf<Dialog?>(null) }

    Scaffold(
        containerColor = Surface0,
        topBar = {
            TopAppBar(
                title = { Text("Manage categories") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, "Back") } },
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { dialog = Dialog.Add }) { Icon(Icons.Default.Add, "Add category") }
        },
    ) { padding ->
        LazyColumn(Modifier.padding(padding), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            items(categories, key = { it.id }) { category ->
                val index = categories.indexOf(category)
                Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(category.name, modifier = Modifier.weight(1f))
                    IconButton(onClick = { onMove(category.id, -1) }, enabled = index > 0) { Icon(Icons.Default.KeyboardArrowUp, "Move up") }
                    IconButton(onClick = { onMove(category.id, 1) }, enabled = index < categories.lastIndex) { Icon(Icons.Default.KeyboardArrowDown, "Move down") }
                    IconButton(onClick = { dialog = Dialog.Rename(category) }) { Icon(Icons.Default.Edit, "Rename") }
                    IconButton(onClick = { dialog = Dialog.Delete(category) }, enabled = categories.size > 1) { Icon(Icons.Default.Delete, "Delete") }
                }
            }
        }
    }

    when (val d = dialog) {
        null -> Unit
        Dialog.Add -> NameDialog("New category", "", "Add", { dialog = null }) { onAdd(it); dialog = null }
        is Dialog.Rename -> NameDialog("Rename category", d.category.name, "Save", { dialog = null }) { onRename(d.category.id, it); dialog = null }
        is Dialog.Delete -> AlertDialog(
            onDismissRequest = { dialog = null },
            title = { Text("Delete \"${d.category.name}\"?") },
            text = { Text("All tasks in this category will be deleted too.") },
            confirmButton = { TextButton(onClick = { onDelete(d.category.id); dialog = null }) { Text("Delete") } },
            dismissButton = { TextButton(onClick = { dialog = null }) { Text("Cancel") } },
        )
    }
}

@Composable
private fun NameDialog(title: String, initial: String, confirm: String, onDismiss: () -> Unit, onConfirm: (String) -> Unit) {
    var text by remember { mutableStateOf(initial) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { OutlinedTextField(text, { text = it }, singleLine = true, label = { Text("Name") }) },
        confirmButton = { TextButton(onClick = { onConfirm(text) }, enabled = text.isNotBlank()) { Text(confirm) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}
