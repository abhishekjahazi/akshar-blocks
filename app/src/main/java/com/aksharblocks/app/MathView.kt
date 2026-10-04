package com.aksharblocks.app

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import kotlin.math.min

/**
 * The maths games, all with pictures: add two groups (🍎🍎 + 🍎 = ?), take some away, tap the
 * side with more (or fewer), and finish a pattern (🔴🔵🔴 ?). Every question is spoken in
 * English and then in Hindi.
 */
class MathView(
    context: Context, speaker: Speaker, player: Player, private val game: MathGame,
) : GameView(context, speaker, player) {

    override val title = game.label
    override val skyColor = Palette.ICE

    private val lang = English

    private var thing = Counting.things.first()
    private var sum = Sum(1, 1)
    private var askMore = true
    private var pattern = Pattern(emptyList(), "", emptyList())

    /** The answer tiles: numbers, or pictures for patterns. */
    private var options = emptyList<String>()
    private var answer = ""

    private var roundTime = 0f
    private var solved = false
    private var roundToken = 0

    private val shakeTime = FloatArray(OPTION_COUNT) { 1f }
    private val picture = RectF()
    private val tiles = Array(OPTION_COUNT) { RectF() }
    private val sides = Array(2) { RectF() }
    private val area = RectF()
    private val drawRect = RectF()
    private val strike = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        color = Palette.TOMATO
    }

    init {
        newRound()
    }

    private fun newRound() {
        roundToken++
        thing = Pick.anyExcept(Counting.things.filter { canDraw(it.emoji) }.ifEmpty { Counting.things }, thing, random)
        when (game) {
            MathGame.ADD -> {
                sum = Maths.add(random)
                setNumberOptions(sum.total)
            }
            MathGame.TAKE_AWAY -> {
                sum = Maths.takeAway(random)
                setNumberOptions(sum.left)
            }
            MathGame.MORE -> {
                sum = Maths.compare(random)
                askMore = random.nextBoolean()
                options = emptyList()
            }
            MathGame.PATTERN -> {
                pattern = Maths.pattern(random)
                options = pattern.choices
                answer = pattern.answer
            }
        }
        shakeTime.fill(1f)
        solved = false
        roundTime = 0f
        ask()
    }

    private fun setNumberOptions(right: Int) {
        options = Maths.choices(right, random).map { it.toString() }
        answer = right.toString()
    }

    private fun ask() = speaker.say(
        when (game) {
            MathGame.ADD -> MathWords.addAsk(thing)
            MathGame.TAKE_AWAY -> MathWords.takeAwayAsk(thing)
            MathGame.MORE -> if (askMore) MathWords.MORE_ASK else MathWords.FEWER_ASK
            MathGame.PATTERN -> MathWords.PATTERN_ASK
        },
        lang.locale,
    )

    private fun rightLine(): String = when (game) {
        MathGame.ADD -> MathWords.addRight(sum)
        MathGame.TAKE_AWAY -> MathWords.takeAwayRight(sum)
        MathGame.MORE -> MathWords.compareRight(sum, askMore)
        MathGame.PATTERN -> MathWords.PATTERN_RIGHT
    }

    private fun wrongLine(): String = when (game) {
        MathGame.ADD -> MathWords.ADD_WRONG
        MathGame.TAKE_AWAY -> MathWords.TAKE_AWAY_WRONG
        else -> MathWords.LOOK_AGAIN
    }

    override fun update(dt: Float) {
        roundTime += dt
        for (i in shakeTime.indices) shakeTime[i] += dt
    }

    override fun drawGame(canvas: Canvas) {
        if (game == MathGame.MORE) {
            drawCompare(canvas)
            return
        }
        val areaH = contentBottom - contentTop
        val landscape = width > height
        if (landscape) {
            picture.set(contentLeft, contentTop, contentLeft + (contentRight - contentLeft) * 0.66f, contentBottom - dp(10f))
        } else {
            picture.set(contentLeft, contentTop, contentRight, contentTop + areaH * 0.58f)
        }
        val appear = popIn(roundTime / 0.4f)
        canvas.save()
        canvas.scale(appear, appear, picture.centerX(), picture.centerY())
        val sink = drawBlock(canvas, picture, Palette.WHITE, radius = dp(36f), depth = dp(10f))
        area.set(picture.left + dp(14f), picture.top + dp(14f) + sink, picture.right - dp(14f), picture.bottom - dp(14f) + sink)
        when (game) {
            MathGame.ADD -> drawAdd(canvas)
            MathGame.TAKE_AWAY -> drawTakeAway(canvas)
            else -> drawPattern(canvas)
        }
        canvas.restore()
        drawOptions(canvas, landscape, areaH)
    }

    /** Two groups with a plus between them, and "3 + 2 = ?" underneath. */
    private fun drawAdd(canvas: Canvas) {
        val sentenceH = area.height() * 0.24f
        val groupsBottom = area.bottom - sentenceH
        val plusW = area.width() * 0.12f
        val groupW = (area.width() - plusW) / 2f
        drawRect.set(area.left, area.top, area.left + groupW, groupsBottom)
        drawGroup(canvas, drawRect, sum.a)
        drawText(canvas, "+", area.centerX(), (area.top + groupsBottom) / 2f, plusW * 1.6f, Palette.INK)
        drawRect.set(area.right - groupW, area.top, area.right, groupsBottom)
        drawGroup(canvas, drawRect, sum.b)
        val result = if (solved) "${sum.total}" else "?"
        drawText(canvas, "${sum.a} + ${sum.b} = $result", area.centerX(), groupsBottom + sentenceH / 2f, sentenceH * 0.7f, Palette.INK, area.width() * 0.8f)
    }

    /** All the things, the ones taken away crossed out, and "5 − 2 = ?" underneath. */
    private fun drawTakeAway(canvas: Canvas) {
        val sentenceH = area.height() * 0.24f
        val top = area.top
        val bottom = area.bottom - sentenceH
        val cols = min(sum.a, 5)
        val rows = (sum.a + 4) / 5
        // Big enough to see clearly, but 9 things still fit in two rows of five.
        val cell = minOf(area.width() / cols, (bottom - top) / rows, area.width() / 4f)
        val left = area.centerX() - cols * cell / 2f
        val gridTop = (top + bottom) / 2f - rows * cell / 2f
        // The crossing-out draws itself a moment after the round starts, so the child sees it happen.
        val crossed = ((roundTime - 0.8f) / 0.6f).coerceIn(0f, 1f)
        strike.strokeWidth = cell * 0.07f
        for (i in 0 until sum.a) {
            val cx = left + cell * (i % 5 + 0.5f)
            val cy = gridTop + cell * (i / 5 + 0.5f)
            drawEmoji(canvas, thing.emoji, cx, cy, cell * 0.78f)
            if (i >= sum.left && crossed > 0f) {
                val r = cell * 0.36f * crossed
                canvas.drawLine(cx - r, cy - r, cx + r, cy + r, strike)
                canvas.drawLine(cx + r, cy - r, cx - r, cy + r, strike)
            }
        }
        val result = if (solved) "${sum.left}" else "?"
        drawText(canvas, "${sum.a} − ${sum.b} = $result", area.centerX(), bottom + sentenceH / 2f, sentenceH * 0.7f, Palette.INK, area.width() * 0.8f)
    }

    /** The pattern in a row (two rows when long), with a "?" block where the next one goes. */
    private fun drawPattern(canvas: Canvas) {
        val items = pattern.shown + "?"
        val perRow = if (items.size > 6 && area.width() < area.height() * 1.6f) (items.size + 1) / 2 else items.size
        val rows = (items.size + perRow - 1) / perRow
        val cell = min(area.width() / perRow, area.height() / rows * 0.8f)
        val gridTop = area.centerY() - rows * cell / 2f
        items.forEachIndexed { i, item ->
            val row = i / perRow
            val inRow = min(perRow, items.size - row * perRow)
            val cx = area.centerX() - inRow * cell / 2f + cell * (i % perRow + 0.5f)
            val cy = gridTop + cell * (row + 0.5f)
            if (item == "?") {
                squareAt(drawRect, cx, cy, cell * 0.4f)
                if (solved) drawEmoji(canvas, pattern.answer, cx, cy, cell * 0.66f)
                else drawLetterBlock(canvas, drawRect, Palette.SUN, "?", Palette.INK, depth = dp(4f))
            } else {
                drawEmoji(canvas, item, cx, cy, cell * 0.66f)
            }
        }
    }

    /** Up to five things, three to a row, centered in [box]. */
    private fun drawGroup(canvas: Canvas, box: RectF, count: Int) {
        val cols = min(count, 3)
        val rows = (count + 2) / 3
        // Sized for the bigger group, so things on both sides of the plus are the same size.
        val biggest = maxOf(sum.a, sum.b)
        val cell = minOf(box.width() / min(biggest, 3), box.height() / ((biggest + 2) / 3), box.width() / 2f)
        val left = box.centerX() - cols * cell / 2f
        val top = box.centerY() - rows * cell / 2f
        for (i in 0 until count) {
            drawEmoji(canvas, thing.emoji, left + cell * (i % 3 + 0.5f), top + cell * (i / 3 + 0.5f), cell * 0.8f)
        }
    }

    private fun drawOptions(canvas: Canvas, landscape: Boolean, areaH: Float) {
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
                drawLetterBlock(canvas, drawRect, Palette.TILES[i % Palette.TILES.size], options[i], Palette.WHITE)
            }
        }
    }

    /** Two white cards side by side (stacked in portrait); the child taps one. */
    private fun drawCompare(canvas: Canvas) {
        val landscape = width > height
        val gap = dp(18f)
        val labelH = dp(56f)
        if (landscape) {
            val w = (contentRight - contentLeft - gap) / 2f
            sides[0].set(contentLeft, contentTop, contentLeft + w, contentBottom - labelH)
            sides[1].set(contentRight - w, contentTop, contentRight, contentBottom - labelH)
        } else {
            val h = (contentBottom - contentTop - gap * 2 - labelH * 2) / 2f
            sides[0].set(contentLeft, contentTop, contentRight, contentTop + h)
            sides[1].set(contentLeft, sides[0].bottom + labelH + gap, contentRight, sides[0].bottom + labelH + gap + h)
        }
        val counts = intArrayOf(sum.a, sum.b)
        val rightSide = rightSide()
        for (i in 0..1) {
            val box = sides[i]
            var scale = popIn((roundTime - i * 0.1f) / 0.4f)
            if (solved) scale *= if (i == rightSide) 1.04f else 0.92f
            canvas.save()
            canvas.translate(shakeOffset(shakeTime[i]), 0f)
            canvas.scale(scale, scale, box.centerX(), box.centerY())
            val face = if (solved && i == rightSide) 0xFFE6FCF5.toInt() else Palette.WHITE
            val sink = drawBlock(canvas, box, face, radius = dp(30f), depth = dp(9f))
            area.set(box.left + dp(12f), box.top + dp(12f) + sink, box.right - dp(12f), box.bottom - dp(12f) + sink)
            drawThings(canvas, area, counts[i])
            canvas.restore()
            // After the answer, each side shows how many it has.
            if (solved) drawText(canvas, "${counts[i]}", box.centerX(), box.bottom + labelH / 2f + dp(6f), labelH * 0.7f, Palette.INK)
        }
    }

    /**
     * [count] things in rows of five, centered in [box]. Both sides use the same size (made for
     * nine), so a side with more things never looks smaller.
     */
    private fun drawThings(canvas: Canvas, box: RectF, count: Int) {
        val cell = min(box.width() / 5f, box.height() / 2f)
        val cols = min(count, 5)
        val rows = (count + 4) / 5
        val left = box.centerX() - cols * cell / 2f
        val top = box.centerY() - rows * cell / 2f
        for (i in 0 until count) {
            drawEmoji(canvas, thing.emoji, left + cell * (i % 5 + 0.5f), top + cell * (i / 5 + 0.5f), cell * 0.82f)
        }
    }

    /** Which side (0 or 1) answers the question. */
    private fun rightSide(): Int = if ((sum.a > sum.b) == askMore) 0 else 1

    override fun onTap(x: Float, y: Float) {
        if (solved) return
        if (game == MathGame.MORE) {
            val i = sides.indexOfFirst { it.contains(x, y) }
            if (i < 0) return
            if (i == rightSide()) win(sides[i].centerX(), sides[i].centerY()) else miss(i)
            return
        }
        if (picture.contains(x, y)) {
            ask()
            return
        }
        val i = tiles.indexOfFirst { it.contains(x, y) }
        if (i < 0 || i >= options.size) return
        if (options[i] == answer) win(tiles[i].centerX(), tiles[i].centerY()) else miss(i)
    }

    private fun win(x: Float, y: Float) {
        solved = true
        addStar()
        celebrate(x, y)
        speaker.say("${lang.praise(random)}|${rightLine()}", lang.locale)
        val token = roundToken
        after(NEXT_SECONDS) { if (token == roundToken) newRound() }
    }

    private fun miss(i: Int) {
        Sounds.play(Sound.WRONG)
        shakeTime[i] = 0f
        speaker.say(wrongLine(), lang.locale)
    }

    private companion object {
        const val OPTION_COUNT = 3
        const val NEXT_SECONDS = 4.2f
    }
}
