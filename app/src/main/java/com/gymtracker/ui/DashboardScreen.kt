package com.gymtracker.ui

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.gymtracker.data.PtPurchase
import com.gymtracker.data.TrainingSession
import com.gymtracker.viewmodel.GymViewModel
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

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
            purchased = state.personalTrainingsPurchased,
            used = state.personalTrainingsUsed,
            remaining = state.personalTrainingsRemaining,
            ptPurchases = state.ptPurchases,
            onAddPt = { vm.addPtPurchase(it) },
            onManagePurchases = onOpenSettings
        )

        CheckInCard(
            selectedDateSession = state.selectedDateSession,
            selectedDateFormatted = state.selectedDateFormatted,
            isSelectedDateToday = state.isSelectedDateToday,
            onCheckIn = { isPersonal -> vm.checkIn(isPersonal) },
            onRemoveSession = { vm.removeSession() },
            onPreviousDay = { vm.setSelectedDate(LocalDate.parse(state.selectedDate).minusDays(1)) },
            onNextDay = {
                val next = LocalDate.parse(state.selectedDate).plusDays(1)
                if (!next.isAfter(LocalDate.now())) vm.setSelectedDate(next)
            },
            onSelectDate = { vm.setSelectedDate(it) },
            onResetToToday = { vm.setSelectedDate(LocalDate.now()) }
        )

        ActivityHeatmap(
            sessions = state.allSessions,
            onDateClick = { vm.setSelectedDate(it) },
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

    if (showDatePicker) {
        GymDatePickerDialog(
            title = "When did you pay?",
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
    purchased: Int,
    used: Int,
    remaining: Int,
    ptPurchases: List<PtPurchase>,
    onAddPt: (Int) -> Unit,
    onManagePurchases: () -> Unit
) {
    var showAddDialog by remember { mutableStateOf(false) }

    GymCard(icon = Icons.Default.Person, title = "Personal Trainings", iconTint = GymPurple) {
        if (purchased == 0) {
            EmptyStateRow(
                message = "No personal trainings added",
                actionLabel = "Add PT",
                onClick = { showAddDialog = true }
            )
        } else {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                PTStat(label = "Purchased", value = purchased, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.weight(1f))
                PTStat(label = "Used", value = used, color = GymYellow, modifier = Modifier.weight(1f))
                PTStat(
                    label = "Remaining",
                    value = remaining,
                    color = if (remaining == 0) GymRed else GymGreen,
                    modifier = Modifier.weight(1f)
                )
            }

            if (purchased > 0) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.padding(top = 8.dp)
                ) {
                    repeat(minOf(purchased, 20)) { idx ->
                        val isUsed = idx < used
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(if (isUsed) GymYellow else GymPurple.copy(alpha = 0.35f))
                        )
                    }
                    if (purchased > 20) {
                        Text("+${purchased - 20}", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }

            Spacer(Modifier.height(8.dp))

            FilledTonalButton(
                onClick = { showAddDialog = true },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text("Add PT")
            }

            TextButton(
                onClick = onManagePurchases,
                contentPadding = PaddingValues(0.dp)
            ) {
                Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(14.dp))
                Spacer(Modifier.width(4.dp))
                Text("Manage purchases", fontSize = 12.sp)
            }
        }
    }

    if (showAddDialog) {
        val currentMonth = java.time.YearMonth.now()
        val currentMonthTotal = ptPurchases
            .find { it.month == currentMonth.toString() }?.count ?: 0
        AddPtSessionsDialog(
            currentMonth = currentMonth,
            currentTotal = currentMonthTotal,
            onConfirm = { count ->
                onAddPt(count)
                showAddDialog = false
            },
            onDismiss = { showAddDialog = false }
        )
    }
}

