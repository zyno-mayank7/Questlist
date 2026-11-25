package com.mayank.questlist.overlay

import android.app.KeyguardManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.provider.Settings
import android.util.Log
import android.view.Gravity
import android.view.WindowManager
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.unit.dp
import androidx.core.app.NotificationCompat
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.mayank.questlist.R
import com.mayank.questlist.data.AppDatabase
import com.mayank.questlist.data.Task
import com.mayank.questlist.data.TaskLog
import com.mayank.questlist.data.TaskRepository
import com.mayank.questlist.scheduler.AlarmScheduler
import com.mayank.questlist.scheduler.NotificationHelper
import com.mayank.questlist.ui.overlay.QuestPopup
import com.mayank.questlist.ui.theme.*
import com.mayank.questlist.util.TimeUtils
import kotlinx.coroutines.*

class OverlayService : Service() {

    private lateinit var windowManager: WindowManager
    private lateinit var repository: TaskRepository
    private var overlayLifecycleOwner: OverlayLifecycleOwner? = null
    private var composeView: ComposeView? = null
    private var isAttached = false

    private val serviceScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var autoHideJob: Job? = null

    private val activeTasks = mutableStateListOf<Task>()

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        val db = AppDatabase.getDatabase(applicationContext)
        repository = TaskRepository(db.taskDao(), db.playerStatsDao())

