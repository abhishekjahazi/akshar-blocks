package com.aksharblocks.app

import android.speech.tts.TextToSpeech
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

/**
 * Not a test of the app: reports which voices this phone has for each app language, into
 * voice-check.txt in the app's files (see store/voice/README.md). Run it to see whether
 * Marathi gets its own voice or falls back to the Hindi one.
 */
@RunWith(AndroidJUnit4::class)
class VoiceCheck {

    @Test
    fun listVoices() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val context = instrumentation.targetContext
        val started = CountDownLatch(1)
        lateinit var tts: TextToSpeech
        instrumentation.runOnMainSync { tts = TextToSpeech(context) { started.countDown() } }
        assertTrue(started.await(30, TimeUnit.SECONDS))

        val report = StringBuilder()
        for (language in Voice.LANGUAGES) {
            val locale = Voice.localeOf(language)
            report.append("$language: availability=${tts.isLanguageAvailable(locale)}\n")
            tts.voices.orEmpty()
                .filter { it.locale.language == locale.language }
                .sortedBy { it.name }
                .forEach { voice ->
                    val installed = TextToSpeech.Engine.KEY_FEATURE_NOT_INSTALLED !in voice.features.orEmpty()
                    report.append("  ${voice.name} network=${voice.isNetworkConnectionRequired} installed=$installed\n")
                }
        }
        File(requireNotNull(context.getExternalFilesDir(null)), "voice-check.txt").writeText(report.toString())
        tts.shutdown()
    }
}