@Composable
fun PTStat(label: String, value: Int, color: Color, modifier: Modifier = Modifier) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "$value",
                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                color = color
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun CheckInCard(
    selectedDateSession: TrainingSession?,
    selectedDateFormatted: String,
    isSelectedDateToday: Boolean,
    onCheckIn: (Boolean) -> Unit,
    onRemoveSession: () -> Unit,
    onPreviousDay: () -> Unit,
    onNextDay: () -> Unit,
    onSelectDate: (LocalDate) -> Unit,
    onResetToToday: () -> Unit
) {
    var showDatePicker by remember { mutableStateOf(false) }
    val alreadyCheckedIn = selectedDateSession != null

    GymCard(icon = Icons.Default.EditCalendar, title = "Log Training", iconTint = GymGreen) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            IconButton(onClick = onPreviousDay, modifier = Modifier.size(36.dp)) {
                Icon(
                    Icons.Default.ChevronLeft,
                    contentDescription = "Previous day",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Text(
                text = selectedDateFormatted,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Medium
            )

            IconButton(
                onClick = onNextDay,
                enabled = !isSelectedDateToday,
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    Icons.Default.ChevronRight,
                    contentDescription = "Next day",
                    tint = if (isSelectedDateToday)
                        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
                    else
                        MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextButton(onClick = { showDatePicker = true }) {
                Icon(
                    Icons.Default.CalendarMonth,
                    contentDescription = "Pick date",
                    modifier = Modifier.size(16.dp)
                )
                Spacer(Modifier.width(4.dp))
                Text("Pick date", fontSize = 12.sp)
            }

            if (!isSelectedDateToday) {
                Spacer(Modifier.width(8.dp))
                TextButton(onClick = onResetToToday) {
                    Icon(
                        Icons.Default.Today,
                        contentDescription = "Go to today",
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(Modifier.width(4.dp))
                    Text("Today", fontSize = 12.sp)
                }
            }
        }

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
                        text = if (selectedDateSession!!.isPersonalTraining) "Personal Training Done!" else "Training Done!",
                        fontWeight = FontWeight.Bold,
                        color = GymGreen
                    )
                    Text(
                        text = if (selectedDateSession.isPersonalTraining) "Personal training session logged" else "Regular training session logged",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(Modifier.height(10.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                val switchLabel = if (selectedDateSession!!.isPersonalTraining) "Change to Regular" else "Change to PT"
                OutlinedButton(
                    onClick = { onCheckIn(!selectedDateSession.isPersonalTraining) },
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

    if (showDatePicker) {
        GymDatePickerDialog(
            title = "Select training date",
            maxDateMillis = System.currentTimeMillis(),
            onConfirm = { dateStr ->
                onSelectDate(LocalDate.parse(dateStr))
                showDatePicker = false
            },
            onDismiss = { showDatePicker = false }
        )
    }
}

@Composable
fun ActivityHeatmap(
    sessions: List<TrainingSession>,
    onDateClick: (LocalDate) -> Unit,
    onDeleteSession: (String) -> Unit,
    onEditSession: (String, Boolean) -> Unit
) {
    val sessionMap = remember(sessions) { sessions.associateBy { it.date } }
    var showCalendar by remember { mutableStateOf(false) }
    var calendarMonth by remember { mutableStateOf(YearMonth.now()) }

    val today = LocalDate.now()
    val weeksToShow = 16
    val startDate = today.minusWeeks(weeksToShow.toLong()).let { d ->
        d.minusDays(((d.dayOfWeek.value - 1) % 7).toLong())
    }
    val endDate = today.let { d ->
        d.plusDays((7 - d.dayOfWeek.value).toLong())
    }

    val allDays = buildList {
        var d = startDate
        while (!d.isAfter(endDate)) {
            add(d)
            d = d.plusDays(1)
        }
    }

    val weeks = allDays.chunked(7)

    val monthLabels = buildList {
        var lastMonth = -1
        weeks.forEachIndexed { idx, week ->
            val month = week.first().monthValue
            if (month != lastMonth) {
                add(idx to week.first().month.getDisplayName(TextStyle.SHORT, Locale.ENGLISH))
                lastMonth = month
            }
        }
    }

    val dayLabels = listOf("Mon", "", "Wed", "", "Fri", "", "Sun")

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
        val weekCount = weeks.size
        BoxWithConstraints(modifier = Modifier.fillMaxWidth().padding(end = labelGap + spacing)) {
            val availableWidthDp = maxWidth - dayLabelWidth - labelGap
            val totalSpacing = spacing * (weekCount - 1)
            val cellSize = ((availableWidthDp - totalSpacing) / weekCount)

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

                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(spacing)
                    ) {
                        weeks.forEachIndexed { idx, _ ->
                            val label = monthLabels.find { it.first == idx }?.second ?: ""
                            Box(
                                modifier = Modifier
                                    .weight(1f)
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
                                                    Modifier.clickable { onDateClick(date) }
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

        val totalTrainings = sessions.size
        val ptCount = sessions.count { it.isPersonalTraining }
        val regularCount = totalTrainings - ptCount

        Spacer(Modifier.height(10.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            MiniStat("Total", "$totalTrainings sessions", MaterialTheme.colorScheme.onSurfaceVariant, Modifier.weight(1f))
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
                onDateClick = onDateClick,
                onDeleteSession = onDeleteSession,
                onEditSession = onEditSession
            )
        }
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
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(value, fontWeight = FontWeight.Bold, color = color, fontSize = 14.sp)
            Text(label, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
fun TrainingCalendar(
    yearMonth: YearMonth,
    sessionMap: Map<String, TrainingSession>,
    onMonthChange: (YearMonth) -> Unit,
    onDateClick: (LocalDate) -> Unit,
    onDeleteSession: (String) -> Unit,
    onEditSession: (String, Boolean) -> Unit
) {
    val today = LocalDate.now()
    val daysInMonth = yearMonth.lengthOfMonth()
    val firstDayOfWeek = yearMonth.atDay(1).dayOfWeek.value
    val monthFormatter = remember { DateTimeFormatter.ofPattern("MMMM yyyy", Locale.ENGLISH) }
    val sessionDialogFormatter = remember { DateTimeFormatter.ofPattern("EEEE, d MMMM yyyy", Locale.ENGLISH) }
    val canGoForward = yearMonth.isBefore(YearMonth.now())
    var tappedSessionDate by remember { mutableStateOf<LocalDate?>(null) }

    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            IconButton(onClick = { onMonthChange(yearMonth.minusMonths(1)) }, modifier = Modifier.size(36.dp)) {
                Icon(Icons.Default.ChevronLeft, contentDescription = "Previous month", tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Text(
                text = yearMonth.format(monthFormatter),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            IconButton(
                onClick = { onMonthChange(yearMonth.plusMonths(1)) },
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

        Spacer(Modifier.height(8.dp))

        val dayHeaders = listOf("Mo", "Tu", "We", "Th", "Fr", "Sa", "Su")
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
                                    if (!isFuture) Modifier.clickable {
                                        if (session != null) {
                                            tappedSessionDate = date
                                        } else {
                                            onDateClick(date)
                                        }
                                    } else Modifier
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

    var editingSessionDate by remember { mutableStateOf<LocalDate?>(null) }

    tappedSessionDate?.let { date ->
        val dateStr = date.toString()
        val session = sessionMap[dateStr]
        if (session != null) {
            val typeLabel = if (session.isPersonalTraining) "Personal Training" else "Regular Training"
            AlertDialog(
                onDismissRequest = { tappedSessionDate = null },
                title = { Text(date.format(sessionDialogFormatter)) },
                text = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(if (session.isPersonalTraining) HeatmapPT else HeatmapRegular)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(typeLabel, style = MaterialTheme.typography.bodyMedium)
                    }
                },
                confirmButton = {
                    TextButton(onClick = {
                        editingSessionDate = date
                        tappedSessionDate = null
                    }) { Text("Edit") }
                },
                dismissButton = {
                    TextButton(onClick = {
                        onDeleteSession(dateStr)
                        tappedSessionDate = null
                    }) {
                        Text("Delete", color = GymRed)
                    }
                }
            )
        } else {
            tappedSessionDate = null
        }
    }

    editingSessionDate?.let { date ->
        val dateStr = date.toString()
        val session = sessionMap[dateStr]
        if (session != null) {
            AlertDialog(
                onDismissRequest = { editingSessionDate = null },
                title = { Text("Edit Session") },
                text = {
                    Column {
                        Text(
                            date.format(sessionDialogFormatter),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.height(16.dp))
                        Text("Change session type:", style = MaterialTheme.typography.bodyMedium)
                        Spacer(Modifier.height(12.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            val isCurrentlyPt = session.isPersonalTraining
                            OutlinedButton(
                                onClick = {
                                    if (isCurrentlyPt) {
                                        onEditSession(dateStr, false)
                                    }
                                    editingSessionDate = null
                                },
                                modifier = Modifier.weight(1f),
                                border = BorderStroke(
                                    1.dp,
                                    if (!isCurrentlyPt) HeatmapRegular else MaterialTheme.colorScheme.outline
                                ),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    containerColor = if (!isCurrentlyPt) HeatmapRegular.copy(alpha = 0.15f) else Color.Transparent
                                )
                            ) {
                                Text(
                                    "Regular",
                                    color = if (!isCurrentlyPt) HeatmapRegular else MaterialTheme.colorScheme.onSurface
                                )
                            }
                            OutlinedButton(
                                onClick = {
                                    if (!isCurrentlyPt) {
                                        onEditSession(dateStr, true)
                                    }
                                    editingSessionDate = null
                                },
                                modifier = Modifier.weight(1f),
                                border = BorderStroke(
                                    1.dp,
                                    if (isCurrentlyPt) HeatmapPT else MaterialTheme.colorScheme.outline
                                ),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    containerColor = if (isCurrentlyPt) HeatmapPT.copy(alpha = 0.15f) else Color.Transparent
                                )
                            ) {
                                Text(
                                    "Personal",
                                    color = if (isCurrentlyPt) HeatmapPT else MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                },
                confirmButton = {},
                dismissButton = {
                    TextButton(onClick = { editingSessionDate = null }) {
                        Text("Cancel")
                    }
                }
            )
        } else {
            editingSessionDate = null
        }
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
    onCopy: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
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
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            content()
        }
    }
}

@Composable
fun EmptyStateRow(message: String, actionLabel: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = message,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodyMedium
        )
        FilledTonalButton(onClick = onClick) {
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

@Composable
fun PTCountDialog(
    current: Int,
    onConfirm: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    var count by remember { mutableStateOf(if (current == 0) "" else current.toString()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Personal Training Sessions") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    "How many personal training sessions did you purchase?",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OutlinedTextField(
                    value = count,
                    onValueChange = { if (it.all { c -> c.isDigit() } && it.length <= 3) count = it },
                    label = { Text("Number of sessions") },
                    singleLine = true,
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                        keyboardType = androidx.compose.ui.text.input.KeyboardType.Number
                    )
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

@Composable
fun AddPtSessionsDialog(
    currentMonth: java.time.YearMonth,
    currentTotal: Int,
    onConfirm: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    var count by remember { mutableStateOf("") }
    val monthLabel = "${currentMonth.month.getDisplayName(java.time.format.TextStyle.FULL, Locale.ENGLISH)} ${currentMonth.year}"

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add PT Sessions") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "Adding to $monthLabel (currently $currentTotal)",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                OutlinedTextField(
                    value = count,
                    onValueChange = { if (it.all { c -> c.isDigit() } && it.length <= 3) count = it },
                    label = { Text("Sessions purchased") },
                    singleLine = true,
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                        keyboardType = androidx.compose.ui.text.input.KeyboardType.Number
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val c = count.toIntOrNull() ?: 0
                    if (c > 0) onConfirm(c)
                },
                enabled = (count.toIntOrNull() ?: 0) > 0
            ) { Text("Add") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

private fun formatDisplayDate(isoDate: String): String {
    return try {
        LocalDate.parse(isoDate).format(DateTimeFormatter.ofPattern("dd MMM yyyy"))
    } catch (e: Exception) { isoDate }
}
