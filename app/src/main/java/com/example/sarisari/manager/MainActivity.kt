package com.example.sarisari.manager

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.example.sarisari.manager.core.designsystem.SariSariManagerTheme
import com.example.sarisari.manager.core.navigation.SariSariApp
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            SariSariManagerTheme {
                SariSariApp()
            }
        }
    }
}
