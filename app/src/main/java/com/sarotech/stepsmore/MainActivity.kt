package com.sarotech.stepsmore

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.sarotech.stepsmore.service.StepsMoreService
import com.sarotech.stepsmore.ui.StepsViewModel
import com.sarotech.stepsmore.ui.screens.AboutScreen
import com.sarotech.stepsmore.ui.screens.AchievementsScreen
import com.sarotech.stepsmore.ui.screens.BackupScreen
import com.sarotech.stepsmore.ui.screens.DailyGoalScreen
import com.sarotech.stepsmore.ui.screens.HomeScreen
import com.sarotech.stepsmore.ui.screens.SettingsScreen
import com.sarotech.stepsmore.ui.theme.StepsMoreTheme

class MainActivity : ComponentActivity() {

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) {
        if (hasActivityPermission()) {
            StepsMoreService.start(this, flush = true)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(android.graphics.Color.BLACK),
            navigationBarStyle = SystemBarStyle.dark(android.graphics.Color.BLACK)
        )

        requestRequiredPermissions()

        val app = application as StepsMoreApp
        val repo = app.repository
        setContent {
            val vm: StepsViewModel = viewModel(factory = StepsViewModel.Factory(repo))
            val settings by vm.settings.collectAsStateWithLifecycle()
            StepsMoreTheme(themeMode = settings.themeMode, accentColorHex = settings.accentColorHex) {
                StepsMoreNavHost(vm)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        if (hasActivityPermission()) {
            StepsMoreService.start(this, flush = true)
        }
    }

    private fun hasActivityPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.ACTIVITY_RECOGNITION
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun requestRequiredPermissions() {
        val needed = mutableListOf<String>()
        if (!hasActivityPermission()) {
            needed.add(Manifest.permission.ACTIVITY_RECOGNITION)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
            != PackageManager.PERMISSION_GRANTED
        ) {
            needed.add(Manifest.permission.POST_NOTIFICATIONS)
        }

        if (needed.isNotEmpty()) {
            permissionLauncher.launch(needed.toTypedArray())
        } else {
            StepsMoreService.start(this, flush = true)
        }
    }
}

@Composable
private fun StepsMoreNavHost(viewModel: StepsViewModel) {
    val navController = rememberNavController()
    NavHost(navController = navController, startDestination = "home") {
        composable("home") {
            HomeScreen(
                viewModel = viewModel,
                onNavigateAchievements = { navController.navigate("achievements") },
                onNavigateBackup = { navController.navigate("backup") },
                onNavigateDailyGoal = { navController.navigate("daily_goal") },
                onNavigateSettings = { navController.navigate("settings") }
            )
        }
        composable("achievements") {
            AchievementsScreen(viewModel = viewModel, onBack = { navController.popBackStack() })
        }
        composable("backup") {
            BackupScreen(viewModel = viewModel, onBack = { navController.popBackStack() })
        }
        composable("daily_goal") {
            DailyGoalScreen(viewModel = viewModel, onBack = { navController.popBackStack() })
        }
        composable("settings") {
            SettingsScreen(
                viewModel = viewModel,
                onNavigateAbout = { navController.navigate("about") },
                onBack = { navController.popBackStack() }
            )
        }
        composable("about") {
            AboutScreen(onBack = { navController.popBackStack() })
        }
    }
}
