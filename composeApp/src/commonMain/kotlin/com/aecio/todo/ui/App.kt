package com.aecio.todo.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import com.aecio.todo.ui.screens.CategoryScreen
import com.aecio.todo.ui.screens.TaskEditorScreen
import com.aecio.todo.ui.screens.TaskListScreen

@Composable
internal expect fun ProvidePlatformViewModelOwner(content: @Composable () -> Unit)

@Composable
fun App() {
    MaterialTheme(
        colorScheme = lightColorScheme(
            primary = androidx.compose.ui.graphics.Color(0xFF3F51B5),
            secondary = androidx.compose.ui.graphics.Color(0xFFFF5722),
            tertiary = androidx.compose.ui.graphics.Color(0xFF00796B),
        ),
    ) {
        ProvidePlatformViewModelOwner {
            val vm: TodoViewModel = viewModel()
            AppNavHost(vm)
        }
    }
}

@Composable
internal fun AppNavHost(vm: TodoViewModel) {
    val backStack = remember { mutableStateListOf<Screen>(Screen.TaskList) }
    val current = backStack.last()

    when (current) {
        Screen.TaskList -> TaskListScreen(
            vm = vm,
            onOpenTask = { backStack.add(Screen.TaskEditor(it)) },
            onManageCategories = { backStack.add(Screen.Categories) },
        )
        is Screen.TaskEditor -> TaskEditorScreen(
            vm = vm,
            taskId = current.taskId,
            onBack = { backStack.removeAt(backStack.lastIndex) },
        )
        Screen.Categories -> CategoryScreen(
            vm = vm,
            onBack = { backStack.removeAt(backStack.lastIndex) },
        )
    }
}

internal class SimpleViewModelStoreOwner : ViewModelStoreOwner {
    override val viewModelStore: ViewModelStore = ViewModelStore()
}