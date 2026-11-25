package com.mayank.questlist.scheduler

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.mayank.questlist.data.AppDatabase
import com.mayank.questlist.data.Task
import com.mayank.questlist.data.TaskLog
import com.mayank.questlist.data.TaskRepository
import com.mayank.questlist.util.TimeUtils
import java.util.Calendar

class AlarmScheduler(private val context: Context) {

    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    private val db = AppDatabase.getDatabase(context)
    private val repository = TaskRepository(db.taskDao(), db.playerStatsDao())

    suspend fun scheduleNext(task: Task) {
        if (!task.enabled) {
            cancel(task.id)
            return
        }

        val now = Calendar.getInstance()
        val todayDate = TimeUtils.getTodayDateString()
        val todayLog = repository.getLogForTaskAndDate(task.id, todayDate)
        val isCompletedToday = todayLog?.status == TaskLog.STATUS_COMPLETED

        val triggerTimeMillis = calculateNextTriggerTime(task, now, isCompletedToday)

        if (triggerTimeMillis <= 0L) {
            Log.d(TAG, "No valid trigger time computed for task '${task.title}' (ID: ${task.id})")
            return
        }

        val intent = Intent(context, ReminderReceiver::class.java).apply {
            action = ACTION_REMINDER_TRIGGER
            putExtra(EXTRA_TASK_ID, task.id)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            task.id.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        scheduleAlarm(triggerTimeMillis, pendingIntent, task.title)
    }

    fun cancel(taskId: Long) {
        val intent = Intent(context, ReminderReceiver::class.java).apply {
            action = ACTION_REMINDER_TRIGGER
            putExtra(EXTRA_TASK_ID, taskId)
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            taskId.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(pendingIntent)
        Log.d(TAG, "Cancelled alarm for task ID: $taskId")
    }

    suspend fun rescheduleAll() {
        Log.d(TAG, "Rescheduling all enabled tasks...")
        val enabledTasks = repository.getEnabledTasksSync()
        for (task in enabledTasks) {
            scheduleNext(task)
        }
        scheduleMidnightAlarm()
    }

    fun scheduleMidnightAlarm() {
        val midnightCal = Calendar.getInstance().apply {
            add(Calendar.DAY_OF_YEAR, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        val intent = Intent(context, MidnightReceiver::class.java).apply {
            action = ACTION_MIDNIGHT_TICK
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            MIDNIGHT_REQUEST_CODE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        scheduleAlarm(midnightCal.timeInMillis, pendingIntent, "Midnight Daily Reset")
    }

    fun scheduleDebugReminder(task: Task, delaySeconds: Long = 10) {
        val triggerTimeMillis = System.currentTimeMillis() + (delaySeconds * 1000L)
        val intent = Intent(context, ReminderReceiver::class.java).apply {
            action = ACTION_REMINDER_TRIGGER
            putExtra(EXTRA_TASK_ID, task.id)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            (task.id + 99999).toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        scheduleAlarm(triggerTimeMillis, pendingIntent, "DEBUG: ${task.title}")
    }

    private fun scheduleAlarm(triggerTimeMillis: Long, pendingIntent: PendingIntent, label: String) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !alarmManager.canScheduleExactAlarms()) {
            Log.w(TAG, "Exact alarm permission not granted. Falling back to setAndAllowWhileIdle for '$label'")
            alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerTimeMillis, pendingIntent)
        } else {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerTimeMillis, pendingIntent)
        }

        Log.d(
            TAG,
            "Scheduled '$label' for trigger time: ${TimeUtils.formatMillisToDateTime(triggerTimeMillis)}"
        )
    }

    private fun calculateNextTriggerTime(task: Task, now: Calendar, isCompletedToday: Boolean): Long {
        val todayIndex = TimeUtils.getTodayDayIndex()
        val isTodayScheduled = TimeUtils.isScheduledForToday(task.daysOfWeekMask)

        val startMin = task.startMinutes
        val endMin = task.endMinutes
        val intervalMin = task.repeatIntervalMin.coerceAtLeast(1)

        val isCrossMidnight = endMin < startMin

        val todayStartCal = (now.clone() as Calendar).apply {
            set(Calendar.HOUR_OF_DAY, startMin / 60)
            set(Calendar.MINUTE, startMin % 60)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        val todayEndCal = (todayStartCal.clone() as Calendar).apply {
            if (isCrossMidnight) {
                add(Calendar.DAY_OF_YEAR, 1)
            }
            set(Calendar.HOUR_OF_DAY, endMin / 60)
            set(Calendar.MINUTE, endMin % 60)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        if (isTodayScheduled && !isCompletedToday) {
            if (now.before(todayStartCal)) {
                return todayStartCal.timeInMillis
            } else if (now.before(todayEndCal)) {
                val windowDurationMs = todayEndCal.timeInMillis - todayStartCal.timeInMillis
                val intervalMs = intervalMin * 60_000L

                if (intervalMs < windowDurationMs) {
                    val elapsedMs = now.timeInMillis - todayStartCal.timeInMillis
                    val k = (elapsedMs / intervalMs) + 1
                    val nextTriggerCal = (todayStartCal.clone() as Calendar).apply {
                        add(Calendar.MILLISECOND, (k * intervalMs).toInt())
                    }

                    if (nextTriggerCal.before(todayEndCal)) {
                        return nextTriggerCal.timeInMillis
                    }
                }
            }
        }

        val dayOffset = findNextScheduledDayOffset(task.daysOfWeekMask, todayIndex)
        val nextDayStartCal = (todayStartCal.clone() as Calendar).apply {
            add(Calendar.DAY_OF_YEAR, dayOffset)
        }
        return nextDayStartCal.timeInMillis
    }

    private fun findNextScheduledDayOffset(daysMask: Int, currentDayIndex: Int): Int {
        for (offset in 1..7) {
            val candidateDay = (currentDayIndex + offset) % 7
            if (TimeUtils.isDayEnabled(daysMask, candidateDay)) {
                return offset
            }
        }
        return 1
    }

    companion object {
        const val TAG = "QuestScheduler"
        const val ACTION_REMINDER_TRIGGER = "com.mayank.questlist.ACTION_REMINDER_TRIGGER"
        const val ACTION_MIDNIGHT_TICK = "com.mayank.questlist.ACTION_MIDNIGHT_TICK"
        const val EXTRA_TASK_ID = "extra_task_id"
        private const val MIDNIGHT_REQUEST_CODE = 999999
    }
}