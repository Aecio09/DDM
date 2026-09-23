package com.aecio.todo.db

import android.content.Context
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.android.AndroidSqliteDriver

internal lateinit var appContext: Context

actual fun createDatabaseDriver(): SqlDriver =
    AndroidSqliteDriver(
        schema = TodoDatabase.Schema,
        context = appContext,
        name = "todo.db",
    ).apply {
        execute(null, "PRAGMA foreign_keys = ON", 0)
    }