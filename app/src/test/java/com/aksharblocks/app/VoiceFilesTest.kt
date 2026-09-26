package com.aksharblocks.app

import org.junit.Assert.assertTrue
import org.junit.BeforeClass
import org.junit.Test
import java.io.File

/** The built-in voice must cover everything the games say. */
class VoiceFilesTest {

    companion object {
        @JvmStatic
        @BeforeClass
        fun loadContent() {
            Content.loadFrom(File("src/main/assets"))
        }
    }

    @Test
    fun everyScriptLineHasAVoiceFile() {
        for ((language, lines) in VoiceScript.lines()) {
            val files = File("src/main/assets/voice/$language").list().orEmpty()
                .map { it.substringBeforeLast('.') }.toSet()
            val missing = lines.filter { it.file !in files }
            assertTrue(
                "$language: ${missing.size} lines have no voice file, e.g. ${missing.take(5).map { it.text }}. " +
                    "Remake the voice (store/voice/make-voice.sh).",
                missing.isEmpty(),
            )
        }
    }

    @Test
    fun voiceFilesAreSmall() {
        val files = File("src/main/assets/voice").walk().filter { it.isFile }.toList()
        assertTrue(files.isNotEmpty())
        val big = files.filter { it.length() > 60_000 }
        assertTrue("unexpectedly large clips: ${big.map { it.name }}", big.isEmpty())
    }
}
