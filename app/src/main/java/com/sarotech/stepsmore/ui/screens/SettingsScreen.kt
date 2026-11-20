package com.sarotech.stepsmore.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material.icons.outlined.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sarotech.stepsmore.BuildConfig
import com.sarotech.stepsmore.ui.StepsViewModel
import com.sarotech.stepsmore.ui.components.ChoiceDialog
import com.sarotech.stepsmore.ui.components.NumberInputDialog
import com.sarotech.stepsmore.ui.components.PreferenceGroupHeader
import com.sarotech.stepsmore.ui.components.PreferenceRow
import com.sarotech.stepsmore.ui.components.SubScreenTopBar
import com.sarotech.stepsmore.ui.theme.StepsMoreColors
import com.sarotech.stepsmore.util.BatteryOptimizationHelper
import java.util.Locale

@Composable
fun SettingsScreen(
    viewModel: StepsViewModel,
    onNavigateAbout: () -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val batteryUnrestricted = BatteryOptimizationHelper.isIgnoringBatteryOptimizations(context)

    var showHeightDialog by remember { mutableStateOf(false) }
    var showStepLengthDialog by remember { mutableStateOf(false) }
    var showWeightDialog by remember { mutableStateOf(false) }
    var showSensorDialog by remember { mutableStateOf(false) }
    var showUnitDialog by remember { mutableStateOf(false) }
    var showDateFormatDialog by remember { mutableStateOf(false) }
    var showFirstDayDialog by remember { mutableStateOf(false) }
    var showThemeDialog by remember { mutableStateOf(false) }
    var showAccentDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(StepsMoreColors.BgPrimary)
            .navigationBarsPadding()
    ) {
        SubScreenTopBar(title = "Settings", onBack = onBack)

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(bottom = 24.dp)
        ) {
            PreferenceGroupHeader("Personal data")
            PreferenceRow(
                icon = Icons.Outlined.AccessibilityNew,
                title = "Height",
                subtitle = "${settings.heightCm} cm",
                showChevron = false,
                onClick = { showHeightDialog = true }
            )
            PreferenceRow(
                icon = Icons.Outlined.DirectionsWalk,
                title = "Step length",
                subtitle = String.format(Locale.US, "%.2f cm", settings.stepLengthCm),
                showChevron = false,
                onClick = { showStepLengthDialog = true }
            )
            PreferenceRow(
                icon = Icons.Outlined.MonitorWeight,
                title = "Weight",
                subtitle = "${settings.weightKg} kg",
                showChevron = false,
                showBottomDivider = false,
                onClick = { showWeightDialog = true }
            )

            PreferenceGroupHeader("General")
            PreferenceRow(
                icon = Icons.Outlined.Speed,
                title = "Step sensor engine",
                subtitle = settings.sensorMode,
                onClick = { showSensorDialog = true }
            )
            PreferenceRow(
                icon = Icons.Outlined.BatteryChargingFull,
                title = "Battery optimization",
                subtitle = if (batteryUnrestricted) "Unrestricted (Recommended)" else "Restricted — tap to allow background tracking",
                onClick = { BatteryOptimizationHelper.requestIgnoreBatteryOptimizations(context) }
            )
            PreferenceRow(
                icon = Icons.Outlined.Straighten,
                title = "Unit system",
                subtitle = if (settings.unitSystem == "Metric") "Metric (km)" else "Imperial (mi)",
                onClick = { showUnitDialog = true }
            )
            PreferenceRow(
                icon = Icons.Outlined.CalendarMonth,
                title = "Date format",
                subtitle = settings.dateFormat,
                onClick = { showDateFormatDialog = true }
            )
            PreferenceRow(
                icon = Icons.Outlined.Today,
                title = "First day of the week",
                subtitle = settings.firstDayOfWeek,
                showBottomDivider = false,
                onClick = { showFirstDayDialog = true }
            )

            PreferenceGroupHeader("Appearance")
            PreferenceRow(
                icon = Icons.Outlined.Contrast,
                title = "Theme",
                subtitle = settings.themeMode,
                onClick = { showThemeDialog = true }
            )
            PreferenceRow(
                icon = Icons.Outlined.Palette,
                title = "Accent color",
                subtitle = settings.accentColorHex,
                showBottomDivider = false,
                onClick = { showAccentDialog = true }
            )

            PreferenceGroupHeader("App info")
            PreferenceRow(
                icon = Icons.Outlined.Info,
                title = "About",
                subtitle = "StepsMore ${BuildConfig.VERSION_NAME} • SAROTECH",
                showBottomDivider = false,
                onClick = onNavigateAbout
            )
        }
    }

    if (showHeightDialog) {
        NumberInputDialog(
            title = "Height",
            initialValue = settings.heightCm.toString(),
            suffix = "cm",
            onDismiss = { showHeightDialog = false },
            onConfirm = {
                it.toIntOrNull()?.let { h -> viewModel.setHeightCm(h) }
                showHeightDialog = false
            }
        )
    }

    if (showStepLengthDialog) {
        NumberInputDialog(
            title = "Step length",
            initialValue = String.format(Locale.US, "%.2f", settings.stepLengthCm),
            suffix = "cm",
            isDecimal = true,
            onDismiss = { showStepLengthDialog = false },
            onConfirm = {
                it.toFloatOrNull()?.let { s -> viewModel.setStepLengthCm(s) }
                showStepLengthDialog = false
            }
        )
    }

    if (showWeightDialog) {
        NumberInputDialog(
            title = "Weight",
            initialValue = settings.weightKg.toString(),
            suffix = "kg",
            onDismiss = { showWeightDialog = false },
            onConfirm = {
                it.toIntOrNull()?.let { w -> viewModel.setWeightKg(w) }
                showWeightDialog = false
            }
        )
    }

    if (showSensorDialog) {
        ChoiceDialog(
            title = "Step sensor engine",
            options = listOf("Auto (Hardware)", "Accelerometer (Instant)"),
            selected = settings.sensorMode,
            onDismiss = { showSensorDialog = false },
            onSelect = { viewModel.setSensorMode(it) }
        )
    }

    if (showUnitDialog) {
        ChoiceDialog(
            title = "Unit system",
            options = listOf("Metric", "Imperial"),
            selected = settings.unitSystem,
            onDismiss = { showUnitDialog = false },
            onSelect = { viewModel.setUnitSystem(it) }
        )
    }

    if (showDateFormatDialog) {
        ChoiceDialog(
            title = "Date format",
            options = listOf("dd/MM/yyyy", "MM/dd/yyyy", "yyyy-MM-dd"),
            selected = settings.dateFormat,
            onDismiss = { showDateFormatDialog = false },
            onSelect = { viewModel.setDateFormat(it) }
        )
    }

    if (showFirstDayDialog) {
        ChoiceDialog(
            title = "First day of the week",
            options = listOf("Monday", "Sunday"),
            selected = settings.firstDayOfWeek,
            onDismiss = { showFirstDayDialog = false },
            onSelect = { viewModel.setFirstDayOfWeek(it) }
        )
    }

    if (showThemeDialog) {
        ChoiceDialog(
            title = "Choose theme",
            options = listOf("Black", "Snow White", "Glass"),
            selected = if (settings.themeMode == "AMOLED Dark") "Black" else settings.themeMode,
            onDismiss = { showThemeDialog = false },
            onSelect = { viewModel.setThemeMode(it) }
        )
    }

    if (showAccentDialog) {
        AccentColorDialog(
            currentHex = settings.accentColorHex,
            onDismiss = { showAccentDialog = false },
            onSave = { viewModel.setAccentColorHex(it); showAccentDialog = false }
        )
    }

}


