package com.mayank.questlist.util

import android.content.Context
import android.content.SharedPreferences

class PreferencesManager(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("questlist_prefs", Context.MODE_PRIVATE)

    fun isFirstLaunchCompleted(): Boolean {
        return prefs.getBoolean(KEY_FIRST_LAUNCH_COMPLETED, false)
    }

    fun setFirstLaunchCompleted(completed: Boolean) {
        prefs.edit().putBoolean(KEY_FIRST_LAUNCH_COMPLETED, completed).apply()
    }

    companion object {
        private const val KEY_FIRST_LAUNCH_COMPLETED = "first_launch_completed"
    }
}