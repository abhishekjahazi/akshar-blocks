package com.aksharblocks.app

import android.content.Context
import android.graphics.Canvas
import android.graphics.RectF
import kotlin.math.min
import kotlin.math.sin

/** Shown when today's play time is used up: a calm goodbye. Only a grown-up can go on. */
class RestView(context: Context, speaker: Speaker, player: Player) : GameView(context, speaker, player) {

    /** The grown-ups lock (goes through the parent gate). */
    var onParent: (() -> Unit)? = null

    override val showHomeButton = false
    override val showStars = false
    override val skyColor = 0xFF1D2B53.toInt()

    private val parentButton = RectF()

    init {
        speaker.say(CommonWords.REST)
    }

    override fun drawGame(canvas: Canvas) {
        val size = dp(52f)
        parentButton.set(contentLeft + dp(4f), topBarCenter - size / 2f, contentLeft + dp(4f) + size, topBarCenter + size / 2f)
        val sink = drawBlock(canvas, parentButton, Palette.WHITE, depth = dp(5f))
        drawEmoji(canvas, "🔒", parentButton.centerX(), parentButton.centerY() + sink, size * 0.45f)

        val cx = width / 2f
        val moon = min(width, height) * 0.32f
        // The moon rocks gently, like a cradle.
        val sway = if (motionEnabled) sin(time * 0.8f) * dp(6f) else 0f
        drawEmoji(canvas, "🌙", cx + sway, height * 0.36f, moon)
        drawText(canvas, "Time to rest!", cx, height * 0.6f, dp(40f), Palette.WHITE, width * 0.86f)
        drawText(canvas, "आराम का समय!", cx, height * 0.6f + dp(58f), dp(32f), 0xFFFFC53D.toInt(), width * 0.86f)
        drawText(canvas, "See you tomorrow 👋", cx, height * 0.6f + dp(118f), dp(22f), 0xCCFFFFFF.toInt(), width * 0.86f)
    }

    override fun onTap(x: Float, y: Float) {
        if (parentButton.contains(x, y)) onParent?.invoke()
    }
}
