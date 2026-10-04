package com.aksharblocks.app

import android.content.Context
import android.graphics.Canvas
import android.graphics.RectF
import kotlin.math.min

/** One big card on a [CardMenuView]: a picture, a name, and what tapping it opens. */
class MenuCard(
    val label: String,
    val subtitle: String,
    val color: Int,
    val picture: String,
    val open: () -> Unit,
)

/**
 * A screen of big picture cards, for corners of Home that hold several sections: the World
 * (colors, animals, fruits…) and Maths (adding, taking away…).
 */
class CardMenuView(
    context: Context, speaker: Speaker, player: Player,
    override val title: String,
    override val skyColor: Int,
    private val items: List<MenuCard>,
    ask: String,
) : GameView(context, speaker, player) {

    private val cards = items.map { RectF() }

    init {
        speaker.say(ask)
    }

    override fun drawGame(canvas: Canvas) {
        val landscape = width > height
        val cols = if (landscape) min(items.size, 3) else 2
        val rows = (items.size + cols - 1) / cols
        val gap = dp(12f)
        val cellW = (contentRight - contentLeft) / cols
        val cellH = min((contentBottom - contentTop) / rows, cellW * 1.1f)
        val gridTop = contentTop + ((contentBottom - contentTop) - cellH * rows) / 2f
        items.forEachIndexed { i, item ->
            val left = contentLeft + cellW * (i % cols)
            val top = gridTop + cellH * (i / cols)
            val card = cards[i]
            card.set(left + gap / 2f, top + gap / 2f, left + cellW - gap / 2f, top + cellH - gap / 2f - dp(7f))

            val appear = popIn((time - 0.2f - i * 0.06f) / 0.4f)
            if (appear <= 0.01f) return@forEachIndexed
            canvas.save()
            canvas.scale(appear, appear, card.centerX(), card.centerY())
            val sink = drawBlock(canvas, card, item.color, radius = dp(26f), depth = dp(8f))
            val h = card.height()
            val w = card.width()
            drawEmoji(canvas, item.picture, card.centerX(), card.top + h * 0.38f + sink, min(h * 0.42f, w * 0.5f))
            drawText(canvas, item.label, card.centerX(), card.top + h * 0.76f + sink, min(h * 0.12f, dp(22f)), Palette.WHITE, w * 0.88f)
            drawText(canvas, item.subtitle, card.centerX(), card.top + h * 0.89f + sink, min(h * 0.08f, dp(15f)), 0xDDFFFFFF.toInt(), w * 0.88f)
            canvas.restore()
        }
    }

    override fun onTap(x: Float, y: Float) {
        val index = cards.indexOfFirst { it.contains(x, y) }
        if (index < 0) return
        Sounds.play(Sound.TAP)
        items[index].open()
    }
}
