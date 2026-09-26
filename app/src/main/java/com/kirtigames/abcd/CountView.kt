package com.kirtigames.abcd

import android.content.Context
import android.graphics.Canvas
import android.graphics.RectF
import kotlin.math.min

/**
 * "How many apples?": count the objects (1–10) and pick the number. After a right answer
 * the objects are counted out loud one by one, each getting its number badge.
 */
class CountView(
    context: Context, speaker: Speaker, player: Player, private val track: Track,
) : GameView(context, speaker, player) {

    override val title = track.lang.countTitle
    override val skyColor = Palette.ICE

    private val lang = track.lang
    private val numbers = track.letters.take(Counting.MAX)

    private var thing = Counting.things.first()
    private var answer: Letter = numbers.first()
    private var options = emptyList<Letter>()

    private var roundTime = 0f
    private var solved = false
    private var roundToken = 0

    /** How many objects have been counted out loud so far (after a right answer). */
    private var counted = 0

    private val shakeTime = FloatArray(OPTION_COUNT) { 1f }
    private val picture = RectF()
    private val objectsArea = RectF()
    private val tiles = Array(OPTION_COUNT) { RectF() }
    private val drawRect = RectF()

    init {
        newRound()
    }

    private fun newRound() {
        roundToken++
        thing = Pick.anyExcept(Counting.things.filter { canDraw(it.emoji) }.ifEmpty { Counting.things }, thing, random)
        answer = Pick.anyExcept(numbers, answer, random)
        // Wrong choices are close to the answer (6 with 5 and 7), which is what counting practice needs.
        val i = numbers.indexOf(answer)
        val near = numbers.subList(maxOf(0, i - 2), minOf(numbers.size, i + 3))
        options = Pick.choices(answer, near, OPTION_COUNT, random).sortedBy { numbers.indexOf(it) }
        shakeTime.fill(1f)
        solved = false
        counted = 0
        roundTime = 0f
        ask()
    }

    private fun ask() = speaker.say(lang.countAsk(thing), lang.locale)

    /** Counts the objects aloud one at a time, then praises and moves on. */
    private fun countOut() {
        val token = roundToken
        val total = numbers.indexOf(answer) + 1
        for (n in 1..total) {
            after(n * STEP_SECONDS) {
                if (token == roundToken) {
                    counted = n
                    speaker.say(lang.name(numbers[n - 1]), lang.locale)
                }
            }
        }
        after(total * STEP_SECONDS + 0.7f) {
            if (token == roundToken) speaker.say(lang.countRight(answer, thing, random), lang.locale)
        }
        after(total * STEP_SECONDS + 3.2f) { if (token == roundToken) newRound() }
    }

    override fun update(dt: Float) {
        roundTime += dt
        for (i in shakeTime.indices) shakeTime[i] += dt
    }

    override fun drawGame(canvas: Canvas) {
        val areaH = contentBottom - contentTop
        val landscape = width > height

        if (landscape) {
            picture.set(contentLeft, contentTop, contentLeft + (contentRight - contentLeft) * 0.62f, contentBottom - dp(10f))
        } else {
            picture.set(contentLeft, contentTop, contentRight, contentTop + areaH * 0.58f)
        }
        val appear = popIn(roundTime / 0.4f)
        canvas.save()
        canvas.scale(appear, appear, picture.centerX(), picture.centerY())
        val sink = drawBlock(canvas, picture, Palette.WHITE, radius = dp(36f), depth = dp(10f))
        objectsArea.set(picture.left + dp(16f), picture.top + dp(16f) + sink, picture.right - dp(16f), picture.bottom - dp(16f) + sink)
        drawCountedObjects(canvas, objectsArea, numbers.indexOf(answer) + 1, thing.emoji, counted) { numbers[it].symbol }
        canvas.restore()

        // Number choices, smallest first.
        for (i in options.indices) {
            val cx: Float
            val cy: Float
            val half: Float
            if (landscape) {
                val left = picture.right + dp(16f)
                val cellH = areaH / OPTION_COUNT
                half = min((contentRight - left) * 0.3f, cellH * 0.38f)
                cx = (left + contentRight) / 2f
                cy = contentTop + cellH * (i + 0.5f)
            } else {
                val top = picture.bottom + dp(24f)
                val cellW = (contentRight - contentLeft) / OPTION_COUNT
                half = min(cellW * 0.4f, (contentBottom - top) * 0.4f)
                cx = contentLeft + cellW * (i + 0.5f)
                cy = (top + contentBottom) / 2f
            }
            squareAt(tiles[i], cx, cy, half)
            var scale = popIn((roundTime - 0.2f - i * 0.08f) / 0.4f)
            if (solved) scale *= if (options[i] == answer) 1.1f else 0.6f
            if (scale > 0.02f) {
                squareAt(drawRect, cx + shakeOffset(shakeTime[i]), cy, half * scale)
                drawLetterBlock(canvas, drawRect, Palette.TILES[i % Palette.TILES.size], options[i].symbol, Palette.WHITE)
            }
        }
    }

    override fun onTap(x: Float, y: Float) {
        if (picture.contains(x, y)) {
            if (!solved) ask()
            return
        }
        if (solved) return
        val i = tiles.indexOfFirst { it.contains(x, y) }
        if (i < 0) return
        if (options[i] == answer) {
            solved = true
            player.correct(track, answer)
            addStar()
            celebrate(tiles[i].centerX(), tiles[i].centerY())
            countOut()
        } else {
            player.wrong(track, answer, options[i])
            shakeTime[i] = 0f
            speaker.say(lang.countWrong(thing), lang.locale)
        }
    }

    private companion object {
        const val OPTION_COUNT = 3
        const val STEP_SECONDS = 0.75f
    }
}
