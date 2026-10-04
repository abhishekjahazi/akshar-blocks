package com.aksharblocks.app

import android.content.Context
import android.graphics.Canvas
import android.graphics.RectF
import kotlin.math.min

enum class GameMode(val emoji: String, val color: Int, val textColor: Int) {
    LEARN("📖", Palette.OCEAN, Palette.WHITE),
    TRACE("✏️", Palette.ORANGE, Palette.WHITE),
    BUILD("➕", Palette.TEAL, Palette.WHITE),
    COUNT("🔢", Palette.GRAPE, Palette.WHITE),
    MEMORY("🃏", Palette.PINK, Palette.WHITE),
    WORDS("🔤", Palette.TOMATO, Palette.WHITE),
    FIND("🔍", Palette.GRASS, Palette.WHITE),
    BALLOONS("🎈", Palette.SUN, Palette.NAVY),
    MATCH("🍎", Palette.GRAPE, Palette.WHITE),
    SOUNDS("👂", Palette.TEAL, Palette.WHITE),
}

/** The four games for one track, under that track's first letters dropping in as blocks. */
class TrackMenuView(
    context: Context, speaker: Speaker, player: Player, private val track: Track,
) : GameView(context, speaker, player) {

    var onPick: ((GameMode) -> Unit)? = null

    override val title = track.label

    private val modes = track.modes
    private val cards = modes.map { RectF() }
    private val sample = track.letters.take(4)
    private val titleBlocks = List(sample.size) { RectF() }

    override fun drawGame(canvas: Canvas) {
        val landscape = width > height
        val titleTop = contentTop
        val titleBottom = titleTop + height * (if (landscape) 0.24f else 0.16f)
        drawTitleBlocks(canvas, sample.map { it.symbol }, sample.indices.map(track::colorFor), titleTop, titleBottom, titleBlocks)

        val bottom = contentBottom
        val cols = if (landscape) modes.size else 2
        val rows = (modes.size + cols - 1) / cols
        val gap = dp(18f)
        val areaTop = titleBottom + dp(16f)
        val cellW = (contentRight - contentLeft) / cols
        val cellH = (bottom - areaTop) / rows
        modes.forEachIndexed { i, mode ->
            // A last row with fewer cards is centered.
            val row = i / cols
            val inRow = minOf(cols, modes.size - row * cols)
            val left = contentLeft + (cols - inRow) * cellW / 2f + cellW * (i % cols)
            val top = areaTop + cellH * row
            val card = cards[i]
            card.set(left + gap / 2f, top + gap / 2f, left + cellW - gap / 2f, top + cellH - gap / 2f - dp(8f))

            // Cards pop in once the title blocks have landed.
            val appear = popIn((time - 0.5f - i * 0.08f) / 0.4f)
            if (appear <= 0.01f) return@forEachIndexed
            canvas.save()
            canvas.scale(appear, appear, card.centerX(), card.centerY())
            val size = min(card.width(), card.height())
            val sink = drawBlock(canvas, card, mode.color, radius = dp(30f), depth = dp(10f))
            drawEmoji(canvas, mode.emoji, card.centerX(), card.top + card.height() * 0.42f + sink, size * 0.4f)
            drawText(
                canvas, track.lang.modeLabel(mode), card.centerX(), card.top + card.height() * 0.8f + sink,
                size * 0.14f, mode.textColor, card.width() * 0.86f,
            )
            canvas.restore()
        }
    }

    override fun onTap(x: Float, y: Float) {
        val block = titleBlocks.indexOfFirst { it.contains(x, y) }
        if (block >= 0) {
            speaker.say(track.lang.name(sample[block]), track.lang.locale)
            return
        }
        val index = cards.indexOfFirst { it.contains(x, y) }
        if (index < 0) return
        val mode = modes[index]
        Sounds.play(Sound.TAP)
        speaker.say(track.lang.modeLabel(mode), track.lang.locale)
        onPick?.invoke(mode)
    }
}
