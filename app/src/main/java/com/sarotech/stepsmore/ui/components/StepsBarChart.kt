package com.sarotech.stepsmore.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sarotech.stepsmore.ui.theme.StepsMoreColors

data class DayBarItem(
    val dateIso: String,
    val dayLabel: String, // e.g., "Sat", "Sun", ...
    val steps: Int,
    val isToday: Boolean
)

@Composable
fun StepsBarChart(
    items: List<DayBarItem>,
    dailyGoal: Int,
    drawTargetLine: Boolean,
    selectedDateIso: String?,
    onBarClick: (DayBarItem) -> Unit,
    modifier: Modifier = Modifier
) {
    if (items.isEmpty()) return
    val maxVal = maxOf(
        items.maxOfOrNull { it.steps } ?: 0,
        if (drawTargetLine) dailyGoal else 1000,
        1000
    )

    Column(modifier = modifier.fillMaxWidth()) {
        // Bar area + value labels + dashed target line
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(176.dp)
                .pointerInput(items) {
                    detectTapGestures { offset ->
                        val slotWidth = size.width.toFloat() / items.size
                        val index = (offset.x / slotWidth).toInt().coerceIn(0, items.lastIndex)
                        onBarClick(items[index])
                    }
                }
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val chartTop = 22.dp.toPx()
                val chartBottom = size.height - 2.dp.toPx()
                val usableHeight = (chartBottom - chartTop).coerceAtLeast(1f)
                val slotWidth = size.width / items.size
                val barWidth = slotWidth * 0.78f

                // Dashed daily goal target line
                if (drawTargetLine && dailyGoal > 0) {
                    val goalRatio = (dailyGoal.toFloat() / maxVal.toFloat()).coerceIn(0f, 1f)
                    val goalY = chartBottom - (usableHeight * goalRatio)
                    drawLine(
                        color = StepsMoreColors.ChartTargetLine,
                        start = Offset(0f, goalY),
                        end = Offset(size.width, goalY),
                        strokeWidth = 1.5.dp.toPx(),
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(14f, 12f), 0f)
                    )
                }

                // Sharp rectangular bars (0dp corner radius)
                items.forEachIndexed { index, item ->
                    if (item.steps > 0) {
                        val ratio = (item.steps.toFloat() / maxVal.toFloat()).coerceIn(0f, 1f)
                        val barHeight = (usableHeight * ratio).coerceAtLeast(4.dp.toPx())
                        val left = index * slotWidth + (slotWidth - barWidth) / 2f
                        val top = chartBottom - barHeight

                        val barColor = when {
                            selectedDateIso == item.dateIso -> StepsMoreColors.ChartBarSelected
                            selectedDateIso != null -> StepsMoreColors.ChartBarDimmed
                            item.isToday -> StepsMoreColors.ChartBarToday
                            else -> StepsMoreColors.ChartBarPast
                        }

                        drawRect(
                            color = barColor,
                            topLeft = Offset(left, top),
                            size = Size(barWidth, barHeight)
                        )
                    }
                }

                // Solid baseline axis
                drawLine(
                    color = StepsMoreColors.DividerMajor,
                    start = Offset(0f, chartBottom),
                    end = Offset(size.width, chartBottom),
                    strokeWidth = 1.dp.toPx()
                )
            }

            // Step value labels aligned cleanly above each non-zero bar
            Row(
                modifier = Modifier.fillMaxSize(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                items.forEach { item ->
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight(),
                        contentAlignment = Alignment.BottomCenter
                    ) {
                        if (item.steps > 0) {
                            val ratio = (item.steps.toFloat() / maxVal.toFloat()).coerceIn(0f, 1f)
                            val bottomPad = (152f * ratio).dp + 6.dp
                            Text(
                                text = item.steps.toString(),
                                fontSize = 11.sp,
                                color = if (selectedDateIso == item.dateIso) {
                                    StepsMoreColors.TextPrimary
                                } else {
                                    StepsMoreColors.TextMuted
                                },
                                modifier = Modifier.padding(bottom = bottomPad),
                                maxLines = 1
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Weekday labels row
        Row(modifier = Modifier.fillMaxWidth()) {
            items.forEach { item ->
                val isSelected = selectedDateIso == item.dateIso
                Text(
                    text = item.dayLabel,
                    fontSize = 12.sp,
                    color = if (isSelected) StepsMoreColors.TextPrimary else StepsMoreColors.TextMuted,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}
