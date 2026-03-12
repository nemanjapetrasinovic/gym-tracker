package com.gymtracker.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gymtracker.data.PtPurchase
import com.gymtracker.viewmodel.GymViewModel
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

@Composable
fun SettingsScreen(
    vm: GymViewModel,
    onBack: () -> Unit,
    onBackup: () -> Unit,
    onBackupRange: (String, String) -> Unit,
    onRestore: () -> Unit,
    modifier: Modifier = Modifier
) {
    val state by vm.uiState.collectAsStateWithLifecycle()
    var showRestoreConfirm by remember { mutableStateOf(false) }
    var editingPurchase by remember { mutableStateOf<PtPurchase?>(null) }
    var showBackupChoice by remember { mutableStateOf(false) }
    var showFromDatePicker by remember { mutableStateOf(false) }
    var showToDatePicker by remember { mutableStateOf(false) }
    var pendingFromDate by remember { mutableStateOf("") }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
        ) {
            IconButton(onClick = onBack, modifier = Modifier.size(36.dp)) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = MaterialTheme.colorScheme.onBackground
                )
            }
            Spacer(Modifier.width(8.dp))
            Text(
                text = "Settings",
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
            )
        }

        // Backup & Restore Card
        GymCard(icon = Icons.Default.CloudUpload, title = "Backup & Restore", iconTint = GymOrange) {
            Text(
                text = "Save your training data to Google Drive or local storage. You can restore it on any device.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Button(
                    onClick = { showBackupChoice = true },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = GymOrange),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.Upload, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Backup Data", fontWeight = FontWeight.Bold)
                }
                Button(
                    onClick = { showRestoreConfirm = true },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = GymBlue),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Restore Data", fontWeight = FontWeight.Bold)
                }
            }

            Spacer(Modifier.height(8.dp))

            val backupText = if (state.lastBackupTimestamp != null) {
                val date = java.time.Instant.ofEpochMilli(state.lastBackupTimestamp!!)
                    .atZone(java.time.ZoneId.systemDefault())
                    .toLocalDate()
                    .format(DateTimeFormatter.ofPattern("d MMMM yyyy", Locale.ENGLISH))
                "Last backup: $date"
            } else {
                "Never backed up"
            }
            Text(
                text = backupText,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // Personal Training Purchases Card
        GymCard(icon = Icons.Default.Person, title = "Personal Training Purchases", iconTint = GymBlue) {
            if (state.ptPurchases.isEmpty()) {
                Text(
                    text = "No purchases recorded yet. Add your monthly PT purchases to track usage.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            } else {
                // Monthly list
                state.ptPurchases.forEach { purchase ->
                    val ym = YearMonth.parse(purchase.month)
                    val label = "${ym.month.getDisplayName(TextStyle.FULL, Locale.ENGLISH)} ${ym.year}"
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 3.dp)
                            .clickable { editingPurchase = purchase }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = label,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.weight(1f)
                            )
                            Text(
                                text = "${purchase.count} sessions",
                                style = MaterialTheme.typography.bodyMedium,
                                color = GymBlue,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(Modifier.width(8.dp))
                            Icon(
                                Icons.Default.Edit,
                                contentDescription = "Edit",
                                modifier = Modifier.size(16.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                Spacer(Modifier.height(10.dp))

                // Summary
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = GymBlue.copy(alpha = 0.1f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        SummaryStat("Total", state.personalTrainingsPurchased, MaterialTheme.colorScheme.onSurface)
                        SummaryStat("Used", state.personalTrainingsUsed, GymYellow)
                        SummaryStat(
                            "Remaining",
                            state.personalTrainingsRemaining,
                            if (state.personalTrainingsRemaining == 0) GymRed else GymGreen
                        )
                    }
                }
            }

        }

        Spacer(Modifier.height(16.dp))
    }

    // Restore confirmation dialog
    if (showRestoreConfirm) {
        AlertDialog(
            onDismissRequest = { showRestoreConfirm = false },
            title = { Text("Restore Data") },
            text = { Text("This will replace all current data with the backup. This cannot be undone. Continue?") },
            confirmButton = {
                TextButton(onClick = {
                    showRestoreConfirm = false
                    onRestore()
                }) { Text("Restore", color = GymRed) }
            },
            dismissButton = {
                TextButton(onClick = { showRestoreConfirm = false }) { Text("Cancel") }
            }
        )
    }

    // Edit purchase dialog
    editingPurchase?.let { purchase ->
        EditPurchaseDialog(
            purchase = purchase,
            onConfirm = { count ->
                vm.setPtPurchaseForMonth(purchase.month, count)
                editingPurchase = null
            },
            onDismiss = { editingPurchase = null }
        )
    }

    // Backup choice dialog
    if (showBackupChoice) {
        AlertDialog(
            onDismissRequest = { showBackupChoice = false },
            title = { Text("Backup Data") },
            text = { Text("Choose what to backup:") },
            confirmButton = {
                TextButton(onClick = {
                    showBackupChoice = false
                    onBackup()
                }) { Text("All Data") }
            },
            dismissButton = {
                TextButton(onClick = {
                    showBackupChoice = false
                    showFromDatePicker = true
                }) { Text("Date Range") }
            }
        )
    }

    // From date picker
    if (showFromDatePicker) {
        GymDatePickerDialog(
            title = "Backup from date",
            onConfirm = { fromDate ->
                pendingFromDate = fromDate
                showFromDatePicker = false
                showToDatePicker = true
            },
            onDismiss = { showFromDatePicker = false }
        )
    }

    // To date picker
    if (showToDatePicker) {
        GymDatePickerDialog(
            title = "Backup to date",
            onConfirm = { toDate ->
                showToDatePicker = false
                onBackupRange(pendingFromDate, toDate)
            },
            onDismiss = { showToDatePicker = false }
        )
    }
}

@Composable
private fun SummaryStat(label: String, value: Int, color: androidx.compose.ui.graphics.Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = "$value",
            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
            color = color
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EditPurchaseDialog(
    purchase: PtPurchase,
    onConfirm: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    var count by remember { mutableStateOf(purchase.count.toString()) }
    val ym = YearMonth.parse(purchase.month)
    val label = "${ym.month.getDisplayName(TextStyle.FULL, Locale.ENGLISH)} ${ym.year}"

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit: $label") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    "Set to 0 to remove this month.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OutlinedTextField(
                    value = count,
                    onValueChange = { if (it.all { c -> c.isDigit() } && it.length <= 3) count = it },
                    label = { Text("Number of sessions") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(count.toIntOrNull() ?: 0) }) { Text("Save") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
