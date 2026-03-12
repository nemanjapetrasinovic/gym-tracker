package com.gymtracker

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.ui.Modifier
import com.gymtracker.ui.DashboardScreen
import com.gymtracker.ui.GymTrackerTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            GymTrackerTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { padding ->
                    DashboardScreen(modifier = Modifier.padding(padding))
                }
            }
        }
    }
}
