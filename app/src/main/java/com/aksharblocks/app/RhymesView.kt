package com.aksharblocks.app

import android.content.Context
import android.graphics.Canvas
import android.graphics.RectF
import kotlin.math.min

/** All the rhymes as picture cards; a child picks one by its picture. */
class RhymesView(context: Context, speaker: Speaker, player: Player) : GameView(context, speaker, player) {

    var onPick: ((Rhyme) -> Unit)? = null

    override val title = CommonWords.RHYMES
    override val skyColor = Palette.PEACH

    private val rhymes = Rhymes.all
    private val cards = rhymes.map { RectF() }

    init {
        speaker.say(CommonWords.RHYMES_ASK)
    }

    override fun drawGame(canvas: Canvas) {
        val landscape = width > height
        val cols = if (landscape) 4 else 3
        val rows = (rhymes.size + cols - 1) / cols
        val gap = dp(12f)
        val cellW = (contentRight - contentLeft) / cols
        val cellH = (contentBottom - contentTop) / rows
        rhymes.forEachIndexed { i, rhyme ->
            val left = contentLeft + cellW * (i % cols)
            val top = contentTop + cellH * (i / cols)
            val card = cards[i]
            card.set(left + gap / 2f, top + gap / 2f, left + cellW - gap / 2f, top + cellH - gap / 2f - dp(7f))

            val appear = popIn((time - 0.2f - i * 0.05f) / 0.4f)
            if (appear <= 0.01f) return@forEachIndexed
            canvas.save()
            canvas.scale(appear, appear, card.centerX(), card.centerY())
            val sink = drawBlock(canvas, card, colorOf(rhyme), radius = dp(24f), depth = dp(8f))
            val h = card.height()
            val w = card.width()
            drawEmoji(canvas, rhyme.emoji, card.centerX(), card.top + h * 0.4f + sink, min(h * 0.5f, w * 0.6f))
            drawText(canvas, rhyme.title, card.centerX(), card.top + h * 0.84f + sink, min(h * 0.11f, dp(16f)), Palette.WHITE, w * 0.88f)
            canvas.restore()
        }
    }

    override fun onTap(x: Float, y: Float) {
        val index = cards.indexOfFirst { it.contains(x, y) }
        if (index < 0) return
        Sounds.play(Sound.TAP)
        onPick?.invoke(rhymes[index])
    }

    companion object {
        /** English, Hindi and Marathi rhymes each have their own color. */
        fun colorOf(rhyme: Rhyme) = when (rhyme.language) {
            "hi" -> Palette.TOMATO
            "mr" -> Palette.INDIGO
            else -> Palette.OCEAN
        }
    }
}
