package com.sarotech.stepsmore.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sarotech.stepsmore.data.StepsRepository
import com.sarotech.stepsmore.data.db.HourlyStepsEntity
import com.sarotech.stepsmore.data.prefs.UserSettings
import com.sarotech.stepsmore.ui.theme.StepsMoreColors
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun HourlyBreakdownCard(
    dateIso: String,
    totalSteps: Int,
    hourlyData: List<HourlyStepsEntity>,
    settings: UserSettings,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    val hourMap = remember(hourlyData) { hourlyData.associate { it.hour to it.steps } }
    val activeHours = remember(hourlyData) { hourlyData.filter { it.steps > 0 }.sortedBy { it.hour } }
    val peakHour = remember(activeHours) { activeHours.maxByOrNull { it.steps }?.hour }
    var selectedHour by remember(dateIso) { mutableStateOf(peakHour) }
    var showAllSlots by remember(dateIso) { mutableStateOf(false) }

    val formattedHeader = remember(dateIso) {
        try {
            val d = LocalDate.parse(dateIso, DateTimeFormatter.ISO_LOCAL_DATE)
            d.format(DateTimeFormatter.ofPattern("EEE, dd MMM", Locale.US))
        } catch (_: Exception) {
            dateIso
        }
    }

    // Day-parts aggregation
    val morningSteps = (5..11).sumOf { hourMap[it] ?: 0 }
    val afternoonSteps = (12..16).sumOf { hourMap[it] ?: 0 }
    val eveningSteps = (17..20).sumOf { hourMap[it] ?: 0 }
    val nightSteps = ((21..23) + (0..4)).sumOf { hourMap[it] ?: 0 }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(StepsMoreColors.SurfaceCard)
            .padding(16.dp)
    ) {
        // Header row
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "$formattedHeader  •  $totalSteps steps",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium,
                    color = StepsMoreColors.TextPrimary
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "${StepsRepository.formatDistance(totalSteps, settings)}  •  ${StepsRepository.calculateCalories(totalSteps, settings)} kcal",
                    fontSize = 12.sp,
                    color = StepsMoreColors.TextMuted
                )
            }
            IconButton(
                onClick = onClose,
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Close hourly breakdown",
                    tint = StepsMoreColors.TextMuted,
                    modifier = Modifier.size(18.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        if (activeHours.isEmpty()) {
            Text(
                text = if (totalSteps > 0) {
                    "Hourly breakdown not available for this day."
                } else {
                    "No steps recorded on this day."
                },
                fontSize = 13.sp,
                color = StepsMoreColors.TextMuted,
                modifier = Modifier.padding(vertical = 12.dp)
            )
        } else {
            // Selected hour callout
            val displayHour = selectedHour ?: peakHour
            if (displayHour != null) {
                val hSteps = hourMap[displayHour] ?: 0
                val label = String.format(Locale.US, "%02d:00 – %02d:00", displayHour, (displayHour + 1) % 24)
                Text(
                    text = "$label  •  $hSteps steps",
                    fontSize = 12.sp,
                    color = StepsMoreColors.TextSecondary
                )
                Spacer(modifier = Modifier.height(8.dp))
            }

            // 24-Hour Mini Bar Chart
            val maxHourSteps = (activeHours.maxOfOrNull { it.steps } ?: 1).coerceAtLeast(1)
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(84.dp)
                    .pointerInput(dateIso, hourMap) {
                        detectTapGestures { offset ->
                            val slotW = size.width / 24f
                            val tappedHour = (offset.x / slotW).toInt().coerceIn(0, 23)
                            selectedHour = tappedHour
                        }
                    }
            ) {
                val slotW = size.width / 24f
                val barW = (slotW * 0.68f).coerceAtLeast(2.dp.toPx())
                val bottomY = size.height - 2.dp.toPx()
                val usableH = bottomY - 4.dp.toPx()

                for (h in 0..23) {
                    val steps = hourMap[h] ?: 0
                    val left = h * slotW + (slotW - barW) / 2f
                    if (steps > 0) {
                        val hRatio = steps.toFloat() / maxHourSteps.toFloat()
                        val barH = (usableH * hRatio).coerceAtLeast(3.dp.toPx())
                        val color = when {
                            selectedHour == h -> StepsMoreColors.ChartBarSelected
                            peakHour == h -> StepsMoreColors.ChartBarPast
                            else -> StepsMoreColors.ChartBarToday
                        }
                        drawRect(
                            color = color,
                            topLeft = Offset(left, bottomY - barH),
                            size = Size(barW, barH)
                        )
                    } else {
                        drawRect(
                            color = StepsMoreColors.DividerList,
                            topLeft = Offset(left, bottomY - 2.dp.toPx()),
                            size = Size(barW, 2.dp.toPx())
                        )
                    }
                }

                drawLine(
                    color = StepsMoreColors.DividerMajor,
                    start = Offset(0f, bottomY),
                    end = Offset(size.width, bottomY),
                    strokeWidth = 1.dp.toPx()
                )
            }

            // Time axis labels
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                listOf("12a", "4a", "8a", "12p", "4p", "8p").forEach { lbl ->
                    Text(text = lbl, fontSize = 10.sp, color = StepsMoreColors.TextMuted)
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 4 Day-Parts Summary Strip
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                DayPartItem("Morning", morningSteps)
                DayPartItem("Afternoon", afternoonSteps)
                DayPartItem("Evening", eveningSteps)
                DayPartItem("Night", nightSteps)
            }

            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider(thickness = 1.dp, color = StepsMoreColors.DividerList)
            Spacer(modifier = Modifier.height(8.dp))

            // Active Time Slots List (Top 4 or all)
            val visibleSlots = if (showAllSlots) activeHours else activeHours.sortedByDescending { it.steps }.take(4).sortedBy { it.hour }
            visibleSlots.forEach { slot ->
                val range = String.format(Locale.US, "%02d:00 – %02d:00", slot.hour, (slot.hour + 1) % 24)
                val dist = StepsRepository.formatDistance(slot.steps, settings)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { selectedHour = slot.hour }
                        .padding(vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = range,
                        fontSize = 13.sp,
                        color = if (selectedHour == slot.hour) StepsMoreColors.TextPrimary else StepsMoreColors.TextSecondary
                    )
                    Text(
                        text = "${slot.steps} steps  •  $dist",
                        fontSize = 13.sp,
                        color = StepsMoreColors.TextMuted
                    )
                }
            }

            if (activeHours.size > 4) {
                Text(
                    text = if (showAllSlots) "Show less" else "Show all ${activeHours.size} active hours",
                    fontSize = 12.sp,
                    color = StepsMoreColors.TextSecondary,
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .clickable { showAllSlots = !showAllSlots }
                        .padding(top = 8.dp, bottom = 2.dp)
                )
            }
        }
    }
}

@Composable
private fun DayPartItem(label: String, steps: Int) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = label, fontSize = 11.sp, color = StepsMoreColors.TextMuted)
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = steps.toString(),
            fontSize = 13.sp,
            color = if (steps > 0) StepsMoreColors.TextPrimary else StepsMoreColors.TextDisabled
        )
    }
}
