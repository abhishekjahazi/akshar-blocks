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
        assertTrue(VoiceScript.lines().getValue("mr").any { Voice.key(it.text) == "शोधा" })
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

class RhymesAndNamesTest {

    companion object {
        @JvmStatic
        @BeforeClass
        fun loadContent() {
            Content.loadFrom(File("src/main/assets"))
        }
    }

    @Test
    fun rhymesLoadWithTheirLanguages() {
        val rhymes = Rhymes.all
        assertEquals(12, rhymes.size)
        assertEquals("Twinkle, Twinkle", rhymes.first().title)
        assertEquals(6, rhymes.first().lines.size)
        assertEquals(setOf("en", "hi", "mr"), rhymes.map { it.language }.toSet())
        rhymes.forEach { assertTrue(it.title, it.lines.size in 2..8 && it.emoji.isNotBlank()) }
        // Every line is recorded in the rhyme's own language.
        val hindi = VoiceScript.lines().getValue("hi").map { Voice.key(it.text) }.toSet()
        assertTrue(Voice.key("मछली जल की रानी है,") in hindi)
    }

    @Test
    fun englishNamesStartWithACapital() {
        val aarav = NameLetters.of("Aarav 2")
        assertEquals(listOf("A", "a", "r", "a", "v"), aarav.map { it.letter.symbol })
        assertEquals(Track.ENGLISH, aarav.first().track)
        assertTrue(aarav.drop(1).all { it.track == Track.LOWER })
        assertEquals(listOf("A", "N", "U").size, NameLetters.of("ANU").size)
        assertEquals("n", NameLetters.of("ANU")[1].letter.symbol)
    }

    @Test
    fun hindiNamesSplitIntoBlocks() {
        val riya = NameLetters.of("रिया")
        assertEquals(listOf("रि", "या"), riya.map { it.letter.symbol })
        assertTrue(riya.all { it.track == Track.BARAKHADI })
        assertEquals(Track.SWAR, NameLetters.of("आरव").first().track)
    }

    @Test
    fun homeCardShowsTheNameOrAPencil() {
        assertEquals(listOf("C", "h", "i"), NameLetters.sample("Child 1", 3))
        assertEquals(listOf("✍️"), NameLetters.sample("123", 3))
        assertTrue(NameLetters.of("A very long name indeed").size <= NameLetters.MAX)
    }
}

class CertificatesTest {

    companion object {
        @JvmStatic
        @BeforeClass
        fun loadContent() {
            Content.loadFrom(File("src/main/assets"))
        }
    }

    @Test
    fun goalsAreReachableMilestones() {
        assertEquals(21, Certificates.goal(Track.ENGLISH))
        assertEquals(11, Certificates.goal(Track.SWAR))
        assertEquals(20, Certificates.goal(Track.NUMBERS))
        assertEquals(60, Certificates.goal(Track.BARAKHADI))
        for (track in Track.entries) {
            val goal = Certificates.goal(track)
            assertTrue(track.name, goal in 1..track.letters.size)
            assertTrue(track.name, Certificates.achievement(track).isNotBlank())
        }
    }
}
