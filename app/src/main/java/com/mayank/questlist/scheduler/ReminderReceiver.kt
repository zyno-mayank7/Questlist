package com.mayank.questlist.scheduler

import android.app.KeyguardManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.PowerManager
import android.provider.Settings
import android.util.Log
import androidx.core.content.ContextCompat
import com.mayank.questlist.data.AppDatabase
import com.mayank.questlist.data.TaskLog
import com.mayank.questlist.data.TaskRepository
import com.mayank.questlist.overlay.OverlayService
import com.mayank.questlist.util.TimeUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class ReminderReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val taskId = intent.getLongExtra(AlarmScheduler.EXTRA_TASK_ID, -1L)
        if (taskId <= 0L) return

        val pendingResult = goAsync()

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val db = AppDatabase.getDatabase(context)
                val repository = TaskRepository(db.taskDao(), db.playerStatsDao())
                val task = repository.getTaskByIdSync(taskId)

                if (task == null || !task.enabled) {
                    Log.d(AlarmScheduler.TAG, "ReminderReceiver: Task $taskId is null or disabled.")
                    return@launch
                }

                val todayDate = TimeUtils.getTodayDateString()

                if (!TimeUtils.isScheduledForToday(task.daysOfWeekMask)) {
                    Log.d(AlarmScheduler.TAG, "ReminderReceiver: Task '${task.title}' is not scheduled for today.")
                    return@launch
                }

                val existingLog = repository.getLogForTaskAndDate(taskId, todayDate)

                if (existingLog?.status == TaskLog.STATUS_COMPLETED) {
                    Log.d(AlarmScheduler.TAG, "ReminderReceiver: Task '${task.title}' is already completed today.")
                    AlarmScheduler(context).scheduleNext(task)
                    return@launch
                }

                val nowMin = TimeUtils.getCurrentMinutes()
                val isCrossMidnight = task.endMinutes < task.startMinutes

                val isInsideWindow = if (!isCrossMidnight) {
                    nowMin in task.startMinutes until task.endMinutes
                } else {
                    nowMin >= task.startMinutes || nowMin < task.endMinutes
                }

                val isWindowEnded = if (!isCrossMidnight) {
                    nowMin >= task.endMinutes
                } else {
                    nowMin in task.endMinutes until task.startMinutes
                }

                if (isWindowEnded) {
                    Log.d(AlarmScheduler.TAG, "ReminderReceiver: Window for '${task.title}' has ended. Marking FAILED.")
                    repository.setTaskStatus(taskId, todayDate, TaskLog.STATUS_FAILED)
                    AlarmScheduler(context).scheduleNext(task)
                    return@launch
                }

                if (isInsideWindow) {
                    val canDrawOverlay = Settings.canDrawOverlays(context)
                    val isScreenOffOrLocked = isScreenOffOrLocked(context)

                    if (canDrawOverlay && !isScreenOffOrLocked) {
                        Log.d(AlarmScheduler.TAG, "ReminderReceiver: Triggering OverlayService for '${task.title}'")
                        val serviceIntent = Intent(context, OverlayService::class.java).apply {
                            putExtra(AlarmScheduler.EXTRA_TASK_ID, task.id)
                        }
                        ContextCompat.startForegroundService(context, serviceIntent)
                    } else {
                        Log.d(AlarmScheduler.TAG, "ReminderReceiver: Screen off/locked or no overlay permission. Falling back to notification for '${task.title}'")
                        NotificationHelper.showReminderNotification(context, task)
                    }

                    AlarmScheduler(context).scheduleNext(task)
                }
            } catch (e: Exception) {
                Log.e(AlarmScheduler.TAG, "Error in ReminderReceiver for task $taskId", e)
            } finally {
                pendingResult.finish()
            }
        }
    }

    private fun isScreenOffOrLocked(context: Context): Boolean {
        val powerManager = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
        val keyguardManager = context.getSystemService(Context.KEYGUARD_SERVICE) as? KeyguardManager
        val isInteractive = powerManager?.isInteractive ?: true
        val isLocked = keyguardManager?.isKeyguardLocked ?: false
        return !isInteractive || isLocked
    }
}