package com.gymtracker.ui

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.gymtracker.data.PtPurchase
import com.gymtracker.data.TrainingSession
import com.gymtracker.ui.GymBlue
import com.gymtracker.ui.GymGreen
import com.gymtracker.ui.GymPurple
import com.gymtracker.ui.GymRed
import com.gymtracker.ui.GymYellow
import com.gymtracker.ui.HeatmapPT
import com.gymtracker.ui.HeatmapRegular
import com.gymtracker.viewmodel.GymViewModel
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale
import kotlin.math.roundToInt
import kotlinx.coroutines.launch

@Composable
fun DashboardScreen(
    modifier: Modifier = Modifier,
    vm: GymViewModel = viewModel(),
    onOpenSettings: () -> Unit = {}
) {
    val state by vm.uiState.collectAsStateWithLifecycle()

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
            Icon(
                imageVector = Icons.Default.FitnessCenter,
                contentDescription = null,
                tint = GymBlue,
                modifier = Modifier.size(28.dp)
            )
            Spacer(Modifier.width(10.dp))
            Text(
                text = "Gym Tracker",
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
            )
            Spacer(Modifier.weight(1f))
            IconButton(onClick = onOpenSettings, modifier = Modifier.size(36.dp)) {
                Icon(
                    Icons.Default.Settings,
                    contentDescription = "Settings",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        MembershipCard(
            startDate = state.membershipStartDate,
            expiryDate = state.membershipExpiryDate,
            daysRemaining = state.membershipDaysRemaining,
            progressFraction = state.membershipProgressFraction,
            onSetDate = { vm.setMembershipStartDate(it) }
        )

        PersonalTrainingCard(
            available = state.ptAvailableThisMonth,
            used = state.ptUsedThisMonth,
            remaining = state.ptRemainingThisMonth,
            purchasedThisMonth = state.ptPurchasedThisMonth,
            carriedOver = state.ptCarriedOver,
            overused = state.ptOverusedThisMonth,
            onAddPurchased = { vm.addPtPurchase(it) },
            onSetPurchased = { vm.setPtPurchaseForMonth(YearMonth.now().toString(), it) }
        )

        CheckInCard(
            todaySession = state.todaySession,
            onCheckIn = { isPersonal -> vm.checkIn(isPersonal) },
            onRemoveSession = { vm.removeSession() }
        )

        ActivityHeatmap(
            sessions = state.allSessions,
            onLogSession = { date, isPt -> vm.checkInForDate(date, isPt) },
            onDeleteSession = { vm.removeSessionForDate(it) },
            onEditSession = { date, isPt -> vm.updateSessionType(date, isPt) }
        )

        SupportCard()

        Spacer(Modifier.height(16.dp))
    }
}

@Composable
fun MembershipCard(
    startDate: String?,
    expiryDate: String?,
    daysRemaining: Int?,
    progressFraction: Float,
    onSetDate: (String) -> Unit
) {
    var showDatePicker by remember { mutableStateOf(false) }

    val statusColor = when {
        daysRemaining == null -> MaterialTheme.colorScheme.onSurfaceVariant
        daysRemaining <= 5    -> GymRed
        daysRemaining <= 10   -> GymYellow
        else                  -> GymGreen
    }

    GymCard(icon = Icons.Default.CardMembership, title = "Membership", iconTint = GymBlue) {
        if (startDate == null) {
            EmptyStateRow(
                message = "No membership date set",
                actionLabel = "Set Payment Date",
                onClick = { showDatePicker = true }
            )
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Valid until",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = expiryDate ?: "",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = statusColor.copy(alpha = 0.15f)
                    ) {
                        Text(
                            text = if (daysRemaining == 0) "Expired" else "$daysRemaining days left",
                            color = statusColor,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                        )
                    }
                }

                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    LinearProgressIndicator(
                        progress = { progressFraction },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp)),
                        color = statusColor,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Paid: ${formatDisplayDate(startDate)}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "${(progressFraction * 100).toInt()}% elapsed",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                if (daysRemaining == 0) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = GymRed.copy(alpha = 0.12f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "Membership has expired. Set a new payment date to renew it.",
                            style = MaterialTheme.typography.bodySmall,
                            color = GymRed,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)
                        )
                    }

                    Button(
                        onClick = { showDatePicker = true },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = GymBlue),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Set New Payment Date", fontWeight = FontWeight.Bold)
                    }
                } else {
                    TextButton(
                        onClick = { showDatePicker = true },
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Change payment date", fontSize = 12.sp)
                    }
                }
            }
        }
    }

    if (showDatePicker) {
        GymDatePickerDialog(
            title = "When did you pay?",
            maxDateMillis = System.currentTimeMillis(),
            onConfirm = { date ->
                onSetDate(date)
                showDatePicker = false
            },
            onDismiss = { showDatePicker = false }
        )
    }
}