@Composable
private fun AccentColorDialog(
    currentHex: String,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit
) {
    val presets = listOf("#7C83FF", "#00BFA6", "#FF6B6B", "#FFB300", "#29B6F6", "#AB7CFF", "#E85D9E", "#8BC34A", "#FFFFFF")
    var hex by remember(currentHex) { mutableStateOf(currentHex) }
    val valid = hex.matches(Regex("^#[0-9A-Fa-f]{6}$"))
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = StepsMoreColors.SurfaceCard,
        title = { Text("Accent color", color = StepsMoreColors.TextPrimary) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Text("Choose a preset or enter your own HEX color", color = StepsMoreColors.TextSecondary)
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    presets.chunked(5).forEach { rowColors ->
                        Row(horizontalArrangement = Arrangement.spacedBy(14.dp), verticalAlignment = Alignment.CenterVertically) {
                            rowColors.forEach { preset ->
                                Box(
                                    modifier = Modifier.size(34.dp).background(Color(android.graphics.Color.parseColor(preset)), CircleShape)
                                        .clickable { hex = preset }
                                )
                            }
                        }
                    }
                }
                OutlinedTextField(
                    value = hex,
                    onValueChange = { hex = it.uppercase().let { v -> if (v.startsWith("#")) v else "#$v" }.take(7) },
                    label = { Text("HEX color") },
                    singleLine = true,
                    isError = !valid,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = StepsMoreColors.TextPrimary,
                        unfocusedTextColor = StepsMoreColors.TextPrimary,
                        focusedBorderColor = StepsMoreColors.Accent,
                        unfocusedBorderColor = StepsMoreColors.InputOutline
                    )
                )
            }
        },
        confirmButton = { TextButton(enabled = valid, onClick = { onSave(hex) }) { Text("Apply", color = StepsMoreColors.Accent) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel", color = StepsMoreColors.TextMuted) } }
    )
}
