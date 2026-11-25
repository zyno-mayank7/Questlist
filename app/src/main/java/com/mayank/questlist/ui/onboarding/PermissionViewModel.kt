package com.mayank.questlist.ui.onboarding

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.mayank.questlist.util.PermissionUtils
import com.mayank.questlist.util.PreferencesManager

class PermissionViewModel(private val preferencesManager: PreferencesManager) : ViewModel() {

    var hasOverlay by mutableStateOf(false)
        private set
    var hasNotification by mutableStateOf(false)
        private set
    var hasExactAlarm by mutableStateOf(false)
        private set
    var isBatteryOptimizedExempt by mutableStateOf(false)
        private set

    val canProceed: Boolean
        get() = hasOverlay && hasNotification

    fun refreshPermissions(context: Context) {
        hasOverlay = PermissionUtils.hasOverlayPermission(context)
        hasNotification = PermissionUtils.hasNotificationPermission(context)
        hasExactAlarm = PermissionUtils.hasExactAlarmPermission(context)
        isBatteryOptimizedExempt = PermissionUtils.isIgnoringBatteryOptimizations(context)
    }

    fun completeOnboarding() {
        preferencesManager.setFirstLaunchCompleted(true)
    }

    class Factory(private val preferencesManager: PreferencesManager) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(PermissionViewModel::class.java)) {
                return PermissionViewModel(preferencesManager) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}