package dev.tasker

import dev.tasker.domain.*
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TaskLogicTest {
    private fun task(id: Long, priority: Int, created: Long, cat: Long = 1, done: Boolean = false, completedAt: Long? = null) =
        Task(id, cat, "t$id", "", priority, done, created, completedAt)

    private fun cats(vararg names: String) = names.mapIndexed { i, n -> Category(i + 1L, n, i) }

    @Test fun openTasksSortedByPriorityDescendingThenCreation() {
        val sorted = sortOpen(listOf(task(1, 3, 100), task(2, 10, 300), task(3, 10, 200), task(4, 1, 50), task(5, 3, 90)))
        assertEquals(listOf(3L, 2L, 5L, 1L, 4L), sorted.map { it.id })
    }

    @Test fun sortOpenExcludesCompleted() {
        val sorted = sortOpen(listOf(task(1, 5, 1), task(2, 9, 2, done = true, completedAt = 5)))
        assertEquals(listOf(1L), sorted.map { it.id })
    }

    @Test fun completedSortedMostRecentFirst() {
        val sorted = sortCompleted(listOf(task(1, 5, 1, done = true, completedAt = 10), task(2, 5, 2, done = true, completedAt = 30), task(3, 5, 3)))
        assertEquals(listOf(2L, 1L), sorted.map { it.id })
    }

    @Test fun openCountsPerCategoryIgnoreCompleted() {
        val c = cats("Daily", "Weekly", "Monthly")
        val counts = openCounts(c, listOf(task(1, 1, 1, 1), task(2, 1, 2, 1), task(3, 1, 3, 2), task(4, 1, 4, 1, done = true, completedAt = 1)))
        assertEquals(mapOf(1L to 2, 2L to 1, 3L to 0), counts)
    }

    @Test fun clampPriorityKeepsRange() {
        assertEquals(1, clampPriority(-4)); assertEquals(10, clampPriority(99)); assertEquals(7, clampPriority(7))
    }

    @Test fun priorityColorGoesFromCalmToRed() {
        val colors = (1..10).map { priorityColor(it) }
        assertEquals(10, colors.toSet().size)
        fun red(c: Int) = (c shr 16) and 0xFF
        fun green(c: Int) = (c shr 8) and 0xFF
        assertEquals(0xFF, colors.first() ushr 24)
        assertTrue("1 is calm (green/teal dominates)", green(colors.first()) > red(colors.first()))
        assertTrue("10 is red", red(colors.last()) > 200 && green(colors.last()) < 120)
        // Green channel decreases monotonically as urgency rises.
        assertEquals(colors.map { green(it) }.sortedDescending(), colors.map { green(it) })
    }

    @Test fun moveCategoryUpAndDown() {
        val c = cats("Daily", "Weekly", "Monthly")
        assertEquals(listOf("Weekly", "Daily", "Monthly"), moveCategory(c, 2, -1).map { it.name })
        assertEquals(listOf("Weekly", "Monthly", "Daily"), moveCategory(c, 1, 2).map { it.name })
        assertEquals(listOf(0, 1, 2), moveCategory(c, 3, -2).map { it.position })
    }

    @Test fun moveCategoryClampsAtEnds() {
        val c = cats("Daily", "Weekly")
        assertEquals(listOf("Daily", "Weekly"), moveCategory(c, 1, -1).map { it.name })
        assertEquals(listOf("Daily", "Weekly"), moveCategory(c, 2, 1).map { it.name })
        assertEquals(listOf("Daily", "Weekly"), moveCategory(c, 99, 1).map { it.name })
    }

    @Test fun dragToIndex() {
        val c = cats("A", "B", "C", "D")
        assertEquals(listOf("D", "A", "B", "C"), moveCategoryToIndex(c, 3, 0).map { it.name })
    }

    @Test fun changedPositionsOnlyReportsDifferences() {
        val c = cats("A", "B", "C")
        val moved = moveCategory(c, 1, 1) // A and B swap, C untouched
        assertEquals(setOf(1L, 2L), changedPositions(c, moved).map { it.id }.toSet())
    }

    @Test fun moveTaskChangesOnlyCategory() {
        val tasks = listOf(task(1, 7, 10, cat = 1), task(2, 2, 20, cat = 1))
        val moved = moveTask(tasks, 1, 3)
        assertEquals(3L, moved[0].categoryId)
        assertEquals(tasks[0].copy(categoryId = 3), moved[0])
        assertEquals(tasks[1], moved[1])
    }

    @Test fun georgianNamesSurvive() {
        val c = listOf(Category(1, "დღეს", 0), Category(2, "short", 1))
        assertEquals("დღეს", moveCategory(c, 1, 1).last().name)
    }
}
