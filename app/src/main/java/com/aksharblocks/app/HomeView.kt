package com.aksharblocks.app

import android.content.Context
import android.graphics.Canvas
import android.graphics.RectF
import kotlin.math.abs
import kotlin.math.min
import kotlin.math.sin

/** First screen: today's games, then every section (letters, numbers, pictures, rhymes, my name). */
class HomeView(context: Context, speaker: Speaker, player: Player) : GameView(context, speaker, player) {

    var onPick: ((Track) -> Unit)? = null

    /** Grown-ups button (goes through the parent gate). */
    var onParent: (() -> Unit)? = null

    /** The child name tag was tapped. */
    var onChild: (() -> Unit)? = null

    /** The star counter was tapped: open the sticker album. */
    var onAlbum: (() -> Unit)? = null

    /** The "today's games" banner was tapped. */
    var onPath: (() -> Unit)? = null

    /** Which of today's steps are done, as bits (see [Player.pathDone]). */
    var pathDone = 0

    override val starsTappable = true

    override val showHomeButton = false

    /** Rhymes was tapped. */
    var onRhymes: (() -> Unit)? = null

    /** "My name" was tapped: trace the child's own name. */
    var onName: (() -> Unit)? = null

    /** One section on Home: a track, or one of the extra corners (rhymes, my name). */
    private class Section(
        val label: String,
        val subtitle: String,
        val color: Int,
        /** A few letters or pictures shown on little white blocks. */
        val sample: List<String>,
        val sampleColor: (Int) -> Int,
        val open: () -> Unit,
    )

    private val sections: List<Section> = Track.entries.map { track ->
        Section(track.label, track.subtitle, track.color, track.letters.take(SAMPLE_SIZE).map { it.symbol }, track::colorFor) { onPick?.invoke(track) }
    } + listOf(
        Section("Rhymes", "कविताएँ", Palette.RUST, listOf("🎵", "⭐", "🐟"), { Palette.RUST }) { onRhymes?.invoke() },
        Section("My name", "मेरा नाम", Palette.ROYAL, NameLetters.sample(player.profile.name, SAMPLE_SIZE), Track.ENGLISH::colorFor) { onName?.invoke() },
    )
    private val cards = sections.map { RectF() }
    private val pathBanner = RectF()
    private val pathDot = RectF()
    private val miniBlock = RectF()
    private val parentButton = RectF()
    private val childTag = RectF()

    override fun drawGame(canvas: Canvas) {
        drawTopRow(canvas)

        val landscape = width > height
        val titleTop = contentTop - dp(16f)
        val titleBottom = titleTop + height * (if (landscape) 0.22f else 0.14f)
        drawPathBanner(canvas, titleTop + dp(8f), titleBottom)

        // A grid of sections: three columns in portrait, five in landscape.
        val gap = dp(10f)
        val areaTop = titleBottom + dp(4f)
        val cols = if (landscape) 5 else 3
        val rows = (sections.size + cols - 1) / cols
        val cellW = (contentRight - contentLeft) / cols
        val cellH = (contentBottom - areaTop) / rows
        sections.forEachIndexed { i, section ->
            val card = cards[i]
            // A last row with fewer sections is centered.
            val row = i / cols
            val inRow = minOf(cols, sections.size - row * cols)
            val left = contentLeft + (cols - inRow) * cellW / 2f + cellW * (i % cols)
            val top = areaTop + cellH * row
            card.set(left + gap / 2f, top + gap / 2f, left + cellW - gap / 2f, top + cellH - gap / 2f - dp(7f))

            val appear = popIn((time - 0.4f - i * 0.05f) / 0.4f)
            if (appear <= 0.01f) return@forEachIndexed
            canvas.save()
            canvas.scale(appear, appear, card.centerX(), card.centerY())
            val sink = drawBlock(canvas, card, section.color, radius = dp(24f), depth = dp(8f))
            val h = card.height()
            val w = card.width()
            drawText(canvas, section.label, card.centerX(), card.top + h * 0.25f + sink, min(h * 0.17f, dp(24f)), Palette.WHITE, w * 0.86f)
            drawText(canvas, section.subtitle, card.centerX(), card.top + h * 0.45f + sink, min(h * 0.1f, dp(14f)), 0xDDFFFFFF.toInt(), w * 0.86f)

            // A row of the section's first letters (or pictures) on little white blocks.
            val count = section.sample.size
            val side = min(h * 0.27f, (w - dp(14f)) / SAMPLE_SIZE - dp(4f))
            val rowWidth = side * count + dp(4f) * (count - 1)
            var x = card.centerX() - rowWidth / 2f
            val y = card.top + h * 0.73f + sink
            section.sample.forEachIndexed { j, symbol ->
                miniBlock.set(x, y - side / 2f, x + side, y + side / 2f)
                drawBlock(canvas, miniBlock, Palette.WHITE, depth = dp(3f), pressable = false)
                drawText(canvas, symbol, miniBlock.centerX(), miniBlock.centerY(), side * 0.62f, section.sampleColor(j), side * 0.86f)
                x += side + dp(4f)
            }
            canvas.restore()
        }
    }

