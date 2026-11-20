package dev.tasker.ui

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue

private enum class Route { Home, Editor, Manage }

/** Tiny state-based navigation: Home, task editor (new/edit) and Manage categories. */
@Composable
fun TaskerApp(vm: MainViewModel) {
    val categories by vm.categories.collectAsState()
    val tasks by vm.tasks.collectAsState()
    var route by rememberSaveable { mutableStateOf(Route.Home) }
    var editingTaskId by rememberSaveable { mutableLongStateOf(0L) } // 0 = new task
    var selectedCategoryId by rememberSaveable { mutableLongStateOf(-1L) }

    val cats = categories ?: return // wait for the first load
    if (route != Route.Home) BackHandler { route = Route.Home }

    when (route) {
        Route.Home -> HomeScreen(
            categories = cats,
            tasks = tasks,
            selectedCategoryId = selectedCategoryId,
            onSelectCategory = { selectedCategoryId = it },
            onToggle = { vm.setCompleted(it.id, !it.completed) },
            onOpenTask = { editingTaskId = it.id; route = Route.Editor },
            onAddTask = { editingTaskId = 0L; route = Route.Editor },
            onManageCategories = { route = Route.Manage },
        )
        Route.Editor -> TaskEditorScreen(
            categories = cats,
            task = tasks.firstOrNull { it.id == editingTaskId },
            defaultCategoryId = selectedCategoryId,
            onSave = { categoryId, title, notes, priority ->
                if (editingTaskId == 0L) vm.addTask(categoryId, title, notes, priority)
                else vm.updateTask(editingTaskId, categoryId, title, notes, priority)
                selectedCategoryId = categoryId
                route = Route.Home
            },
            onDelete = { vm.deleteTask(editingTaskId); route = Route.Home },
            onBack = { route = Route.Home },
        )
        Route.Manage -> ManageCategoriesScreen(
            categories = cats,
            onAdd = vm::addCategory,
            onRename = vm::renameCategory,
            onDelete = vm::deleteCategory,
            onMove = vm::moveCategory,
            onBack = { route = Route.Home },
        )
    }
}
