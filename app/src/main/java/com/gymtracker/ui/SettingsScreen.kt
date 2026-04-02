package com.gymtracker.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gymtracker.ui.GymBlue
import com.gymtracker.ui.GymGreen
import com.gymtracker.ui.GymPurple
import com.gymtracker.ui.GymRed
import com.gymtracker.ui.GymYellow
import com.gymtracker.viewmodel.PtCarrySource
import com.gymtracker.viewmodel.GymViewModel
import java.time.LocalDate
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
    val warningYellow = gymYellowForTheme()
    var showRestoreConfirm by remember { mutableStateOf(false) }
    var showBackupChoice by remember { mutableStateOf(false) }
    var showFromDatePicker by remember { mutableStateOf(false) }
    var showToDatePicker by remember { mutableStateOf(false) }
    var showPtMonthPicker by remember { mutableStateOf(false) }
    var pendingFromDate by remember { mutableStateOf("") }
    val selectedPtMonth = YearMonth.parse(state.ptEditorMonth)
    val canGoForward = selectedPtMonth.isBefore(YearMonth.now())
    val selectedEntry = state.ptSelectedMonthBreakdown
    val monthLabel = selectedPtMonth.month.getDisplayName(TextStyle.FULL, Locale.ENGLISH) + " " + selectedPtMonth.year
    var purchasedInput by remember(selectedEntry?.month, selectedEntry?.purchased) {
        mutableStateOf(
            when (val purchased = selectedEntry?.purchased ?: 0) {
                0 -> ""
                else -> purchased.toString()
            }
        )
    }
    var carryInput by remember(selectedEntry?.month, selectedEntry?.manualCarrySeed) {
        mutableStateOf(
            when (val carry = selectedEntry?.manualCarrySeed ?: 0) {
                0 -> ""
                else -> carry.toString()
            }
        )
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
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

        GymCard(icon = Icons.Default.CloudUpload, title = "Backup & Restore", iconTint = GymBlue) {
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
                    colors = ButtonDefaults.buttonColors(containerColor = GymBlue),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.Upload, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Backup Data", fontWeight = FontWeight.Bold)
                }
                Button(
                    onClick = { showRestoreConfirm = true },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = GymPurple),
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

        GymCard(icon = Icons.Default.Person, title = "PT Session Editor", iconTint = GymPurple) {
            Text(
                text = "Edit one month at a time. Set purchased sessions for the month and seed carried sessions when earlier history is missing.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                IconButton(
                    onClick = { vm.setSelectedPtEditorMonth(selectedPtMonth.minusMonths(1)) },
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(Icons.Default.ChevronLeft, contentDescription = "Previous month", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }

                TextButton(onClick = { showPtMonthPicker = true }) {
                    Text(monthLabel, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                    Spacer(Modifier.width(6.dp))
                    Icon(Icons.Default.CalendarMonth, contentDescription = "Pick month", modifier = Modifier.size(16.dp))
                }

                IconButton(
                    onClick = { vm.setSelectedPtEditorMonth(selectedPtMonth.plusMonths(1)) },
                    enabled = canGoForward,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        Icons.Default.ChevronRight,
                        contentDescription = "Next month",
                        tint = if (canGoForward) MaterialTheme.colorScheme.onSurfaceVariant
                        else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
                    )
                }
            }

            Spacer(Modifier.height(10.dp))

            if (selectedEntry != null) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = GymPurple.copy(alpha = 0.1f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        SummaryStat(
                            label = "Available",
                            value = selectedEntry.available,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.weight(1f)
                        )
                        SummaryStat(
                            label = "Used",
                            value = selectedEntry.used,
                            color = warningYellow,
                            modifier = Modifier.weight(1f)
                        )
                        SummaryStat(
                            label = "Left",
                            value = selectedEntry.carriedOut,
                            color = if (selectedEntry.carriedOut == 0) GymRed else GymGreen,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                Spacer(Modifier.height(12.dp))

                OutlinedTextField(
                    value = purchasedInput,
                    onValueChange = { if (it.all { c -> c.isDigit() } && it.length <= 3) purchasedInput = it },
                    label = { Text("Purchased this month") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(Modifier.height(12.dp))

                val carryFieldValue = if (selectedEntry.hasPreviousHistory) {
                    selectedEntry.carriedIn.toString()
                } else {
                    carryInput
                }
                OutlinedTextField(
                    value = carryFieldValue,
                    onValueChange = { if (it.all { c -> c.isDigit() } && it.length <= 3) carryInput = it },
                    label = { Text("Carried in") },
                    singleLine = true,
                    enabled = !selectedEntry.hasPreviousHistory,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(Modifier.height(6.dp))

                val carrySourceText = when (selectedEntry.carrySource) {
                    PtCarrySource.Manual -> "Manual starting carry"
                    PtCarrySource.Derived -> "Derived from earlier history"
                    PtCarrySource.None -> if (selectedEntry.hasPreviousHistory) "No carry from earlier history" else "No manual carry set"
                }
                Text(
                    text = carrySourceText,
                    style = MaterialTheme.typography.labelSmall,
                    color = if (selectedEntry.carrySource == PtCarrySource.Manual) GymPurple else MaterialTheme.colorScheme.onSurfaceVariant
                )

                if (selectedEntry.hasPreviousHistory) {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "Carry is locked for this month because earlier app history determines it.",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "Use this when you know the carry for a month but earlier records were never added to the app.",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                if (selectedEntry.manualCarryIgnored) {
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = "A saved manual carry exists for this month, but it is ignored because earlier history now takes over.",
                        style = MaterialTheme.typography.labelSmall,
                        color = GymRed
                    )
                }

                if (selectedEntry.overused > 0) {
                    Spacer(Modifier.height(10.dp))
                    Text(
                        text = "Overused by ${selectedEntry.overused} session" + if (selectedEntry.overused == 1) "" else "s",
                        style = MaterialTheme.typography.bodySmall,
                        color = GymRed
                    )
                }

                Spacer(Modifier.height(12.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Button(
                        onClick = {
                            vm.setPtPurchaseForMonth(selectedEntry.month, purchasedInput.toIntOrNull() ?: 0)
                            if (!selectedEntry.hasPreviousHistory) {
                                vm.setPtCarrySeedForMonth(selectedEntry.month, carryInput.toIntOrNull() ?: 0)
                            }
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = GymPurple),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Save Month", fontWeight = FontWeight.Bold)
                    }

                    if (selectedEntry.manualCarrySeed != null) {
                        OutlinedButton(
                            onClick = { vm.setPtCarrySeedForMonth(selectedEntry.month, 0) },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = GymRed),
                            border = BorderStroke(1.dp, GymRed.copy(alpha = 0.5f))
                        ) {
                            Text("Clear Carry")
                        }
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

            Surface(
                shape = RoundedCornerShape(10.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    SummaryStat(
                        label = "Total",
                        value = state.personalTrainingsPurchased,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f)
                    )
                    SummaryStat(
                        label = "Used",
                        value = state.personalTrainingsUsed,
                        color = warningYellow,
                        modifier = Modifier.weight(1f)
                    )
                    SummaryStat(
                        label = "Left Now",
                        value = state.ptRemainingThisMonth,
                        color = if (state.ptRemainingThisMonth == 0) GymRed else GymGreen,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            if (state.ptTotalOverused > 0) {
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "Overused by ${state.ptTotalOverused} session" + if (state.ptTotalOverused == 1) "" else "s" +
                        " across the visible history. Correct the affected month or seed an earlier carry month.",
                    style = MaterialTheme.typography.bodySmall,
                    color = GymRed
                )
            }
        }

        Spacer(Modifier.height(16.dp))
    }

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

    if (showPtMonthPicker) {
        GymDatePickerDialog(
            title = "Jump to month",
            maxDateMillis = System.currentTimeMillis(),
            onConfirm = { date ->
                vm.setSelectedPtEditorMonth(YearMonth.from(LocalDate.parse(date)))
                showPtMonthPicker = false
            },
            onDismiss = { showPtMonthPicker = false }
        )
    }
}

@Composable
private fun SummaryStat(
    label: String,
    value: Int,
    color: androidx.compose.ui.graphics.Color,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
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
