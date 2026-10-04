package com.aksharblocks.app

import android.content.Context
import android.graphics.Canvas
import android.graphics.RectF
import kotlin.math.abs
import kotlin.math.min
import kotlin.math.sin

/**
 * One rhyme: its picture, and its lines read out one by one with the line being said lit up.
 * Tapping a line plays from there; at the end the child gets a star.
 */
class RhymeView(
    context: Context, speaker: Speaker, player: Player, private val rhyme: Rhyme,
) : GameView(context, speaker, player) {

    override val title = rhyme.title
    override val skyColor = Palette.PEACH

    /** The line being said, or -1 before starting and after the end. */
    private var current = -1
    private var playing = false

    /** Changes on every play, pause or jump, so a line finishing late can tell it's stale. */
    private var playToken = 0

    private val picture = RectF()
    private val textArea = RectF()
    private val lineRects = rhyme.lines.map { RectF() }
    private val playButton = RectF()
    private val againButton = RectF()

    init {
        after(0.6f) { play(0) }
    }

    private fun play(from: Int) {
        playing = true
        current = from
        playToken++
        sayLine()
    }

    private fun sayLine() {
        val token = playToken
        speaker.say(rhyme.lines[current], rhyme.locale) {
            if (token != playToken || !playing) return@say
            if (current < rhyme.lines.lastIndex) {
                after(LINE_GAP) {
                    if (token == playToken && playing) {
                        current++
                        sayLine()
                    }
                }
            } else {
                finish()
            }
        }
    }

    private fun pause() {
        playing = false
        playToken++
        speaker.stop()
    }

    private fun finish() {
        playing = false
        current = -1
        addStar()
        celebrate(picture.centerX(), picture.centerY(), 50)
    }

    override fun drawGame(canvas: Canvas) {
        val landscape = width > height
        val button = dp(68f)
        val buttonTop = contentBottom - button - dp(6f)
        playButton.set(width / 2f - dp(10f) - button * 1.5f, buttonTop, width / 2f - dp(10f), buttonTop + button)
        againButton.set(width / 2f + dp(10f), buttonTop, width / 2f + dp(10f) + button * 1.5f, buttonTop + button)

        // Picture: on top in portrait, on the left in landscape.
        if (landscape) {
            val side = min(buttonTop - contentTop - dp(16f), (contentRight - contentLeft) * 0.3f)
            picture.set(contentLeft, contentTop, contentLeft + side, contentTop + side)
            textArea.set(picture.right + dp(16f), contentTop, contentRight, buttonTop - dp(16f))
        } else {
            val side = min((buttonTop - contentTop) * 0.3f, contentRight - contentLeft)
            picture.set(width / 2f - side / 2f, contentTop, width / 2f + side / 2f, contentTop + side)
            textArea.set(contentLeft, picture.bottom + dp(12f), contentRight, buttonTop - dp(16f))
        }
        // The picture sways gently while the rhyme plays.
        val sway = if (playing && motionEnabled) sin(time * 5f) * dp(6f) else 0f
        drawEmoji(canvas, rhyme.emoji, picture.centerX() + sway, picture.centerY() - abs(sway) * 0.5f, picture.height() * 0.85f)

        // The lines, the one being said on a yellow block.
        val rowH = min(textArea.height() / rhyme.lines.size, dp(64f))
        val top = textArea.top + (textArea.height() - rowH * rhyme.lines.size) / 2f
        rhyme.lines.forEachIndexed { i, line ->
            val rect = lineRects[i]
            rect.set(textArea.left, top + rowH * i + dp(3f), textArea.right, top + rowH * (i + 1) - dp(3f))
            val lit = i == current
            if (lit) drawBlock(canvas, rect, Palette.SUN, radius = dp(14f), depth = dp(4f), pressable = false)
            drawText(canvas, line, rect.centerX(), rect.centerY(), min(rowH * 0.42f, dp(24f)), if (lit) Palette.NAVY else Theme.subtext, rect.width() * 0.94f)
        }

        // Play / pause, and start again.
        val playSink = drawBlock(canvas, playButton, Palette.GRASS, depth = dp(6f))
        drawEmoji(canvas, if (playing) "⏸️" else "▶️", playButton.centerX(), playButton.centerY() + playSink, button * 0.5f)
        val againSink = drawBlock(canvas, againButton, Palette.WHITE, depth = dp(6f))
        drawEmoji(canvas, "🔁", againButton.centerX(), againButton.centerY() + againSink, button * 0.5f)
    }

    override fun onTap(x: Float, y: Float) {
        when {
            playButton.contains(x, y) -> if (playing) pause() else play(current.coerceAtLeast(0))
            againButton.contains(x, y) -> play(0)
            else -> {
                val line = lineRects.indexOfFirst { it.contains(x, y) }
                if (line >= 0) play(line)
            }
        }
    }

    private companion object {
        /** A short breath between lines. */
        const val LINE_GAP = 0.25f
    }
}
