package com.mayank.questlist.scheduler

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.mayank.questlist.data.AppDatabase
import com.mayank.questlist.data.TaskLog
import com.mayank.questlist.data.TaskRepository
import com.mayank.questlist.util.TimeUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MidnightReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        Log.d(AlarmScheduler.TAG, "MidnightReceiver: Daily midnight reset triggered.")

        val pendingResult = goAsync()

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val db = AppDatabase.getDatabase(context)
                val repository = TaskRepository(db.taskDao(), db.playerStatsDao())

                val yesterdayDate = TimeUtils.getYesterdayDateString()
                val todayDate = TimeUtils.getTodayDateString()

                val allTasks = repository.getAllTasksSync()

                for (task in allTasks) {
                    val yesterdayLog = repository.getLogForTaskAndDate(task.id, yesterdayDate)
                    if (yesterdayLog != null && yesterdayLog.status == TaskLog.STATUS_PENDING) {
                        repository.setTaskStatus(task.id, yesterdayDate, TaskLog.STATUS_FAILED)
                    }

                    if (task.enabled && TimeUtils.isScheduledForToday(task.daysOfWeekMask)) {
                        val todayLog = repository.getLogForTaskAndDate(task.id, todayDate)
                        if (todayLog == null) {
                            repository.setTaskStatus(task.id, todayDate, TaskLog.STATUS_PENDING)
                        }
                    }
                }

                val scheduler = AlarmScheduler(context)
                scheduler.rescheduleAll()
            } catch (e: Exception) {
                Log.e(AlarmScheduler.TAG, "Error in MidnightReceiver", e)
            } finally {
                pendingResult.finish()
            }
        }
    }
}