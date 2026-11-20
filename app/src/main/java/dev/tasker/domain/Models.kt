package dev.tasker.domain

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
