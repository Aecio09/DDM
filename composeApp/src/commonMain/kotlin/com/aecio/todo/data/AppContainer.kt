package com.aecio.todo.data

import app.cash.sqldelight.db.SqlDriver
import com.aecio.todo.db.TodoDatabase
import com.aecio.todo.notifications.NotificationScheduler

class AppContainer(private val driver: SqlDriver) {
    private val database = TodoDatabase(driver)

    val taskRepository = TaskRepository(database.todoDatabaseQueries)
    val categoryRepository = CategoryRepository(database.todoDatabaseQueries)
    val notifications = NotificationScheduler
}

object App {
    lateinit var container: AppContainer
        private set

    fun init(driver: SqlDriver) {
        if (!::container.isInitialized) {
            container = AppContainer(driver)
        }
    }
}