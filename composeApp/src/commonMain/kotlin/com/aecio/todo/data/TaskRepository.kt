package com.aecio.todo.data

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import com.aecio.todo.db.Task
import com.aecio.todo.db.TodoDatabaseQueries
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow

class TaskRepository(private val queries: TodoDatabaseQueries) {

    fun observeAll(): Flow<List<Task>> =
        queries.selectAllTasks().asFlow().mapToList(Dispatchers.Default)

    suspend fun getById(id: Long): Task? = queries.selectTaskById(id).executeAsOneOrNull()

    suspend fun insert(
        title: String,
        description: String?,
        completed: Boolean,
        dueDateEpochMillis: Long?,
        createdAtEpochMillis: Long,
        categoryId: Long?,
        notificationId: Long?,
    ): Long {
        queries.insertTask(
            title = title,
            description = description,
            completed = if (completed) 1 else 0,
            dueDateEpochMillis = dueDateEpochMillis,
            createdAtEpochMillis = createdAtEpochMillis,
            categoryId = categoryId,
            notificationId = notificationId,
        )
        return queries.selectLastTaskId().executeAsOne()
    }

    suspend fun update(
        id: Long,
        title: String,
        description: String?,
        completed: Boolean,
        dueDateEpochMillis: Long?,
        categoryId: Long?,
        notificationId: Long?,
    ) {
        queries.updateTask(
            title = title,
            description = description,
            completed = if (completed) 1 else 0,
            dueDateEpochMillis = dueDateEpochMillis,
            categoryId = categoryId,
            notificationId = notificationId,
            id = id,
        )
    }

    suspend fun setCompleted(id: Long, completed: Boolean) {
        queries.setTaskCompleted(if (completed) 1 else 0, id)
    }

    suspend fun setNotificationId(id: Long, notificationId: Long?) {
        queries.setTaskNotificationId(notificationId, id)
    }

    suspend fun clearCategoryFor(categoryId: Long) {
        queries.updateTasksCategoryToNull(categoryId)
    }

    suspend fun delete(id: Long) {
        queries.deleteTask(id)
    }
}