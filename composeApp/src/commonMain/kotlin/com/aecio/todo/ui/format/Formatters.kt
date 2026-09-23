package com.aecio.todo.ui.format

import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

private fun pad(value: Int): String = value.toString().padStart(2, '0')

fun formatDueDateTime(instant: Instant): String {
    val ldt = instant.toLocalDateTime(TimeZone.currentSystemDefault())
    return "${pad(ldt.dayOfMonth)}/${pad(ldt.monthNumber)}/${ldt.year} ${pad(ldt.hour)}:${pad(ldt.minute)}"
}

fun formatDateOnly(instant: Instant): String {
    val ldt = instant.toLocalDateTime(TimeZone.currentSystemDefault())
    return "${pad(ldt.dayOfMonth)}/${pad(ldt.monthNumber)}/${ldt.year}"
}

fun formatTimeOnly(instant: Instant): String {
    val ldt = instant.toLocalDateTime(TimeZone.currentSystemDefault())
    return "${pad(ldt.hour)}:${pad(ldt.minute)}"
}

fun formatCurrentDuePreview(due: Instant?): String =
    due?.let { "Prazo: ${formatDueDateTime(it)}" } ?: "Sem prazo definido"