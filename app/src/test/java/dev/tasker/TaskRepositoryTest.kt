package dev.tasker

import dev.tasker.domain.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** In-memory store emulating Room, including cascade delete, so persistence rules are testable on the JVM. */
class FakeStore : TaskStore {
    val cats = MutableStateFlow<List<Category>>(emptyList())
    val tasksFlow = MutableStateFlow<List<Task>>(emptyList())
    private var nextCat = 1L
    private var nextTask = 1L

    override fun observeCategories(): Flow<List<Category>> = cats
    override fun observeTasks(): Flow<List<Task>> = tasksFlow
    override suspend fun categories() = cats.value
    override suspend fun insertCategory(name: String, position: Int): Long {
        val id = nextCat++; cats.value = cats.value + Category(id, name, position); return id
    }
    override suspend fun renameCategory(id: Long, name: String) { cats.value = cats.value.map { if (it.id == id) it.copy(name = name) else it } }
    override suspend fun updatePositions(categories: List<Category>) {
        val byId = categories.associateBy { it.id }
        cats.value = cats.value.map { byId[it.id]?.let { n -> it.copy(position = n.position) } ?: it }
    }
    override suspend fun deleteCategory(id: Long) {
        cats.value = cats.value.filter { it.id != id }; tasksFlow.value = tasksFlow.value.filter { it.categoryId != id }
    }
    override suspend fun insertTask(task: Task): Long { val id = nextTask++; tasksFlow.value = tasksFlow.value + task.copy(id = id); return id }
    override suspend fun updateTask(task: Task) { tasksFlow.value = tasksFlow.value.map { if (it.id == task.id) task else it } }
    override suspend fun task(id: Long) = tasksFlow.value.firstOrNull { it.id == id }
    override suspend fun deleteTask(id: Long) { tasksFlow.value = tasksFlow.value.filter { it.id != id } }
}

class TaskRepositoryTest {
    private var now = 1000L
    private val store = FakeStore()
    private val repo = TaskRepository(store) { now++ }

    private fun names() = runBlocking { sortCategories(store.categories()).map { it.name } }

    private fun seed() = runBlocking {
        listOf("Daily", "Weekly", "Monthly").map { repo.addCategory(it)!! }
    }

    @Test fun newCategoriesAppendInOrder() { seed(); assertEquals(listOf("Daily", "Weekly", "Monthly"), names()) }

    @Test fun blankCategoryNameRejected() = runBlocking { assertNull(repo.addCategory("   ")); assertEquals(0, store.categories().size) }

    @Test fun unicodeCategoryNameRoundTrips() = runBlocking {
        val id = repo.addCategory("  დღეს+ ")!!
        assertEquals("დღეს+", store.categories().single { it.id == id }.name)
        repo.renameCategory(id, "კვირა")
        assertEquals("კვირა", store.categories().single { it.id == id }.name)
    }

    @Test fun reorderPersistsInStore() = runBlocking {
        val ids = seed()
        repo.moveCategory(ids[2], -2) // Monthly to the front
        assertEquals(listOf("Monthly", "Daily", "Weekly"), names())
    }

    @Test fun reorderSurvivesReloadThroughFlow() = runBlocking {
        val ids = seed()
        repo.moveCategory(ids[0], 1)
        val order = TaskRepository(store).categories.first().map { it.name }
        assertEquals(listOf("Weekly", "Daily", "Monthly"), order)
        assertEquals(listOf(0, 1, 2), store.categories().sortedBy { it.position }.map { it.position })
    }

    @Test fun newCategoryAfterReorderGoesLast() = runBlocking {
        val ids = seed()
        repo.moveCategory(ids[2], -2)
        repo.addCategory("Yearly")
        assertEquals(listOf("Monthly", "Daily", "Weekly", "Yearly"), names())
    }

    @Test fun deleteCategoryRemovesTasksAndClosesGap() = runBlocking {
        val ids = seed()
        repo.addTask(ids[1], "x", "", 5)
        assertTrue(repo.deleteCategory(ids[0]))
        assertEquals(listOf("Weekly", "Monthly"), names())
        assertEquals(listOf(0, 1), store.categories().map { it.position }.sorted())
        assertTrue(repo.deleteCategory(ids[1]))
        assertEquals(0, store.tasksFlow.value.size)
    }

    @Test fun lastCategoryCannotBeDeleted() = runBlocking {
        val ids = seed()
        repo.deleteCategory(ids[0]); repo.deleteCategory(ids[1])
        assertFalse(repo.deleteCategory(ids[2]))
        assertEquals(listOf("Monthly"), names())
    }

    @Test fun moveTaskBetweenCategoriesKeepsEverythingElse() = runBlocking {
        val ids = seed()
        val t = repo.addTask(ids[0], "Pay rent", "notes", 9)!!
        repo.setCompleted(t, true)
        val before = store.task(t)!!
        repo.moveTask(t, ids[2])
        val after = store.task(t)!!
        assertEquals(ids[2], after.categoryId)
        assertEquals(before.copy(categoryId = ids[2]), after)
        assertEquals(mapOf(ids[0] to 0, ids[1] to 0, ids[2] to 0), openCounts(store.categories(), store.tasksFlow.value))
    }

    @Test fun editorMoveViaUpdateTask() = runBlocking {
        val ids = seed()
        val t = repo.addTask(ids[0], "a", "", 3)!!
        repo.updateTask(t, ids[1], " b ", "n", 99)
        val saved = store.task(t)!!
        assertEquals(Triple(ids[1], "b", 10), Triple(saved.categoryId, saved.title, saved.priority))
    }

    @Test fun priorityClampedAndSortedEndToEnd() = runBlocking {
        val ids = seed()
        repo.addTask(ids[0], "low", "", 0); repo.addTask(ids[0], "high", "", 10); repo.addTask(ids[0], "mid", "", 5); repo.addTask(ids[0], "high2", "", 10)
        val open = sortOpen(store.tasksFlow.value.filter { it.categoryId == ids[0] })
        assertEquals(listOf("high", "high2", "mid", "low"), open.map { it.title })
        assertEquals(1, open.last().priority)
    }

    @Test fun completeAndUncomplete() = runBlocking {
        val ids = seed()
        val t = repo.addTask(ids[0], "a", "", 4)!!
        repo.setCompleted(t, true)
        assertTrue(store.task(t)!!.completed); assertEquals(true, store.task(t)!!.completedAt != null)
        repo.setCompleted(t, false)
        assertFalse(store.task(t)!!.completed); assertNull(store.task(t)!!.completedAt)
    }

    @Test fun blankTaskRejectedAndDelete() = runBlocking {
        val ids = seed()
        assertNull(repo.addTask(ids[0], "  ", "", 5))
        val t = repo.addTask(ids[0], "a", "", 5)!!
        repo.deleteTask(t)
        assertEquals(0, store.tasksFlow.value.size)
    }
}