@Composable
fun PersonalTrainingCard(
    available: Int,
    used: Int,
    remaining: Int,
    purchasedThisMonth: Int,
    carriedOver: Int,
    overused: Int,
    onAddPurchased: (Int) -> Unit,
    onSetPurchased: (Int) -> Unit
) {
    var showAddDialog by remember { mutableStateOf(false) }
    var showEditDialog by remember { mutableStateOf(false) }
    val hasAnyPt = available > 0 || used > 0
    val remainingColor = if (remaining > 0) GymGreen else GymRed
    val remainingLabel = if (remaining == 1) "session left" else "sessions left"

    GymCard(
        icon = Icons.Default.Person,
        title = "Personal Trainings",
        iconTint = GymPurple,
        action = {
            IconButton(onClick = { showEditDialog = true }, modifier = Modifier.size(32.dp)) {
                Icon(
                    Icons.Default.Edit,
                    contentDescription = "Adjust month total",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    ) {
        if (!hasAnyPt) {
            EmptyStateRow(
                message = "No personal trainings added",
                actionLabel = "Add Sessions",
                onClick = { showAddDialog = true }
            )
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.Bottom
                ) {
                    Text(
                        text = remaining.toString(),
                        style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = remainingLabel,
                        style = MaterialTheme.typography.titleSmall,
                        color = remainingColor,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(bottom = 3.dp)
                    )
                }

                Text(
                    text = "Used $used of $available available this month",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Text(
                    text = "$purchasedThisMonth bought this month" +
                        if (carriedOver > 0) " + $carriedOver carried in" else "",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Button(
                        onClick = { showAddDialog = true },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = GymPurple),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Add Sessions", fontWeight = FontWeight.Bold)
                    }
                }

                if (overused > 0) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = GymRed.copy(alpha = 0.12f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "Overused by $overused session" + if (overused == 1) "" else "s" +
                                ". Adjust purchases for this month or an earlier one.",
                            style = MaterialTheme.typography.bodySmall,
                            color = GymRed,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)
                        )
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        AddPtSessionsDialog(
            onConfirm = { count ->
                onAddPurchased(count)
                showAddDialog = false
            },
            onDismiss = { showAddDialog = false }
        )
    }

    if (showEditDialog) {
        val currentMonth = YearMonth.now()
        val monthLabel = currentMonth.month.getDisplayName(java.time.format.TextStyle.FULL, java.util.Locale.ENGLISH) + " " + currentMonth.year
        EditCurrentMonthPtDialog(
            monthLabel = monthLabel,
            purchasedThisMonth = purchasedThisMonth,
            carriedOver = carriedOver,
            onConfirm = { count -> onSetPurchased(count); showEditDialog = false },
            onDismiss = { showEditDialog = false }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddPtSessionsDialog(
    onConfirm: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    var count by remember { mutableStateOf("0") }
    val countValue = count.toIntOrNull() ?: 0
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Sessions") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                PtCountPicker(
                    value = count,
                    onValueChange = { count = sanitizePtCountInput(it) },
                    label = "How many sessions did you buy?"
                )
                Text(
                    text = "Add 1 for a single session or any larger bundle.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(countValue) },
                enabled = countValue > 0
            ) { Text("Add") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
private fun PtCountPicker(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier
) {
    val parsedValue = value.toIntOrNull() ?: 0

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            FilledTonalIconButton(
                onClick = { onValueChange((parsedValue - 1).coerceAtLeast(0).toString()) },
                enabled = parsedValue > 0
            ) {
                Icon(Icons.Default.Remove, contentDescription = "Decrease session count")
            }

            OutlinedTextField(
                value = value,
                onValueChange = { onValueChange(sanitizePtCountInput(it)) },
                singleLine = true,
                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                    keyboardType = androidx.compose.ui.text.input.KeyboardType.Number
                ),
                modifier = Modifier.weight(1f)
            )

            FilledTonalIconButton(
                onClick = { onValueChange((parsedValue + 1).coerceAtMost(999).toString()) }
            ) {
                Icon(Icons.Default.Add, contentDescription = "Increase session count")
            }
        }
    }
}

private fun sanitizePtCountInput(input: String): String {
    if (input.isEmpty()) return ""

    return if (input.all { it.isDigit() } && input.length <= 3) {
        input
    } else {
        input.filter { it.isDigit() }.take(3)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EditCurrentMonthPtDialog(
    monthLabel: String,
    purchasedThisMonth: Int,
    carriedOver: Int,
    onConfirm: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    var count by remember { mutableStateOf(purchasedThisMonth.toString()) }
    val countValue = count.toIntOrNull() ?: 0
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(monthLabel) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                if (carriedOver > 0) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, tint = GymPurple, modifier = Modifier.size(14.dp))
                        Text("+$carriedOver carried over from last month", style = MaterialTheme.typography.bodySmall, color = GymPurple)
                    }
                }
                PtCountPicker(
                    value = count,
                    onValueChange = { count = sanitizePtCountInput(it) },
                    label = "Sessions purchased this month"
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(countValue) }) { Text("Save") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
fun CheckInCard(
    todaySession: TrainingSession?,
    onCheckIn: (Boolean) -> Unit,
    onRemoveSession: () -> Unit
) {
    val todayFormatter = remember { DateTimeFormatter.ofPattern("EEEE, d MMMM yyyy", Locale.ENGLISH) }
    val todayLabel = LocalDate.now().format(todayFormatter)
    val alreadyCheckedIn = todaySession != null

    GymCard(icon = Icons.Default.EditCalendar, title = "Log Training", iconTint = GymGreen) {
        Text(
            text = todayLabel,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.Medium
        )

        Spacer(Modifier.height(6.dp))

        Text(
            text = "To log or edit past dates, use the heatmap or calendar below.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(Modifier.height(8.dp))

        if (alreadyCheckedIn) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(GymGreen.copy(alpha = 0.12f))
                    .padding(14.dp)
            ) {
                Text("\u2705", fontSize = 20.sp)
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (todaySession!!.isPersonalTraining) "Personal Training Done!" else "Training Done!",
                        fontWeight = FontWeight.Bold,
                        color = GymGreen
                    )
                    Text(
                        text = if (todaySession.isPersonalTraining) "Today's personal training session is logged" else "Today's regular training session is logged",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(Modifier.height(10.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                val switchLabel = if (todaySession!!.isPersonalTraining) "Change to Regular" else "Change to PT"
                OutlinedButton(
                    onClick = { onCheckIn(!todaySession.isPersonalTraining) },
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp)
                ) {
                    Text(switchLabel, fontSize = 12.sp)
                }
                OutlinedButton(
                    onClick = onRemoveSession,
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = GymRed),
                    border = BorderStroke(1.dp, GymRed.copy(alpha = 0.5f))
                ) {
                    Text("Undo Check-in", fontSize = 12.sp)
                }
            }
        } else {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Button(
                    onClick = { onCheckIn(false) },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = GymBlue),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.FitnessCenter, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Regular", fontWeight = FontWeight.Bold)
                }
                Button(
                    onClick = { onCheckIn(true) },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = GymPurple),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.Person, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Personal", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun ActivityHeatmap(
    sessions: List<TrainingSession>,
    onLogSession: (String, Boolean) -> Unit,
    onDeleteSession: (String) -> Unit,
    onEditSession: (String, Boolean) -> Unit
) {
    val sessionMap = remember(sessions) { sessions.associateBy { it.date } }
    var showCalendar by remember { mutableStateOf(false) }
    var calendarMonth by remember { mutableStateOf(YearMonth.now()) }
    var pendingDate by remember { mutableStateOf<LocalDate?>(null) }

    val today = LocalDate.now()
    val weeksToShow = 16
    val fallbackStartDate = remember(today) {
        today.minusWeeks(weeksToShow.toLong()).let { d ->
            d.minusDays(((d.dayOfWeek.value - 1) % 7).toLong())
        }
    }
    val earliestSessionDate = remember(sessions) {
        sessions.minOfOrNull { LocalDate.parse(it.date) }
    }
    val startDate = remember(earliestSessionDate, fallbackStartDate) {
        val earliestAlignedWeek = earliestSessionDate?.let { firstSession ->
            firstSession.minusDays(((firstSession.dayOfWeek.value - 1) % 7).toLong())
        }
        if (earliestAlignedWeek != null && earliestAlignedWeek.isBefore(fallbackStartDate)) {
            earliestAlignedWeek
        } else {
            fallbackStartDate
        }
    }
    val endDate = remember(today) {
        today.plusDays((7 - today.dayOfWeek.value).toLong())
    }

    val allDays = remember(startDate, endDate) {
        buildList {
            var d = startDate
            while (!d.isAfter(endDate)) {
                add(d)
                d = d.plusDays(1)
            }
        }
    }

    val weeks = remember(allDays) { allDays.chunked(7) }

    val monthLabels = remember(weeks) {
        buildMap {
            var lastMonth = -1
            weeks.forEachIndexed { idx, week ->
                val month = week.first().monthValue
                if (month != lastMonth) {
                    put(idx, week.first().month.getDisplayName(TextStyle.SHORT, Locale.ENGLISH))
                    lastMonth = month
                }
            }
        }
    }
    val dayLabels = listOf("Mon", "", "Wed", "", "Fri", "", "Sun")
    val heatmapScrollState = rememberScrollState()

    LaunchedEffect(weeks.size) {
        if (weeks.size > weeksToShow) {
            repeat(2) { withFrameNanos { } }
            heatmapScrollState.scrollTo(heatmapScrollState.maxValue)
        } else if (heatmapScrollState.value != 0) {
            heatmapScrollState.scrollTo(0)
        }
    }

    GymCard(icon = Icons.Default.CalendarMonth, title = "Activity", iconTint = GymYellow) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Spacer(Modifier.weight(1f))
            LegendItem(color = HeatmapRegular, label = "Regular")
            LegendItem(color = HeatmapPT, label = "Personal Training")
            Spacer(Modifier.weight(1f))
        }

        val dayLabelWidth = 26.dp
        val labelGap = 4.dp
        val spacing = 2.dp
        val viewportWeekCount = weeksToShow
        val totalWeekCount = weeks.size
        BoxWithConstraints(modifier = Modifier.fillMaxWidth().padding(end = labelGap + spacing)) {
            val availableWidthDp = maxWidth - dayLabelWidth - labelGap
            val viewportSpacing = spacing * (viewportWeekCount - 1)
            val cellSize = ((availableWidthDp - viewportSpacing) / viewportWeekCount)
            val contentWidth = cellSize * totalWeekCount + spacing * (totalWeekCount - 1).coerceAtLeast(0)

            Row(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(end = labelGap)) {
                    Text(
                        text = "",
                        fontSize = 9.sp,
                        lineHeight = 9.sp,
                        modifier = Modifier.width(dayLabelWidth)
                    )
                    Spacer(Modifier.height(spacing))
                    Column(verticalArrangement = Arrangement.spacedBy(spacing)) {
                        dayLabels.forEach { label ->
                            Box(
                                modifier = Modifier
                                    .width(dayLabelWidth)
                                    .height(cellSize),
                                contentAlignment = Alignment.CenterEnd
                            ) {
                                Text(
                                    text = label,
                                    fontSize = 9.sp,
                                    lineHeight = 9.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = TextAlign.End
                                )
                            }
                        }
                    }
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .horizontalScroll(heatmapScrollState)
                ) {
                    Column(modifier = Modifier.width(contentWidth)) {
                        Row(
                            modifier = Modifier.width(contentWidth),
                            horizontalArrangement = Arrangement.spacedBy(spacing)
                        ) {
                            weeks.forEachIndexed { idx, _ ->
                                val label = monthLabels[idx] ?: ""
                                Box(
                                    modifier = Modifier
                                        .width(cellSize)
                                        .graphicsLayer(clip = false)
                                ) {
                                    Text(
                                        text = label,
                                        fontSize = 9.sp,
                                        lineHeight = 9.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        textAlign = TextAlign.Start,
                                        softWrap = false,
                                        overflow = TextOverflow.Visible
                                    )
                                }
                            }
                        }
                        Spacer(Modifier.height(spacing))
                        Row(
                            modifier = Modifier.width(contentWidth),
                            horizontalArrangement = Arrangement.spacedBy(spacing)
                        ) {
                            weeks.forEach { week ->
                                Column(
                                    verticalArrangement = Arrangement.spacedBy(spacing)
                                ) {
                                    repeat(7) { dayOfWeek ->
                                        val date = week.getOrNull(dayOfWeek)
                                        val session = date?.let { sessionMap[it.toString()] }
                                        val isFuture = date?.isAfter(today) ?: true
                                        val emptyColor = MaterialTheme.colorScheme.surfaceVariant
                                        val cellColor = when {
                                            date == null -> Color.Transparent
                                            isFuture -> emptyColor.copy(alpha = 0.4f)
                                            session?.isPersonalTraining == true -> HeatmapPT
                                            session != null -> HeatmapRegular
                                            else -> emptyColor
                                        }
                                        Box(
                                            modifier = Modifier
                                                .size(cellSize)
                                                .clip(RoundedCornerShape(2.dp))
                                                .background(cellColor)
                                                .then(
                                                    if (date != null && !isFuture) {
                                                        Modifier.clickable { pendingDate = date }
                                                    } else Modifier
                                                )
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        val totalTrainings = sessions.size
        val ptCount = sessions.count { it.isPersonalTraining }
        val regularCount = totalTrainings - ptCount

        Spacer(Modifier.height(10.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            MiniStat("Total", "$totalTrainings", MaterialTheme.colorScheme.onSurface, Modifier.weight(1f))
            MiniStat("Regular", "$regularCount", HeatmapRegular, Modifier.weight(1f))
            MiniStat("PT", "$ptCount", HeatmapPT, Modifier.weight(1f))
        }

        Spacer(Modifier.height(10.dp))
        TextButton(
            onClick = { showCalendar = !showCalendar },
            contentPadding = PaddingValues(0.dp),
            modifier = Modifier.align(Alignment.CenterHorizontally)
        ) {
            Icon(
                if (showCalendar) Icons.Default.ExpandLess else Icons.Default.CalendarMonth,
                contentDescription = null,
                modifier = Modifier.size(16.dp)
            )
            Spacer(Modifier.width(4.dp))
            Text(
                if (showCalendar) "Hide calendar" else "View calendar",
                fontSize = 12.sp
            )
        }

        AnimatedVisibility(visible = showCalendar) {
            TrainingCalendar(
                yearMonth = calendarMonth,
                sessionMap = sessionMap,
                onMonthChange = { calendarMonth = it },
                onLogSession = onLogSession,
                onDeleteSession = onDeleteSession,
                onEditSession = onEditSession
            )
        }
    }

    pendingDate?.let { date ->
        TrainingSessionActionDialog(
            date = date,
            session = sessionMap[date.toString()],
            onLogSession = onLogSession,
            onDeleteSession = onDeleteSession,
            onEditSession = onEditSession,
            onDismiss = { pendingDate = null }
        )
    }
}

@Composable
fun LegendItem(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(color)
        )
        Spacer(Modifier.width(5.dp))
        Text(label, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
fun MiniStat(label: String, value: String, color: Color, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(value, fontWeight = FontWeight.Bold, color = color, fontSize = 16.sp)
        Text(label, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
fun TrainingCalendar(
    yearMonth: YearMonth,
    sessionMap: Map<String, TrainingSession>,
    onMonthChange: (YearMonth) -> Unit,
    onLogSession: (String, Boolean) -> Unit,
    onDeleteSession: (String) -> Unit,
    onEditSession: (String, Boolean) -> Unit
) {
    val today = LocalDate.now()
    val currentMonth = remember { YearMonth.now() }
    val monthFormatter = remember { DateTimeFormatter.ofPattern("MMMM yyyy", Locale.ENGLISH) }
    var pendingDate by remember { mutableStateOf<LocalDate?>(null) }
    var visibleMonth by remember { mutableStateOf(yearMonth) }
    val scope = rememberCoroutineScope()
    val animatedOffsetPx = remember(yearMonth) { Animatable(0f) }
    var dragOffsetPx by remember(yearMonth) { mutableFloatStateOf(0f) }
    var isDragging by remember { mutableStateOf(false) }
    var isAnimating by remember { mutableStateOf(false) }
    var calendarWidthPx by remember { mutableFloatStateOf(0f) }
    val canGoForward = visibleMonth.isBefore(currentMonth)

    LaunchedEffect(yearMonth) {
        visibleMonth = yearMonth
    }

    fun animateMonthChange(
        targetMonth: YearMonth,
        targetOffsetPx: Float,
        initialOffsetPx: Float = 0f
    ) {
        if (isAnimating || targetMonth == yearMonth || targetMonth.isAfter(currentMonth)) return
        visibleMonth = targetMonth
        isAnimating = true
        scope.launch {
            animatedOffsetPx.stop()
            isDragging = false
            animatedOffsetPx.snapTo(initialOffsetPx)
            animatedOffsetPx.animateTo(
                targetValue = targetOffsetPx,
                animationSpec = tween(durationMillis = 220)
            )
            onMonthChange(targetMonth)
            dragOffsetPx = 0f
            animatedOffsetPx.snapTo(0f)
            isAnimating = false
        }
    }

    fun requestMonthChange(targetMonth: YearMonth, targetOffsetPx: Float) {
        if (isAnimating || targetMonth == yearMonth || targetMonth.isAfter(currentMonth)) return
        if (calendarWidthPx > 0f) {
            animateMonthChange(targetMonth, targetOffsetPx)
        } else {
            visibleMonth = targetMonth
            onMonthChange(targetMonth)
        }
    }

    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            IconButton(
                onClick = {
                    requestMonthChange(yearMonth.minusMonths(1), calendarWidthPx)
                },
                enabled = !isAnimating,
                modifier = Modifier.size(36.dp)
            ) {
                Icon(Icons.Default.ChevronLeft, contentDescription = "Previous month", tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Text(
                text = visibleMonth.format(monthFormatter),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            IconButton(
                onClick = {
                    requestMonthChange(yearMonth.plusMonths(1), -calendarWidthPx)
                },
                enabled = canGoForward && !isAnimating,
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

        Spacer(Modifier.height(8.dp))

        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .onSizeChanged { calendarWidthPx = it.width.toFloat() }
                .clipToBounds()
                .pointerInput(yearMonth, canGoForward, calendarWidthPx, isAnimating) {
                    if (calendarWidthPx <= 0f || isAnimating) return@pointerInput
                    detectHorizontalDragGestures(
                        onDragStart = {
                            if (isAnimating) return@detectHorizontalDragGestures
                            isDragging = true
                            scope.launch {
                                animatedOffsetPx.stop()
                                animatedOffsetPx.snapTo(dragOffsetPx)
                            }
                        },
                        onDragEnd = {
                            val swipeThresholdPx = calendarWidthPx * 0.2f
                            val targetMonth = when {
                                dragOffsetPx > swipeThresholdPx -> yearMonth.minusMonths(1)
                                dragOffsetPx < -swipeThresholdPx && canGoForward -> yearMonth.plusMonths(1)
                                else -> null
                            }
                            val targetOffsetPx = when {
                                dragOffsetPx > swipeThresholdPx -> calendarWidthPx
                                dragOffsetPx < -swipeThresholdPx && canGoForward -> -calendarWidthPx
                                else -> 0f
                            }

                            if (targetMonth != null) {
                                animateMonthChange(
                                    targetMonth = targetMonth,
                                    targetOffsetPx = targetOffsetPx,
                                    initialOffsetPx = dragOffsetPx
                                )
                            } else {
                                isAnimating = true
                                scope.launch {
                                    animatedOffsetPx.stop()
                                    animatedOffsetPx.snapTo(dragOffsetPx)
                                    isDragging = false
                                    animatedOffsetPx.animateTo(
                                        targetValue = 0f,
                                        animationSpec = tween(durationMillis = 220)
                                    )
                                    dragOffsetPx = 0f
                                    animatedOffsetPx.snapTo(0f)
                                    isAnimating = false
                                }
                            }
                        },
                        onDragCancel = {
                            isAnimating = true
                            scope.launch {
                                animatedOffsetPx.stop()
                                animatedOffsetPx.snapTo(dragOffsetPx)
                                isDragging = false
                                animatedOffsetPx.animateTo(
                                    targetValue = 0f,
                                    animationSpec = tween(durationMillis = 220)
                                )
                                dragOffsetPx = 0f
                                animatedOffsetPx.snapTo(0f)
                                isAnimating = false
                            }
                        }
                    ) { change, dragAmount ->
                        change.consume()
                        val minOffset = if (canGoForward) -calendarWidthPx else 0f
                        dragOffsetPx = (dragOffsetPx + dragAmount).coerceIn(minOffset, calendarWidthPx)
                    }
                }
        ) {
            val monthOffsetPx = if (isDragging) dragOffsetPx else animatedOffsetPx.value
            val previousMonth = yearMonth.minusMonths(1)
            val nextMonth = yearMonth.plusMonths(1)

            Box(modifier = Modifier.fillMaxWidth()) {
                if (monthOffsetPx > 0f) {
                    CalendarMonthContent(
                        modifier = Modifier
                            .fillMaxWidth()
                            .offset { IntOffset((monthOffsetPx - calendarWidthPx).roundToInt(), 0) },
                        yearMonth = previousMonth,
                        sessionMap = sessionMap,
                        today = today,
                        onDateClick = { pendingDate = it }
                    )
                }

                CalendarMonthContent(
                    modifier = Modifier
                        .fillMaxWidth()
                        .offset { IntOffset(monthOffsetPx.roundToInt(), 0) },
                    yearMonth = yearMonth,
                    sessionMap = sessionMap,
                    today = today,
                    onDateClick = { pendingDate = it }
                )

                if (monthOffsetPx < 0f && canGoForward) {
                    CalendarMonthContent(
                        modifier = Modifier
                            .fillMaxWidth()
                            .offset { IntOffset((monthOffsetPx + calendarWidthPx).roundToInt(), 0) },
                        yearMonth = nextMonth,
                        sessionMap = sessionMap,
                        today = today,
                        onDateClick = { pendingDate = it }
                    )
                }
            }
        }
    }

    pendingDate?.let { date ->
        TrainingSessionActionDialog(
            date = date,
            session = sessionMap[date.toString()],
            onLogSession = onLogSession,
            onDeleteSession = onDeleteSession,
            onEditSession = onEditSession,
            onDismiss = { pendingDate = null }
        )
    }
}

@Composable
private fun CalendarMonthContent(
    modifier: Modifier = Modifier,
    yearMonth: YearMonth,
    sessionMap: Map<String, TrainingSession>,
    today: LocalDate,
    onDateClick: (LocalDate) -> Unit
) {
    val daysInMonth = yearMonth.lengthOfMonth()
    val firstDayOfWeek = yearMonth.atDay(1).dayOfWeek.value
    val dayHeaders = listOf("Mo", "Tu", "We", "Th", "Fr", "Sa", "Su")

    Column(modifier = modifier) {
        Row(modifier = Modifier.fillMaxWidth()) {
            dayHeaders.forEach { header ->
                Text(
                    text = header,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(Modifier.height(4.dp))

        for (row in 0 until 6) {
            Row(modifier = Modifier.fillMaxWidth()) {
                for (col in 0 until 7) {
                    val cellIndex = row * 7 + col
                    val day = cellIndex - (firstDayOfWeek - 1) + 1
                    if (day in 1..daysInMonth) {
                        val date = yearMonth.atDay(day)
                        val dateStr = date.toString()
                        val session = sessionMap[dateStr]
                        val isFuture = date.isAfter(today)
                        val isToday = date == today

                        val bgColor = when {
                            session?.isPersonalTraining == true -> HeatmapPT
                            session != null -> HeatmapRegular
                            else -> Color.Transparent
                        }

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .aspectRatio(1f)
                                .padding(2.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(bgColor.copy(alpha = if (session != null) 0.25f else 0f))
                                .then(
                                    if (isToday) Modifier.border(
                                        1.5.dp,
                                        MaterialTheme.colorScheme.primary,
                                        RoundedCornerShape(8.dp)
                                    ) else Modifier
                                )
                                .then(
                                    if (!isFuture) Modifier.clickable { onDateClick(date) } else Modifier
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "$day",
                                    fontSize = 13.sp,
                                    fontWeight = if (session != null) FontWeight.Bold else FontWeight.Normal,
                                    color = when {
                                        isFuture -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
                                        session != null -> if (session.isPersonalTraining) HeatmapPT else HeatmapRegular
                                        else -> MaterialTheme.colorScheme.onSurface
                                    }
                                )
                                if (session != null) {
                                    Box(
                                        modifier = Modifier
                                            .size(5.dp)
                                            .clip(CircleShape)
                                            .background(if (session.isPersonalTraining) HeatmapPT else HeatmapRegular)
                                    )
                                }
                            }
                        }
                    } else {
                        Spacer(modifier = Modifier.weight(1f).aspectRatio(1f))
                    }
                }
            }
        }

        Spacer(Modifier.height(8.dp))
        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            LegendItem(color = HeatmapRegular, label = "Regular")
            LegendItem(color = HeatmapPT, label = "Personal")
            Spacer(Modifier.weight(1f))
            val monthSessions = sessionMap.values.filter {
                it.date.startsWith(yearMonth.toString())
            }
            Text(
                text = "${monthSessions.size} sessions",
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun TrainingSessionActionDialog(
    date: LocalDate,
    session: TrainingSession?,
    onLogSession: (String, Boolean) -> Unit,
    onDeleteSession: (String) -> Unit,
    onEditSession: (String, Boolean) -> Unit,
    onDismiss: () -> Unit
) {
    val dateLabel = remember(date) { date.format(DateTimeFormatter.ofPattern("d MMM yyyy", Locale.ENGLISH)) }
    if (session == null) {
        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text("Log session") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        dateLabel,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilledTonalButton(
                            onClick = { onLogSession(date.toString(), false); onDismiss() },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.filledTonalButtonColors(containerColor = GymBlue.copy(alpha = 0.15f))
                        ) { Text("Regular", color = GymBlue) }
                        FilledTonalButton(
                            onClick = { onLogSession(date.toString(), true); onDismiss() },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.filledTonalButtonColors(containerColor = GymPurple.copy(alpha = 0.15f))
                        ) { Text("Personal", color = GymPurple) }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = onDismiss) { Text("Cancel") }
            }
        )
    } else {
        val isPt = session.isPersonalTraining
        val typeLabel = if (isPt) "Personal Training" else "Regular"
        val typeColor = if (isPt) HeatmapPT else HeatmapRegular
        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text("Session on $dateLabel") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Box(modifier = Modifier.size(10.dp).clip(RoundedCornerShape(2.dp)).background(typeColor))
                        Text(typeLabel, style = MaterialTheme.typography.bodyMedium)
                    }
                    Text(
                        text = "Change session type:",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                if (isPt) onEditSession(date.toString(), false)
                                onDismiss()
                            },
                            modifier = Modifier.weight(1f),
                            border = BorderStroke(
                                1.dp,
                                if (!isPt) HeatmapRegular else MaterialTheme.colorScheme.outline
                            ),
                            colors = ButtonDefaults.outlinedButtonColors(
                                containerColor = if (!isPt) HeatmapRegular.copy(alpha = 0.15f) else Color.Transparent
                            )
                        ) {
                            Text(
                                "Regular",
                                color = if (!isPt) HeatmapRegular else MaterialTheme.colorScheme.onSurface
                            )
                        }
                        OutlinedButton(
                            onClick = {
                                if (!isPt) onEditSession(date.toString(), true)
                                onDismiss()
                            },
                            modifier = Modifier.weight(1f),
                            border = BorderStroke(
                                1.dp,
                                if (isPt) HeatmapPT else MaterialTheme.colorScheme.outline
                            ),
                            colors = ButtonDefaults.outlinedButtonColors(
                                containerColor = if (isPt) HeatmapPT.copy(alpha = 0.15f) else Color.Transparent
                            )
                        ) {
                            Text(
                                "Personal",
                                color = if (isPt) HeatmapPT else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                Row {
                    TextButton(onClick = { onDeleteSession(date.toString()); onDismiss() }) {
                        Text("Remove", color = GymRed)
                    }
                    TextButton(onClick = onDismiss) { Text("Close") }
                }
            }
        )
    }
}

@Composable
fun SupportCard() {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    var copiedLabel by remember { mutableStateOf<String?>(null) }

    val ethAddress = "0xa4d644491df24eae8732732218872e433d1add66"
    val btcAddress = "bc1qmmjclsthfgexp2vl42909wzqvtmjruuvch6vgn"

    GymCard(icon = Icons.Default.Favorite, title = "Buy Me a Coffee", iconTint = GymBlue) {
        Button(
            onClick = {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://paypal.me/nemanjapetrasinovic"))
                context.startActivity(intent)
            },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = GymBlue),
            shape = RoundedCornerShape(12.dp)
        ) {
            @Suppress("DEPRECATION")
            Icon(Icons.Default.OpenInNew, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(6.dp))
            Text("Support via PayPal", fontWeight = FontWeight.Bold)
        }

        Spacer(Modifier.height(12.dp))

        Text(
            text = "Or send crypto",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(Modifier.height(8.dp))

        CryptoAddressRow(
            label = "ETH",
            address = ethAddress,
            isCopied = copiedLabel == "ETH",
            borderColor = GymBlue,
            onCopy = {
                clipboardManager.setText(AnnotatedString(ethAddress))
                copiedLabel = "ETH"
            }
        )

        Spacer(Modifier.height(6.dp))

        CryptoAddressRow(
            label = "BTC",
            address = btcAddress,
            isCopied = copiedLabel == "BTC",
            borderColor = GymBlue,
            onCopy = {
                clipboardManager.setText(AnnotatedString(btcAddress))
                copiedLabel = "BTC"
            }
        )
    }
}

@Composable
private fun CryptoAddressRow(
    label: String,
    address: String,
    isCopied: Boolean,
    borderColor: Color,
    onCopy: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, borderColor.copy(alpha = 0.4f), RoundedCornerShape(10.dp))
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            fontWeight = FontWeight.Bold,
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = address.take(10) + "..." + address.takeLast(6),
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        IconButton(onClick = onCopy, modifier = Modifier.size(32.dp)) {
            Icon(
                imageVector = if (isCopied) Icons.Default.Check else Icons.Default.ContentCopy,
                contentDescription = "Copy $label address",
                tint = if (isCopied) GymGreen else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

@Composable
fun GymCard(
    icon: ImageVector,
    title: String,
    iconTint: Color,
    action: (@Composable () -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 2.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 12.dp)
            ) {
                Surface(
                    shape = CircleShape,
                    color = iconTint.copy(alpha = 0.15f),
                    modifier = Modifier.size(32.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(18.dp))
                    }
                }
                Spacer(Modifier.width(10.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f)
                )
                action?.invoke()
            }
            content()
        }
    }
}

@Composable
fun EmptyStateRow(message: String, actionLabel: String, onClick: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = message,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodyMedium
        )
        FilledTonalButton(
            onClick = onClick,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(actionLabel, fontSize = 12.sp)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GymDatePickerDialog(
    title: String,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit,
    maxDateMillis: Long? = null
) {
    val state = rememberDatePickerState(
        initialSelectedDateMillis = System.currentTimeMillis(),
        selectableDates = if (maxDateMillis != null) {
            object : SelectableDates {
                override fun isSelectableDate(utcTimeMillis: Long): Boolean {
                    return utcTimeMillis <= maxDateMillis
                }
            }
        } else {
            object : SelectableDates {}
        }
    )

    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = {
                state.selectedDateMillis?.let { millis ->
                    val date = java.time.Instant.ofEpochMilli(millis)
                        .atZone(java.time.ZoneId.of("UTC"))
                        .toLocalDate()
                        .toString()
                    onConfirm(date)
                }
            }) { Text("Confirm") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    ) {
        DatePicker(state = state, title = { Text(title, modifier = Modifier.padding(start = 24.dp, top = 16.dp)) })
    }
}

private fun formatDisplayDate(isoDate: String): String {
    return try {
        LocalDate.parse(isoDate).format(DateTimeFormatter.ofPattern("dd MMM yyyy"))
    } catch (e: Exception) { isoDate }
}
