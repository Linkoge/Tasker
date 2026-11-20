package dev.tasker.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import dev.tasker.domain.Task
import dev.tasker.domain.TaskRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

class MainViewModel(private val repo: TaskRepository) : ViewModel() {
    val categories = repo.categories
    val openCounts = repo.openCounts

    init {
        viewModelScope.launch { repo.seedDefaultsIfEmpty() }
    }

    fun openTasks(categoryId: Long): Flow<List<Task>> = repo.openTasks(categoryId)
    fun completedTasks(categoryId: Long): Flow<List<Task>> = repo.completedTasks(categoryId)

    private fun run(block: suspend TaskRepository.() -> Unit) { viewModelScope.launch { repo.block() } }

    fun addCategory(name: String) = run { addCategory(name) }
    fun renameCategory(id: Long, name: String) = run { renameCategory(id, name) }
    fun deleteCategory(id: Long) = run { deleteCategory(id) }
    fun moveCategory(id: Long, delta: Int) = run { moveCategory(id, delta) }
    fun addTask(categoryId: Long, title: String, notes: String, priority: Int) =
        run { addTask(categoryId, title, notes, priority) }
    fun updateTask(id: Long, title: String, notes: String, priority: Int, categoryId: Long) =
        run { updateTask(id, title, notes, priority, categoryId) }
    fun setCompleted(id: Long, completed: Boolean) = run { setCompleted(id, completed) }
    fun deleteTask(id: Long) = run { deleteTask(id) }

    companion object {
        fun factory(repo: TaskRepository) = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T = MainViewModel(repo) as T
        }
    }
}
