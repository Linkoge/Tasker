package dev.tasker.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import dev.tasker.domain.Category
import dev.tasker.domain.DEFAULT_PRIORITY
import dev.tasker.domain.MAX_PRIORITY
import dev.tasker.domain.MIN_PRIORITY
import dev.tasker.domain.Task
import dev.tasker.domain.priorityColor

/** Create (task == null) or edit a task: title, notes, priority 1-10 and category (move). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskEditorScreen(
    categories: List<Category>,
    task: Task?,
    defaultCategoryId: Long,
    onSave: (categoryId: Long, title: String, notes: String, priority: Int) -> Unit,
    onDelete: () -> Unit,
    onBack: () -> Unit,
) {
    var title by rememberSaveable { mutableStateOf(task?.title ?: "") }
    var notes by rememberSaveable { mutableStateOf(task?.notes ?: "") }
    var priority by rememberSaveable { mutableFloatStateOf((task?.priority ?: DEFAULT_PRIORITY).toFloat()) }
    var categoryId by rememberSaveable {
        mutableLongStateOf(task?.categoryId ?: categories.firstOrNull { it.id == defaultCategoryId }?.id ?: categories.first().id)
    }
    var menuOpen by remember { mutableStateOf(false) }
    val category = categories.firstOrNull { it.id == categoryId } ?: categories.first()
    val level = priority.toInt()
    val levelColor = Color(priorityColor(level))

    Scaffold(
        containerColor = Surface0,
        topBar = {
            TopAppBar(
                title = { Text(if (task == null) "New task" else "Edit task") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, "Back") } },
                actions = { if (task != null) IconButton(onClick = onDelete) { Icon(Icons.Default.Delete, "Delete task") } },
            )
        },
    ) { padding ->
        Column(
            Modifier.padding(padding).padding(16.dp).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            OutlinedTextField(title, { title = it }, label = { Text("Title") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(notes, { notes = it }, label = { Text("Notes (optional)") }, minLines = 3, modifier = Modifier.fillMaxWidth())

            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Priority (10 = most urgent)", modifier = Modifier.weight(1f))
                    PriorityIndicator(level)
                }
                Slider(
                    value = priority, onValueChange = { priority = it },
                    valueRange = MIN_PRIORITY.toFloat()..MAX_PRIORITY.toFloat(),
                    steps = MAX_PRIORITY - MIN_PRIORITY - 1,
                    colors = SliderDefaults.colors(thumbColor = levelColor, activeTrackColor = levelColor),
                )
            }

            Column {
                Text("Category")
                OutlinedButton(onClick = { menuOpen = true }, modifier = Modifier.fillMaxWidth()) {
                    Text(category.name, modifier = Modifier.weight(1f))
                    Text("▾")
                }
                DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                    categories.forEach {
                        DropdownMenuItem(text = { Text(it.name) }, onClick = { categoryId = it.id; menuOpen = false })
                    }
                }
            }

            Button(
                onClick = { onSave(categoryId, title, notes, level) },
                enabled = title.isNotBlank(),
                modifier = Modifier.fillMaxWidth(),
            ) { Text("Save") }
        }
    }
}
