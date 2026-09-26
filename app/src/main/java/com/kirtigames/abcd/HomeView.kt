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
        val titleBottom = titleTop + height * (if (landscape) 0.26f else 0.17f)
        drawTitleBlocks(canvas, TITLE, TITLE_COLORS, titleTop, titleBottom, titleBlocks)

        // Portrait: three wide blocks stacked. Landscape: three side by side.
        val gap = dp(18f)
        val areaTop = titleBottom + dp(12f)
        val count = tracks.size
        tracks.forEachIndexed { i, track ->
            val card = cards[i]
            if (landscape) {
                val cellW = (contentRight - contentLeft) / count
                val left = contentLeft + cellW * i
                card.set(left + gap / 2f, areaTop, left + cellW - gap / 2f, contentBottom - dp(10f))
            } else {
                val cellH = (contentBottom - areaTop) / count
                val top = areaTop + cellH * i
                card.set(contentLeft, top + gap / 2f, contentRight, top + cellH - gap / 2f - dp(10f))
            }

            val appear = popIn((time - 0.4f - i * 0.1f) / 0.4f)
            if (appear <= 0.01f) return@forEachIndexed
            canvas.save()
            canvas.scale(appear, appear, card.centerX(), card.centerY())
            val sink = drawBlock(canvas, card, track.color, radius = dp(30f), depth = dp(10f))
            val h = card.height()
            val w = card.width()
            drawText(canvas, track.label, card.centerX(), card.top + h * 0.24f + sink, min(h * 0.2f, dp(40f)), Palette.WHITE, w * 0.9f)
            drawText(canvas, track.subtitle, card.centerX(), card.top + h * 0.43f + sink, min(h * 0.1f, dp(20f)), 0xDDFFFFFF.toInt(), w * 0.9f)

            // A row of the track's first letters on little white blocks.
            val sample = track.letters.take(SAMPLE_SIZE)
            val side = min(h * 0.3f, (w - dp(24f)) / SAMPLE_SIZE - dp(10f))
            val rowWidth = side * SAMPLE_SIZE + dp(10f) * (SAMPLE_SIZE - 1)
            var x = card.centerX() - rowWidth / 2f
            val y = card.top + h * 0.72f + sink
            sample.forEachIndexed { j, letter ->
                miniBlock.set(x, y - side / 2f, x + side, y + side / 2f)
                drawBlock(canvas, miniBlock, Palette.WHITE, depth = dp(4f), pressable = false)
                drawText(canvas, letter.symbol, miniBlock.centerX(), miniBlock.centerY(), side * 0.62f, track.colorFor(j), side * 0.86f)
                x += side + dp(10f)
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
