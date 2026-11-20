package dev.tasker.ui

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable

@Composable
fun TaskerApp(vm: MainViewModel) {
    TaskerTheme {
        var managing by rememberSaveable { mutableStateOf(false) }
        BackHandler(enabled = managing) { managing = false }
        if (managing) ManageListsScreen(vm, onBack = { managing = false })
        else HomeScreen(vm, onManageLists = { managing = true })
    }
}
