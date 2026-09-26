package com.aksharblocks.app

import android.content.Context
import android.graphics.Canvas
import android.graphics.RectF
import kotlin.math.exp
import kotlin.math.min
import kotlin.math.sin

/** The sticker album: earned stickers, locked ones still to win, the streak and progress to the next. */
class AlbumView(context: Context, speaker: Speaker, player: Player) : GameView(context, speaker, player) {

    override val title = "My stickers"
    override val skyColor = Palette.PEACH

    private val slots = Array(Stickers.all.size) { RectF() }
    private val drawRect = RectF()
    private val barTrack = RectF()
    private var tapped = -1
    private var tappedAt = 0f

    init {
        val streak = player.streak
        speaker.say(if (streak >= 2) CommonWords.streak(streak) else CommonWords.YOUR_STICKERS)
    }

    override fun drawGame(canvas: Canvas) {
        val stars = player.stars
        val unlocked = Stickers.unlocked(stars)
        val streak = player.streak

        // Streak and progress toward the next sticker.
        var y = contentTop + dp(18f)
        val info = if (streak >= 2) "🔥 $streak days in a row" else "⭐ $unlocked of ${Stickers.all.size} stickers"
        drawText(canvas, info, width / 2f, y, dp(22f), Palette.INK, contentRight - contentLeft)
        y += dp(34f)
        val toNext = Stickers.starsToNext(stars)
        if (toNext > 0) {
            barTrack.set(contentLeft + dp(24f), y, contentRight - dp(24f), y + dp(16f))
            fillPaint.color = Palette.edgeOf(Palette.WHITE)
            canvas.drawRoundRect(barTrack, dp(8f), dp(8f), fillPaint)
            val done = (Stickers.STARS_EACH - toNext) / Stickers.STARS_EACH.toFloat()
            fillPaint.color = Palette.SUN
            canvas.drawRoundRect(barTrack.left, barTrack.top, barTrack.left + barTrack.width() * done, barTrack.bottom, dp(8f), dp(8f), fillPaint)
            y += dp(34f)
            drawText(canvas, "$toNext more ⭐ for the next sticker", width / 2f, y, dp(17f), Palette.INK, contentRight - contentLeft)
        }
        y += dp(22f)

        // The album: five across, bigger stickers once earned.
        val cols = if (width > height) 10 else 5
        val rows = (Stickers.all.size + cols - 1) / cols
        val cellW = (contentRight - contentLeft) / cols
        val cellH = min(cellW, (contentBottom - y) / rows)
        val half = min(cellW, cellH) * 0.42f
        Stickers.all.forEachIndexed { i, sticker ->
            val cx = contentLeft + cellW * (i % cols + 0.5f)
            val cy = y + cellH * (i / cols + 0.5f)
            squareAt(slots[i], cx, cy, half)
            var scale = 1f
            if (i == tapped && motionEnabled) {
                val t = time - tappedAt
                scale += 0.18f * exp(-5f * t) * sin(t * 16f)
            }
            squareAt(drawRect, cx, cy, half * scale)
            if (i < unlocked) {
                val sink = drawBlock(canvas, drawRect, Palette.WHITE, depth = dp(5f))
                drawEmoji(canvas, sticker.emoji, cx, cy + sink, half * 1.3f)
            } else {
                drawBlock(canvas, drawRect, Palette.edgeOf(Palette.WHITE), depth = dp(3f), pressable = false)
                drawText(canvas, "?", cx, cy, half, Palette.WHITE)
            }
        }
    }

    override fun onTap(x: Float, y: Float) {
        val i = slots.indexOfFirst { it.contains(x, y) }
        if (i < 0) return
        tapped = i
        tappedAt = time
        if (i < Stickers.unlocked(player.stars)) {
            speaker.say(Stickers.all[i].name)
        } else {
            speaker.say(CommonWords.WIN_MORE_STARS)
        }
    }
}
