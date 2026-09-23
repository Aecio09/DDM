package com.aecio.todo.data

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import com.aecio.todo.db.Category
import com.aecio.todo.db.TodoDatabaseQueries
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow

class CategoryRepository(private val queries: TodoDatabaseQueries) {

    fun observeAll(): Flow<List<Category>> =
        queries.selectAllCategories().asFlow().mapToList(Dispatchers.Default)

    suspend fun getById(id: Long): Category? = queries.selectCategoryById(id).executeAsOneOrNull()

    suspend fun insert(name: String, color: Long?): Long {
        queries.insertCategory(name, color)
        return queries.selectLastCategoryId().executeAsOne()
    }

    suspend fun rename(id: Long, name: String, color: Long?) {
        queries.updateCategory(name, color, id)
    }

    suspend fun delete(id: Long) {
        queries.deleteCategory(id)
    }
}