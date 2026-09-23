package com.aecio.todo

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.aecio.todo.data.App
import com.aecio.todo.db.appContext
import com.aecio.todo.db.createDatabaseDriver
import com.aecio.todo.notifications.attachPermissionActivity
import com.aecio.todo.ui.App

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        appContext = applicationContext
        attachPermissionActivity(this)
        App.init(createDatabaseDriver())
        enableEdgeToEdge()
        setContent {
            App()
        }
    }
}