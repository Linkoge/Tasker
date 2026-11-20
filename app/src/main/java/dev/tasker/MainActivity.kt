package dev.tasker

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.lifecycle.viewmodel.compose.viewModel
import dev.tasker.data.AppDatabase
import dev.tasker.data.RoomTaskStore
import dev.tasker.domain.TaskRepository
import dev.tasker.ui.MainViewModel
import dev.tasker.ui.TaskerApp

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val repository = TaskRepository(RoomTaskStore(AppDatabase.get(this).dao()))
        setContent {
            val vm: MainViewModel = viewModel(factory = MainViewModel.factory(repository))
            TaskerApp(vm)
        }
    }
}
