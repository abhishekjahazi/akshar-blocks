package com.kirtigames.abcd

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.speech.tts.TextToSpeech
import android.util.Log
import java.util.Locale

/**
 * Speaks to the child: recorded clips when every part of the sentence has been recorded
 * (see [Voice]), otherwise the phone's text-to-speech voice, in English or Hindi.
 */
class Speaker(context: Context) : TextToSpeech.OnInitListener {

    private val assets = context.applicationContext.assets
    private val tts = TextToSpeech(context.applicationContext, this)

    @Volatile
    private var ready = false
    private var currentLocale: Locale? = null

    /** Last thing asked for before the engine finished starting up. */
    private var pending: Pair<String, Locale>? = null

    /** The recording playing now, and a counter that makes older playback stop chaining. */
    private var player: MediaPlayer? = null
    private var playToken = 0

    private var rate = NORMAL_RATE

    /** Slower speech for the youngest children (a parent setting). */
    var slow: Boolean = false
        set(value) {
            field = value
            rate = if (value) SLOW_RATE else NORMAL_RATE
            if (ready) tts.setSpeechRate(rate)
        }

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

    /** Says [text] in [locale], interrupting anything that is still being said. */
    fun say(text: String, locale: Locale = English.locale) {
        val clips = Voice.clipsFor(text, locale)
        if (clips != null) {
            if (ready) tts.stop()
            pending = null
            playClips(clips, 0, ++playToken)
            return
        }
        stopClips()
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
        val spoken = Voice.spokenText(text)
        tts.speak(spoken, TextToSpeech.QUEUE_FLUSH, null, spoken.hashCode().toString())
    }

    /** Plays recordings one after another; a newer sentence cancels the rest. */
    private fun playClips(clips: List<String>, index: Int, token: Int) {
        releasePlayer()
        if (index >= clips.size || token != playToken) return
        // Kept in a local until it's playing, so a clip that fails to load is still released.
        val next = MediaPlayer()
        try {
            next.setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_GAME)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                    .build(),
            )
            assets.openFd(clips[index]).use { fd -> next.setDataSource(fd.fileDescriptor, fd.startOffset, fd.length) }
            next.setOnCompletionListener { playClips(clips, index + 1, token) }
            next.prepare()
            if (slow) next.playbackParams = next.playbackParams.setSpeed(SLOW_CLIP_SPEED)
            next.start()
            player = next
        } catch (e: Exception) {
            next.release()
            // A broken recording must not silence the game: skip to the next part.
            Log.w(TAG, "Could not play ${clips[index]}", e)
            playClips(clips, index + 1, token)
        }
    }

    private fun stopClips() {
        playToken++
        releasePlayer()
    }

    private fun releasePlayer() {
        player?.release()
        player = null
    }

    fun stop() {
        pending = null
        stopClips()
        if (ready) tts.stop()
    }

    fun shutdown() {
        stopClips()
        tts.shutdown()
    }

    private companion object {
        const val TAG = "Speaker"
        const val NORMAL_RATE = 0.85f
        const val SLOW_RATE = 0.7f
        const val SLOW_CLIP_SPEED = 0.85f
    }
}
