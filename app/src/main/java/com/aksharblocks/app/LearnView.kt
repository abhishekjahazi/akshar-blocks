package com.aksharblocks.app

import android.content.Context
import android.graphics.Canvas
import android.graphics.RectF
import kotlin.math.exp
import kotlin.math.min
import kotlin.math.sin

/** Flip through a track's letters: big letter, picture and word, read out loud. */
class LearnView(
    context: Context, speaker: Speaker, player: Player, private val track: Track, start: Int = 0,
) : GameView(context, speaker, player) {

    private val letters = track.letters
    private var index = start.coerceIn(0, track.letters.lastIndex)

    /** Seconds since the card last bounced. */
    private var bounceTime = 0f

    /** Slide-in offset after changing letters: ±1 eases back to 0. */
    private var slide = 0f

    private val card = RectF()
    private val prevButton = RectF()
    private val nextButton = RectF()
    private val letterBlock = RectF()
    private val objectsArea = RectF()

    override val title get() = track.lang.learnTitle(index + 1, letters.size)
    override val showStars = false
    override val skyColor = Palette.ICE

    init {
        announce()
    }

    private fun announce() {
        speaker.say(track.lang.learn(letters[index]), track.lang.locale)
        bounceTime = 0f
    }

    private fun go(step: Int) {
        index = (index + step + letters.size) % letters.size
        if (step > 0) point()
        slide = step.toFloat()
        announce()
    }

    override fun update(dt: Float) {
        bounceTime += dt
        slide *= exp(-10f * dt)
    }

    override fun drawGame(canvas: Canvas) {
        val landscape = width > height
        val button = dp(72f)

        // Portrait: card on top, arrows underneath. Landscape: arrows either side.
        if (landscape) {
            val midY = (contentTop + contentBottom) / 2f
            prevButton.set(contentLeft, midY - button / 2f, contentLeft + button, midY + button / 2f)
            nextButton.set(contentRight - button, midY - button / 2f, contentRight, midY + button / 2f)
            card.set(prevButton.right + dp(20f), contentTop, nextButton.left - dp(20f), contentBottom - dp(8f))
        } else {
            val buttonTop = contentBottom - button - dp(8f)
            val cx = width / 2f
            prevButton.set(cx - dp(16f) - button * 1.6f, buttonTop, cx - dp(16f), buttonTop + button)
            nextButton.set(cx + dp(16f), buttonTop, cx + dp(16f) + button * 1.6f, buttonTop + button)
            card.set(contentLeft, contentTop, contentRight, buttonTop - dp(32f))
        }

        val letter = letters[index]
        val color = track.colorFor(index)
        val w = card.width()
        val h = card.height()

        canvas.save()
        val scale = if (motionEnabled) 1f + 0.06f * exp(-5f * bounceTime) * sin(bounceTime * 16f) else 1f
        if (motionEnabled) canvas.translate(slide * w * 0.4f, 0f)
        canvas.scale(scale, scale, card.centerX(), card.centerY())

        val sink = drawBlock(canvas, card, Palette.WHITE, radius = dp(36f), depth = dp(10f))
        canvas.translate(0f, sink)

        // The letter sits on its own colored block; letters without a word get the whole card.
        val word = letter.word
        val side = if (word == null) min(h * 0.6f, w * 0.7f) else min(h * 0.36f, w * 0.62f)
        val halfWidth = min(side * 0.75f, w / 2f - dp(40f))
        val blockTop = if (word == null) card.centerY() - side / 2f else card.top + h * 0.07f
        letterBlock.set(card.centerX() - halfWidth, blockTop, card.centerX() + halfWidth, blockTop + side)
        drawBlock(canvas, letterBlock, color, radius = side * 0.24f, depth = dp(9f), pressable = false)
        drawText(
            canvas, track.display(letter), letterBlock.centerX(), letterBlock.centerY(),
            side * 0.66f, Palette.WHITE, letterBlock.width() * 0.86f,
        )
        if (word != null && track.isNumbers) {
            // Small numbers are shown as that many stars, so the child sees how many it means.
            val value = index + 1
            if (value <= Counting.MAX) {
                objectsArea.set(card.left + dp(16f), card.top + h * 0.47f, card.right - dp(16f), card.top + h * 0.78f)
                drawCountedObjects(canvas, objectsArea, value, "⭐")
            }
            val wordY = if (value <= Counting.MAX) card.top + h * 0.88f else card.top + h * 0.7f
            drawText(canvas, word, card.centerX(), wordY, min(h * 0.1f, w * 0.15f), Palette.INK, w * 0.86f)
        } else if (word != null) {
            // Older phones can't draw every emoji; show the word without a picture then.
            val emoji = letter.emoji?.takeIf { canDraw(it) }
            if (emoji != null) {
                drawEmoji(canvas, emoji, card.centerX(), card.top + h * 0.64f, min(h * 0.22f, w * 0.4f))
            }
            val wordY = if (emoji != null) card.top + h * 0.88f else card.top + h * 0.7f
            drawWord(canvas, word, letter.headLength, color, card.centerX(), wordY, min(h * 0.1f, w * 0.15f), w * 0.86f)
        }
        canvas.restore()

        drawArrowBlock(canvas, prevButton, Palette.SUN, pointsRight = false)
        drawArrowBlock(canvas, nextButton, Palette.SUN, pointsRight = true)
    }

    override fun onTap(x: Float, y: Float) {
        when {
            prevButton.contains(x, y) -> go(-1)
            nextButton.contains(x, y) -> go(1)
            card.contains(x, y) -> announce()
        }
    }

    override fun onSwipe(towardsLeft: Boolean) = go(if (towardsLeft) 1 else -1)
}
