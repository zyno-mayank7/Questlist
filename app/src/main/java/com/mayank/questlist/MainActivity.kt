package com.mayank.questlist

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.mayank.questlist.data.AppDatabase
import com.mayank.questlist.data.TaskRepository
import com.mayank.questlist.scheduler.AlarmScheduler
import com.mayank.questlist.scheduler.NotificationHelper
import com.mayank.questlist.ui.edit.EditTaskScreen
import com.mayank.questlist.ui.edit.EditTaskViewModel
import com.mayank.questlist.ui.history.HistoryScreen
import com.mayank.questlist.ui.home.HomeScreen
import com.mayank.questlist.ui.home.HomeViewModel
import com.mayank.questlist.ui.onboarding.PermissionScreen
import com.mayank.questlist.ui.onboarding.PermissionViewModel
import com.mayank.questlist.ui.settings.SettingsScreen
import com.mayank.questlist.ui.theme.QuestListTheme
import com.mayank.questlist.util.PermissionUtils
import com.mayank.questlist.util.PreferencesManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            QuestListApp()
        }
    }
}

@Composable
fun QuestListApp() {
    val context = LocalContext.current
    val repository = remember {
        val db = AppDatabase.getDatabase(context.applicationContext)
        TaskRepository(db.taskDao(), db.playerStatsDao())
    }
    val preferencesManager = remember {
        PreferencesManager(context.applicationContext)
    }

    LaunchedEffect(Unit) {
        NotificationHelper.createNotificationChannel(context.applicationContext)
        withContext(Dispatchers.IO) {
            val scheduler = AlarmScheduler(context.applicationContext)
            scheduler.scheduleMidnightAlarm()
            scheduler.rescheduleAll()
        }
    }

    val isFirstLaunchCompleted = remember { preferencesManager.isFirstLaunchCompleted() }
    val hasOverlay = remember { PermissionUtils.hasOverlayPermission(context) }

    val startDestination = if (!isFirstLaunchCompleted || !hasOverlay) {
        "onboarding"
    } else {
        "home"
    }

    QuestListTheme {
        val navController = rememberNavController()

        NavHost(
            navController = navController,
            startDestination = startDestination,
            enterTransition = {
                fadeIn(animationSpec = tween(200)) + slideInHorizontally(
                    initialOffsetX = { 300 },
                    animationSpec = tween(200)
                )
            },
            exitTransition = {
                fadeOut(animationSpec = tween(200)) + slideOutHorizontally(
                    targetOffsetX = { -300 },
                    animationSpec = tween(200)
                )
            },
            popEnterTransition = {
                fadeIn(animationSpec = tween(200)) + slideInHorizontally(
                    initialOffsetX = { -300 },
                    animationSpec = tween(200)
                )
            },
            popExitTransition = {
                fadeOut(animationSpec = tween(200)) + slideOutHorizontally(
                    targetOffsetX = { 300 },
                    animationSpec = tween(200)
                )
            }
        ) {
            composable("onboarding") {
                val permissionViewModel: PermissionViewModel = viewModel(
                    factory = PermissionViewModel.Factory(preferencesManager)
                )
                PermissionScreen(
                    viewModel = permissionViewModel,
                    onGetStarted = {
                        navController.navigate("home") {
                            popUpTo("onboarding") { inclusive = true }
                        }
                    }
                )
            }

            composable("home") {
                val homeViewModel: HomeViewModel = viewModel(
                    factory = HomeViewModel.Factory(repository)
                )
                HomeScreen(
                    viewModel = homeViewModel,
                    repository = repository,
                    onAddTaskClick = {
                        navController.navigate("edit_task/-1")
                    },
                    onEditTaskClick = { taskId ->
                        navController.navigate("edit_task/$taskId")
                    },
                    onFixOverlayClick = {
                        navController.navigate("onboarding")
                    },
                    onNavigateHistory = {
                        navController.navigate("history")
                    },
                    onNavigateSettings = {
                        navController.navigate("settings")
                    }
                )
            }

            composable("history") {
                HistoryScreen(
                    repository = repository,
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            composable("settings") {
                SettingsScreen(
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            composable(
                route = "edit_task/{taskId}",
                arguments = listOf(
                    navArgument("taskId") { type = NavType.LongType }
                )
            ) { backStackEntry ->
                val taskId = backStackEntry.arguments?.getLong("taskId") ?: -1L
                val editViewModel: EditTaskViewModel = viewModel(
                    factory = EditTaskViewModel.Factory(repository, taskId)
                )
                EditTaskScreen(
                    viewModel = editViewModel,
                    onNavigateBack = {
                        navController.popBackStack()
                    }
                )
            }
        }
    }
}