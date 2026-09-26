package com.kirtigames.abcd

import android.content.Context
import android.graphics.Canvas
import android.graphics.RectF
import kotlin.math.exp
import kotlin.math.min
import kotlin.math.sin

/**
 * The बारहखड़ी chart for one consonant: twelve syllable blocks. Tapping one says
 * "क, आ, का" and shows how it is made (क + ◌ा = का). 🔊 plays the whole row.
 */
class BarakhadiView(
    context: Context, speaker: Speaker, player: Player, private val track: Track,
) : GameView(context, speaker, player) {

    private val consonants = track.letters.mapNotNull { it.group }.distinct()
    private var index = 0
    private var row: List<Letter> = emptyList()

    /** The syllable last tapped or played, and when (for its bounce). */
    private var selected = -1
    private var selectedAt = 0f

    /** Changes when the consonant changes or playback restarts, so old playback steps stop. */
    private var playToken = 0

    private val cells = Array(Barakhadi.matras.size) { RectF() }
    private val drawRect = RectF()
    private val consonantBlock = RectF()
    private val prevButton = RectF()
    private val nextButton = RectF()
    private val playButton = RectF()

    private val consonant get() = consonants[index]

    override val title get() = BarakhadiWords.title(consonant)
    override val showStars = false
    override val skyColor = Palette.LILAC

    init {
        showConsonant(0)
    }

    private fun showConsonant(i: Int) {
        index = (i + consonants.size) % consonants.size
        row = track.letters.filter { it.group == consonant }
        selected = -1
        playToken++
        speaker.say(BarakhadiWords.intro(consonant), Hindi.locale)
    }

    private fun sayCell(i: Int) {
        playToken++
        selected = i
        selectedAt = time
        speaker.say(BarakhadiWords.sound(consonant, Barakhadi.matras[i], row[i].symbol), Hindi.locale)
    }

    /** Reads the whole row, lighting up each syllable in turn. */
    private fun playAll() {
        val token = ++playToken
        row.forEachIndexed { i, syllable ->
            after(i * STEP_SECONDS) {
                if (token == playToken) {
                    selected = i
                    selectedAt = time
                    speaker.say(syllable.symbol, Hindi.locale)
                }
            }
        }
    }

    override fun drawGame(canvas: Canvas) {
        val landscape = width > height
        val big = dp(84f)

        // Row 1: ◀ consonant ▶
        val top = contentTop
        consonantBlock.set(width / 2f - big / 2f, top, width / 2f + big / 2f, top + big)
        val arrow = dp(64f)
        val arrowY = top + (big - arrow) / 2f
        prevButton.set(consonantBlock.left - dp(28f) - arrow, arrowY, consonantBlock.left - dp(28f), arrowY + arrow)
        nextButton.set(consonantBlock.right + dp(28f), arrowY, consonantBlock.right + dp(28f) + arrow, arrowY + arrow)
        drawArrowBlock(canvas, prevButton, Palette.SUN, pointsRight = false)
        drawArrowBlock(canvas, nextButton, Palette.SUN, pointsRight = true)
        drawLetterBlock(canvas, consonantBlock, Palette.INK, consonant, Palette.WHITE, textScale = 0.6f)

        // Row 2: how the chosen syllable is made, and the play-all button.
        val equationY = consonantBlock.bottom + dp(52f)
        val play = dp(56f)
        playButton.set(contentRight - play, equationY - play / 2f, contentRight, equationY + play / 2f)
        val playSink = drawBlock(canvas, playButton, Palette.WHITE, depth = dp(5f))
        drawEmoji(canvas, "🔊", playButton.centerX(), playButton.centerY() + playSink, play * 0.5f)
        val equation = if (selected >= 0) {
            val matra = Barakhadi.matras[selected]
            "$consonant + ${matra.shown} = ${row[selected].symbol}"
        } else {
            "छूकर सुनो 👇"
        }
        drawText(canvas, equation, (contentLeft + playButton.left) / 2f, equationY, dp(34f), Palette.INK, playButton.left - contentLeft - dp(12f))

        // The twelve syllables.
        val gridTop = equationY + dp(44f)
        val cols = if (landscape) 6 else 3
        val rows = (row.size + cols - 1) / cols
        val cellW = (contentRight - contentLeft) / cols
        val cellH = (contentBottom - gridTop) / rows
        val half = min(cellW, cellH) * 0.42f
        row.forEachIndexed { i, syllable ->
            val cx = contentLeft + cellW * (i % cols + 0.5f)
            val cy = gridTop + cellH * (i / cols + 0.5f)
            squareAt(cells[i], cx, cy, half)
            var scale = 1f
            if (i == selected && motionEnabled) {
                val t = time - selectedAt
                scale += 0.12f * exp(-5f * t) * sin(t * 16f)
            }
            squareAt(drawRect, cx, cy, half * scale)
            val face = if (i == selected) Palette.INK else track.colorFor(i)
            drawLetterBlock(canvas, drawRect, face, syllable.symbol, Palette.WHITE, textScale = 0.5f)
        }
    }

    override fun onTap(x: Float, y: Float) {
        when {
            prevButton.contains(x, y) -> showConsonant(index - 1)
            nextButton.contains(x, y) -> showConsonant(index + 1)
            consonantBlock.contains(x, y) -> speaker.say(consonant, Hindi.locale)
            playButton.contains(x, y) -> playAll()
            else -> {
                val i = cells.indexOfFirst { it.contains(x, y) }
                if (i >= 0 && i < row.size) sayCell(i)
            }
        }
    }

    override fun onSwipe(towardsLeft: Boolean) = showConsonant(index + if (towardsLeft) 1 else -1)

    private companion object {
        const val STEP_SECONDS = 1.1f
    }
}
