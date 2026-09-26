package com.kirtigames.abcd

import android.content.Context

/** The child's star total, kept on the device between visits. */
class StarBank(context: Context) {

    private val prefs = context.getSharedPreferences("progress", Context.MODE_PRIVATE)

    var count: Int = prefs.getInt(KEY, 0)
        private set

    fun add() {
        count++
        prefs.edit().putInt(KEY, count).apply()
    }

    private companion object {
        const val KEY = "stars"
    }
}
