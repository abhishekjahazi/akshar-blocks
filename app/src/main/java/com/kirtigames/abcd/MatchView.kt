package com.kirtigames.abcd

import android.content.Context
import android.graphics.Canvas
import android.graphics.RectF
import kotlin.math.min

/** Show a picture; the child picks the letter its word starts with. */
class MatchView(
    context: Context, speaker: Speaker, stars: StarBank, private val track: Track,
) : GameView(context, speaker, stars) {

    override val title = track.lang.matchTitle
    override val skyColor = Palette.LILAC

    private val lang = track.lang
    private val pictures = track.pictureLetters
    private var card = pictures.random(random)
    private var options = emptyList<Letter>()
    private val tiles = Array(OPTION_COUNT) { RectF() }
    private val drawRect = RectF()
    private val shakeTime = FloatArray(OPTION_COUNT) { 1f }
    private var roundTime = 0f
    private var solvedIndex = -1
    private var solvedAt = 0f
    private val picture = RectF()

    init {
        startRound()
    }

    private fun newRound() {
        card = Pick.anyExcept(pictures, card, random)
        startRound()
    }

    private fun startRound() {
        options = Pick.choices(card, track.letters, OPTION_COUNT, random)
        shakeTime.fill(1f)
        solvedIndex = -1
        roundTime = 0f
        ask()
    }

    private fun ask() = speaker.say(lang.matchAsk(card), lang.locale)

    override fun update(dt: Float) {
        roundTime += dt
        for (i in shakeTime.indices) shakeTime[i] += dt
    }

    override fun drawGame(canvas: Canvas) {
        val areaW = contentRight - contentLeft
        val areaH = contentBottom - contentTop
        val landscape = width > height

        // Picture on top (portrait) or on the left (landscape); letter tiles in the rest.
        if (landscape) {
            picture.set(contentLeft, contentTop, contentLeft + areaW * 0.55f, contentBottom - dp(10f))
        } else {
            picture.set(contentLeft, contentTop, contentRight, contentTop + areaH * 0.6f)
        }

        val appear = popIn(roundTime / 0.4f)
        canvas.save()
        canvas.scale(appear, appear, picture.centerX(), picture.centerY())
        val sink = drawBlock(canvas, picture, Palette.WHITE, radius = dp(36f), depth = dp(10f))
        val pw = picture.width()
        val ph = picture.height()
        drawEmoji(canvas, card.emoji.orEmpty(), picture.centerX(), picture.top + ph * 0.42f + sink, min(ph * 0.5f, pw * 0.55f))

        // Until solved, the letter part of the word is hidden behind a "?".
        val solved = solvedIndex >= 0
        val word = card.word.orEmpty()
        val head = card.headLength
        val shown = if (solved) word else "?" + word.substring(head)
        val highlight = if (solved) track.colorFor(card) else Palette.TOMATO
        drawWord(
            canvas, shown, if (solved) head else 1, highlight,
            picture.centerX(), picture.top + ph * 0.84f + sink, min(ph * 0.13f, pw * 0.14f), pw * 0.86f,
        )
        canvas.restore()

        for (i in 0 until OPTION_COUNT) {
            val tileX: Float
            val tileY: Float
            val half: Float
            if (landscape) {
                val left = picture.right + dp(16f)
                val cellH = areaH / OPTION_COUNT
                half = min((contentRight - left) * 0.3f, cellH * 0.38f)
                tileX = (left + contentRight) / 2f
                tileY = contentTop + cellH * (i + 0.5f)
            } else {
                val top = picture.bottom + dp(24f)
                val cellW = areaW / OPTION_COUNT
                half = min(cellW * 0.4f, (contentBottom - top) * 0.4f)
                tileX = contentLeft + cellW * (i + 0.5f)
                tileY = (top + contentBottom) / 2f
            }
            squareAt(tiles[i], tileX, tileY, half)

            var scale = popIn((roundTime - 0.2f - i * 0.08f) / 0.4f)
            if (solved) {
                val fade = ((roundTime - solvedAt) / 0.3f).coerceIn(0f, 1f)
                scale *= if (i == solvedIndex) 1f + 0.12f * fade else 1f - fade
            }
            if (scale > 0.02f) {
                squareAt(drawRect, tileX + shakeOffset(shakeTime[i]), tileY, half * scale)
                drawLetterBlock(canvas, drawRect, Palette.TILES[i % Palette.TILES.size], options[i].symbol, Palette.WHITE)
            }
        }
    }

    override fun onTap(x: Float, y: Float) {
        if (picture.contains(x, y)) {
            ask()
            return
        }
        if (solvedIndex >= 0) return

        val i = tiles.indexOfFirst { it.contains(x, y) }
        if (i < 0) return
        if (options[i] == card) {
            solvedIndex = i
            solvedAt = roundTime
            addStar()
            celebrate(tiles[i].centerX(), tiles[i].centerY())
            speaker.say(lang.matchRight(card, random), lang.locale)
            after(2.8f) { newRound() }
        } else {
            shakeTime[i] = 0f
            speaker.say(lang.matchWrong(card), lang.locale)
        }
    }

    private companion object {
        const val OPTION_COUNT = 3
    }
}
