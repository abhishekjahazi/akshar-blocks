package com.aksharblocks.app

import android.content.Context
import android.graphics.Canvas
import android.graphics.RectF
import kotlin.math.min

/**
 * Opposites: a picture with its name ("Big! बड़ा!") and three pictures to choose its opposite
 * from. The right one grows and shows its name, and the pair is said together.
 */
class OppositesView(context: Context, speaker: Speaker, player: Player) : GameView(context, speaker, player) {

    override val title = OppositeWords.TITLE
    override val skyColor = Palette.PEACH

    private var round = Opposites.round(random)
    private var roundTime = 0f
    private var solved = false
    private var roundToken = 0

    private val shakeTime = FloatArray(Opposites.CHOICES) { 1f }
    private val shownCard = RectF()
    private val choiceCards = Array(Opposites.CHOICES) { RectF() }
    private val drawRect = RectF()

    init {
        ask()
    }

    private fun ask() = speaker.say(OppositeWords.ask(round.shown), English.locale)

    private fun next() {
        roundToken++
        round = Opposites.round(random, Opposites.pairOf(round.shown))
        roundTime = 0f
        solved = false
        shakeTime.fill(1f)
        ask()
    }

    override fun update(dt: Float) {
        roundTime += dt
        for (i in shakeTime.indices) shakeTime[i] += dt
    }

    override fun drawGame(canvas: Canvas) {
        val landscape = width > height
        val areaH = contentBottom - contentTop
        val choicesTop: Float
        val choicesLeft: Float
        if (landscape) {
            shownCard.set(contentLeft, contentTop, contentLeft + (contentRight - contentLeft) * 0.4f, contentBottom - dp(10f))
            choicesLeft = shownCard.right + dp(24f)
            choicesTop = contentTop + areaH * 0.22f
        } else {
            shownCard.set(contentLeft, contentTop, contentRight, contentTop + areaH * 0.48f)
            choicesLeft = contentLeft
            choicesTop = shownCard.bottom + areaH * 0.13f
        }

        // The picture to find the opposite of, with its names.
        val appear = popIn(roundTime / 0.4f)
        canvas.save()
        canvas.scale(appear, appear, shownCard.centerX(), shownCard.centerY())
        val sink = drawBlock(canvas, shownCard, Palette.WHITE, radius = dp(34f), depth = dp(10f))
        val h = shownCard.height()
        val w = shownCard.width()
        drawEmoji(canvas, round.shown.emoji, shownCard.centerX(), shownCard.top + h * 0.38f + sink, min(h * 0.46f, w * 0.6f))
        drawText(canvas, round.shown.english, shownCard.centerX(), shownCard.top + h * 0.75f + sink, min(h * 0.14f, dp(40f)), Palette.INK, w * 0.9f)
        drawText(canvas, round.shown.hindi, shownCard.centerX(), shownCard.top + h * 0.9f + sink, min(h * 0.1f, dp(28f)), Palette.ORANGE, w * 0.9f)
        canvas.restore()

        // "Opposite?" between the picture and the choices.
        val questionY = if (landscape) contentTop + areaH * 0.1f else (shownCard.bottom + choicesTop) / 2f
        val questionX = if (landscape) (choicesLeft + contentRight) / 2f else shownCard.centerX()
        drawText(canvas, QUESTION, questionX, questionY, dp(22f), Palette.INK, contentRight - choicesLeft)

        // Three pictures to pick the opposite from.
        val gap = dp(12f)
        val cellW = (contentRight - choicesLeft - gap * (Opposites.CHOICES - 1)) / Opposites.CHOICES
        val cellH = min(cellW * 1.3f, contentBottom - choicesTop - dp(8f))
        round.choices.forEachIndexed { i, side ->
            val left = choicesLeft + (cellW + gap) * i
            val card = choiceCards[i]
            card.set(left, choicesTop, left + cellW, choicesTop + cellH)
            var scale = popIn((roundTime - 0.25f - i * 0.08f) / 0.4f)
            val right = side == round.answer
            if (solved) scale *= if (right) 1.08f else 0.82f
            if (scale <= 0.02f) return@forEachIndexed
            drawRect.set(card)
            drawRect.offset(shakeOffset(shakeTime[i]), 0f)
            canvas.save()
            canvas.scale(scale, scale, drawRect.centerX(), drawRect.centerY())
            val face = if (solved && right) Palette.GRASS else Palette.WHITE
            val s = drawBlock(canvas, drawRect, face, radius = dp(24f), depth = dp(7f))
            val ch = drawRect.height()
            if (solved && right) {
                // The answer shows its names, like the picture above.
                drawEmoji(canvas, side.emoji, drawRect.centerX(), drawRect.top + ch * 0.36f + s, min(ch * 0.4f, cellW * 0.62f))
                drawText(canvas, side.english, drawRect.centerX(), drawRect.top + ch * 0.72f + s, min(ch * 0.13f, dp(26f)), Palette.WHITE, cellW * 0.9f)
                drawText(canvas, side.hindi, drawRect.centerX(), drawRect.top + ch * 0.87f + s, min(ch * 0.11f, dp(20f)), Palette.WHITE, cellW * 0.9f)
            } else {
                drawEmoji(canvas, side.emoji, drawRect.centerX(), drawRect.centerY() + s, min(ch * 0.5f, cellW * 0.66f))
            }
            canvas.restore()
        }
    }

    override fun onTap(x: Float, y: Float) {
        if (showingReveal) return
        if (shownCard.contains(x, y)) {
            if (!solved) ask()
            return
        }
        if (solved) return
        val i = choiceCards.indexOfFirst { it.contains(x, y) }
        if (i < 0) return
        val tapped = round.choices[i]
        if (tapped == round.answer) {
            solved = true
            addStar()
            celebrate(choiceCards[i].centerX(), choiceCards[i].centerY())
            speaker.say("${English.praise(random)}|${OppositeWords.right(round.shown, round.answer)}", English.locale)
            val token = roundToken
            after(NEXT_SECONDS) { if (token == roundToken) next() }
        } else {
            Sounds.play(Sound.WRONG)
            shakeTime[i] = 0f
            speaker.say(OppositeWords.wrong(tapped), English.locale)
        }
    }

    private companion object {
        const val QUESTION = "Opposite? · उल्टा?"
        const val NEXT_SECONDS = 4f
    }
}
