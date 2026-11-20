package dev.tasker

import dev.tasker.domain.Category
import dev.tasker.domain.Task
import dev.tasker.domain.TaskLogic
import org.junit.Assert.assertEquals
import org.junit.Test

class TaskLogicTest {
    private fun task(id: Long, priority: Int, createdAt: Long = id, completed: Boolean = false, completedAt: Long? = null) =
        Task(id, 1, "t$id", "", priority, completed, createdAt, completedAt)

    @Test fun openTasksSortByPriorityHighestFirstThenOldest() {
        val sorted = TaskLogic.sortOpen(listOf(task(1, 3), task(2, 10), task(3, 7), task(4, 10, createdAt = 0)))
        assertEquals(listOf(4L, 2L, 3L, 1L), sorted.map { it.id })
    }

    @Test fun sortOpenExcludesCompletedAndSortCompletedIsNewestFirst() {
        val tasks = listOf(task(1, 9), task(2, 10, completed = true, completedAt = 5), task(3, 1, completed = true, completedAt = 9))
        assertEquals(listOf(1L), TaskLogic.sortOpen(tasks).map { it.id })
        assertEquals(listOf(3L, 2L), TaskLogic.sortCompleted(tasks).map { it.id })
    }

    @Test fun priorityIsClampedToOneThroughTen() {
        assertEquals(1, TaskLogic.clampPriority(0))
        assertEquals(1, TaskLogic.clampPriority(-5))
        assertEquals(1, TaskLogic.clampPriority(1))
        assertEquals(10, TaskLogic.clampPriority(10))
        assertEquals(10, TaskLogic.clampPriority(11))
        assertEquals(6, TaskLogic.clampPriority(6))
    }

    @Test fun urgencyGrowsWithPriority() {
        assertEquals(0f, TaskLogic.urgency(1), 0f)
        assertEquals(1f, TaskLogic.urgency(10), 0f)
        assertEquals(1f, TaskLogic.urgency(99), 0f)
        (1 until 10).forEach { assert(TaskLogic.urgency(it) < TaskLogic.urgency(it + 1)) }
    }

    private val cats = listOf(Category(1, "Daily", 0), Category(2, "Weekly", 1), Category(3, "Monthly", 2))

    @Test fun moveCategoryUpAndDownRenumbersPositions() {
        val down = TaskLogic.moveCategory(cats, 0, 1)
        assertEquals(listOf("Weekly", "Daily", "Monthly"), down.map { it.name })
        assertEquals(listOf(0, 1, 2), down.map { it.position })
        val up = TaskLogic.moveCategory(cats, 2, -1)
        assertEquals(listOf("Daily", "Monthly", "Weekly"), up.map { it.name })
    }

    @Test fun moveCategoryOutOfRangeIsNoOp() {
        assertEquals(cats, TaskLogic.moveCategory(cats, 0, -1))
        assertEquals(cats, TaskLogic.moveCategory(cats, 2, 1))
        assertEquals(cats, TaskLogic.moveCategory(cats, 7, 1))
    }

    @Test fun moveCategoryUsesSavedOrderNotListOrder() {
        val shuffled = listOf(Category(3, "Monthly", 2), Category(1, "Daily", 0), Category(2, "Weekly", 1))
        assertEquals(listOf("Daily", "Monthly", "Weekly"), TaskLogic.moveCategory(shuffled, 1, 1).map { it.name })
    }

    @Test fun moveTaskChangesOnlyCategory() {
        val t = task(1, 8)
        val moved = TaskLogic.moveTask(t, 3)
        assertEquals(3L, moved.categoryId)
        assertEquals(t.copy(categoryId = 3), moved)
    }
}
