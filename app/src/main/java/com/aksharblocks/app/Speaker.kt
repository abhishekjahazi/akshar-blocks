package com.aksharblocks.app

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.os.Handler
import android.os.Looper
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
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
    private var pending: Triple<String, Locale, (() -> Unit)?>? = null

    private val main = Handler(Looper.getMainLooper())

    /** Called once the sentence being said now has finished (not when it is cut off). */
    private var onDone: (() -> Unit)? = null

    /** Told when speech starts and stops, so background music can play quieter under it. */
    var onSpeaking: ((Boolean) -> Unit)? = null

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
        tts.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String) {}
            override fun onDone(utteranceId: String) { main.post { if (utteranceId == lastUtterance) finished() } }
            @Deprecated("Deprecated in Java")
            override fun onError(utteranceId: String) { main.post { if (utteranceId == lastUtterance) finished() } }
        })
        ready = true
        pending?.let { (text, locale, done) -> say(text, locale, done) }
        pending = null
    }

    /** The id of the last utterance queued for the phone voice; its end is the sentence's end. */
    private var lastUtterance: String? = null

    /**
     * Says [text] in [locale], interrupting anything that is still being said. [done] runs when
     * it has all been said (rhymes use it to go on to the next line).
     */
    fun say(text: String, locale: Locale = English.locale, done: (() -> Unit)? = null) {
        onDone = done
        onSpeaking?.invoke(true)
        val clips = Voice.clipsFor(text, locale)
        if (clips != null) {
            if (ready) tts.stop()
            pending = null
            lastUtterance = null
            playClips(clips, 0, ++playToken)
            return
        }
        stopClips()
        if (!ready) {
            pending = Triple(text, locale, done)
            return
        }
        // A sentence can switch language part way ("Cow!|@hi गाय!"): each run is queued in its own voice.
        val runs = Voice.spokenRuns(text, locale)
        runs.forEachIndexed { i, (runLocale, spoken) ->
            setLanguage(runLocale)
            val mode = if (i == 0) TextToSpeech.QUEUE_FLUSH else TextToSpeech.QUEUE_ADD
            val id = "say${playToken}_$i"
            if (i == runs.lastIndex) lastUtterance = id
            tts.speak(spoken, mode, null, id)
        }
    }

    private fun setLanguage(locale: Locale) {
        if (locale == currentLocale) return
        var result = tts.setLanguage(locale)
        // Phones without a Marathi voice read Marathi with the Hindi one: same script, close sounds.
        if (result < TextToSpeech.LANG_AVAILABLE && locale.language == "mr") result = tts.setLanguage(Hindi.locale)
        if (result < TextToSpeech.LANG_AVAILABLE) Log.w(TAG, "No voice installed for $locale")
        currentLocale = locale
    }

    /** Plays recordings one after another; a newer sentence cancels the rest. */
    private fun playClips(clips: List<String>, index: Int, token: Int) {
        releasePlayer()
        if (token != playToken) return
        if (index >= clips.size) {
            finished()
            return
        }
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

    private fun finished() {
        onSpeaking?.invoke(false)
        val done = onDone ?: return
        onDone = null
        done()
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
        onDone = null
        lastUtterance = null
        stopClips()
        if (ready) tts.stop()
        onSpeaking?.invoke(false)
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
