package com.aecio.todo.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TimePickerDialog
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.aecio.todo.model.dueAt
import com.aecio.todo.model.isCompleted
import com.aecio.todo.ui.TaskDraft
import com.aecio.todo.ui.TodoViewModel
import com.aecio.todo.ui.format.formatDateOnly
import com.aecio.todo.ui.format.formatTimeOnly
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskEditorScreen(
    vm: TodoViewModel,
    taskId: Long?,
    onBack: () -> Unit,
) {
    val task = taskId?.let { id -> vm.tasks.value.firstOrNull { it.id == id } }
    val categories by vm.categories.collectAsState()

    val scope = rememberCoroutineScope()
    val snackbar = remember { SnackbarHostState() }

    var title by remember { mutableStateOf(task?.title ?: "") }
    var description by remember { mutableStateOf(task?.description ?: "") }
    var completed by remember { mutableStateOf(task?.isCompleted ?: false) }
    var selectedCategoryId by remember { mutableStateOf(task?.categoryId) }
    var dueAt by remember { mutableStateOf(task?.dueAt) }

    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }
    var showCategoryMenu by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var saving by remember { mutableStateOf(false) }

    val tz = TimeZone.currentSystemDefault()

    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = dateMillisAtUtcStartOfDay(dueAt),
        initialDisplayedMonthMillis = dateMillisAtUtcStartOfDay(dueAt),
    )
    val timePickerState = rememberTimePickerState(
        initialHour = dueAt?.toLocalDateTime(tz)?.hour ?: 12,
        initialMinute = dueAt?.toLocalDateTime(tz)?.minute ?: 0,
        is24Hour = true,
    )

    fun applySelectedDate() {
        val millis = datePickerState.selectedDateMillis ?: return
        val time = dueAt?.toLocalDateTime(tz) ?: LocalDateTime(1970, 1, 1, 12, 0)
        dueAt = combineDateAndTime(millis, time.hour, time.minute, tz)
        showDatePicker = false
    }

    fun applySelectedTime() {
        val now = Clock.System.now().toLocalDateTime(tz)
        val base = dueAt?.toLocalDateTime(tz)
            ?: LocalDateTime(now.year, now.monthNumber, now.dayOfMonth, 12, 0)
        dueAt = LocalDateTime(
            base.year, base.monthNumber, base.dayOfMonth,
            timePickerState.hour, timePickerState.minute,
        ).toInstant(tz)
        showTimePicker = false
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbar) },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                TextButton(onClick = onBack) {
                    Text("Cancelar")
                }
            }

            Text(
                text = if (task == null) "Nova tarefa" else "Editar tarefa",
                style = MaterialTheme.typography.headlineSmall,
            )

            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("Título *") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )

            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text("Descrição") },
                minLines = 3,
                maxLines = 6,
                modifier = Modifier.fillMaxWidth(),
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text("Concluída", style = MaterialTheme.typography.bodyLarge)
                Switch(checked = completed, onCheckedChange = { completed = it })
            }

            Text("Categoria", style = MaterialTheme.typography.bodyLarge)
            OutlinedButton(
                onClick = { showCategoryMenu = true },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(vm.categoryById(selectedCategoryId)?.name ?: "Sem categoria")
                DropdownMenu(
                    expanded = showCategoryMenu,
                    onDismissRequest = { showCategoryMenu = false },
                ) {
                    DropdownMenuItem(
                        text = { Text("Sem categoria") },
                        onClick = {
                            selectedCategoryId = null
                            showCategoryMenu = false
                        },
                    )
                    categories.forEach { category ->
                        DropdownMenuItem(
                            text = { Text(category.name) },
                            onClick = {
                                selectedCategoryId = category.id
                                showCategoryMenu = false
                            },
                        )
                    }
                }
            }

            Text("Prazo (opcional)", style = MaterialTheme.typography.bodyLarge)
            if (dueAt == null) {
                OutlinedButton(
                    onClick = { showDatePicker = true },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("Definir data e hora")
                }
            } else {
                Text(
                    "Data: ${formatDateOnly(dueAt!!)}   Hora: ${formatTimeOnly(dueAt!!)}",
                    style = MaterialTheme.typography.bodyLarge,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = { showDatePicker = true }) {
                        Text("Data")
                    }
                    OutlinedButton(onClick = { showTimePicker = true }) {
                        Text("Hora")
                    }
                    TextButton(onClick = { dueAt = null }) {
                        Text("Remover prazo")
                    }
                }
            }

            if (task != null) {
                OutlinedButton(
                    onClick = { showDeleteConfirm = true },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.error,
                    ),
                ) {
                    Icon(Icons.Default.Delete, contentDescription = null)
                    Spacer(Modifier.width(6.dp))
                    Text("Excluir tarefa")
                }
            }

            Button(
                onClick = {
                    if (!saving) {
                        saving = true
                        scope.launch {
                            val draft = TaskDraft(
                                id = taskId,
                                title = title,
                                description = description,
                                dueAt = dueAt,
                                categoryId = selectedCategoryId,
                                completed = completed,
                            )
                            val error = if (taskId == null) {
                                vm.createTask(draft)
                            } else {
                                vm.updateTask(draft)
                            }
                            saving = false
                            if (error != null) {
                                snackbar.showSnackbar(error)
                            } else {
                                onBack()
                            }
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(if (saving) "Salvando..." else "Salvar")
            }

            Spacer(Modifier.height(8.dp))
        }
    }

    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = ::applySelectedDate) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("Cancelar") }
            },
        ) {
            DatePicker(state = datePickerState)
        }
    }

    if (showTimePicker) {
        TimePickerDialog(
            onDismissRequest = { showTimePicker = false },
            title = { Text("Hora do prazo") },
            confirmButton = {
                TextButton(onClick = ::applySelectedTime) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showTimePicker = false }) { Text("Cancelar") }
            },
        ) {
            TimePicker(state = timePickerState)
        }
    }

    if (showDeleteConfirm && task != null) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Excluir tarefa") },
            text = { Text("Tem certeza que deseja excluir \"${task.title}\"?") },
            confirmButton = {
                TextButton(onClick = {
                    showDeleteConfirm = false
                    vm.deleteTask(task)
                    onBack()
                }) { Text("Excluir", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) { Text("Cancelar") }
            },
        )
    }
}

private fun dateMillisAtUtcStartOfDay(instant: Instant?): Long? {
    if (instant == null) return null
    val ldt = instant.toLocalDateTime(TimeZone.currentSystemDefault())
    return LocalDate(ldt.year, ldt.month, ldt.dayOfMonth)
        .atStartOfDayIn(TimeZone.UTC)
        .toEpochMilliseconds()
}

private fun combineDateAndTime(dateMillis: Long, hour: Int, minute: Int, tz: TimeZone): Instant {
    val date = Instant.fromEpochMilliseconds(dateMillis).toLocalDateTime(TimeZone.UTC)
    return LocalDateTime(date.year, date.month, date.dayOfMonth, hour, minute).toInstant(tz)
}