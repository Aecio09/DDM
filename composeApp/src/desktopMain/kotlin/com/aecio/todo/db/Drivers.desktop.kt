package com.aecio.todo.db

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import java.io.File

actual fun createDatabaseDriver(): SqlDriver {
    val dir = File(System.getProperty("user.home"), ".ddm-todo")
    dir.mkdirs()
    val dbFile = File(dir, "todo.db")
    val driver = JdbcSqliteDriver("jdbc:sqlite:${dbFile.absolutePath}")
    TodoDatabase.Schema.createIfNotExists(driver)
    driver.execute(null, "PRAGMA foreign_keys = ON", 0)
    return driver
}