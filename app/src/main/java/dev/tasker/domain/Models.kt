package dev.tasker.domain

const val MIN_PRIORITY = 1
const val MAX_PRIORITY = 10
const val DEFAULT_PRIORITY = 5

data class Category(val id: Long, val name: String, val position: Int)

data class Task(
    val id: Long,
    val categoryId: Long,
    val title: String,
    val notes: String,
    val priority: Int,
    val completed: Boolean,
    val createdAt: Long,
    val completedAt: Long?,
)
