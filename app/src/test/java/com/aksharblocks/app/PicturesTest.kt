package com.aksharblocks.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.BeforeClass
import org.junit.Test
import java.io.File
import java.util.Locale
import kotlin.random.Random

/** Colors, shapes, animals and Marathi. */
class PicturesTest {

    companion object {
        @JvmStatic
        @BeforeClass
        fun loadContent() {
            Content.loadFrom(File("src/main/assets"))
        }
    }

    @Test
    fun pictureTracksLoad() {
        assertEquals(9, Track.COLORS.letters.size)
        assertEquals(7, Track.SHAPES.letters.size)
        assertEquals(18, Track.ANIMALS.letters.size)
        assertEquals(Letter("🐄", "Cow", "🐄", hindi = "गाय", sound = "moo"), Track.ANIMALS.letters.first())
    }

    @Test
    fun everyPictureHasAnEnglishAndHindiName() {
        for (track in Track.entries.filter { it.isPictures }) {
            for (item in track.letters) {
                assertTrue("${track.name} ${item.symbol}", !item.word.isNullOrBlank() && !item.hindi.isNullOrBlank())
                assertEquals(item.symbol, item.emoji)
            }
            // Memory needs six different pictures.
            assertTrue(track.name, track.pictureLetters.size >= 6)
        }
    }

    @Test
    fun enoughAnimalsMakeASoundForTheSoundsGame() {
        val withSound = Track.ANIMALS.letters.filter { it.sound != null }
        assertTrue(withSound.size >= 8)
        assertEquals(withSound.size, withSound.map { it.sound }.toSet().size)
    }

    @Test
    fun pictureTracksSayTheHindiNameInHindi() {
        val cow = Track.ANIMALS.letters.first()
        val segments = Voice.segments(AnimalWords.learn(cow), "en")
        assertEquals(
            listOf(Voice.Segment("en", "Cow!"), Voice.Segment("hi", "गाय!"), Voice.Segment("en", "The cow says moo!")),
            segments,
        )
        assertEquals(
            listOf(Locale.forLanguageTag("en-IN") to "Cow!", Locale.forLanguageTag("hi-IN") to "गाय!", Locale.forLanguageTag("en-IN") to "The cow says moo!"),
            Voice.spokenRuns(AnimalWords.learn(cow), English.locale),
        )
        assertEquals("Find the color Red! लाल!", Voice.spokenText(ColorWords.find(Track.COLORS.letters.first())))
        assertEquals("Who says moo?", Voice.spokenText(AnimalWords.soundAsk(cow)))
    }

    @Test
    fun pictureFindGamesDontShowTheAnswer() {
        assertEquals("?", Track.COLORS.prompt(Track.COLORS.letters.first()))
        assertEquals("B", Track.LOWER.prompt(Letter("b")))
    }

    @Test
    fun marathiHasItsLettersAndVoice() {
        val letters = Track.MARATHI.letters.map { it.symbol }
        assertEquals(49, letters.size) // 13 vowels, 36 consonants (with ळ, क्ष, ज्ञ)
        assertEquals("अ", letters.first())
        assertTrue("ळ" in letters)
        assertEquals("mr", Voice.languageOf(Marathi.locale))
        assertEquals("क शोधा!", Voice.spokenText(Marathi.find(Letter("क"))))
        assertTrue(Marathi.learn(Track.MARATHI.letters.first()).contains("अननस"))
        // Marathi lines are recorded in their own voice.
        assertTrue(VoiceScript.lines().getValue("mr").any { it.text == "शोधा!" })
        assertTrue(Voice.clipId("mr", "शोधा").startsWith("mr_"))
    }

    @Test
    fun everyGameModeHasALabelInEveryLanguage() {
        for (lang in listOf(English, Hindi, Marathi, AnimalWords)) {
            GameMode.entries.forEach { assertTrue(lang.modeLabel(it).isNotBlank()) }
        }
        assertTrue(AnimalWords.soundRight(Track.ANIMALS.letters.first(), Random(1)).endsWith("The cow says moo!"))
    }
}
