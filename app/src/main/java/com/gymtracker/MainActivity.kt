package com.gymtracker

import android.os.Bundle
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import com.gymtracker.ui.DashboardScreen
import com.gymtracker.ui.GymTrackerTheme
import com.gymtracker.ui.SettingsScreen
import com.gymtracker.viewmodel.GymViewModel
import com.gymtracker.viewmodel.Screen
import kotlinx.coroutines.launch
import java.time.LocalDate

class MainActivity : ComponentActivity() {

    private val vm: GymViewModel by viewModels()

    private var pendingBackupRange: Pair<String, String>? = null

    private val backupLauncher = registerForActivityResult(
        ActivityResultContracts.CreateDocument("text/csv")
    ) { uri ->
        uri ?: return@registerForActivityResult
        lifecycleScope.launch {
            try {
                val range = pendingBackupRange
                pendingBackupRange = null
                val csv = if (range != null) {
                    vm.createBackupCsv(range.first, range.second)
                } else {
                    vm.createBackupCsv()
                }
                contentResolver.openOutputStream(uri)?.use { it.write(csv.toByteArray()) }
                vm.setLastBackupTimestamp(System.currentTimeMillis())
                Toast.makeText(this@MainActivity, "Backup saved", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(this@MainActivity, "Backup failed: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    private val restoreLauncher = registerForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri ?: return@registerForActivityResult
        try {
            val csv = contentResolver.openInputStream(uri)?.bufferedReader()?.readText() ?: return@registerForActivityResult
            vm.restoreFromCsv(csv)
            Toast.makeText(this, "Data restored", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Toast.makeText(this, "Restore failed: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            GymTrackerTheme {
                val screen by vm.currentScreen.collectAsStateWithLifecycle()
                Scaffold(modifier = Modifier.fillMaxSize()) { padding ->
                    when (screen) {
                        Screen.Dashboard -> DashboardScreen(
                            modifier = Modifier.padding(padding),
                            vm = vm,
                            onOpenSettings = { vm.navigateTo(Screen.Settings) }
                        )
                        Screen.Settings -> {
                            BackHandler { vm.navigateTo(Screen.Dashboard) }
                            SettingsScreen(
                            vm = vm,
                            onBack = { vm.navigateTo(Screen.Dashboard) },
                            onBackup = {
                                pendingBackupRange = null
                                val date = LocalDate.now().toString()
                                backupLauncher.launch("gym-tracker-backup-$date.csv")
                            },
                            onBackupRange = { from, to ->
                                pendingBackupRange = from to to
                                backupLauncher.launch("gym-tracker-backup-$from-to-$to.csv")
                            },
                            onRestore = {
                                restoreLauncher.launch(arrayOf("text/*", "application/octet-stream"))
                            },
                            modifier = Modifier.padding(padding)
                        )
                        }
                    }
                }
            }
        }
    }
}
