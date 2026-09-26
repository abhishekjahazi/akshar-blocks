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
 * Not a test: a tool that makes the built-in voice. It asks the phone's text-to-speech
 * engine to say every line of the recording scripts (same voice, speed and pitch as the
 * app) and saves each as WAV in Android/data/com.aksharblocks.app.debug/files/voice-wav/.
 * store/voice/make-voice.sh pulls, trims and compresses them into assets/voice/.
 *
 * Run: gradlew connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.aksharblocks.app.VoiceMaker
 *   (add -Pandroid.testInstrumentationRunnerArguments.enVoice=... and .hiVoice=... to choose voices)
 */
@RunWith(AndroidJUnit4::class)
class VoiceMaker {

    @Test
    fun makeVoiceClips() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val context = instrumentation.targetContext
        Content.load(context.assets)
        val out = requireNotNull(context.getExternalFilesDir("voice-wav"))

        val started = CountDownLatch(1)
        var status = TextToSpeech.ERROR
        lateinit var tts: TextToSpeech
        instrumentation.runOnMainSync {
            tts = TextToSpeech(context) {
                status = it
                started.countDown()
            }
        }
        assertTrue("TTS did not start", started.await(30, TimeUnit.SECONDS))
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

        // Optional: pick exact voices (see VoiceSamples), e.g. -e enVoice en-in-x-ena-local -e hiVoice hi-in-x-hia-local
        val arguments = InstrumentationRegistry.getArguments()
        for ((language, lines) in VoiceScript.lines()) {
            val locale = if (language == "hi") Hindi.locale else English.locale
            val result = tts.setLanguage(locale)
            assertTrue("no $language voice on this phone", result >= TextToSpeech.LANG_AVAILABLE)
            arguments.getString("${language}Voice")?.let { name ->
                val voice = tts.voices.orEmpty().firstOrNull { it.name == name }
                assertTrue("voice $name is not on this phone", voice != null)
                tts.voice = voice
            }
            val dir = File(out, language).apply { mkdirs() }
            for (line in lines) {
                val file = File(dir, "${line.file}.wav")
                if (file.length() > MIN_BYTES) continue // made on an earlier run
                val latch = CountDownLatch(1)
                finished[line.file] = latch
                assertEquals(TextToSpeech.SUCCESS, tts.synthesizeToFile(line.text, Bundle(), file, line.file))
                assertTrue("timed out on ${line.text}", latch.await(30, TimeUnit.SECONDS))
            }
        }
        tts.shutdown()
    }

    private companion object {
        const val MIN_BYTES = 1000L
    }
}
