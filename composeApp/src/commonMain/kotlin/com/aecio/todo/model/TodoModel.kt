package com.aecio.todo.model

import com.aecio.todo.db.Task
import kotlinx.datetime.Instant

val Task.isCompleted: Boolean get() = completed != 0L
val Task.dueAt: Instant? get() = dueDateEpochMillis?.let { Instant.fromEpochMilliseconds(it) }
val Task.createdAt: Instant get() = Instant.fromEpochMilliseconds(createdAtEpochMillis)

enum class StatusFilter { ALL, PENDING, COMPLETED }