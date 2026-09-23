package com.aecio.todo.ui

sealed interface Screen {
    data object TaskList : Screen
    data object Categories : Screen
    data class TaskEditor(val taskId: Long?) : Screen
}