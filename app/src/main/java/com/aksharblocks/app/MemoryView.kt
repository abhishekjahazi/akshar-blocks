package com.aksharblocks.app

import android.content.Context
import android.graphics.Canvas
import android.graphics.RectF
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.min

/**
 * Memory: flip two cards at a time to find pairs. Letter tracks pair a letter with its
 * picture (A ↔ 🍎); number tracks pair a number with that many objects (3 ↔ 🍎🍎🍎).
 * Every card is said out loud when it turns over.
 */
class MemoryView(
    context: Context, speaker: Speaker, player: Player, private val track: Track,
) : GameView(context, speaker, player) {

    /** One card: the letter it belongs to, and whether it shows the letter or its picture. */
    private class Card(val letter: Letter, val showsSymbol: Boolean) {
        var up = false
        var matched = false
        var flipAt = -10f
    }

    override val title = track.lang.memoryTitle
    override val skyColor = Palette.SKY

    private val lang = track.lang
    private var cards: List<Card> = emptyList()
    private val rects = List(PAIRS * 2) { RectF() }
    private val drawRect = RectF()
    private val inner = RectF()
    private val open = ArrayList<Card>()
    private var boardToken = 0

    /** What number cards are made of this board (🍎, ⭐, …). */
    private var countEmoji = "🍎"

    init {
        newBoard()
    }

    private fun pool(): List<Letter> =
        if (track.isNumbers) track.letters.take(Counting.MAX)
        else track.pictureLetters.filter { canDraw(it.emoji) }

    private fun newBoard() {
        boardToken++
        val chosen = pool().shuffled(random).take(PAIRS)
        cards = chosen.flatMap { listOf(Card(it, showsSymbol = true), Card(it, showsSymbol = false)) }.shuffled(random)
        open.clear()
        countEmoji = Counting.things.filter { canDraw(it.emoji) }.ifEmpty { Counting.things }.random(random).emoji
        speaker.say(lang.memoryAsk, lang.locale)
    }

    /** What the voice says when a card turns over: the letter, its word, or the number. */
    private fun say(card: Card) {
        val text = if (card.showsSymbol || track.isNumbers) lang.name(card.letter) else card.letter.word.orEmpty()
        speaker.say("$text!", lang.locale)
    }

    override fun drawGame(canvas: Canvas) {
        val landscape = width > height
        val cols = if (landscape) 4 else 3
        val rows = (cards.size + cols - 1) / cols
        val cellW = (contentRight - contentLeft) / cols
        val cellH = (contentBottom - contentTop) / rows
        val gap = dp(10f)

        cards.forEachIndexed { i, card ->
            val rect = rects[i]
            val left = contentLeft + cellW * (i % cols)
            val top = contentTop + cellH * (i / cols)
            rect.set(left + gap, top + gap, left + cellW - gap, top + cellH - gap - dp(6f))

            // A flip squeezes the card to nothing and opens it again showing the other side.
            val progress = ((time - card.flipAt) / FLIP_SECONDS).coerceIn(0f, 1f)
            val faceUp = card.up || card.matched
            val showFace = if (progress < 1f) (if (faceUp) progress > 0.5f else progress < 0.5f) else faceUp
            val squeeze = if (progress < 1f && motionEnabled) abs(cos(progress * PI.toFloat())) else 1f

            canvas.save()
            canvas.scale(squeeze.coerceAtLeast(0.02f), 1f, rect.centerX(), rect.centerY())
            if (showFace) drawFace(canvas, rect, card) else drawBack(canvas, rect)
            canvas.restore()
        }
    }

    private fun drawBack(canvas: Canvas, rect: RectF) {
        val sink = drawBlock(canvas, rect, track.color, radius = dp(18f), depth = dp(7f))
        drawText(canvas, "?", rect.centerX(), rect.centerY() + sink, min(rect.width(), rect.height()) * 0.5f, Palette.WHITE)
    }

    private fun drawFace(canvas: Canvas, rect: RectF, card: Card) {
        drawRect.set(rect)
        if (card.matched) drawRect.inset(rect.width() * 0.04f, rect.height() * 0.04f)
        val sink = drawBlock(canvas, drawRect, Palette.WHITE, radius = dp(18f), depth = dp(7f), pressable = false)
        val size = min(drawRect.width(), drawRect.height())
        when {
            card.showsSymbol -> drawText(
                canvas, card.letter.symbol, drawRect.centerX(), drawRect.centerY() + sink,
                size * 0.6f, track.colorFor(card.letter), drawRect.width() * 0.86f,
            )
            track.isNumbers -> {
                inner.set(drawRect.left + dp(8f), drawRect.top + dp(8f) + sink, drawRect.right - dp(8f), drawRect.bottom - dp(8f) + sink)
                drawCountedObjects(canvas, inner, track.letters.indexOf(card.letter) + 1, countEmoji)
            }
            else -> drawEmoji(canvas, card.letter.emoji.orEmpty(), drawRect.centerX(), drawRect.centerY() + sink, size * 0.6f)
        }
        if (card.matched) {
            fillPaint.color = Palette.GRASS
            val r = size * 0.12f
            canvas.drawCircle(drawRect.right - r * 0.9f, drawRect.top + r * 0.9f + sink, r, fillPaint)
            drawText(canvas, "✓", drawRect.right - r * 0.9f, drawRect.top + r * 0.9f + sink, r * 1.3f, Palette.WHITE)
        }
    }

    override fun onTap(x: Float, y: Float) {
        val i = rects.indexOfFirst { it.contains(x, y) }
        if (i < 0 || i >= cards.size) return
        val card = cards[i]
        if (card.up || card.matched || open.size >= 2) return

        card.up = true
        card.flipAt = time
        Sounds.play(Sound.TAP)
        say(card)
        open += card
        if (open.size < 2) return

        val (first, second) = open
        if (first.letter == second.letter) {
            first.matched = true
            second.matched = true
            open.clear()
            player.correct(track, first.letter)
            addStar()
            celebrate(rects[i].centerX(), rects[i].centerY(), 30)
            // Let the second card's name be heard before the praise.
            val token = boardToken
            after(0.9f) { if (token == boardToken) speaker.say(lang.found(first.letter, random), lang.locale) }
            if (cards.all { it.matched }) {
                after(2.2f) {
                    if (token == boardToken) {
                        speaker.say(lang.memoryDone(), lang.locale)
                        repeat(4) { celebrate(width * (0.2f + 0.2f * it), height * 0.4f, 36) }
                    }
                }
                after(5.5f) { if (token == boardToken) newBoard() }
            }
        } else {
            // Not a pair: give the child a moment to look, then turn both back.
            val token = boardToken
            after(1.3f) {
                if (token == boardToken) {
                    for (c in listOf(first, second)) {
                        c.up = false
                        c.flipAt = time
                    }
                    open.clear()
                }
            }
        }
    }

    private companion object {
        const val PAIRS = 6
        const val FLIP_SECONDS = 0.3f
    }
}
