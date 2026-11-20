package dev.tasker.domain

/** Pure rules shared by the repository and UI; kept free of Android types so they are unit-testable. */
object TaskLogic {
    const val MIN_PRIORITY = 1
    const val MAX_PRIORITY = 10
    const val DEFAULT_PRIORITY = 5
    val DEFAULT_CATEGORIES = listOf("Daily", "Weekly", "Monthly")

    fun clampPriority(value: Int): Int = value.coerceIn(MIN_PRIORITY, MAX_PRIORITY)

    /** 0.0 for the least urgent priority, 1.0 for the most urgent; drives the indicator intensity. */
    fun urgency(priority: Int): Float =
        (clampPriority(priority) - MIN_PRIORITY).toFloat() / (MAX_PRIORITY - MIN_PRIORITY)

    /** Open tasks: highest priority first, older tasks first on ties. */
    fun sortOpen(tasks: List<Task>): List<Task> =
        tasks.filter { !it.completed }
            .sortedWith(compareByDescending<Task> { it.priority }.thenBy { it.createdAt }.thenBy { it.id })

    /** Completed tasks: most recently completed first. */
    fun sortCompleted(tasks: List<Task>): List<Task> =
        tasks.filter { it.completed }
            .sortedWith(compareByDescending<Task> { it.completedAt ?: 0L }.thenByDescending { it.id })

    fun sortCategories(categories: List<Category>): List<Category> =
        categories.sortedWith(compareBy<Category> { it.position }.thenBy { it.id })

    /**
     * Moves the category at [from] one step by [delta] (-1 = up/left, +1 = down/right) and
     * returns the list with positions renumbered 0..n-1. Out-of-range moves return the list unchanged
     * (but normalized).
     */
    fun moveCategory(categories: List<Category>, from: Int, delta: Int): List<Category> {
        val list = sortCategories(categories).toMutableList()
        val to = from + delta
        if (from in list.indices && to in list.indices) {
            val item = list.removeAt(from)
            list.add(to, item)
        }
        return list.mapIndexed { index, c -> c.copy(position = index) }
    }

    fun nextPosition(categories: List<Category>): Int = (categories.maxOfOrNull { it.position } ?: -1) + 1

    /** Returns the task reassigned to [targetCategoryId]; priority and completion state are kept. */
    fun moveTask(task: Task, targetCategoryId: Long): Task = task.copy(categoryId = targetCategoryId)

    fun normalizeTitle(title: String): String = title.trim()
}
