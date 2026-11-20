package dev.tasker.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.tasker.domain.Category
import dev.tasker.domain.Task
import dev.tasker.domain.TaskLogic
import kotlin.math.roundToInt

/** Create/edit dialog. The "List" picker moves the task to another category on save. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskDialog(
    initial: Task?,
    categories: List<Category>,
    initialCategoryId: Long,
    onDismiss: () -> Unit,
    onSave: (title: String, notes: String, priority: Int, categoryId: Long) -> Unit,
    onDelete: (() -> Unit)?,
) {
    var title by remember { mutableStateOf(initial?.title ?: "") }
    var notes by remember { mutableStateOf(initial?.notes ?: "") }
    var priority by remember { mutableIntStateOf(initial?.priority ?: TaskLogic.DEFAULT_PRIORITY) }
    var categoryId by remember { mutableLongStateOf(initialCategoryId) }
    var pickerOpen by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initial == null) "New task" else "Edit task") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(title, { title = it }, label = { Text("Title") }, singleLine = true,
                    modifier = Modifier.fillMaxWidth())
                OutlinedTextField(notes, { notes = it }, label = { Text("Notes (optional)") }, minLines = 2,
                    modifier = Modifier.fillMaxWidth())
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Priority", Modifier.weight(1f))
                    PriorityIndicator(priority)
                }
                Slider(
                    value = priority.toFloat(),
                    onValueChange = { priority = TaskLogic.clampPriority(it.roundToInt()) },
                    valueRange = TaskLogic.MIN_PRIORITY.toFloat()..TaskLogic.MAX_PRIORITY.toFloat(),
                    steps = TaskLogic.MAX_PRIORITY - TaskLogic.MIN_PRIORITY - 1,
                )
                ExposedDropdownMenuBox(expanded = pickerOpen, onExpandedChange = { pickerOpen = it }) {
                    OutlinedTextField(
                        value = categories.firstOrNull { it.id == categoryId }?.name ?: "",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text(if (initial == null) "List" else "List (move task)") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(pickerOpen) },
                        modifier = Modifier.fillMaxWidth().menuAnchor(MenuAnchorType.PrimaryNotEditable),
                    )
                    ExposedDropdownMenu(expanded = pickerOpen, onDismissRequest = { pickerOpen = false }) {
                        categories.forEach { c ->
                            DropdownMenuItem(text = { Text(c.name) }, onClick = { categoryId = c.id; pickerOpen = false })
                        }
                    }
                }
                if (onDelete != null) {
                    TextButton(onClick = onDelete, colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error)) { Text("Delete task") }
                }
            }
        },
        confirmButton = {
            TextButton(enabled = title.isNotBlank(), onClick = { onSave(title, notes, priority, categoryId) }) {
                Text("Save")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}
