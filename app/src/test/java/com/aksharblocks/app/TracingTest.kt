package com.aksharblocks.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.BeforeClass
import org.junit.Test
import java.io.File

class TracingTest {

    companion object {
        @JvmStatic
        @BeforeClass
        fun loadContent() {
            Content.loadFrom(File("src/main/assets"))
        }
    }

    @Test
    fun parsesStraightAndCurvedStrokes() {
        val strokes = Tracing.parseStrokes("0.5,0 0.1,1 ; ~0.2,0 0.8,0.5 0.2,1")
        assertEquals(2, strokes.size)
        assertFalse(strokes[0].smooth)
        assertEquals(listOf(P(0.5f, 0f), P(0.1f, 1f)), strokes[0].points)
        assertTrue(strokes[1].smooth)
        assertEquals(3, strokes[1].points.size)
    }

    @Test
    fun everyEnglishLetterHasStrokesInsideItsBox() {
        for (letter in Track.ENGLISH.letters) {
            val strokes = Content.strokes(Track.ENGLISH, letter)
            requireNotNull(strokes) { "no strokes for ${letter.symbol}" }
            assertTrue(letter.symbol, strokes.isNotEmpty())
            for (point in strokes.flatMap { it.points }) {
                assertTrue("${letter.symbol} $point", point.x in 0f..1f && point.y in 0f..1f)
            }
        }
    }

    @Test
    fun hindiLettersFallBackToTheirShape() {
        assertNull(Content.strokes(Track.SWAR, Track.SWAR.letters.first()))
    }

    @Test
    fun sampledStrokeStartsAndEndsOnItsPoints() {
        val stroke = Stroke(listOf(P(0f, 0f), P(1f, 0f), P(1f, 1f)), smooth = true)
        val samples = stroke.sample(perSegment = 8)
        assertEquals(P(0f, 0f), samples.first())
        assertEquals(P(1f, 1f), samples.last())
        assertEquals(2 * 8 + 1, samples.size)
    }

    private fun gridForLetterT(): Pair<TraceGrid, List<List<P>>> {
        val strokes = Tracing.parseStrokes("0.12,0 0.88,0 ; 0.5,0 0.5,1").map { it.sample() }
        val grid = TraceGrid()
        strokes.forEach { grid.addTargetLine(it, Tracing.TARGET_RADIUS) }
        return grid to strokes
    }

    @Test
    fun tracingOverTheLetterPasses() {
        val (grid, strokes) = gridForLetterT()
        for (line in strokes) for (i in 1 until line.size) grid.paint(line[i - 1], line[i], Tracing.CRAYON_RADIUS)
        assertTrue("coverage ${grid.coverage}", grid.coverage >= Tracing.PASS_WITH_STROKES)
        assertTrue("outside ${grid.outsideShare}", grid.outsideShare <= Tracing.MAX_OUTSIDE)
    }

    @Test
    fun tracingHalfTheLetterIsNotEnough() {
        val (grid, strokes) = gridForLetterT()
        val top = strokes[0]
        for (i in 1 until top.size) grid.paint(top[i - 1], top[i], Tracing.CRAYON_RADIUS)
        assertTrue("coverage ${grid.coverage}", grid.coverage < Tracing.PASS_WITH_STROKES)
    }

    @Test
    fun letterAIsNotDoneUntilItsBarIsDrawn() {
        val strokes = Content.strokes(Track.ENGLISH, Track.ENGLISH.letters.first())!!.map { it.sample() }
        val grid = TraceGrid()
        strokes.forEach { grid.addTargetLine(it, Tracing.TARGET_RADIUS) }
        fun trace(line: List<P>) {
            for (i in 1 until line.size) grid.paint(line[i - 1], line[i], Tracing.CRAYON_RADIUS)
        }

        // Both slanted sides alone already cover most of the letter...
        trace(strokes[0])
        trace(strokes[1])
        assertTrue("coverage ${grid.coverage}", grid.coverage >= Tracing.PASS_WITH_STROKES)
        // ...but the bar is still missing, so it must not count yet.
        assertTrue("weakest ${grid.weakestStroke}", grid.weakestStroke < Tracing.PASS_EACH_STROKE)

        trace(strokes[2])
        assertTrue("weakest ${grid.weakestStroke}", grid.weakestStroke >= Tracing.PASS_EACH_STROKE)
    }

    @Test
    fun scribblingOverTheWholeBoxDoesNotPass() {
        val (grid, _) = gridForLetterT()
        var y = 0f
        while (y <= 1f) {
            grid.paint(P(0f, y), P(1f, y), Tracing.CRAYON_RADIUS)
            y += 0.1f
        }
        assertTrue("coverage ${grid.coverage}", grid.coverage >= Tracing.PASS_WITH_STROKES)
        assertTrue("outside ${grid.outsideShare}", grid.outsideShare > Tracing.MAX_OUTSIDE)
    }
}
