package dev.tasker.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.tasker.domain.Category

/** Create, rename, delete and reorder lists. Up/down order here is the tab order on the home screen. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManageListsScreen(vm: MainViewModel, onBack: () -> Unit) {
    val categories by vm.categories.collectAsState(initial = emptyList())
    var naming by remember { mutableStateOf<Category?>(null) }
    var creating by remember { mutableStateOf(false) }
    var deleting by remember { mutableStateOf<Category?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Manage lists") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") } },
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { creating = true },
                icon = { Icon(Icons.Filled.Add, null) },
                text = { Text("New list") },
            )
        },
    ) { padding ->
        LazyColumn(Modifier.padding(padding).fillMaxSize(), contentPadding = PaddingValues(bottom = 96.dp)) {
            itemsIndexed(categories, key = { _, c -> c.id }) { index, c ->
                ListItem(
                    headlineContent = { Text(c.name) },
                    trailingContent = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(enabled = index > 0, onClick = { vm.moveCategory(c.id, -1) }) {
                                Icon(Icons.Filled.KeyboardArrowUp, "Move up")
                            }
                            IconButton(enabled = index < categories.lastIndex, onClick = { vm.moveCategory(c.id, 1) }) {
                                Icon(Icons.Filled.KeyboardArrowDown, "Move down")
                            }
                            IconButton(onClick = { naming = c }) { Icon(Icons.Filled.Edit, "Rename") }
                            IconButton(enabled = categories.size > 1, onClick = { deleting = c }) {
                                Icon(Icons.Filled.Delete, "Delete")
                            }
                        }
                    },
                )
                HorizontalDivider()
            }
        }
    }

    if (creating) NameDialog("New list", "", onDismiss = { creating = false }) {
        vm.addCategory(it); creating = false
    }
    naming?.let { c ->
        NameDialog("Rename list", c.name, onDismiss = { naming = null }) { vm.renameCategory(c.id, it); naming = null }
    }
    deleting?.let { c ->
        AlertDialog(
            onDismissRequest = { deleting = null },
            title = { Text("Delete \"${c.name}\"?") },
            text = { Text("All tasks in this list will be deleted too.") },
            confirmButton = { TextButton(onClick = { vm.deleteCategory(c.id); deleting = null }) { Text("Delete") } },
            dismissButton = { TextButton(onClick = { deleting = null }) { Text("Cancel") } },
        )
    }
}

@Composable
private fun NameDialog(title: String, initial: String, onDismiss: () -> Unit, onSave: (String) -> Unit) {
    var name by remember { mutableStateOf(initial) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { OutlinedTextField(name, { name = it }, label = { Text("Name") }, singleLine = true) },
        confirmButton = { TextButton(enabled = name.isNotBlank(), onClick = { onSave(name) }) { Text("Save") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}
