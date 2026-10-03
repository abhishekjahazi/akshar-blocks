package com.aksharblocks.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import kotlin.math.abs

class PolishTest {

    @Test
    fun soundsAreShortAndGentle() {
        for (sound in Sound.entries) {
            val samples = Synth.samples(sound)
            val seconds = samples.size / Synth.RATE.toFloat()
            assertTrue("$sound lasts $seconds s", seconds in 0.03f..1f)
            val peak = samples.maxOf { abs(it.toInt()) } / Short.MAX_VALUE.toFloat()
            assertTrue("$sound peak $peak", peak in 0.05f..0.35f)
        }
    }

    @Test
    fun soundsFadeOutWithoutAClick() {
        for (sound in Sound.entries) {
            val samples = Synth.samples(sound)
            val tail = samples.takeLast(50).maxOf { abs(it.toInt()) } / Short.MAX_VALUE.toFloat()
            assertTrue("$sound ends at $tail", tail < 0.05f)
        }
    }

    @Test
    fun artFileNamesMatchNoto() {
        assertEquals("emoji_u1f34e.png", Art.fileName("🍎"))
        assertEquals("emoji_u2600.png", Art.fileName("☀️"))           // variation selector dropped
        assertEquals("emoji_u0031_20e3.png", Art.fileName("1️⃣"))      // keycap
        assertEquals("emoji_u1f468_200d_2696.png", Art.fileName("👨‍⚖️")) // joined sequence
    }

    @Test
    fun everyPictureInTheContentHasArtwork() {
        Content.loadFrom(File("src/main/assets"))
        val art = File("src/main/assets/art").list().orEmpty().toSet()
        val used = Track.entries.flatMap { it.letters }.mapNotNull { it.emoji } +
            Track.entries.flatMap { it.words }.map { it.emoji } +
            Stickers.all.map { it.emoji } + Counting.things.map { it.emoji } + GameMode.entries.map { it.emoji } +
            Rhymes.all.map { it.emoji } + Track.entries.map { Certificates.medal(it) } +
            // Pictures drawn by the screens themselves.
            listOf("✍️", "🎵", "⭐", "🐟", "👍", "▶️", "⏸️", "🔁", "🏆", "🔊", "🔄", "👆", "✅", "🔒", "🏠", "🌙", "👋")
        val missing = used.toSet().filter { Art.fileName(it) !in art }
        assertTrue("no artwork for $missing", missing.isEmpty())
    }
}
