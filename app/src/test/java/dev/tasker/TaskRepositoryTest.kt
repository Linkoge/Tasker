package dev.tasker

import dev.tasker.domain.Category
import dev.tasker.domain.Task
import dev.tasker.domain.TaskRepository
import dev.tasker.domain.TaskStore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test

/** In-memory TaskStore mirroring the Room behaviour (cascade delete of a list's tasks). */
class FakeStore : TaskStore {
    val cats = MutableStateFlow<List<Category>>(emptyList())
    val tasks = MutableStateFlow<List<Task>>(emptyList())
    private var nextId = 1L

    override fun observeCategories() = cats
    override fun observeTasks(categoryId: Long) = tasks.map { l -> l.filter { it.categoryId == categoryId } }
    override fun observeOpenCounts() = tasks.map { l -> l.filter { !it.completed }.groupingBy { it.categoryId }.eachCount() }
    override suspend fun categories() = cats.value
    override suspend fun task(id: Long) = tasks.value.firstOrNull { it.id == id }
    override suspend fun insertCategory(category: Category) = nextId++.also { cats.value += category.copy(id = it) }
    override suspend fun updateCategories(categories: List<Category>) {
        val byId = categories.associateBy { it.id }
        cats.value = cats.value.map { byId[it.id] ?: it }
    }
    override suspend fun deleteCategory(id: Long) {
        cats.value = cats.value.filter { it.id != id }
        tasks.value = tasks.value.filter { it.categoryId != id }
    }
    override suspend fun insertTask(task: Task) = nextId++.also { tasks.value += task.copy(id = it) }
    override suspend fun updateTask(task: Task) { tasks.value = tasks.value.map { if (it.id == task.id) task else it } }
    override suspend fun deleteTask(id: Long) { tasks.value = tasks.value.filter { it.id != id } }
}

class TaskRepositoryTest {
    private var now = 100L
    private val store = FakeStore()
    private val repo = TaskRepository(store) { now++ }

    private suspend fun names() = repo.categories.first().map { it.name }
    private suspend fun id(name: String) = repo.categories.first().first { it.name == name }.id

    @Test fun seedsDefaultsOnlyOnce() = runBlocking {
        repo.seedDefaultsIfEmpty()
        repo.seedDefaultsIfEmpty()
        assertEquals(listOf("Daily", "Weekly", "Monthly"), names())
    }

    @Test fun createRenameDeleteLists() = runBlocking {
        repo.seedDefaultsIfEmpty()
        assertNotNull(repo.addCategory("  Someday "))
        assertNull(repo.addCategory("   "))
        assertEquals(listOf("Daily", "Weekly", "Monthly", "Someday"), names())
        repo.renameCategory(id("Weekly"), "This week")
        assertEquals(listOf("Daily", "This week", "Monthly", "Someday"), names())
        repo.addTask(id("Monthly"), "pay rent", "", 5)
        assertTrue(repo.deleteCategory(id("Monthly")))
        assertEquals(listOf("Daily", "This week", "Someday"), names())
        assertEquals(listOf(0, 1, 2), repo.categories.first().map { it.position })
        assertTrue(store.tasks.value.isEmpty())
    }

    @Test fun lastListCannotBeDeleted() = runBlocking {
        val only = repo.addCategory("Only")!!
        assertFalse(repo.deleteCategory(only))
        assertEquals(listOf("Only"), names())
    }

    @Test fun reorderIsPersistedAndControlsTabOrder() = runBlocking {
        repo.seedDefaultsIfEmpty()
        repo.moveCategory(id("Monthly"), -1)
        assertEquals(listOf("Daily", "Monthly", "Weekly"), names())
        repo.moveCategory(id("Monthly"), -1)
        assertEquals(listOf("Monthly", "Daily", "Weekly"), names())
        repo.moveCategory(id("Monthly"), -1) // already first
        assertEquals(listOf("Monthly", "Daily", "Weekly"), names())
        // Order comes from stored positions, not insertion order.
        assertEquals(listOf("Monthly", "Daily", "Weekly"), store.cats.value.sortedBy { it.position }.map { it.name })
    }

    @Test fun tasksSortedByPriorityAndPriorityClamped() = runBlocking {
        repo.seedDefaultsIfEmpty()
        val daily = id("Daily")
        repo.addTask(daily, "low", "", -3)
        repo.addTask(daily, "high", "", 42)
        repo.addTask(daily, "mid", "note", 6)
        val open = repo.openTasks(daily).first()
        assertEquals(listOf("high", "mid", "low"), open.map { it.title })
        assertEquals(listOf(10, 6, 1), open.map { it.priority })
        assertNull(repo.addTask(daily, "  ", "", 5))
    }

    @Test fun completeMovesToCompletedAndBack() = runBlocking {
        repo.seedDefaultsIfEmpty()
        val daily = id("Daily")
        val t = repo.addTask(daily, "a", "", 5)!!
        repo.setCompleted(t, true)
        assertTrue(repo.openTasks(daily).first().isEmpty())
        assertEquals(listOf(t), repo.completedTasks(daily).first().map { it.id })
        assertEquals(0, repo.openCounts.first()[daily] ?: 0)
        repo.setCompleted(t, false)
        assertEquals(listOf(t), repo.openTasks(daily).first().map { it.id })
        assertNull(store.task(t)!!.completedAt)
    }

    @Test fun moveTaskBetweenListsViaEditAndDirectly() = runBlocking {
        repo.seedDefaultsIfEmpty()
        val daily = id("Daily"); val monthly = id("Monthly"); val weekly = id("Weekly")
        val t = repo.addTask(daily, "renew licence", "", 8)!!
        repo.updateTask(t, "renew licence!", "bring photo", 9, monthly)
        assertTrue(repo.openTasks(daily).first().isEmpty())
        val moved = repo.openTasks(monthly).first().single()
        assertEquals("renew licence!", moved.title)
        assertEquals("bring photo", moved.notes)
        assertEquals(9, moved.priority)
        repo.moveTask(t, weekly)
        assertEquals(weekly, store.task(t)!!.categoryId)
        repo.moveTask(t, 9999) // unknown list: ignored
        assertEquals(weekly, store.task(t)!!.categoryId)
    }

    @Test fun deleteTask() = runBlocking {
        repo.seedDefaultsIfEmpty()
        val t = repo.addTask(id("Daily"), "x", "", 5)!!
        repo.deleteTask(t)
        assertNull(store.task(t))
    }
}
