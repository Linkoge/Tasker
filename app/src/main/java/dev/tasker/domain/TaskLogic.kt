package dev.tasker.domain

/** Pure, JVM-testable rules for ordering, reordering and moving. No Android dependencies. */

fun clampPriority(value: Int): Int = value.coerceIn(MIN_PRIORITY, MAX_PRIORITY)

/** Open tasks: highest priority first, ties broken by creation time (oldest first), then id. */
fun sortOpen(tasks: List<Task>): List<Task> =
    tasks.filter { !it.completed }
        .sortedWith(compareByDescending<Task> { it.priority }.thenBy { it.createdAt }.thenBy { it.id })

/** Completed tasks: most recently completed first. */
fun sortCompleted(tasks: List<Task>): List<Task> =
    tasks.filter { it.completed }
        .sortedWith(compareByDescending<Task> { it.completedAt ?: 0L }.thenByDescending { it.id })

/** Number of open (not completed) tasks per category id; categories without open tasks map to 0. */
fun openCounts(categories: List<Category>, tasks: List<Task>): Map<Long, Int> {
    val open = tasks.filter { !it.completed }.groupingBy { it.categoryId }.eachCount()
    return categories.associate { it.id to (open[it.id] ?: 0) }
}

/** Categories in tab order (by position, id as a stable tiebreaker). */
fun sortCategories(categories: List<Category>): List<Category> =
    categories.sortedWith(compareBy<Category> { it.position }.thenBy { it.id })

/** Rewrites positions to 0..n-1 following the list order. */
fun normalizePositions(ordered: List<Category>): List<Category> =
    ordered.mapIndexed { index, category -> category.copy(position = index) }

/** Moves the category with [id] by [delta] places (negative = up/left). Out-of-range moves clamp. */
fun moveCategory(categories: List<Category>, id: Long, delta: Int): List<Category> {
    val ordered = sortCategories(categories).toMutableList()
    val from = ordered.indexOfFirst { it.id == id }
    if (from < 0) return normalizePositions(ordered)
    val to = (from + delta).coerceIn(0, ordered.lastIndex)
    ordered.add(to, ordered.removeAt(from))
    return normalizePositions(ordered)
}

/** Reorders by moving the item at index [from] to index [to] (as a drag would). */
fun moveCategoryToIndex(categories: List<Category>, from: Int, to: Int): List<Category> {
    val ordered = sortCategories(categories).toMutableList()
    if (from !in ordered.indices) return normalizePositions(ordered)
    ordered.add(to.coerceIn(0, ordered.lastIndex), ordered.removeAt(from))
    return normalizePositions(ordered)
}

/** Only categories whose position actually changed, so persistence writes the minimum. */
fun changedPositions(before: List<Category>, after: List<Category>): List<Category> {
    val old = before.associate { it.id to it.position }
    return after.filter { old[it.id] != it.position }
}

/** Returns [tasks] with task [taskId] moved to [categoryId]; other fields (priority, state) are kept. */
fun moveTask(tasks: List<Task>, taskId: Long, categoryId: Long): List<Task> =
    tasks.map { if (it.id == taskId) it.copy(categoryId = categoryId) else it }

/**
 * Urgency colour (ARGB int) for a priority: calm teal at 1, through yellow/orange, to red at 10.
 * Interpolates hue in HSV from 170° down to 0°.
 */
fun priorityColor(priority: Int): Int {
    val p = clampPriority(priority)
    val t = (p - MIN_PRIORITY).toFloat() / (MAX_PRIORITY - MIN_PRIORITY)
    val hue = 170f * (1f - t)
    val saturation = 0.55f + 0.35f * t
    return hsvToArgb(hue, saturation, 0.95f)
}

internal fun hsvToArgb(hue: Float, s: Float, v: Float): Int {
    val c = v * s
    val h = (hue % 360f) / 60f
    val x = c * (1f - kotlin.math.abs(h % 2f - 1f))
    val (r1, g1, b1) = when {
        h < 1f -> Triple(c, x, 0f)
        h < 2f -> Triple(x, c, 0f)
        h < 3f -> Triple(0f, c, x)
        h < 4f -> Triple(0f, x, c)
        h < 5f -> Triple(x, 0f, c)
        else -> Triple(c, 0f, x)
    }
    val m = v - c
    fun channel(f: Float) = ((f + m) * 255f + 0.5f).toInt().coerceIn(0, 255)
    return (0xFF shl 24) or (channel(r1) shl 16) or (channel(g1) shl 8) or channel(b1)
}
