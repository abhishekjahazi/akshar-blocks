package com.aksharblocks.app

import android.content.Context
import android.graphics.Canvas
import android.graphics.RectF
import kotlin.math.min

/** "Who is playing?": one big block per child. */
class ProfilePickerView(
    context: Context, speaker: Speaker, player: Player, private val profiles: List<Profile>,
) : GameView(context, speaker, player) {

    var onPick: ((Profile) -> Unit)? = null

    override val title = "Who is playing?"
    override val showStars = false

    private val cards = profiles.map { RectF() }

    override fun drawGame(canvas: Canvas) {
        val landscape = width > height
        val cols = if (landscape) min(profiles.size, 4) else min(profiles.size, 2)
        val rows = (profiles.size + cols - 1) / cols
        val gap = dp(18f)
        val cellW = (contentRight - contentLeft) / cols
        val cellH = min((contentBottom - contentTop) / rows, cellW * 1.2f)
        val top = contentTop + ((contentBottom - contentTop) - cellH * rows) / 2f

        profiles.forEachIndexed { i, profile ->
            val left = contentLeft + cellW * (i % cols)
            val cellTop = top + cellH * (i / cols)
            val card = cards[i]
            card.set(left + gap / 2f, cellTop + gap / 2f, left + cellW - gap / 2f, cellTop + cellH - gap / 2f - dp(8f))
            val face = if (profile.id == player.profile.id) Palette.OCEAN else Palette.WHITE
            val ink = if (profile.id == player.profile.id) Palette.WHITE else Palette.INK

            val appear = popIn((time - i * 0.08f) / 0.4f)
            canvas.save()
            canvas.scale(appear, appear, card.centerX(), card.centerY())
            val sink = drawBlock(canvas, card, face, radius = dp(30f), depth = dp(10f))
            val size = min(card.width(), card.height())
            drawEmoji(canvas, profile.avatar, card.centerX(), card.top + card.height() * 0.42f + sink, size * 0.45f)
            drawText(canvas, profile.name, card.centerX(), card.top + card.height() * 0.8f + sink, size * 0.14f, ink, card.width() * 0.86f)
            canvas.restore()
        }
    }

    override fun onTap(x: Float, y: Float) {
        val index = cards.indexOfFirst { it.contains(x, y) }
        if (index >= 0) {
            Sounds.play(Sound.TAP)
            onPick?.invoke(profiles[index])
        }
    }
}
