package com.aksharblocks.app

import android.content.Context
import java.util.TimeZone

/** Today as a day number in the phone's own time zone. */
fun todayNumber(): Long {
    val now = System.currentTimeMillis()
    return (now + TimeZone.getDefault().getOffset(now)) / (24L * 60 * 60 * 1000)
}

/** How long the app has been played today (all children together), kept on the device. */
class PlayTime(context: Context) {

    private val prefs = context.getSharedPreferences("play_time", Context.MODE_PRIVATE)

    private fun freshDay() {
        val today = todayNumber()
        if (prefs.getLong(KEY_DAY, -1) != today) {
            prefs.edit().putLong(KEY_DAY, today).putLong(KEY_SECONDS, 0).putInt(KEY_EXTRA, 0).apply()
        }
    }

    /** Seconds played today. */
    val secondsToday: Long
        get() {
            freshDay()
            return prefs.getLong(KEY_SECONDS, 0)
        }

    /** Minutes a parent added today with "a little more time". */
    val extraMinutesToday: Int
        get() {
            freshDay()
            return prefs.getInt(KEY_EXTRA, 0)
        }

    fun add(seconds: Long) {
        if (seconds <= 0) return
        freshDay()
        prefs.edit().putLong(KEY_SECONDS, prefs.getLong(KEY_SECONDS, 0) + seconds).apply()
    }

    fun addExtraMinutes(minutes: Int) {
        freshDay()
        prefs.edit().putInt(KEY_EXTRA, prefs.getInt(KEY_EXTRA, 0) + minutes).apply()
    }

    private companion object {
        const val KEY_DAY = "day"
        const val KEY_SECONDS = "seconds"
        const val KEY_EXTRA = "extra_minutes"
    }
}

/** The daily limit rule, kept separate so it can be tested. */
object PlayLimit {

    /** Choices offered to parents; 0 means no limit. */
    val CHOICES = listOf(0, 15, 30, 45, 60)

    fun isOver(limitMinutes: Int, secondsToday: Long, extraMinutes: Int): Boolean =
        limitMinutes > 0 && secondsToday >= (limitMinutes + extraMinutes) * 60L
}
