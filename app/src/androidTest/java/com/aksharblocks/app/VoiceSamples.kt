package com.aksharblocks.app

import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

/**
 * Not a test: a tool for choosing the app's voice. Every English and Hindi voice installed
 * on the phone says the same sample sentences (at the app's speed and pitch), saved as
 * Android/data/com.aksharblocks.app.debug/files/voice-samples/<voice name>.wav.
 *
 * Run: gradlew connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.aksharblocks.app.VoiceSamples
 */
@RunWith(AndroidJUnit4::class)
class VoiceSamples {

    @Test
    fun sampleEveryVoice() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val context = instrumentation.targetContext
        val out = requireNotNull(context.getExternalFilesDir("voice-samples")).apply { deleteRecursively(); mkdirs() }

        val started = CountDownLatch(1)
        var status = TextToSpeech.ERROR
        lateinit var tts: TextToSpeech
        instrumentation.runOnMainSync {
            tts = TextToSpeech(context) {
                status = it
                started.countDown()
            }
        }
        assertTrue(started.await(30, TimeUnit.SECONDS))
        assertEquals(TextToSpeech.SUCCESS, status)
        tts.setSpeechRate(0.85f)
        tts.setPitch(1.15f)

        val finished = ConcurrentHashMap<String, CountDownLatch>()
        tts.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String) {}
            override fun onDone(utteranceId: String) { finished[utteranceId]?.countDown() }
            @Deprecated("Deprecated in Java")
            override fun onError(utteranceId: String) { finished[utteranceId]?.countDown() }
        })

        // Voices that work offline and are already downloaded.
        val voices = tts.voices.orEmpty()
            .filter { it.locale.language == "en" || it.locale.language == "hi" }
            .filter { !it.isNetworkConnectionRequired }
            .filter { TextToSpeech.Engine.KEY_FEATURE_NOT_INSTALLED !in it.features.orEmpty() }
            .sortedBy { it.name }

        val list = StringBuilder()
        for (voice in voices) {
            tts.voice = voice
            val text = if (voice.locale.language == "hi") HINDI_SAMPLE else ENGLISH_SAMPLE
            val latch = CountDownLatch(1)
            finished[voice.name] = latch
            val result = tts.synthesizeToFile(text, Bundle(), File(out, "${voice.name}.wav"), voice.name)
            if (result == TextToSpeech.SUCCESS) latch.await(30, TimeUnit.SECONDS)
            list.append("${voice.name}\t${voice.locale}\tquality=${voice.quality}\n")
        }
        File(out, "voices.txt").writeText(list.toString())
        tts.shutdown()
        assertTrue("no offline English or Hindi voices found", voices.isNotEmpty())
    }

    private companion object {
        const val ENGLISH_SAMPLE = "Hello! Find the letter B! Great job! A is for Apple."
        const val HINDI_SAMPLE = "नमस्ते! क ढूंढो! शाबाश! अ से अनानास।"
    }
}
