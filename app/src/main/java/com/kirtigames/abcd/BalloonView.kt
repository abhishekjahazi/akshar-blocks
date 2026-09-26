package com.kirtigames.abcd

import android.content.Context
import android.graphics.Canvas
import android.graphics.RectF
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.min
import kotlin.math.sin

/** Letter balloons float up; pop them in alphabet order. */
class BalloonView(
    context: Context, speaker: Speaker, player: Player, private val track: Track,
) : GameView(context, speaker, player) {

    private class Balloon(
        val letter: Letter, val x: Float, var y: Float,
        val color: Int, val speed: Float, val phase: Float,
    ) {
        /** Seconds since a wrong tap started it wobbling. */
        var shake = 1f
    }

    private val lang = track.lang
    private val letters = track.letters
    private val balloons = ArrayList<Balloon>()

    /** Index of the letter to pop next. */
    private var nextIndex = 0
    private val next get() = letters[nextIndex]
    private var spawnTimer = 0f
    private var lastHint = -10f
    private var finished = false
    private val stripRect = RectF()

    private val radius get() = (min(width, height) * 0.11f).coerceIn(dp(38f), dp(80f))

    override val title get() = if (finished) lang.doneTitle else lang.popTitle(next)

    init {
        speaker.say(lang.balloonsStart(next), lang.locale)
    }

    override fun update(dt: Float) {
        if (width == 0) return
        if (!finished) {
            spawnTimer -= dt
            if (spawnTimer <= 0f && balloons.size < MAX_BALLOONS) {
                spawn()
                spawnTimer = 1.1f
            }
        }
        for (b in balloons) {
            b.y -= b.speed * dt
            b.shake += dt
        }
        balloons.removeAll { it.y < -radius * 3f }
    }

    private fun spawn() {
        // Always keep the letter the child is looking for on screen.
        val hasNext = balloons.any { it.letter == next }
        val letter = if (!hasNext || random.nextFloat() < 0.3f) next else Pick.anyExcept(letters, next, random)

        // Try a few spots and keep the one furthest from balloons that just started rising.
        val r = radius
        val span = (contentRight - contentLeft - 2f * r).coerceAtLeast(1f)
        val recent = balloons.filter { it.y > height * 0.6f }
        var x = contentLeft + r
        var bestGap = -1f
        repeat(6) {
            val candidate = contentLeft + r + random.nextFloat() * span
            val gap = recent.minOfOrNull { abs(it.x - candidate) } ?: Float.MAX_VALUE
            if (gap > bestGap) {
                bestGap = gap
                x = candidate
            }
        }
        balloons += Balloon(
            letter = letter,
            x = x,
            y = height + r * 1.2f,
            color = BALLOON_COLORS[random.nextInt(BALLOON_COLORS.size)],
            speed = height / 9f * (0.85f + random.nextFloat() * 0.3f),
            phase = random.nextFloat() * 6.28f,
        )
    }

    /** Where the balloon is drawn horizontally, including its drift and wobble. */
    private fun drawnX(b: Balloon) = b.x + sin(time * 1.5f + b.phase) * dp(8f) + shakeOffset(b.shake)

    override fun drawGame(canvas: Canvas) {
        for (b in balloons) drawBalloon(canvas, b)
        drawProgress(canvas)
    }

    private fun drawBalloon(canvas: Canvas, b: Balloon) {
        val r = radius
        val cx = drawnX(b)
        val cy = b.y

        strokePaint.color = Palette.INK
        strokePaint.alpha = 110
        strokePaint.strokeWidth = dp(2f)
        path.reset()
        path.moveTo(cx, cy + r * 1.05f)
        path.quadTo(cx + dp(10f), cy + r * 1.5f, cx, cy + r * 2f)
        canvas.drawPath(path, strokePaint)
        strokePaint.alpha = 255

        fillPaint.color = b.color
        path.reset()
        path.moveTo(cx, cy + r * 0.95f)
        path.lineTo(cx - r * 0.12f, cy + r * 1.1f)
        path.lineTo(cx + r * 0.12f, cy + r * 1.1f)
        path.close()
        canvas.drawPath(path, fillPaint)
        // Darker side first, then the face nudged up-left, like the toy blocks.
        fillPaint.color = Palette.edgeOf(b.color)
        canvas.drawOval(cx - r * 0.88f, cy - r, cx + r * 0.88f, cy + r, fillPaint)
        fillPaint.color = b.color
        canvas.drawOval(cx - r * 0.88f, cy - r, cx + r * 0.78f, cy + r * 0.88f, fillPaint)
        fillPaint.color = 0x66FFFFFF
        canvas.drawOval(cx - r * 0.55f, cy - r * 0.72f, cx - r * 0.2f, cy - r * 0.38f, fillPaint)

        drawText(canvas, b.letter.symbol, cx - r * 0.05f, cy - r * 0.06f, r * 1.05f, textColorOn(b.color), r * 1.4f)
    }

    /** All the track's letters at the bottom: popped ones in color, the next one dark. Two rows for long tracks. */
    private fun drawProgress(canvas: Canvas) {
        val rows = if (letters.size > 26) 2 else 1
        val perRow = ceil(letters.size / rows.toFloat()).toInt()
        val rowHeight = dp(36f)
        val stripHeight = rowHeight * rows + dp(4f)
        stripRect.set(contentLeft, contentBottom - stripHeight - dp(5f), contentRight, contentBottom - dp(5f))
        drawBlock(canvas, stripRect, Palette.WHITE, radius = dp(20f), depth = dp(5f), pressable = false)

        val cell = (stripRect.width() - dp(16f)) / perRow
        val size = min(cell * 0.95f, dp(22f))
        letters.forEachIndexed { i, letter ->
            val color = when {
                finished || i < nextIndex -> track.colorFor(i)
                i == nextIndex -> Palette.INK
                else -> 0xFFA9B6D3.toInt()
            }
            val letterSize = if (i == nextIndex && !finished) size * 1.35f else size
            val x = stripRect.left + dp(8f) + cell * (i % perRow + 0.5f)
            val y = stripRect.top + dp(2f) + rowHeight * (i / perRow + 0.5f)
            drawText(canvas, letter.symbol, x, y, letterSize, color, cell * 1.2f)
        }
    }

    override fun onPress(x: Float, y: Float) {
        if (finished) return
        val b = balloons.asReversed().firstOrNull { distance(x, y, drawnX(it), it.y) <= radius * 1.15f } ?: return
        if (b.letter == next) {
            player.correct(track, next)
            pop(b)
        } else {
            player.wrong(track, next, b.letter)
            Sounds.play(Sound.WRONG)
            b.shake = 0f
            if (time - lastHint > 1.5f) {
                lastHint = time
                speaker.say(lang.notThis(b.letter, next), lang.locale)
            }
        }
    }

    private fun pop(b: Balloon) {
        balloons.remove(b)
        Sounds.play(Sound.POP)
        celebrate(drawnX(b), b.y, 24)
        addStar()
        if (nextIndex == letters.lastIndex) {
            finished = true
            speaker.say(lang.balloonsDone(), lang.locale)
            repeat(5) { celebrate(width * (0.15f + 0.175f * it), height * 0.45f, 40) }
            after(5f) {
                nextIndex = 0
                finished = false
                balloons.clear()
                speaker.say(lang.balloonsAgain(next), lang.locale)
            }
        } else {
            speaker.say("${lang.name(b.letter)}!", lang.locale)
            nextIndex++
        }
    }

    private companion object {
        const val MAX_BALLOONS = 7

        // Yellow is too light for white letters.
        fun textColorOn(color: Int) = if (color == Palette.SUN) Palette.INK else Palette.WHITE
        val BALLOON_COLORS = intArrayOf(
            Palette.TOMATO, Palette.SUN, Palette.GRASS, Palette.OCEAN,
            Palette.GRAPE, Palette.PINK, Palette.TEAL, Palette.ORANGE,
        )
    }
}
