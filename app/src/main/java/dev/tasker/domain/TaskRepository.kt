package dev.tasker.domain

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** App rules on top of a [TaskStore]; all persistence goes through the store. */
class TaskRepository(private val store: TaskStore, private val clock: () -> Long = System::currentTimeMillis) {

    val categories: Flow<List<Category>> = store.observeCategories().map { sortCategories(it) }
    val tasks: Flow<List<Task>> = store.observeTasks()

    suspend fun addCategory(name: String): Long? {
        val clean = name.trim()
        if (clean.isEmpty()) return null
        val next = (store.categories().maxOfOrNull { it.position } ?: -1) + 1
        return store.insertCategory(clean, next)
    }

    suspend fun renameCategory(id: Long, name: String) {
        val clean = name.trim()
        if (clean.isNotEmpty()) store.renameCategory(id, clean)
    }

    /** Deletes the category and its tasks, then closes the position gap. The last category cannot be deleted. */
    suspend fun deleteCategory(id: Long): Boolean {
        val all = sortCategories(store.categories())
        if (all.size <= 1 || all.none { it.id == id }) return false
        store.deleteCategory(id)
        val remaining = all.filter { it.id != id }
        store.updatePositions(changedPositions(remaining, normalizePositions(remaining)))
        return true
    }

    /** Moves a category by [delta] places and persists the new order. */
    suspend fun moveCategory(id: Long, delta: Int) {
        val before = store.categories()
        val after = moveCategory(before, id, delta)
        store.updatePositions(changedPositions(before, after))
    }

    suspend fun addTask(categoryId: Long, title: String, notes: String, priority: Int): Long? {
        val clean = title.trim()
        if (clean.isEmpty()) return null
        return store.insertTask(
            Task(0, categoryId, clean, notes.trim(), clampPriority(priority), false, clock(), null)
        )
    }

    suspend fun updateTask(id: Long, categoryId: Long, title: String, notes: String, priority: Int) {
        val clean = title.trim()
        val existing = store.task(id) ?: return
        if (clean.isEmpty()) return
        store.updateTask(existing.copy(categoryId = categoryId, title = clean, notes = notes.trim(), priority = clampPriority(priority)))
    }

    suspend fun moveTask(id: Long, categoryId: Long) {
        val existing = store.task(id) ?: return
        if (existing.categoryId != categoryId) store.updateTask(existing.copy(categoryId = categoryId))
    }

    suspend fun setCompleted(id: Long, completed: Boolean) {
        val existing = store.task(id) ?: return
        store.updateTask(existing.copy(completed = completed, completedAt = if (completed) clock() else null))
    }

    suspend fun deleteTask(id: Long) = store.deleteTask(id)
}
