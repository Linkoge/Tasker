package dev.tasker.domain

import kotlinx.coroutines.flow.Flow

/** Persistence port. Room implements it in the app; tests use an in-memory fake. */
interface TaskStore {
    fun observeCategories(): Flow<List<Category>>
    fun observeTasks(categoryId: Long): Flow<List<Task>>
    fun observeOpenCounts(): Flow<Map<Long, Int>>
    suspend fun categories(): List<Category>
    suspend fun task(id: Long): Task?
    suspend fun insertCategory(category: Category): Long
    suspend fun updateCategories(categories: List<Category>)
    suspend fun deleteCategory(id: Long)
    suspend fun insertTask(task: Task): Long
    suspend fun updateTask(task: Task)
    suspend fun deleteTask(id: Long)
}
