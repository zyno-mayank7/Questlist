package com.mayank.questlist.scheduler

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        Log.d(AlarmScheduler.TAG, "BootReceiver received action: $action. Rescheduling all alarms.")

        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                AlarmScheduler(context).rescheduleAll()
            } catch (e: Exception) {
                Log.e(AlarmScheduler.TAG, "Error rescheduling alarms on boot", e)
            } finally {
                pendingResult.finish()
            }
        }
    }
}