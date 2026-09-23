package com.aecio.todo.ui.theme

import androidx.compose.ui.graphics.Color

val CategoryPalette: List<Long> = listOf(
    0xFFEF5350L, // red
    0xFFEC407AL, // pink
    0xFFAB47BCL, // purple
    0xFF5C6BC0L, // indigo
    0xFF42A5F5L, // blue
    0xFF26C6DAL, // cyan
    0xFF26A69AL, // teal
    0xFF66BB6AL, // green
    0xFFFFA726L, // orange
    0xFFFF7043L, // deep orange
)

fun categoryColor(argb: Long?): Color =
    if (argb != null) Color(argb) else Color(0xFF757575)