package com.aecio.todo

import androidx.compose.runtime.remember
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import com.aecio.todo.data.App
import com.aecio.todo.db.createDatabaseDriver
import com.aecio.todo.ui.App

fun main() = application {
    App.init(createDatabaseDriver())
    Window(
        onCloseRequest = ::exitApplication,
        title = "Todo App",
        state = rememberWindowState(width = 420.dp, height = 800.dp),
    ) {
        App()
    }
}