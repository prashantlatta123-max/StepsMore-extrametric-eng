package com.sarotech.stepsmore.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sarotech.stepsmore.ui.StepsViewModel
import com.sarotech.stepsmore.ui.components.MonochromeSwitch
import com.sarotech.stepsmore.ui.components.SubScreenTopBar
import com.sarotech.stepsmore.ui.theme.StepsMoreColors

@Composable
fun DailyGoalScreen(
    viewModel: StepsViewModel,
    onBack: () -> Unit
) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    var goalText by remember(settings.dailyGoal) { mutableStateOf(settings.dailyGoal.toString()) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(StepsMoreColors.BgPrimary)
            .navigationBarsPadding()
    ) {
        SubScreenTopBar(title = "Daily Goal", onBack = onBack)

        Spacer(modifier = Modifier.height(14.dp))

        OutlinedTextField(
            value = goalText,
            onValueChange = { input ->
                val digits = input.filter { it.isDigit() }
                goalText = digits
                digits.toIntOrNull()?.let { if (it >= 500) viewModel.updateDailyGoal(it) }
            },
            label = { Text("Daily step goal", color = StepsMoreColors.TextSecondary, fontSize = 13.sp) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            shape = RoundedCornerShape(8.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = StepsMoreColors.TextPrimary,
                unfocusedBorderColor = StepsMoreColors.InputOutline,
                focusedTextColor = StepsMoreColors.TextPrimary,
                unfocusedTextColor = StepsMoreColors.TextPrimary,
                focusedContainerColor = StepsMoreColors.BgPrimary,
                unfocusedContainerColor = StepsMoreColors.BgPrimary
            ),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp)
        )

        Spacer(modifier = Modifier.height(18.dp))

        GoalToggleRow(
            title = "Notify when reached",
            checked = settings.notifyWhenReached,
            onCheckedChange = { viewModel.setNotifyWhenReached(it) }
        )
        GoalToggleRow(
            title = "Show progress in live\nnotification",
            checked = settings.showProgressInNotification,
            onCheckedChange = { viewModel.setShowProgressInNotification(it) }
        )
        GoalToggleRow(
            title = "Encouraging notifications",
            checked = settings.encouragingNotifications,
            onCheckedChange = { viewModel.setEncouragingNotifications(it) }
        )
        GoalToggleRow(
            title = "Draw step target line on chart",
            checked = settings.drawTargetLine,
            showDivider = false,
            onCheckedChange = { viewModel.setDrawTargetLine(it) }
        )
    }
}

@Composable
private fun GoalToggleRow(
    title: String,
    checked: Boolean,
    showDivider: Boolean = true,
    onCheckedChange: (Boolean) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title,
                fontSize = 16.sp,
                color = StepsMoreColors.TextPrimary,
                modifier = Modifier.weight(1f)
            )
            MonochromeSwitch(checked = checked, onCheckedChange = onCheckedChange)
        }
        if (showDivider) {
            HorizontalDivider(
                modifier = Modifier.padding(horizontal = 16.dp),
                thickness = 1.dp,
                color = StepsMoreColors.DividerList
            )
        }
    }
}
