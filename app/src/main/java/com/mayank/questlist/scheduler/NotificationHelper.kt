package com.mayank.questlist.scheduler

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.mayank.questlist.MainActivity
import com.mayank.questlist.R
import com.mayank.questlist.data.Task
import com.mayank.questlist.util.TimeUtils

object NotificationHelper {

    const val CHANNEL_ID = "quest_reminders_channel"
    private const val CHANNEL_NAME = "Quest Reminders"

    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "High-priority notifications for active Daily Quests"
                enableVibration(true)
                enableLights(true)
            }
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    fun showReminderNotification(context: Context, task: Task) {
        createNotificationChannel(context)

        val contentIntent = Intent(context, MainActivity::class.java)
        val contentPendingIntent = PendingIntent.getActivity(
            context,
            task.id.toInt(),
            contentIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val markDoneIntent = Intent(context, MarkDoneReceiver::class.java).apply {
            action = MarkDoneReceiver.ACTION_MARK_DONE
            putExtra(AlarmScheduler.EXTRA_TASK_ID, task.id)
        }
        val markDonePendingIntent = PendingIntent.getBroadcast(
            context,
            task.id.toInt(),
            markDoneIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val timeWindow = "${TimeUtils.formatMinutesToTime(task.startMinutes)} - ${TimeUtils.formatMinutesToTime(task.endMinutes)}"
        val goalStr = if (!task.goalText.isNullOrBlank()) "GOAL: ${task.goalText}" else "Active Window: $timeWindow"

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("[Daily Quest: ${task.title} has arrived.]")
            .setContentText(goalStr)
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText("$goalStr\nWindow: $timeWindow\nWARNING: Complete before window ends to clear the quest.")
            )
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setAutoCancel(true)
            .setContentIntent(contentPendingIntent)
            .addAction(
                R.drawable.ic_launcher_foreground,
                "MARK DONE",
                markDonePendingIntent
            )
            .build()

        try {
            NotificationManagerCompat.from(context).notify(task.id.toInt(), notification)
        } catch (e: SecurityException) {
            Log.e(AlarmScheduler.TAG, "Failed to post notification due to missing permission", e)
        }
    }

    fun cancelNotification(context: Context, taskId: Long) {
        NotificationManagerCompat.from(context).cancel(taskId.toInt())
    }
}