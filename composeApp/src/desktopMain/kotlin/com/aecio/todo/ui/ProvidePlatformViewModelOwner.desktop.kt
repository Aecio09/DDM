package com.aecio.todo.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner

@Composable
internal actual fun ProvidePlatformViewModelOwner(content: @Composable () -> Unit) {
    val owner = remember { SimpleViewModelStoreOwner() }
    CompositionLocalProvider(LocalViewModelStoreOwner provides owner) {
        content()
    }
}