        createForegroundNotificationChannel()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(
                NOTIFICATION_ID,
                createForegroundNotification(),
                ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
            )
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                NOTIFICATION_ID,
                createForegroundNotification(),
                ServiceInfo.FOREGROUND_SERVICE_TYPE_MANIFEST
            )
        } else {
            startForeground(NOTIFICATION_ID, createForegroundNotification())
        }
        Log.d(TAG, "OverlayService onCreate & startForeground initialized.")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val taskId = intent?.getLongExtra(AlarmScheduler.EXTRA_TASK_ID, 9999L) ?: 9999L
        Log.d(TAG, "onStartCommand received for taskId: $taskId")

        serviceScope.launch(Dispatchers.IO) {
            val dbTask = repository.getTaskByIdSync(taskId)
            val task = dbTask ?: if (taskId == 9999L) {
                Task(
                    id = 9999L,
                    title = "Strength Training",
                    goalText = "100 Push-ups, 100 Sit-ups, 100 Squats, 10km Run",
                    startMinutes = 360,
                    endMinutes = 1439,
                    repeatIntervalMin = 10,
                    daysOfWeekMask = 127,
                    enabled = true
                )
            } else null

            withContext(Dispatchers.Main) {
                if (task == null) {
                    Log.w(TAG, "Task ID $taskId not found in DB.")
                    if (activeTasks.isEmpty()) stopSelf()
                    return@withContext
                }

                val canDraw = Settings.canDrawOverlays(this@OverlayService)
                Log.d(TAG, "canDrawOverlays check result: $canDraw")

                val isScreenOff = isScreenOffOrLocked()
                Log.d(TAG, "isScreenOffOrLocked check result: $isScreenOff")

                if (!canDraw || isScreenOff) {
                    Log.d(TAG, "Fallback to heads-up notification (canDraw=$canDraw, isScreenOff=$isScreenOff)")
                    NotificationHelper.showReminderNotification(this@OverlayService, task)
                    if (activeTasks.isEmpty()) stopSelf()
                    return@withContext
                }

                val existingIndex = activeTasks.indexOfFirst { it.id == task.id }
                if (existingIndex >= 0) {
                    activeTasks[existingIndex] = task
                } else {
                    activeTasks.add(task)
                }

                triggerVibration()
                showOrUpdateOverlay()
                resetAutoHideTimer()
            }
        }

        return START_NOT_STICKY
    }

    private fun showOrUpdateOverlay() {
        val canDraw = Settings.canDrawOverlays(this)
        Log.d(TAG, "showOrUpdateOverlay called. canDrawOverlays = $canDraw, composeView = $composeView")

        if (!canDraw) return

        if (composeView == null) {
            val lifecycleOwner = OverlayLifecycleOwner()
            lifecycleOwner.onCreate()
            lifecycleOwner.onResume()
            this.overlayLifecycleOwner = lifecycleOwner

            val params = WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                        WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                        WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                        WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED,
                PixelFormat.TRANSLUCENT
            ).apply {
                gravity = Gravity.CENTER
            }

            val view = ComposeView(this).apply {
                setViewTreeLifecycleOwner(lifecycleOwner)
                setViewTreeViewModelStoreOwner(lifecycleOwner)
                setViewTreeSavedStateRegistryOwner(lifecycleOwner)

                setContent {
                    QuestListTheme {
                        OverlayWindowContent(
                            activeTasks = activeTasks,
                            onComplete = { task -> onTaskComplete(task) },
                            onSnooze = { task -> onTaskSnooze(task) },
                            onDismiss = { onDismissAll() }
                        )
                    }
                }
            }

            try {
                Log.d(TAG, "Attempting windowManager.addView(composeView, params)...")
                windowManager.addView(view, params)
                composeView = view
                isAttached = true
                Log.d(TAG, "windowManager.addView successful! QuestOverlay displayed.")
            } catch (e: Exception) {
                Log.e(TAG, "windowManager.addView failed: ${e.message}", e)
                Toast.makeText(
                    applicationContext,
                    "Overlay Error: ${e.localizedMessage}",
                    Toast.LENGTH_LONG
                ).show()
                isAttached = false
            }
        }
    }

    private fun onTaskComplete(task: Task) {
        Log.d(TAG, "QuestOverlay tick complete for '${task.title}' (ID: ${task.id})")
        serviceScope.launch(Dispatchers.IO) {
            val todayDate = TimeUtils.getTodayDateString()
            if (task.id != 9999L) {
                repository.setTaskStatus(task.id, todayDate, TaskLog.STATUS_COMPLETED, System.currentTimeMillis())
                AlarmScheduler(applicationContext).scheduleNext(task)
            }

            withContext(Dispatchers.Main) {
                activeTasks.remove(task)
                if (activeTasks.isEmpty()) {
                    removeOverlayViewAndStop()
                }
            }
        }
    }

    private fun onTaskSnooze(task: Task) {
        Log.d(TAG, "QuestOverlay snooze 5m for '${task.title}' (ID: ${task.id})")
        serviceScope.launch(Dispatchers.IO) {
            if (task.id != 9999L) {
                AlarmScheduler(applicationContext).scheduleDebugReminder(task, delaySeconds = 300)
            }
            withContext(Dispatchers.Main) {
                activeTasks.remove(task)
                if (activeTasks.isEmpty()) {
                    removeOverlayViewAndStop()
                }
            }
        }
    }

    private fun onDismissAll() {
        Log.d(TAG, "QuestOverlay dismissed by user.")
        removeOverlayViewAndStop()
    }

    private fun resetAutoHideTimer() {
        autoHideJob?.cancel()
        autoHideJob = serviceScope.launch {
            delay(60_000L)
            Log.d(TAG, "Auto-hide timer expired (60s). Hiding QuestOverlay.")
            removeOverlayViewAndStop()
        }
    }

    private fun removeOverlayViewAndStop() {
        Log.d(TAG, "removeOverlayViewAndStop called. isAttached=$isAttached")
        autoHideJob?.cancel()
        if (isAttached && composeView != null) {
            try {
                windowManager.removeView(composeView)
                Log.d(TAG, "windowManager.removeView executed successfully.")
            } catch (e: Exception) {
                Log.e(TAG, "Error in windowManager.removeView", e)
            } finally {
                isAttached = false
                composeView = null
            }
        }

        overlayLifecycleOwner?.onDestroy()
        overlayLifecycleOwner = null
        activeTasks.clear()
        stopSelf()
    }

    private fun isScreenOffOrLocked(): Boolean {
        val powerManager = getSystemService(POWER_SERVICE) as PowerManager
        val keyguardManager = getSystemService(KEYGUARD_SERVICE) as KeyguardManager
        val isInteractive = powerManager.isInteractive
        val isLocked = keyguardManager.isKeyguardLocked
        return !isInteractive || isLocked
    }

    private fun triggerVibration() {
        try {
            val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager = getSystemService(VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                vibratorManager?.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                getSystemService(VIBRATOR_SERVICE) as? Vibrator
            }

            if (vibrator != null && vibrator.hasVibrator()) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vibrator.vibrate(VibrationEffect.createOneShot(200, VibrationEffect.DEFAULT_AMPLITUDE))
                } else {
                    @Suppress("DEPRECATION")
                    vibrator.vibrate(200)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to vibrate on overlay show", e)
        }
    }

    private fun createForegroundNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "QuestList Active Window",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Keeps QuestList overlay window active"
            }
            val manager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    private fun createForegroundNotification() = NotificationCompat.Builder(this, CHANNEL_ID)
        .setSmallIcon(R.drawable.ic_launcher_foreground)
        .setContentTitle("QuestList System Active")
        .setContentText("Monitoring active daily quests")
        .setPriority(NotificationCompat.PRIORITY_LOW)
        .setOngoing(true)
        .build()

    override fun onDestroy() {
        removeOverlayViewAndStop()
        serviceScope.cancel()
        super.onDestroy()
        Log.d(TAG, "OverlayService destroyed.")
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        const val TAG = "QuestOverlay"
        const val CHANNEL_ID = "overlay_service_channel"
        private const val NOTIFICATION_ID = 8888
    }
}

@Composable
fun OverlayWindowContent(
    activeTasks: List<Task>,
    onComplete: (Task) -> Unit,
    onSnooze: (Task) -> Unit,
    onDismiss: () -> Unit
) {
    val currentTask = activeTasks.firstOrNull() ?: return

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0x59000000)),
        contentAlignment = Alignment.Center
    ) {
        QuestPopup(
            taskTitle = currentTask.title,
            goalText = currentTask.goalText,
            endMinutes = currentTask.endMinutes,
            onComplete = { onComplete(currentTask) },
            onSnooze = { onSnooze(currentTask) },
            onDismiss = onDismiss
        )
    }
}