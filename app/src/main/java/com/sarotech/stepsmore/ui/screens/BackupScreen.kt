package com.sarotech.stepsmore.ui.screens

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.sarotech.stepsmore.ui.StepsViewModel
import com.sarotech.stepsmore.ui.components.PreferenceGroupHeader
import com.sarotech.stepsmore.ui.components.PreferenceRow
import com.sarotech.stepsmore.ui.components.SubScreenTopBar
import com.sarotech.stepsmore.ui.theme.StepsMoreColors
import kotlinx.coroutines.launch

@Composable
fun BackupScreen(
    viewModel: StepsViewModel,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var backupLocationLabel by remember { mutableStateOf("Not set") }

    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri != null) {
            scope.launch {
                try {
                    val json = viewModel.exportBackupJson()
                    context.contentResolver.openOutputStream(uri)?.use { it.write(json.toByteArray()) }
                    backupLocationLabel = uri.lastPathSegment ?: "Selected"
                    Toast.makeText(context, "Backup exported successfully", Toast.LENGTH_SHORT).show()
                } catch (e: Exception) {
                    Toast.makeText(context, "Export failed: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            scope.launch {
                try {
                    val json = context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
                    if (json != null) {
                        val count = viewModel.importBackupJson(json)
                        Toast.makeText(context, "Restored $count days of step data", Toast.LENGTH_SHORT).show()
                    }
                } catch (e: Exception) {
                    Toast.makeText(context, "Import failed: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(StepsMoreColors.BgPrimary)
            .navigationBarsPadding()
    ) {
        SubScreenTopBar(title = "Backup", onBack = onBack)

        PreferenceGroupHeader("Import")
        PreferenceRow(
            icon = Icons.Outlined.UploadFile,
            title = "Import",
            subtitle = "Restore step data from a file",
            showBottomDivider = false,
            onClick = { importLauncher.launch(arrayOf("application/json", "*/*")) }
        )

        PreferenceGroupHeader("Export")
        PreferenceRow(
            icon = Icons.Outlined.EventRepeat,
            title = "Automatic backup frequency",
            subtitle = "Disabled",
            enabled = false,
            showBottomDivider = true
        )
        PreferenceRow(
            icon = Icons.Outlined.History,
            title = "Number of backups to keep",
            subtitle = "5",
            enabled = false,
            showBottomDivider = true
        )
        PreferenceRow(
            icon = Icons.Outlined.FolderOpen,
            title = "Backup location",
            subtitle = backupLocationLabel,
            showBottomDivider = true,
            onClick = { exportLauncher.launch("stepsmore_backup.json") }
        )
        PreferenceRow(
            icon = Icons.Outlined.CloudDownload,
            title = "Manual backup",
            subtitle = null,
            showBottomDivider = false,
            onClick = { exportLauncher.launch("stepsmore_backup.json") }
        )
    }
}
