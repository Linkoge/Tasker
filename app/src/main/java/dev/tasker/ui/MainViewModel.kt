package dev.tasker.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import dev.tasker.data.AppDatabase
import dev.tasker.data.RoomTaskStore
import dev.tasker.domain.Category
import dev.tasker.domain.Task
import dev.tasker.domain.TaskRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class MainViewModel(app: Application) : AndroidViewModel(app) {
    private val repo = TaskRepository(RoomTaskStore(AppDatabase.create(app)))

    /** null until the first database emission, so the UI does not flash an empty state. */
    val categories: StateFlow<List<Category>?> =
        repo.categories.stateIn(viewModelScope, SharingStarted.Eagerly, null)
    val tasks: StateFlow<List<Task>> =
        repo.tasks.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    fun addCategory(name: String) = viewModelScope.launch { repo.addCategory(name) }
    fun renameCategory(id: Long, name: String) = viewModelScope.launch { repo.renameCategory(id, name) }
    fun deleteCategory(id: Long) = viewModelScope.launch { repo.deleteCategory(id) }
    fun moveCategory(id: Long, delta: Int) = viewModelScope.launch { repo.moveCategory(id, delta) }

    fun addTask(categoryId: Long, title: String, notes: String, priority: Int) =
        viewModelScope.launch { repo.addTask(categoryId, title, notes, priority) }
    fun updateTask(id: Long, categoryId: Long, title: String, notes: String, priority: Int) =
        viewModelScope.launch { repo.updateTask(id, categoryId, title, notes, priority) }
    fun setCompleted(id: Long, completed: Boolean) = viewModelScope.launch { repo.setCompleted(id, completed) }
    fun deleteTask(id: Long) = viewModelScope.launch { repo.deleteTask(id) }
}
