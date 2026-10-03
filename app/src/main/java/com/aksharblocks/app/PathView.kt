package com.aksharblocks.app

import android.content.Context
import android.graphics.Canvas
import android.graphics.RectF
import kotlin.math.abs
import kotlin.math.min
import kotlin.math.sin

/**
 * Today's games: the day's steps one under another, finished ones ticked and the next one
 * bouncing. Any step can be tapped; the next one is the big obvious choice.
 */
class PathView(
    context: Context, speaker: Speaker, player: Player,
    private val steps: List<PathStep>,
    private val done: Int,
    /** True when arriving here straight after finishing a step. */
    private val justFinished: Boolean,
) : GameView(context, speaker, player) {

    var onPick: ((Int) -> Unit)? = null

    override val title = CommonWords.TODAYS_GAMES
    override val skyColor = Palette.MINT

    private val cards = steps.map { RectF() }
    private val icon = RectF()
    private val badge = RectF()
    private val trophy = RectF()

    private fun isDone(i: Int) = done and (1 shl i) != 0
    private val next = steps.indices.firstOrNull { !isDone(it) }
    private val allDone get() = next == null

    init {
        val step = next?.let { steps[it] }
        when {
            step == null -> speaker.say(CommonWords.PATH_DONE)
            justFinished -> {
                speaker.say(CommonWords.PATH_NEXT)
                after(2.2f) { speaker.say(step.track.lang.modeLabel(step.mode), step.track.lang.locale) }
            }
            else -> speaker.say(CommonWords.PATH_START)
        }
        if (allDone && justFinished) after(0.5f) { celebrate(width / 2f, height * 0.25f, 60) }
    }

    override fun drawGame(canvas: Canvas) {
        val landscape = width > height
        var top = contentTop
        if (allDone) {
            // A trophy over the list once every step is done.
            val side = min(dp(110f), height * 0.14f)
            trophy.set(width / 2f - side / 2f, top, width / 2f + side / 2f, top + side)
            val bob = if (motionEnabled) sin(time * 2.5f) * dp(4f) else 0f
            drawEmoji(canvas, "🏆", trophy.centerX(), trophy.centerY() + bob, side * 0.9f)
            top = trophy.bottom + dp(12f)
        }

        // Portrait: one step per row. Landscape: two by two.
        val cols = if (landscape) 2 else 1
        val rows = (steps.size + cols - 1) / cols
        val gap = dp(16f)
        val cellW = (contentRight - contentLeft) / cols
        val cellH = min((contentBottom - top) / rows, dp(150f))
        val gridTop = top + ((contentBottom - top) - cellH * rows) / 2f

        steps.forEachIndexed { i, step ->
            val left = contentLeft + cellW * (i % cols)
            val cardTop = gridTop + cellH * (i / cols)
            val card = cards[i]
            card.set(left + gap / 2f, cardTop + gap / 2f, left + cellW - gap / 2f, cardTop + cellH - gap / 2f - dp(8f))

            val appear = popIn((time - 0.2f - i * 0.1f) / 0.4f)
            if (appear <= 0.01f) return@forEachIndexed
            val current = i == next
            // The next step bounces gently so a child knows where to tap.
            val bounce = if (current && motionEnabled) abs(sin(time * 3f)) * dp(6f) else 0f
            canvas.save()
            canvas.scale(appear, appear, card.centerX(), card.centerY())
            canvas.translate(0f, -bounce)
            drawStep(canvas, card, step, i, current)
            canvas.restore()
        }
    }

    private fun drawStep(canvas: Canvas, card: RectF, step: PathStep, i: Int, current: Boolean) {
        val finished = isDone(i)
        val face = if (finished) Palette.edgeOf(Palette.WHITE) else Palette.WHITE
        val sink = drawBlock(canvas, card, face, radius = dp(26f), depth = dp(if (current) 10f else 6f))
        val h = card.height()

        // The game's picture on a block in its own colour.
        val iconSide = h * 0.7f
        icon.set(card.left + h * 0.15f, card.centerY() - iconSide / 2f + sink, card.left + h * 0.15f + iconSide, card.centerY() + iconSide / 2f + sink)
        drawBlock(canvas, icon, step.mode.color, depth = dp(5f), pressable = false)
        drawEmoji(canvas, step.mode.emoji, icon.centerX(), icon.centerY(), iconSide * 0.55f)

        // The section and the game, both in the section's language.
        val textLeft = icon.right + dp(14f)
        val badgeSide = h * 0.42f
        val textWidth = card.right - textLeft - badgeSide - dp(24f)
        val textX = textLeft + textWidth / 2f
        drawText(canvas, step.track.label, textX, card.top + h * 0.36f + sink, min(h * 0.22f, dp(28f)), step.track.color, textWidth)
        drawText(canvas, step.track.lang.modeLabel(step.mode), textX, card.top + h * 0.66f + sink, min(h * 0.17f, dp(20f)), Palette.INK, textWidth)

        // Ticked when done; a play arrow on the next one; otherwise the step number.
        badge.set(card.right - badgeSide - dp(16f), card.centerY() - badgeSide / 2f + sink, card.right - dp(16f), card.centerY() + badgeSide / 2f + sink)
        when {
            finished -> drawEmoji(canvas, "✅", badge.centerX(), badge.centerY(), badgeSide * 0.9f)
            current -> {
                drawBlock(canvas, badge, Palette.SUN, radius = badgeSide / 2f, depth = dp(4f), pressable = false)
                path.reset()
                val s = badgeSide * 0.2f
                path.moveTo(badge.centerX() + s * 1.1f, badge.centerY())
                path.lineTo(badge.centerX() - s * 0.7f, badge.centerY() - s)
                path.lineTo(badge.centerX() - s * 0.7f, badge.centerY() + s)
                path.close()
                fillPaint.color = Palette.INK
                canvas.drawPath(path, fillPaint)
            }
            else -> drawText(canvas, "${i + 1}", badge.centerX(), badge.centerY(), badgeSide * 0.6f, Palette.edgeOf(Palette.WHITE))
        }
    }

    override fun onTap(x: Float, y: Float) {
        if (allDone && trophy.contains(x, y)) {
            celebrate(trophy.centerX(), trophy.centerY(), 30)
            speaker.say(CommonWords.PATH_DONE)
            return
        }
        val index = cards.indexOfFirst { it.contains(x, y) }
        if (index < 0) return
        Sounds.play(Sound.TAP)
        val step = steps[index]
        speaker.say(step.track.lang.modeLabel(step.mode), step.track.lang.locale)
        onPick?.invoke(index)
    }
}
