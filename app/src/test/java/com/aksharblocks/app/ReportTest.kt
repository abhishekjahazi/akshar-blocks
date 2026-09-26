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
