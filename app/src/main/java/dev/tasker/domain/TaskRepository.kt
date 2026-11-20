package dev.tasker.domain

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class TaskRepository(private val store: TaskStore, private val clock: () -> Long = System::currentTimeMillis) {

    val categories: Flow<List<Category>> = store.observeCategories().map(TaskLogic::sortCategories)
    val openCounts: Flow<Map<Long, Int>> = store.observeOpenCounts()

    fun openTasks(categoryId: Long): Flow<List<Task>> = store.observeTasks(categoryId).map(TaskLogic::sortOpen)
    fun completedTasks(categoryId: Long): Flow<List<Task>> =
        store.observeTasks(categoryId).map(TaskLogic::sortCompleted)

    /** Inserts the default lists only when no list exists yet. */
    suspend fun seedDefaultsIfEmpty() {
        if (store.categories().isNotEmpty()) return
        TaskLogic.DEFAULT_CATEGORIES.forEachIndexed { i, name -> store.insertCategory(Category(0, name, i)) }
    }

    suspend fun addCategory(name: String): Long? {
        val clean = TaskLogic.normalizeTitle(name)
        if (clean.isEmpty()) return null
        return store.insertCategory(Category(0, clean, TaskLogic.nextPosition(store.categories())))
    }

    suspend fun renameCategory(id: Long, name: String) {
        val clean = TaskLogic.normalizeTitle(name)
        if (clean.isEmpty()) return
        val target = store.categories().firstOrNull { it.id == id } ?: return
        store.updateCategories(listOf(target.copy(name = clean)))
    }

    /** Deletes a list and its tasks. The last remaining list cannot be deleted. */
    suspend fun deleteCategory(id: Long): Boolean {
        val all = store.categories()
        if (all.size <= 1 || all.none { it.id == id }) return false
        store.deleteCategory(id)
        store.updateCategories(TaskLogic.sortCategories(all.filter { it.id != id })
            .mapIndexed { i, c -> c.copy(position = i) })
        return true
    }

    suspend fun moveCategory(id: Long, delta: Int) {
        val sorted = TaskLogic.sortCategories(store.categories())
        val index = sorted.indexOfFirst { it.id == id }
        if (index < 0) return
        store.updateCategories(TaskLogic.moveCategory(sorted, index, delta))
    }

    suspend fun addTask(categoryId: Long, title: String, notes: String, priority: Int): Long? {
        val clean = TaskLogic.normalizeTitle(title)
        if (clean.isEmpty()) return null
        return store.insertTask(
            Task(0, categoryId, clean, notes.trim(), TaskLogic.clampPriority(priority), false, clock(), null)
        )
    }

    /** Saves edits; when [categoryId] differs from the task's list, the task is moved there. */
    suspend fun updateTask(id: Long, title: String, notes: String, priority: Int, categoryId: Long) {
        val clean = TaskLogic.normalizeTitle(title)
        val task = store.task(id) ?: return
        if (clean.isEmpty()) return
        if (store.categories().none { it.id == categoryId }) return
        val edited = task.copy(title = clean, notes = notes.trim(), priority = TaskLogic.clampPriority(priority))
        store.updateTask(TaskLogic.moveTask(edited, categoryId))
    }

    suspend fun moveTask(id: Long, categoryId: Long) {
        val task = store.task(id) ?: return
        if (store.categories().none { it.id == categoryId }) return
        store.updateTask(TaskLogic.moveTask(task, categoryId))
    }

    suspend fun setCompleted(id: Long, completed: Boolean) {
        val task = store.task(id) ?: return
        store.updateTask(task.copy(completed = completed, completedAt = if (completed) clock() else null))
    }

    suspend fun deleteTask(id: Long) = store.deleteTask(id)
}
