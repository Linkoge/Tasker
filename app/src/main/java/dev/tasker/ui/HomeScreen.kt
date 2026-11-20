package dev.tasker.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.tasker.domain.Category
import dev.tasker.domain.Task
import dev.tasker.domain.openCounts
import dev.tasker.domain.priorityColor
import dev.tasker.domain.sortCompleted
import dev.tasker.domain.sortOpen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    categories: List<Category>,
    tasks: List<Task>,
    selectedCategoryId: Long,
    onSelectCategory: (Long) -> Unit,
    onToggle: (Task) -> Unit,
    onOpenTask: (Task) -> Unit,
    onAddTask: () -> Unit,
    onManageCategories: () -> Unit,
) {
    val selected = categories.firstOrNull { it.id == selectedCategoryId } ?: categories.first()
    val selectedIndex = categories.indexOf(selected)
    val counts = openCounts(categories, tasks)
    val inCategory = tasks.filter { it.categoryId == selected.id }
    val open = sortOpen(inCategory)
    val completed = sortCompleted(inCategory)
    var completedExpanded by rememberSaveable { mutableStateOf(false) }
    var menuOpen by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Tasker") },
                actions = {
                    IconButton(onClick = { menuOpen = true }) { Icon(Icons.Default.MoreVert, "Menu") }
                    DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                        DropdownMenuItem(text = { Text("Manage categories") }, onClick = { menuOpen = false; onManageCategories() })
                    }
                },
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onAddTask, containerColor = MaterialTheme.colorScheme.primaryContainer) {
                Icon(Icons.Default.Add, "Add task")
            }
        },
    ) { padding ->
        Column(Modifier.padding(padding).fillMaxSize()) {
            ScrollableTabRow(selectedTabIndex = selectedIndex, edgePadding = 8.dp, containerColor = MaterialTheme.colorScheme.background) {
                categories.forEach { category ->
                    Tab(
                        selected = category.id == selected.id,
                        onClick = { onSelectCategory(category.id) },
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(category.name, maxLines = 1)
                                val count = counts[category.id] ?: 0
                                if (count > 0) {
                                    Box(
                                        Modifier.padding(start = 8.dp).clip(CircleShape).background(Color(0xFF3C3C40))
                                            .padding(horizontal = 8.dp, vertical = 2.dp),
                                    ) { Text(count.toString(), fontSize = 13.sp) }
                                }
                            }
                        },
                    )
                }
            }
            LazyColumn(Modifier.fillMaxSize(), contentPadding = androidx.compose.foundation.layout.PaddingValues(12.dp, 12.dp, 12.dp, 96.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                item {
                    SectionCard {
                        Text(selected.name, fontWeight = FontWeight.Bold, fontSize = 18.sp, modifier = Modifier.padding(16.dp))
                        if (open.isEmpty()) {
                            Text("No open tasks", color = Color.Gray, modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 16.dp))
                        }
                        open.forEach { TaskRow(it, onToggle, onOpenTask) }
                    }
                }
                item {
                    SectionCard {
                        Row(
                            Modifier.fillMaxWidth().clickable { completedExpanded = !completedExpanded }.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text("Completed (${completed.size})", fontSize = 18.sp, modifier = Modifier.weight(1f))
                            Icon(if (completedExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown, "Toggle completed")
                        }
                        if (completedExpanded) completed.forEach { TaskRow(it, onToggle, onOpenTask) }
                    }
                }
            }
        }
    }
}

@Composable
private fun SectionCard(content: @Composable () -> Unit) {
    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp)).background(Color(0xFF1F1F21))) { content() }
}

@Composable
private fun TaskRow(task: Task, onToggle: (Task) -> Unit, onOpen: (Task) -> Unit) {
    Row(
        Modifier.fillMaxWidth().clickable { onOpen(task) }.padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier.size(28.dp).clip(CircleShape).border(BorderStroke(2.dp, Color(0xFFB0B0B5)), CircleShape)
                .background(if (task.completed) MaterialTheme.colorScheme.primary else Color.Transparent)
                .clickable { onToggle(task) },
            contentAlignment = Alignment.Center,
        ) {
            if (task.completed) Icon(Icons.Default.Check, "Mark open", tint = Color.Black, modifier = Modifier.size(18.dp))
        }
        Column(Modifier.weight(1f).padding(horizontal = 16.dp)) {
            Text(
                task.title, fontSize = 18.sp, maxLines = 2, overflow = TextOverflow.Ellipsis,
                color = if (task.completed) Color.Gray else Color.Unspecified,
                textDecoration = if (task.completed) androidx.compose.ui.text.style.TextDecoration.LineThrough else null,
            )
            if (task.notes.isNotEmpty()) Text(task.notes, fontSize = 13.sp, color = Color.Gray, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        PriorityIndicator(task.priority)
    }
}

/** Right-side urgency indicator: the number plus a coloured vertical bar (calm teal -> red). */
@Composable
fun PriorityIndicator(priority: Int, modifier: Modifier = Modifier) {
    val color = Color(priorityColor(priority))
    Row(modifier, verticalAlignment = Alignment.CenterVertically) {
        Text(priority.toString(), color = color, fontWeight = FontWeight.Bold, fontSize = 18.sp)
        Box(Modifier.padding(start = 8.dp).width(6.dp).height(34.dp).clip(RoundedCornerShape(3.dp)).background(color))
    }
}
