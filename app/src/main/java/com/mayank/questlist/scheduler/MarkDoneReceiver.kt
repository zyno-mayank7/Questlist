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

class MarkDoneReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val taskId = intent.getLongExtra(AlarmScheduler.EXTRA_TASK_ID, -1L)
        if (taskId <= 0L) return

        val pendingResult = goAsync()

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val db = AppDatabase.getDatabase(context)
                val repository = TaskRepository(db.taskDao(), db.playerStatsDao())
                val todayDate = TimeUtils.getTodayDateString()

                Log.d(AlarmScheduler.TAG, "MarkDoneReceiver: Marking task ID $taskId as DONE via notification.")

                repository.setTaskStatus(
                    taskId = taskId,
                    date = todayDate,
                    status = TaskLog.STATUS_COMPLETED,
                    completedAt = System.currentTimeMillis()
                )

                NotificationHelper.cancelNotification(context, taskId)

                val task = repository.getTaskByIdSync(taskId)
                task?.let {
                    AlarmScheduler(context).scheduleNext(it)
                }
            } catch (e: Exception) {
                Log.e(AlarmScheduler.TAG, "Error in MarkDoneReceiver", e)
            } finally {
                pendingResult.finish()
            }
        }
    }

    companion object {
        const val ACTION_MARK_DONE = "com.mayank.questlist.ACTION_MARK_DONE"
    }
}