package com.aecio.todo.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aecio.todo.data.App
import com.aecio.todo.db.Category
import com.aecio.todo.db.Task
import com.aecio.todo.model.StatusFilter
import com.aecio.todo.model.dueAt
import com.aecio.todo.model.isCompleted
import com.aecio.todo.notifications.NotificationScheduler
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant

data class TaskDraft(
    val id: Long?,
    val title: String,
    val description: String,
    val dueAt: Instant?,
    val categoryId: Long?,
    val completed: Boolean,
)

class TodoViewModel : ViewModel() {

    private val container get() = App.container
    private val taskRepository get() = container.taskRepository
    private val categoryRepository get() = container.categoryRepository

    private val _tasks = MutableStateFlow<List<Task>>(emptyList())
    val tasks: StateFlow<List<Task>> = _tasks

    private val _categories = MutableStateFlow<List<Category>>(emptyList())
    val categories: StateFlow<List<Category>> = _categories

    private val _statusFilter = MutableStateFlow(StatusFilter.ALL)
    val statusFilter: StateFlow<StatusFilter> = _statusFilter

    private val _categoryFilter = MutableStateFlow<Long?>(null)
    val categoryFilter: StateFlow<Long?> = _categoryFilter

    val filteredTasks: StateFlow<List<Task>> =
        combine(_tasks, _statusFilter, _categoryFilter) { all, status, categoryId ->
            all.filter { task ->
                val byStatus = when (status) {
                    StatusFilter.ALL -> true
                    StatusFilter.PENDING -> !task.isCompleted
                    StatusFilter.COMPLETED -> task.isCompleted
                }
                val byCategory = categoryId == null || task.categoryId == categoryId
                byStatus && byCategory
            }
        }.stateIn(viewModelScope, kotlinx.coroutines.flow.SharingStarted.Lazily, emptyList())

    init {
        viewModelScope.launch { taskRepository.observeAll().collect { _tasks.value = it } }
        viewModelScope.launch { categoryRepository.observeAll().collect { _categories.value = it } }
    }

    fun setStatusFilter(filter: StatusFilter) {
        _statusFilter.value = filter
    }

    fun setCategoryFilter(categoryId: Long?) {
        _categoryFilter.value = categoryId
    }

    fun categoryById(id: Long?): Category? =
        _categories.value.firstOrNull { it.id == id }

    fun toggleTask(task: Task) {
        viewModelScope.launch {
            val nowCompleted = !task.isCompleted
            taskRepository.setCompleted(task.id, nowCompleted)
            if (nowCompleted) {
                if (task.notificationId != null) {
                    NotificationScheduler.cancel(task.id, task.notificationId)
                }
                if (task.notificationId != null) {
                    taskRepository.setNotificationId(task.id, null)
                }
            } else {
                val due = task.dueAt
                if (due != null && due > Clock.System.now()) {
                    val scheduled = NotificationScheduler.schedule(task.id, task.title, due)
                    if (scheduled != null) taskRepository.setNotificationId(task.id, scheduled)
                }
            }
        }
    }

    suspend fun createTask(draft: TaskDraft): String? {
        val clean = validate(draft)
        if (clean != null) return clean
        val now = Clock.System.now()
        val id = taskRepository.insert(
            title = draft.title.trim(),
            description = draft.description.trim().ifEmpty { null },
            completed = draft.completed,
            dueDateEpochMillis = draft.dueAt?.toEpochMilliseconds(),
            createdAtEpochMillis = now.toEpochMilliseconds(),
            categoryId = draft.categoryId,
            notificationId = null,
        )
        scheduleIfNeeded(newTaskId = id, draft = draft)
        return null
    }

    suspend fun updateTask(draft: TaskDraft): String? {
        val id = draft.id ?: return "Tarefa inválida"
        val clean = validate(draft)
        if (clean != null) return clean

        val old = taskRepository.getById(id)
        val due = draft.dueAt
        val willNotify = due != null && due > Clock.System.now() && !draft.completed
        val oldDue = old?.dueAt
        val oldNotif = old?.notificationId

        // Cancel notification when no longer needed or when due date changed.
        if (oldNotif != null && (oldDue != due || !willNotify)) {
            NotificationScheduler.cancel(id, oldNotif)
        }

        var newNotifId: Long? = if (draft.completed || !willNotify) null else oldNotif
        if (willNotify) {
            if (oldNotif == null || oldDue != due) {
                newNotifId = NotificationScheduler.schedule(id, draft.title.trim(), due!!)
            }
        }

        taskRepository.update(
            id = id,
            title = draft.title.trim(),
            description = draft.description.trim().ifEmpty { null },
            completed = draft.completed,
            dueDateEpochMillis = due?.toEpochMilliseconds(),
            categoryId = draft.categoryId,
            notificationId = newNotifId,
        )
        return null
    }

    fun deleteTask(task: Task) {
        viewModelScope.launch {
            if (task.notificationId != null) {
                NotificationScheduler.cancel(task.id, task.notificationId)
            }
            taskRepository.delete(task.id)
        }
    }

    private suspend fun scheduleIfNeeded(newTaskId: Long, draft: TaskDraft) {
        val due = draft.dueAt
        if (due != null && due > Clock.System.now() && !draft.completed) {
            val scheduled = NotificationScheduler.schedule(newTaskId, draft.title.trim(), due)
            if (scheduled != null) taskRepository.setNotificationId(newTaskId, scheduled)
        }
    }

    private fun validate(draft: TaskDraft): String? =
        if (draft.title.isBlank()) "O título é obrigatório" else null

    // Categories

    suspend fun createCategory(name: String, color: Long?): Long =
        categoryRepository.insert(name.trim(), color)

    suspend fun renameCategory(id: Long, name: String, color: Long?) {
        if (name.isNotBlank()) categoryRepository.rename(id, name.trim(), color)
    }

    fun deleteCategory(category: Category) {
        viewModelScope.launch {
            taskRepository.clearCategoryFor(category.id)
            categoryRepository.delete(category.id)
        }
    }
}