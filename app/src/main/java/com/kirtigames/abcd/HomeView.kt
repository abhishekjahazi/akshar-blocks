package com.kirtigames.abcd

import android.content.Context
import android.graphics.Canvas
import android.graphics.RectF
import kotlin.math.min

/** First screen: pick English ABC, Hindi vowels or Hindi consonants. */
class HomeView(context: Context, speaker: Speaker, player: Player) : GameView(context, speaker, player) {

    var onPick: ((Track) -> Unit)? = null

    /** Grown-ups button (goes through the parent gate). */
    var onParent: (() -> Unit)? = null

    /** The child name tag was tapped. */
    var onChild: (() -> Unit)? = null

    override val showHomeButton = false

    private val tracks = Track.entries
    private val cards = tracks.map { RectF() }
    private val titleBlocks = List(TITLE.size) { RectF() }
    private val miniBlock = RectF()
    private val parentButton = RectF()
    private val childTag = RectF()

    override fun drawGame(canvas: Canvas) {
        drawTopRow(canvas)

        val landscape = width > height
        val titleTop = contentTop - dp(16f)
        val titleBottom = titleTop + height * (if (landscape) 0.22f else 0.14f)
        drawTitleBlocks(canvas, TITLE, TITLE_COLORS, titleTop, titleBottom, titleBlocks)

        // A grid of sections: two columns in portrait, three in landscape.
        val gap = dp(14f)
        val areaTop = titleBottom + dp(8f)
        val cols = if (landscape) 3 else 2
        val rows = (tracks.size + cols - 1) / cols
        val cellW = (contentRight - contentLeft) / cols
        val cellH = (contentBottom - areaTop) / rows
        tracks.forEachIndexed { i, track ->
            val card = cards[i]
            val left = contentLeft + cellW * (i % cols)
            val top = areaTop + cellH * (i / cols)
            card.set(left + gap / 2f, top + gap / 2f, left + cellW - gap / 2f, top + cellH - gap / 2f - dp(8f))

            val appear = popIn((time - 0.4f - i * 0.1f) / 0.4f)
            if (appear <= 0.01f) return@forEachIndexed
            canvas.save()
            canvas.scale(appear, appear, card.centerX(), card.centerY())
            val sink = drawBlock(canvas, card, track.color, radius = dp(30f), depth = dp(10f))
            val h = card.height()
            val w = card.width()
            drawText(canvas, track.label, card.centerX(), card.top + h * 0.24f + sink, min(h * 0.17f, dp(30f)), Palette.WHITE, w * 0.88f)
            drawText(canvas, track.subtitle, card.centerX(), card.top + h * 0.43f + sink, min(h * 0.09f, dp(16f)), 0xDDFFFFFF.toInt(), w * 0.88f)

            // A row of the track's first letters on little white blocks.
            val sample = track.letters.take(SAMPLE_SIZE)
            val side = min(h * 0.28f, (w - dp(20f)) / SAMPLE_SIZE - dp(6f))
            val rowWidth = side * SAMPLE_SIZE + dp(6f) * (SAMPLE_SIZE - 1)
            var x = card.centerX() - rowWidth / 2f
            val y = card.top + h * 0.72f + sink
            sample.forEachIndexed { j, letter ->
                miniBlock.set(x, y - side / 2f, x + side, y + side / 2f)
                drawBlock(canvas, miniBlock, Palette.WHITE, depth = dp(4f), pressable = false)
                drawText(canvas, letter.symbol, miniBlock.centerX(), miniBlock.centerY(), side * 0.62f, track.colorFor(j), side * 0.86f)
                x += side + dp(6f)
            }
            canvas.restore()
        }
    }

    /** Grown-ups lock on the left, then the playing child's picture and name. */
    private fun drawTopRow(canvas: Canvas) {
        val size = dp(52f)
        val barY = topBarCenter
        parentButton.set(contentLeft + dp(4f), barY - size / 2f, contentLeft + dp(4f) + size, barY + size / 2f)
        val lockSink = drawBlock(canvas, parentButton, Palette.WHITE, depth = dp(5f))
        drawEmoji(canvas, "🔒", parentButton.centerX(), parentButton.centerY() + lockSink, size * 0.45f)

        val label = "${player.profile.avatar} ${player.profile.name}"
        textPaint.textSize = dp(22f)
        val maxWidth = width * 0.42f
        val tagWidth = min(textPaint.measureText(label) + dp(32f), maxWidth)
        childTag.set(parentButton.right + dp(12f), barY - size / 2f, parentButton.right + dp(12f) + tagWidth, barY + size / 2f)
        val tagSink = drawBlock(canvas, childTag, Palette.WHITE, radius = size / 2f, depth = dp(5f))
        drawText(canvas, label, childTag.centerX(), childTag.centerY() + tagSink, dp(22f), Palette.INK, tagWidth - dp(24f))
    }

    override fun onTap(x: Float, y: Float) {
        if (parentButton.contains(x, y)) {
            onParent?.invoke()
            return
        }
        if (childTag.contains(x, y)) {
            onChild?.invoke()
            return
        }
        val block = titleBlocks.indexOfFirst { it.contains(x, y) }
        if (block >= 0) {
            val track = TITLE_TRACKS[block]
            speaker.say(track.lang.name(track.letters[0]), track.lang.locale)
            return
        }
        val index = cards.indexOfFirst { it.contains(x, y) }
        if (index >= 0) onPick?.invoke(tracks[index])
    }

    private companion object {
        const val SAMPLE_SIZE = 4
        val TITLE = listOf("A", "अ", "क")
        val TITLE_TRACKS = listOf(Track.ENGLISH, Track.SWAR, Track.VYANJAN)
        val TITLE_COLORS = listOf(Palette.OCEAN, Palette.TOMATO, Palette.GRASS)
    }
}
