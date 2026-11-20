package dev.tasker.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.tasker.domain.Category
import dev.tasker.domain.Task

/** Which task the editor dialog is showing: a new task, an existing one, or nothing. */
private sealed interface Editor {
    data object New : Editor
    data class Edit(val task: Task) : Editor
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(vm: MainViewModel, onManageLists: () -> Unit) {
    val categories by vm.categories.collectAsState(initial = emptyList())
    val counts by vm.openCounts.collectAsState(initial = emptyMap())
    var selectedId by rememberSaveable { mutableStateOf<Long?>(null) }
    val selected = categories.firstOrNull { it.id == selectedId } ?: categories.firstOrNull()
    var editor by remember { mutableStateOf<Editor?>(null) }
    var menuOpen by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Tasker") },
                actions = {
                    Box {
                        IconButton(onClick = { menuOpen = true }) { Icon(Icons.Filled.MoreVert, "Menu") }
                        DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                            DropdownMenuItem(
                                text = { Text("Manage lists") },
                                onClick = { menuOpen = false; onManageLists() },
                            )
                        }
                    }
                },
            )
        },
        floatingActionButton = {
            if (selected != null) {
                FloatingActionButton(onClick = { editor = Editor.New }) { Icon(Icons.Filled.Add, "Add task") }
            }
        },
    ) { padding ->
        Column(Modifier.padding(padding).fillMaxSize()) {
            if (selected == null) return@Column
            val selectedIndex = categories.indexOf(selected).coerceAtLeast(0)
            ScrollableTabRow(selectedTabIndex = selectedIndex, edgePadding = 8.dp) {
                categories.forEach { c ->
                    Tab(
                        selected = c.id == selected.id,
                        onClick = { selectedId = c.id },
                        text = { TabLabel(c.name, counts[c.id] ?: 0) },
                    )
                }
            }
            TaskList(
                vm = vm,
                category = selected,
                onOpen = { editor = Editor.Edit(it) },
            )
        }
    }

    when (val e = editor) {
        null -> {}
        Editor.New -> TaskDialog(
            initial = null,
            categories = categories,
            initialCategoryId = selected?.id ?: return,
            onDismiss = { editor = null },
            onSave = { title, notes, priority, catId ->
                vm.addTask(catId, title, notes, priority); editor = null
            },
            onDelete = null,
        )
        is Editor.Edit -> TaskDialog(
            initial = e.task,
            categories = categories,
            initialCategoryId = e.task.categoryId,
            onDismiss = { editor = null },
            onSave = { title, notes, priority, catId ->
                vm.updateTask(e.task.id, title, notes, priority, catId); editor = null
            },
            onDelete = { vm.deleteTask(e.task.id); editor = null },
        )
    }
}

@Composable
private fun TabLabel(name: String, count: Int) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(name, maxLines = 1)
        if (count > 0) {
            Spacer(Modifier.width(6.dp))
            Box(
                Modifier.clip(CircleShape).background(MaterialTheme.colorScheme.surfaceContainerHigh)
                    .padding(horizontal = 7.dp, vertical = 1.dp)
            ) { Text("$count", fontSize = 12.sp) }
        }
    }
}

@Composable
private fun TaskList(vm: MainViewModel, category: Category, onOpen: (Task) -> Unit) {
    val open by remember(category.id) { vm.openTasks(category.id) }.collectAsState(initial = emptyList())
    val done by remember(category.id) { vm.completedTasks(category.id) }.collectAsState(initial = emptyList())
    var showCompleted by rememberSaveable(category.id) { mutableStateOf(false) }
    val cardShape = RoundedCornerShape(20.dp)
    val cardColor = MaterialTheme.colorScheme.surfaceContainer

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(12.dp, 12.dp, 12.dp, 96.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item(key = "open") {
            Column(Modifier.fillMaxWidth().clip(cardShape).background(cardColor).padding(vertical = 8.dp)) {
                Text(
                    category.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp),
                )
                if (open.isEmpty()) {
                    Text(
                        "No tasks yet. Tap + to add one.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
                    )
                }
                open.forEach { t -> TaskRow(t, onToggle = { vm.setCompleted(t.id, true) }, onClick = { onOpen(t) }) }
            }
        }
        if (done.isNotEmpty()) {
            item(key = "done-header") {
                Column(Modifier.fillMaxWidth().clip(cardShape).background(cardColor)) {
                    Row(
                        Modifier.fillMaxWidth().clickable { showCompleted = !showCompleted }
                            .padding(horizontal = 20.dp, vertical = 18.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text("Completed (${done.size})", Modifier.weight(1f), style = MaterialTheme.typography.titleMedium)
                        Icon(
                            if (showCompleted) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown,
                            if (showCompleted) "Collapse" else "Expand",
                        )
                    }
                    if (showCompleted) {
                        done.forEach { t ->
                            TaskRow(t, onToggle = { vm.setCompleted(t.id, false) }, onClick = { onOpen(t) })
                        }
                        Spacer(Modifier.height(8.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun TaskRow(task: Task, onToggle: () -> Unit, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onClick).padding(start = 8.dp, end = 16.dp, top = 4.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Checkbox(checked = task.completed, onCheckedChange = { onToggle() })
        Column(Modifier.weight(1f).padding(vertical = 6.dp)) {
            Text(
                task.title,
                maxLines = 2, overflow = TextOverflow.Ellipsis,
                textDecoration = if (task.completed) TextDecoration.LineThrough else null,
                color = if (task.completed) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
            )
            if (task.notes.isNotBlank()) {
                Text(
                    task.notes, maxLines = 1, overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Spacer(Modifier.width(8.dp))
        PriorityIndicator(task.priority, dimmed = task.completed)
    }
}

/** Right-side priority: the number in a badge plus a bar whose color and height grow with urgency. */
@Composable
fun PriorityIndicator(priority: Int, dimmed: Boolean = false) {
    val color = urgencyColor(priority).copy(alpha = if (dimmed) 0.35f else 1f)
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            Modifier.size(28.dp).clip(CircleShape).background(color.copy(alpha = color.alpha * 0.25f)),
            contentAlignment = Alignment.Center,
        ) { Text("$priority", color = color, fontWeight = FontWeight.Bold, fontSize = 13.sp) }
        Spacer(Modifier.width(6.dp))
        Box(
            Modifier.width(6.dp).height(32.dp).clip(RoundedCornerShape(3.dp)).background(color.copy(alpha = 0.18f)),
            contentAlignment = Alignment.BottomCenter,
        ) {
            Box(
                Modifier.fillMaxWidth().fillMaxHeight(0.1f * priority.coerceIn(1, 10))
                    .clip(RoundedCornerShape(3.dp)).background(color)
            )
        }
    }
}
