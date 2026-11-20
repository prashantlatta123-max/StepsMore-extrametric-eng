package com.sarotech.stepsmore.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sarotech.stepsmore.ui.theme.StepsMoreColors

@Composable
fun SubScreenTopBar(title: String, onBack: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(StepsMoreColors.BgPrimary)
            .statusBarsPadding()
            .height(56.dp)
            .padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onBack) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Back",
                tint = StepsMoreColors.TextSecondary
            )
        }
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = title,
            fontSize = 21.sp,
            color = StepsMoreColors.TextPrimary
        )
    }
}

@Composable
fun PreferenceGroupHeader(title: String) {
    Text(
        text = title,
        fontSize = 13.sp,
        color = StepsMoreColors.TextMuted,
        modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 20.dp, bottom = 8.dp)
    )
}

@Composable
fun PreferenceRow(
    icon: ImageVector,
    title: String,
    subtitle: String? = null,
    enabled: Boolean = true,
    showChevron: Boolean = true,
    showBottomDivider: Boolean = true,
    onClick: (() -> Unit)? = null
) {
    val titleColor = if (enabled) StepsMoreColors.TextPrimary else StepsMoreColors.TextDisabled
    val subColor = if (enabled) StepsMoreColors.TextMuted else StepsMoreColors.TextDisabledSub
    val iconColor = if (enabled) StepsMoreColors.TextPrimary else StepsMoreColors.TextDisabled

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (enabled && onClick != null) Modifier.clickable { onClick() } else Modifier
            )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconColor,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(24.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(text = title, fontSize = 16.sp, color = titleColor)
                if (!subtitle.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(text = subtitle, fontSize = 13.sp, color = subColor)
                }
            }
            if (showChevron) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = null,
                    tint = if (enabled) StepsMoreColors.TextMuted else StepsMoreColors.TextDisabledSub,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
        if (showBottomDivider) {
            HorizontalDivider(
                modifier = Modifier.padding(start = 64.dp, end = 16.dp),
                thickness = 1.dp,
                color = StepsMoreColors.DividerList
            )
        }
    }
}

@Composable
fun MonochromeSwitch(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    enabled: Boolean = true
) {
    Switch(
        checked = checked,
        onCheckedChange = onCheckedChange,
        enabled = enabled,
        thumbContent = if (checked) {
            {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    tint = StepsMoreColors.SwitchTrackOn,
                    modifier = Modifier.size(14.dp)
                )
            }
        } else null,
        colors = SwitchDefaults.colors(
            checkedThumbColor = StepsMoreColors.SwitchThumbActive,
            checkedTrackColor = StepsMoreColors.SwitchTrackOn,
            checkedBorderColor = Color.Transparent,
            uncheckedThumbColor = StepsMoreColors.SwitchThumbActive,
            uncheckedTrackColor = StepsMoreColors.SwitchTrackOff,
            uncheckedBorderColor = StepsMoreColors.SwitchBorderOff,
            disabledUncheckedThumbColor = StepsMoreColors.SwitchThumbDisabled,
            disabledUncheckedTrackColor = StepsMoreColors.SwitchTrackDisabled,
            disabledUncheckedBorderColor = StepsMoreColors.SwitchBorderDisabled
        )
    )
}

@Composable
fun NumberInputDialog(
    title: String,
    initialValue: String,
    suffix: String,
    isDecimal: Boolean = false,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var text by remember { mutableStateOf(initialValue) }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = StepsMoreColors.SurfaceCard,
        shape = RoundedCornerShape(18.dp),
        title = { Text(title, color = StepsMoreColors.TextPrimary, fontSize = 18.sp) },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                singleLine = true,
                suffix = { Text(suffix, color = StepsMoreColors.TextMuted) },
                keyboardOptions = KeyboardOptions(
                    keyboardType = if (isDecimal) KeyboardType.Decimal else KeyboardType.Number
                ),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = StepsMoreColors.TextPrimary,
                    unfocusedBorderColor = StepsMoreColors.InputOutline,
                    focusedTextColor = StepsMoreColors.TextPrimary,
                    unfocusedTextColor = StepsMoreColors.TextPrimary
                )
            )
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(text.trim()) }) {
                Text("Save", color = StepsMoreColors.TextPrimary)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = StepsMoreColors.TextMuted)
            }
        }
    )
}

@Composable
fun ChoiceDialog(
    title: String,
    options: List<String>,
    selected: String,
    onDismiss: () -> Unit,
    onSelect: (String) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = StepsMoreColors.SurfaceCard,
        shape = RoundedCornerShape(18.dp),
        title = { Text(title, color = StepsMoreColors.TextPrimary, fontSize = 18.sp) },
        text = {
            Column {
                options.forEach { option ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                onSelect(option)
                                onDismiss()
                            }
                            .padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = option == selected,
                            onClick = {
                                onSelect(option)
                                onDismiss()
                            },
                            colors = RadioButtonDefaults.colors(
                                selectedColor = StepsMoreColors.TextPrimary,
                                unselectedColor = StepsMoreColors.TextMuted
                            )
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(option, color = StepsMoreColors.TextPrimary, fontSize = 15.sp)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close", color = StepsMoreColors.TextMuted)
            }
        }
    )
}
