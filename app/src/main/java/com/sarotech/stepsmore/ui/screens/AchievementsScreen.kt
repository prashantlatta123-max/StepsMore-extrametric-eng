package com.sarotech.stepsmore.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.TrackChanges
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sarotech.stepsmore.data.StepsRepository
import com.sarotech.stepsmore.ui.StepsViewModel
import com.sarotech.stepsmore.ui.components.SubScreenTopBar
import com.sarotech.stepsmore.ui.theme.StepsMoreColors
import java.text.NumberFormat
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale

private data class BestWeekResult(
    val steps: Int,
    val startIso: String?,
    val endIso: String?
)

private data class UnlockedMilestone(
    val threshold: Int,
    val label: String,
    val unlockedDateIso: String
)

@Composable
fun AchievementsScreen(
    viewModel: StepsViewModel,
    onBack: () -> Unit
) {
    val allDaily by viewModel.allDaily.collectAsStateWithLifecycle()
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val numFmt = remember { NumberFormat.getNumberInstance(Locale.US) }

    val sortedDaily = remember(allDaily) { allDaily.sortedBy { it.date } }
    val top3 = remember(sortedDaily) { sortedDaily.filter { it.steps > 0 }.sortedByDescending { it.steps }.take(3) }
    val totalSteps = remember(sortedDaily) { sortedDaily.sumOf { it.steps } }
    val activeDays = remember(sortedDaily) { sortedDaily.filter { it.steps > 0 } }
    val avgPerDay = if (activeDays.isNotEmpty()) totalSteps / activeDays.size else 0
    val firstDate = activeDays.firstOrNull()?.date

    // True Best 7-day rolling window across entire history
    val bestWeek = remember(activeDays) {
        if (activeDays.isEmpty()) {
            BestWeekResult(0, null, null)
        } else {
            val parsed = activeDays.mapNotNull {
                try { LocalDate.parse(it.date) to it.steps } catch (_: Exception) { null }
            }
            var maxSum = 0
            var bestStart: LocalDate? = null
            var bestEnd: LocalDate? = null
            for (i in parsed.indices) {
                val endDate = parsed[i].first
                val windowStart = endDate.minusDays(6)
                var sum = 0
                var earliestInWindow = endDate
                for (j in i downTo 0) {
                    val d = parsed[j].first
                    if (d.isBefore(windowStart)) break
                    sum += parsed[j].second
                    earliestInWindow = d
                }
                if (sum > maxSum) {
                    maxSum = sum
                    bestStart = earliestInWindow
                    bestEnd = endDate
                }
            }
            BestWeekResult(
                steps = maxSum,
                startIso = bestStart?.format(DateTimeFormatter.ISO_LOCAL_DATE),
                endIso = bestEnd?.format(DateTimeFormatter.ISO_LOCAL_DATE)
            )
        }
    }

    val bestMonth = remember(sortedDaily) {
        sortedDaily.groupBy { it.date.take(7) }
            .mapValues { entry -> entry.value.sumOf { it.steps } }
            .maxByOrNull { it.value }
    }

    val longestStreak = remember(sortedDaily, settings.dailyGoal) {
        var best = 0
        var current = 0
        var prev: LocalDate? = null
        sortedDaily.forEach { day ->
            val d = try { LocalDate.parse(day.date) } catch (_: Exception) { null } ?: return@forEach
            if (day.steps >= settings.dailyGoal) {
                current = if (prev != null && prev!!.plusDays(1) == d) current + 1 else 1
                if (current > best) best = current
                prev = d
            } else {
                current = 0
                prev = null
            }
        }
        best
    }

    // Exact date each cumulative milestone was unlocked
    val unlockedMilestones = remember(sortedDaily) {
        val thresholds = listOf(
            10_000 to "10k",
            50_000 to "50k",
            100_000 to "100k",
            500_000 to "500k",
            1_000_000 to "1M"
        )
        val result = mutableListOf<UnlockedMilestone>()
        var running = 0
        var nextIdx = 0
        for (day in sortedDaily) {
            running += day.steps
            while (nextIdx < thresholds.size && running >= thresholds[nextIdx].first) {
                result.add(
                    UnlockedMilestone(
                        threshold = thresholds[nextIdx].first,
                        label = thresholds[nextIdx].second,
                        unlockedDateIso = day.date
                    )
                )
                nextIdx++
            }
        }
        result
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(StepsMoreColors.BgPrimary)
            .navigationBarsPadding()
    ) {
        SubScreenTopBar(title = "Achievements", onBack = onBack)

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 14.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Records",
                fontSize = 22.sp,
                fontWeight = FontWeight.Medium,
                color = StepsMoreColors.TextPrimary
            )

            Spacer(modifier = Modifier.height(16.dp))

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .background(StepsMoreColors.SurfaceCard)
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("Top 3 days", fontSize = 16.sp, color = StepsMoreColors.TextPrimary)
                Spacer(modifier = Modifier.height(14.dp))
                Row(modifier = Modifier.fillMaxWidth()) {
                    for (i in 0 until 3) {
                        val item = top3.getOrNull(i)
                        Column(
                            modifier = Modifier.weight(1f),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            if (item != null) {
                                Text(
                                    text = "${item.steps} • ${StepsRepository.formatDistance(item.steps, settings)}",
                                    fontSize = 12.sp,
                                    color = StepsMoreColors.TextSecondary,
                                    textAlign = TextAlign.Center
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = StepsRepository.formatDate(item.date, settings.dateFormat),
                                    fontSize = 12.sp,
                                    color = StepsMoreColors.TextMuted
                                )
                            } else {
                                Text(
                                    text = "No data available",
                                    fontSize = 12.sp,
                                    color = StepsMoreColors.TextMuted,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            val bestWeekSub = if (bestWeek.startIso != null && bestWeek.endIso != null) {
                if (bestWeek.startIso == bestWeek.endIso) {
                    StepsRepository.formatDate(bestWeek.startIso, settings.dateFormat)
                } else {
                    "${StepsRepository.formatDate(bestWeek.startIso, settings.dateFormat)} — ${StepsRepository.formatDate(bestWeek.endIso, settings.dateFormat)}"
                }
            } else {
                "No data"
            }

            RecordRowCard(
                label = "Best week",
                primary = "${numFmt.format(bestWeek.steps)} • ${StepsRepository.formatDistance(bestWeek.steps, settings)}",
                secondary = bestWeekSub
            )

            Spacer(modifier = Modifier.height(10.dp))

            val monthLabel = bestMonth?.key?.let {
                try {
                    YearMonth.parse(it).format(DateTimeFormatter.ofPattern("MMMM yyyy", Locale.US))
                } catch (_: Exception) { it }
            } ?: "No data"

            RecordRowCard(
                label = "Most active month",
                primary = "${numFmt.format(bestMonth?.value ?: 0)} • ${StepsRepository.formatDistance(bestMonth?.value ?: 0, settings)}",
                secondary = monthLabel
            )

            Spacer(modifier = Modifier.height(10.dp))

            RecordRowCard(
                label = "Average per day",
                primary = "$avgPerDay • ${StepsRepository.formatDistance(avgPerDay, settings)}",
                secondary = firstDate?.let { "Since ${StepsRepository.formatDate(it, settings.dateFormat)}" } ?: "No data"
            )

            Spacer(modifier = Modifier.height(10.dp))

            RecordRowCard(
                label = "Longest ${numFmt.format(settings.dailyGoal)}+\nsteps streak",
                primary = "$longestStreak days",
                secondary = null
            )

            Spacer(modifier = Modifier.height(22.dp))
            HorizontalDivider(
                modifier = Modifier.padding(horizontal = 10.dp),
                thickness = 1.dp,
                color = StepsMoreColors.DividerMajor
            )
            Spacer(modifier = Modifier.height(22.dp))

            Text(
                text = "Milestones",
                fontSize = 22.sp,
                fontWeight = FontWeight.Medium,
                color = StepsMoreColors.TextPrimary
            )

            Spacer(modifier = Modifier.height(18.dp))

            if (unlockedMilestones.isEmpty()) {
                Text(
                    text = "Walk 10,000 total steps to unlock your first milestone.",
                    fontSize = 13.sp,
                    color = StepsMoreColors.TextMuted,
                    textAlign = TextAlign.Center
                )
            } else {
                unlockedMilestones.forEach { m ->
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 10.dp, horizontal = 16.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.CenterStart)
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(StepsMoreColors.SurfaceBadge),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.TrackChanges,
                                contentDescription = null,
                                tint = Color(0xFFD84339),
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Column(
                            modifier = Modifier.align(Alignment.Center),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "${m.label} • ${StepsRepository.formatDistance(m.threshold, settings)}",
                                fontSize = 16.sp,
                                color = StepsMoreColors.TextPrimary
                            )
                            Text(
                                text = StepsRepository.formatDate(m.unlockedDateIso, settings.dateFormat),
                                fontSize = 13.sp,
                                color = StepsMoreColors.TextMuted
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun RecordRowCard(label: String, primary: String, secondary: String?) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(StepsMoreColors.SurfaceCard)
            .padding(horizontal = 18.dp, vertical = 18.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            fontSize = 15.sp,
            color = StepsMoreColors.TextPrimary,
            modifier = Modifier.weight(1f)
        )
        Column(horizontalAlignment = Alignment.End) {
            Text(text = primary, fontSize = 14.sp, color = StepsMoreColors.TextSecondary)
            if (secondary != null) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(text = secondary, fontSize = 13.sp, color = StepsMoreColors.TextMuted)
            }
        }
    }
}
