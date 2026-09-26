package com.kirtigames.abcd

import android.content.Context
import android.speech.tts.TextToSpeech
import android.util.Log
import java.util.Locale

/** Speaks words out loud with the phone's built-in text-to-speech voice, in English or Hindi. */
class Speaker(context: Context) : TextToSpeech.OnInitListener {

    private val tts = TextToSpeech(context.applicationContext, this)

    @Volatile
    private var ready = false
    private var currentLocale: Locale? = null

    /** Last thing asked for before the engine finished starting up. */
    private var pending: Pair<String, Locale>? = null

    override fun onInit(status: Int) {
        if (status != TextToSpeech.SUCCESS) {
            Log.w(TAG, "Text-to-speech failed to start: $status")
            return
        }
        // A little slower and brighter than normal, for young listeners.
        tts.setSpeechRate(rate)
        tts.setPitch(1.15f)
        ready = true
        pending?.let { (text, locale) -> say(text, locale) }
        pending = null
    }

    private var rate = NORMAL_RATE

    /** Slower speech for the youngest children (a parent setting). */
    var slow: Boolean = false
        set(value) {
            field = value
            rate = if (value) SLOW_RATE else NORMAL_RATE
            if (ready) tts.setSpeechRate(rate)
        }

    /** Says [text] in [locale], interrupting anything that is still being spoken. */
    fun say(text: String, locale: Locale = Locale.US) {
        if (!ready) {
            pending = text to locale
            return
        }
        if (locale != currentLocale) {
            val result = tts.setLanguage(locale)
            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                Log.w(TAG, "No voice installed for $locale")
            }
            currentLocale = locale
        }
        tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, text.hashCode().toString())
    }

    /** False when the phone has no voice for [locale]. Unknown (engine still starting) counts as available. */
    fun hasVoiceFor(locale: Locale): Boolean {
        if (!ready) return true
        return tts.isLanguageAvailable(locale) >= TextToSpeech.LANG_AVAILABLE
    }

    fun stop() {
        pending = null
        if (ready) tts.stop()
    }

    fun shutdown() {
        tts.shutdown()
    }

    private companion object {
        const val TAG = "Speaker"
        const val NORMAL_RATE = 0.85f
        const val SLOW_RATE = 0.7f
    }
}
