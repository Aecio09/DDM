package com.aecio.todo.ui

import androidx.compose.runtime.Composable

@Composable
internal actual fun ProvidePlatformViewModelOwner(content: @Composable () -> Unit) {
    // On Android the ViewModelStoreOwner is provided by the ComponentActivity.
    content()
}