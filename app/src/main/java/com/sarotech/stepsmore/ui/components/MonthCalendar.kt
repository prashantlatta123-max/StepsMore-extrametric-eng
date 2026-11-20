package com.sarotech.stepsmore.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sarotech.stepsmore.ui.theme.StepsMoreColors
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun MonthCalendar(
    yearMonth: YearMonth,
    firstDayOfWeek: String,
    dailyStepsMap: Map<String, Int>,
    dailyGoal: Int,
    selectedDateIso: String?,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    canGoNext: Boolean,
    onDateClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val startsMonday = firstDayOfWeek == "Monday"
    val headers = if (startsMonday) {
        listOf("M", "T", "W", "T", "F", "S", "S")
    } else {
        listOf("S", "M", "T", "W", "T", "F", "S")
    }

    val firstOfMonth = yearMonth.atDay(1)
    val leadingBlanks = if (startsMonday) {
        firstOfMonth.dayOfWeek.value - 1
    } else {
        if (firstOfMonth.dayOfWeek == DayOfWeek.SUNDAY) 0 else firstOfMonth.dayOfWeek.value
    }

    val daysInMonth = yearMonth.lengthOfMonth()
    val totalCells = leadingBlanks + daysInMonth
    val rows = (totalCells + 6) / 7
    val today = LocalDate.now()

    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            IconButton(onClick = onPreviousMonth, modifier = Modifier.size(36.dp)) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                    contentDescription = "Previous month",
                    tint = StepsMoreColors.TextMuted
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = yearMonth.format(DateTimeFormatter.ofPattern("MMMM yyyy", Locale.US)),
                fontSize = 17.sp,
                color = StepsMoreColors.TextPrimary
            )
            Spacer(modifier = Modifier.width(12.dp))
            IconButton(
                onClick = onNextMonth,
                enabled = canGoNext,
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = "Next month",
                    tint = if (canGoNext) StepsMoreColors.TextMuted else StepsMoreColors.TextDisabledSub
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        Row(modifier = Modifier.fillMaxWidth()) {
            headers.forEach { h ->
                Text(
                    text = h,
                    fontSize = 14.sp,
                    color = StepsMoreColors.ChartBarPast,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        for (r in 0 until rows) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp)
            ) {
                for (c in 0 until 7) {
                    val cellIndex = r * 7 + c
                    val dayNum = cellIndex - leadingBlanks + 1
                    if (dayNum in 1..daysInMonth) {
                        val date = yearMonth.atDay(dayNum)
                        val iso = date.format(DateTimeFormatter.ISO_LOCAL_DATE)
                        val steps = dailyStepsMap[iso] ?: 0
                        val isSelected = selectedDateIso == iso
                        val isToday = date == today
                        val goalMet = steps >= dailyGoal && dailyGoal > 0

                        val textColor = when {
                            isSelected -> StepsMoreColors.BgPrimary
                            goalMet || isToday -> StepsMoreColors.TextPrimary
                            steps > 0 -> StepsMoreColors.TextSecondary
                            else -> StepsMoreColors.TextCalendarInactive
                        }

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(36.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(
                                        when {
                                            isSelected -> StepsMoreColors.TextPrimary
                                            goalMet -> StepsMoreColors.SurfaceFab
                                            else -> StepsMoreColors.BgPrimary
                                        }
                                    )
                                    .clickable { onDateClick(iso) },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = dayNum.toString(),
                                    fontSize = 14.sp,
                                    color = textColor
                                )
                            }
                        }
                    } else {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }
    }
}
