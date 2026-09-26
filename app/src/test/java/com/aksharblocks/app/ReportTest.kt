package com.aksharblocks.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ReportTest {

    private val symbols = listOf("A", "B", "C", "D")

    @Test
    fun nothingPlayedYet() {
        val report = Report.build(symbols, emptyMap(), emptyMap(), emptyMap())
        assertFalse(report.played)
        assertEquals(0, report.known)
        assertEquals(4, report.total)
    }

    @Test
    fun letterIsKnownAfterEnoughRightAnswers() {
        val ok = mapOf("A" to Report.KNOWN_AFTER, "B" to Report.KNOWN_AFTER - 1)
        val report = Report.build(symbols, ok, emptyMap(), emptyMap())
        assertTrue(report.played)
        assertEquals(1, report.known)
    }

    @Test
    fun practiceListsLettersMissedMoreThanTheyAreRight_worstFirst() {
        val ok = mapOf("A" to 10, "B" to 1, "C" to 0)
        val miss = mapOf("A" to 2, "B" to 3, "C" to 5, "D" to 1)
        val report = Report.build(symbols, ok, miss, emptyMap())
        // A is mostly right; D was missed only once.
        assertEquals(listOf("C", "B"), report.practice)
    }

    @Test
    fun mixUpsCombineBothDirections() {
        val mix = mapOf(("B" to "D") to 1, ("D" to "B") to 2, ("A" to "C") to 1)
        val report = Report.build(symbols, emptyMap(), mapOf("B" to 1), mix)
        assertEquals(listOf("B" to "D"), report.mixUps)
    }
}

class MasteryTest {

    private fun stats(ok: Int, miss: Int) = LetterStats(mapOf("B" to ok), mapOf("B" to miss), emptyMap())

    @Test
    fun letterMapColours() {
        assertEquals(Mastery.NOT_YET, Report.mastery("B", LetterStats.EMPTY))
        assertEquals(Mastery.LEARNING, Report.mastery("B", stats(ok = 1, miss = 0)))
        assertEquals(Mastery.KNOWN, Report.mastery("B", stats(ok = 3, miss = 1)))
        assertEquals(Mastery.PRACTICE, Report.mastery("B", stats(ok = 1, miss = 3)))
        // Known wins only while right answers outnumber mistakes.
        assertEquals(Mastery.PRACTICE, Report.mastery("B", stats(ok = 3, miss = 4)))
    }

    @Test
    fun levels() {
        assertTrue(Report.level(0f).contains("Just beginning"))
        assertTrue(Report.level(0.1f).contains("Getting started"))
        assertTrue(Report.level(0.5f).contains("Learning well"))
        assertTrue(Report.level(0.8f).contains("Almost there"))
        assertTrue(Report.level(1f).contains("Star learner"))
    }
}
