package com.aksharblocks.app

import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

/**
 * Not a test of the app: says some trial spellings with the app's English voice into
 * files/voice-try/<n>.wav, to compare how the voice reads them (for example phonics sounds).
 * Run with -e words "ih_ihh_ex" (each is said as a question, "ih?", like the Sounds game) (and -e enVoice like VoiceMaker).
 */
@RunWith(AndroidJUnit4::class)
class VoiceTry {

    @Test
    fun sayWords() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val context = instrumentation.targetContext
        val words = InstrumentationRegistry.getArguments().getString("words").orEmpty().split('_').filter { it.isNotBlank() }
        val out = requireNotNull(context.getExternalFilesDir("voice-try")).apply { deleteRecursively(); mkdirs() }

        val started = CountDownLatch(1)
        lateinit var tts: TextToSpeech
        instrumentation.runOnMainSync { tts = TextToSpeech(context) { started.countDown() } }
        assertTrue(started.await(30, TimeUnit.SECONDS))
        tts.language = English.locale
        tts.setSpeechRate(0.85f)
        tts.setPitch(1.15f)
        InstrumentationRegistry.getArguments().getString("enVoice")?.let { name ->
            tts.voices.orEmpty().firstOrNull { it.name == name }?.let { tts.voice = it }
        }
        val done = HashMap<String, CountDownLatch>()
        tts.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String) {}
            override fun onDone(utteranceId: String) { done[utteranceId]?.countDown() }
            @Deprecated("Deprecated in Java")
            override fun onError(utteranceId: String) { done[utteranceId]?.countDown() }
        })
        words.forEachIndexed { i, word ->
            val id = "w$i"
            val latch = CountDownLatch(1)
            done[id] = latch
            tts.synthesizeToFile("$word?", Bundle(), File(out, "$i.wav"), id)
            latch.await(30, TimeUnit.SECONDS)
        }
        File(out, "words.txt").writeText(words.mapIndexed { i, w -> "$i\t$w" }.joinToString("\n"))
        tts.shutdown()
    }
}
