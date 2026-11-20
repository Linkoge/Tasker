package dev.tasker.domain

import kotlinx.coroutines.flow.Flow

/** Persistence boundary. Implemented by Room in the app and by an in-memory fake in unit tests. */
interface TaskStore {
    fun observeCategories(): Flow<List<Category>>
    fun observeTasks(): Flow<List<Task>>
    suspend fun categories(): List<Category>
    suspend fun insertCategory(name: String, position: Int): Long
    suspend fun renameCategory(id: Long, name: String)
    suspend fun updatePositions(categories: List<Category>)
    suspend fun deleteCategory(id: Long)
    suspend fun insertTask(task: Task): Long
    suspend fun updateTask(task: Task)
    suspend fun task(id: Long): Task?
    suspend fun deleteTask(id: Long)
}
