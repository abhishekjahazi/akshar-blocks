package com.aksharblocks.app

import android.content.Context
import android.graphics.Canvas
import android.graphics.RectF
import kotlin.math.min

/**
 * "You earned a certificate!": shown once when a child learns most of a section, with
 * confetti. Parents can share it from the parent area (children can't share from here).
 */
class CertificateView(
    context: Context, speaker: Speaker, player: Player, private val track: Track,
) : GameView(context, speaker, player) {

    /** The child tapped the big tick: carry on to where they were going. */
    var onDone: (() -> Unit)? = null

    override val title = CommonWords.CERTIFICATE
    override val showHomeButton = false
    override val skyColor = Palette.SKY

    private val page = RectF()
    private val okButton = RectF()
    private val earned = player.certificates()[track] ?: todayNumber()

    init {
        Sounds.play(Sound.FANFARE)
        speaker.say(CommonWords.CERTIFICATE_EARNED)
        after(0.4f) { celebrate(width / 2f, height * 0.3f, 80) }
        after(2.5f) { celebrate(width / 2f, height * 0.5f, 60) }
    }

    override fun drawGame(canvas: Canvas) {
        val button = dp(76f)
        okButton.set(width / 2f - button * 0.9f, contentBottom - button, width / 2f + button * 0.9f, contentBottom)
        // A page about A4-shaped (wider in landscape), as big as fits.
        val maxW = contentRight - contentLeft
        val maxH = okButton.top - dp(20f) - contentTop
        val landscape = width > height
        val ratio = if (landscape) 1.4f else 0.72f
        val pageW = min(maxW, maxH * ratio)
        val pageH = pageW / ratio
        val appear = popIn((time - 0.1f) / 0.5f)
        page.set(width / 2f - pageW / 2f, contentTop, width / 2f + pageW / 2f, contentTop + pageH)
        canvas.save()
        canvas.scale(appear, appear, page.centerX(), page.centerY())
        CertificateArt.draw(canvas, page, player.profile.name, track, earned)
        canvas.restore()

        val sink = drawBlock(canvas, okButton, Palette.GRASS, radius = button / 2f, depth = dp(7f))
        drawEmoji(canvas, "👍", okButton.centerX(), okButton.centerY() + sink, button * 0.5f)
    }

    override fun onTap(x: Float, y: Float) {
        if (okButton.contains(x, y)) {
            Sounds.play(Sound.TAP)
            onDone?.invoke()
        } else if (page.contains(x, y)) {
            celebrate(x, y, 24)
        }
    }
}
