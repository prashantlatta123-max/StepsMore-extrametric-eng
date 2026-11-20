package com.sarotech.stepsmore.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sarotech.stepsmore.data.StepsRepository
import com.sarotech.stepsmore.service.StepsMoreService
import com.sarotech.stepsmore.ui.StepsViewModel
import com.sarotech.stepsmore.ui.components.ChoiceDialog
import com.sarotech.stepsmore.ui.components.DayBarItem
import com.sarotech.stepsmore.ui.components.HourlyBreakdownCard
import com.sarotech.stepsmore.ui.components.MonthCalendar
import com.sarotech.stepsmore.ui.components.StepsBarChart
import com.sarotech.stepsmore.ui.theme.StepsMoreColors
import com.sarotech.stepsmore.util.BatteryOptimizationHelper
import java.text.NumberFormat
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun HomeScreen(
    viewModel: StepsViewModel,
    onNavigateAchievements: () -> Unit,
    onNavigateBackup: () -> Unit,
    onNavigateDailyGoal: () -> Unit,
    onNavigateSettings: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val today by viewModel.currentDate.collectAsStateWithLifecycle()
    val todaySteps by viewModel.todaySteps.collectAsStateWithLifecycle()
    val past7Days by viewModel.past7Days.collectAsStateWithLifecycle()
    val allDaily by viewModel.allDaily.collectAsStateWithLifecycle()
    val selectedDateIso by viewModel.selectedDateIso.collectAsStateWithLifecycle()
    val selectedHourly by viewModel.selectedHourlySteps.collectAsStateWithLifecycle()
    val sensorStatus by StepsMoreService.sensorStatus.collectAsStateWithLifecycle()

    var menuExpanded by remember { mutableStateOf(false) }
    var heroExpanded by remember { mutableStateOf(false) }
    var showTimedPauseDialog by remember { mutableStateOf(false) }
    var displayedMonth by remember { mutableStateOf(YearMonth.now()) }
    var dismissBatteryBanner by remember { mutableStateOf(false) }
    var batteryUnrestricted by remember {
        mutableStateOf(BatteryOptimizationHelper.isIgnoringBatteryOptimizations(context))
    }

    // Re-check battery optimization whenever user returns from system settings
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                batteryUnrestricted = BatteryOptimizationHelper.isIgnoringBatteryOptimizations(context)
                viewModel.refreshDate()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    // Reactive today comes from viewModel.currentDate
    val currentMonth = YearMonth.from(today)
    val dailyMap = remember(allDaily) { allDaily.associate { it.date to it.steps } }

    val sevenDayItems = remember(past7Days, todaySteps, today) {
        val map = past7Days.associate { it.date to it.steps }
        (6 downTo 0).map { offset ->
            val d = today.minusDays(offset.toLong())
            val iso = d.format(DateTimeFormatter.ISO_LOCAL_DATE)
            val steps = if (offset == 0) todaySteps else (map[iso] ?: 0)
            DayBarItem(
                dateIso = iso,
                dayLabel = d.format(DateTimeFormatter.ofPattern("EEE", Locale.US)),
                steps = steps,
                isToday = offset == 0
            )
        }
    }

    val rangeStart = StepsRepository.formatDate(
        today.minusDays(6).format(DateTimeFormatter.ISO_LOCAL_DATE),
        settings.dateFormat
    )
    val rangeEnd = StepsRepository.formatDate(
        today.format(DateTimeFormatter.ISO_LOCAL_DATE),
        settings.dateFormat
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(StepsMoreColors.BgPrimary)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(bottom = 96.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Top bar with top-right overflow icon & opaque pure black popup menu
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 6.dp, end = 6.dp),
                contentAlignment = Alignment.CenterEnd
            ) {
                IconButton(onClick = { menuExpanded = true }) {
                    Icon(
                        imageVector = Icons.Outlined.MoreVert,
                        contentDescription = "Menu",
                        tint = StepsMoreColors.TextMuted
                    )
                }

                DropdownMenu(
                    expanded = menuExpanded,
                    onDismissRequest = { menuExpanded = false },
                    containerColor = StepsMoreColors.BgPrimary,
                    tonalElevation = 0.dp,
                    shadowElevation = 0.dp,
                    modifier = Modifier.width(196.dp)
                ) {
                    DropdownMenuItem(
                        text = { Text("Achievements", color = StepsMoreColors.TextPrimary, fontSize = 16.sp) },
                        leadingIcon = {
                            Icon(Icons.Outlined.EmojiEvents, null, tint = StepsMoreColors.TextPrimary)
                        },
                        onClick = {
                            menuExpanded = false
                            onNavigateAchievements()
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Backup", color = StepsMoreColors.TextPrimary, fontSize = 16.sp) },
                        leadingIcon = {
                            Icon(Icons.Outlined.CloudDownload, null, tint = StepsMoreColors.TextPrimary)
                        },
                        onClick = {
                            menuExpanded = false
                            onNavigateBackup()
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Daily Goal", color = StepsMoreColors.TextPrimary, fontSize = 16.sp) },
                        leadingIcon = {
                            Icon(Icons.Outlined.TrackChanges, null, tint = StepsMoreColors.TextPrimary)
                        },
                        onClick = {
                            menuExpanded = false
                            onNavigateDailyGoal()
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Settings", color = StepsMoreColors.TextPrimary, fontSize = 16.sp) },
                        leadingIcon = {
                            Icon(Icons.Outlined.Settings, null, tint = StepsMoreColors.TextPrimary)
                        },
                        onClick = {
                            menuExpanded = false
                            onNavigateSettings()
                        }
                    )
                }
            }

            // Battery optimization setup prompt (only shown if restricted and not dismissed)
            AnimatedVisibility(visible = !batteryUnrestricted && !dismissBatteryBanner) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(StepsMoreColors.SurfaceCard)
                        .clickable { BatteryOptimizationHelper.requestIgnoreBatteryOptimizations(context) }
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Outlined.BatteryAlert,
                        contentDescription = null,
                        tint = StepsMoreColors.TextSecondary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Allow unrestricted background tracking",
                            fontSize = 13.sp,
                            color = StepsMoreColors.TextPrimary
                        )
                        Text(
                            text = "Tap to disable battery optimization for accurate steps",
                            fontSize = 11.sp,
                            color = StepsMoreColors.TextMuted
                        )
                    }
                    IconButton(
                        onClick = { dismissBatteryBanner = true },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Close,
                            contentDescription = "Dismiss",
                            tint = StepsMoreColors.TextMuted,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Live sensor diagnostics: do not leave users guessing why the count stays at zero.
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                colors = CardDefaults.cardColors(containerColor = StepsMoreColors.SurfaceCard),
                shape = RoundedCornerShape(14.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Outlined.DirectionsWalk,
                        contentDescription = null,
                        tint = StepsMoreColors.TextSecondary,
                        modifier = Modifier.size(19.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "Step sensor",
                            color = StepsMoreColors.TextPrimary,
                            fontSize = 12.sp
                        )
                        Text(
                            text = sensorStatus,
                            color = StepsMoreColors.TextSecondary,
                            fontSize = 11.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Hero Today Block
            Text(
                text = if (settings.isPaused) "TODAY • PAUSED" else "TODAY",
                fontSize = 14.sp,
                color = StepsMoreColors.TextSecondary,
                letterSpacing = 0.8.sp
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "$todaySteps steps",
                fontSize = 44.sp,
                color = StepsMoreColors.TextPrimary
            )

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Outlined.DirectionsWalk,
                        contentDescription = null,
                        tint = StepsMoreColors.TextSecondary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = StepsRepository.formatDistance(todaySteps, settings),
                        fontSize = 18.sp,
                        color = StepsMoreColors.TextSecondary
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Outlined.LocalFireDepartment,
                        contentDescription = null,
                        tint = StepsMoreColors.TextSecondary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "${StepsRepository.calculateCalories(todaySteps, settings)} kcal",
                        fontSize = 18.sp,
                        color = StepsMoreColors.TextSecondary
                    )
                }
            }

            IconButton(
                onClick = { heroExpanded = !heroExpanded },
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    imageVector = if (heroExpanded) Icons.Outlined.KeyboardArrowUp else Icons.Outlined.KeyboardArrowDown,
                    contentDescription = "Toggle today details",
                    tint = StepsMoreColors.TextMuted
                )
            }

            AnimatedVisibility(visible = heroExpanded) {
                val pct = ((todaySteps.toFloat() / settings.dailyGoal.coerceAtLeast(1)) * 100).toInt()
                val remaining = (settings.dailyGoal - todaySteps).coerceAtLeast(0)
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(bottom = 10.dp)
                ) {
                    Text(
                        text = "$pct% of goal  •  $remaining steps left",
                        fontSize = 13.sp,
                        color = StepsMoreColors.TextMuted
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = sensorStatus,
                        fontSize = 11.sp,
                        color = StepsMoreColors.TextCalendarInactive
                    )
                }
            }

            val formattedGoal = NumberFormat.getNumberInstance(Locale.US).format(settings.dailyGoal)
            Text(
                text = "DAILY GOAL: $formattedGoal STEPS",
                fontSize = 14.sp,
                color = StepsMoreColors.TextMuted,
                letterSpacing = 0.5.sp
            )

            Spacer(modifier = Modifier.height(16.dp))

            HorizontalDivider(
                modifier = Modifier.padding(horizontal = 24.dp),
                thickness = 1.dp,
                color = StepsMoreColors.DividerMajor
            )

            Spacer(modifier = Modifier.height(22.dp))

            // Past 7 Days Section
            Text(
                text = "PAST 7 DAYS",
                fontSize = 14.sp,
                color = StepsMoreColors.TextSecondary,
                letterSpacing = 0.6.sp
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "$rangeStart - $rangeEnd",
                fontSize = 14.sp,
                color = StepsMoreColors.TextSecondary
            )

            Spacer(modifier = Modifier.height(14.dp))

            StepsBarChart(
                items = sevenDayItems,
                dailyGoal = settings.dailyGoal,
                drawTargetLine = settings.drawTargetLine,
                selectedDateIso = selectedDateIso,
                onBarClick = { item -> viewModel.toggleSelectedDate(item.dateIso) },
                modifier = Modifier.padding(horizontal = 12.dp)
            )

            // Inline Expandable Hourly Breakdown Card
            AnimatedVisibility(
                visible = selectedDateIso != null,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                selectedDateIso?.let { dateIso ->
                    val stepsForSelected = if (dateIso == today.format(DateTimeFormatter.ISO_LOCAL_DATE)) {
                        todaySteps
                    } else {
                        dailyMap[dateIso] ?: 0
                    }
                    HourlyBreakdownCard(
                        dateIso = dateIso,
                        totalSteps = stepsForSelected,
                        hourlyData = selectedHourly,
                        settings = settings,
                        onClose = { viewModel.clearSelectedDate() },
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            // Monthly Calendar Section with Previous/Next Month navigation
            MonthCalendar(
                yearMonth = displayedMonth,
                firstDayOfWeek = settings.firstDayOfWeek,
                dailyStepsMap = dailyMap + (today.format(DateTimeFormatter.ISO_LOCAL_DATE) to todaySteps),
                dailyGoal = settings.dailyGoal,
                selectedDateIso = selectedDateIso,
                onPreviousMonth = { displayedMonth = displayedMonth.minusMonths(1) },
                onNextMonth = {
                    if (displayedMonth.isBefore(currentMonth)) {
                        displayedMonth = displayedMonth.plusMonths(1)
                    }
                },
                canGoNext = displayedMonth.isBefore(currentMonth),
                onDateClick = { iso -> viewModel.toggleSelectedDate(iso) },
                modifier = Modifier.padding(horizontal = 16.dp)
            )
        }

        // Bottom-Right Squircle Pause/Resume FAB (Long-press for Timed Pause)
        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 18.dp, bottom = 18.dp)
                .size(56.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(StepsMoreColors.SurfaceFab)
                .combinedClickable(
                    onClick = { viewModel.togglePause() },
                    onLongClick = { showTimedPauseDialog = true }
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (settings.isPaused) Icons.Outlined.PlayArrow else Icons.Outlined.Pause,
                contentDescription = if (settings.isPaused) "Resume counting" else "Pause counting",
                tint = StepsMoreColors.TextPrimary,
                modifier = Modifier.size(26.dp)
            )
        }
    }

    if (showTimedPauseDialog) {
        ChoiceDialog(
            title = "Timed Pause",
            options = listOf("30 minutes", "1 hour", "2 hours", "Resume now"),
            selected = "",
            onDismiss = { showTimedPauseDialog = false },
            onSelect = { choice ->
                when (choice) {
                    "30 minutes" -> viewModel.setTimedPause(30)
                    "1 hour" -> viewModel.setTimedPause(60)
                    "2 hours" -> viewModel.setTimedPause(120)
                    "Resume now" -> if (settings.isPaused) viewModel.togglePause()
                }
            }
        )
    }
}