    /** A wide yellow block: play today's games, with a dot for each step (green when done). */
    private fun drawPathBanner(canvas: Canvas, top: Float, bottom: Float) {
        val allDone = Integer.bitCount(pathDone) >= DailyPath.STEPS
        val appear = popIn((time - 0.2f) / 0.4f)
        if (appear <= 0.01f) return
        pathBanner.set(contentLeft + dp(7f), top, contentRight - dp(7f), bottom - dp(10f))
        // It bobs until today's games are done, to say "start here".
        val bob = if (!allDone && motionEnabled) abs(sin(time * 2.6f)) * dp(4f) else 0f
        canvas.save()
        canvas.scale(appear, appear, pathBanner.centerX(), pathBanner.centerY())
        canvas.translate(0f, -bob)
        val face = if (allDone) Palette.GRASS else Palette.SUN
        val ink = if (allDone) Palette.WHITE else Palette.INK
        val sink = drawBlock(canvas, pathBanner, face, radius = dp(28f), depth = dp(10f))
        val h = pathBanner.height()

        // The play symbol (or a trophy) in a white circle on the left.
        val circle = h * 0.62f
        val cx = pathBanner.left + h * 0.2f + circle / 2f
        val cy = pathBanner.centerY() + sink
        fillPaint.color = Palette.WHITE
        canvas.drawCircle(cx, cy, circle / 2f, fillPaint)
        if (allDone) {
            drawEmoji(canvas, "🏆", cx, cy, circle * 0.6f)
        } else {
            val s = circle * 0.22f
            path.reset()
            path.moveTo(cx + s * 1.1f, cy)
            path.lineTo(cx - s * 0.7f, cy - s)
            path.lineTo(cx - s * 0.7f, cy + s)
            path.close()
            fillPaint.color = Palette.INK
            canvas.drawPath(path, fillPaint)
        }

        // "Today's games" and a row of dots under it.
        val textLeft = cx + circle / 2f + dp(12f)
        val textWidth = pathBanner.right - dp(16f) - textLeft
        val textX = textLeft + textWidth / 2f
        drawText(canvas, CommonWords.TODAYS_GAMES, textX, pathBanner.top + h * 0.38f + sink, min(h * 0.3f, dp(30f)), ink, textWidth)
        val dot = min(h * 0.14f, dp(16f))
        val dotGap = dot * 0.8f
        var x = textX - (dot * DailyPath.STEPS + dotGap * (DailyPath.STEPS - 1)) / 2f
        val dotY = pathBanner.top + h * 0.72f + sink
        for (i in 0 until DailyPath.STEPS) {
            pathDot.set(x, dotY - dot / 2f, x + dot, dotY + dot / 2f)
            fillPaint.color = if (pathDone and (1 shl i) != 0) (if (allDone) Palette.WHITE else Palette.GRASS) else Palette.edgeOf(face)
            canvas.drawOval(pathDot, fillPaint)
            x += dot + dotGap
        }
        canvas.restore()
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
        if (starsRect.contains(x, y)) {
            onAlbum?.invoke()
            return
        }
        if (pathBanner.contains(x, y)) {
            Sounds.play(Sound.TAP)
            onPath?.invoke()
            return
        }
        val index = cards.indexOfFirst { it.contains(x, y) }
        if (index >= 0) {
            Sounds.play(Sound.TAP)
            sections[index].open()
        }
    }

    private companion object {
        const val SAMPLE_SIZE = 3
    }
}
