package com.mayank.questlist.util

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

object TimeUtils {

    const val ALL_DAYS_MASK = 127 // 0b1111111 (Mon to Sun)
    const val WEEKDAYS_MASK = 31  // Mon to Fri (1 + 2 + 4 + 8 + 16)
    const val WEEKENDS_MASK = 96  // Sat (32) + Sun (64)

    private val dayBits = intArrayOf(
        1,  // Monday    (1 << 0)
        2,  // Tuesday   (1 << 1)
        4,  // Wednesday (1 << 2)
        8,  // Thursday  (1 << 3)
        16, // Friday    (1 << 4)
        32, // Saturday  (1 << 5)
        64  // Sunday    (1 << 6)
    )

    fun getTodayDateString(): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        return sdf.format(Date())
    }

    fun getYesterdayDateString(): String {
        val calendar = Calendar.getInstance()
        calendar.add(Calendar.DAY_OF_YEAR, -1)
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        return sdf.format(calendar.time)
    }

    fun getCurrentMinutes(): Int {
        val calendar = Calendar.getInstance()
        val hour = calendar.get(Calendar.HOUR_OF_DAY)
        val minute = calendar.get(Calendar.MINUTE)
        return hour * 60 + minute
    }

    /**
     * Converts day-of-week index (0 = Monday, ..., 6 = Sunday) to its bitmask value.
     */
    fun dayIndexToMask(dayIndex: Int): Int {
        return if (dayIndex in 0..6) dayBits[dayIndex] else 0
    }

    /**
     * Check if a given day-of-week index (0 = Monday .. 6 = Sunday) is enabled in [mask].
     */
    fun isDayEnabled(mask: Int, dayIndex: Int): Boolean {
        if (dayIndex !in 0..6) return false
        return (mask and dayBits[dayIndex]) != 0
    }

    /**
     * Returns today's day-of-week index (0 = Monday, ..., 6 = Sunday).
     */
    fun getTodayDayIndex(): Int {
        val calendar = Calendar.getInstance()
        val calendarDay = calendar.get(Calendar.DAY_OF_WEEK)
        return when (calendarDay) {
            Calendar.MONDAY -> 0
            Calendar.TUESDAY -> 1
            Calendar.WEDNESDAY -> 2
            Calendar.THURSDAY -> 3
            Calendar.FRIDAY -> 4
            Calendar.SATURDAY -> 5
            Calendar.SUNDAY -> 6
            else -> 0
        }
    }

    /**
     * Checks if today's day is included in the task's [mask].
     */
    fun isScheduledForToday(mask: Int): Boolean {
        return isDayEnabled(mask, getTodayDayIndex())
    }

    /**
     * Formats minutes from midnight (0..1439) into "hh:mm a" (e.g. 06:30 AM).
     */
    fun formatMinutesToTime(minutes: Int): String {
        val hour24 = (minutes / 60) % 24
        val minute = minutes % 60
        val isPm = hour24 >= 12
        val hour12 = when {
            hour24 == 0 -> 12
            hour24 > 12 -> hour24 - 12
            else -> hour24
        }
        val amPm = if (isPm) "PM" else "AM"
        return String.format(Locale.getDefault(), "%02d:%02d %s", hour12, minute, amPm)
    }

    /**
     * Formats minutes to 24-hour "HH:mm" (e.g. 06:30).
     */
    fun formatMinutesTo24Hour(minutes: Int): String {
        val hour = (minutes / 60) % 24
        val minute = minutes % 60
        return String.format(Locale.getDefault(), "%02d:%02d", hour, minute)
    }

    /**
     * Formats epoch milliseconds to human readable date-time.
     */
    fun formatMillisToDateTime(millis: Long): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        return sdf.format(Date(millis))
    }

    fun getDayLabels(): List<String> = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
}