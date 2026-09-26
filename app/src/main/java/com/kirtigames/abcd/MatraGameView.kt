package com.kirtigames.abcd

import android.content.Context
import android.graphics.Canvas
import android.graphics.RectF
import kotlin.math.min

/** "क + ? = की": pick the matra that turns the consonant into the syllable. */
class MatraGameView(
    context: Context, speaker: Speaker, player: Player, private val track: Track,
) : GameView(context, speaker, player) {

    override val title = BarakhadiWords.BUILD_TITLE
    override val skyColor = Palette.MINT

    private val consonants = track.letters.mapNotNull { it.group }.distinct()

    /** अ adds no sign, so it can't be "picked"; every other matra can. */
    private val pickable = Barakhadi.matras.drop(1)

    private var consonant = ""
    private var target = pickable[0]
    private var options = emptyList<Matra>()
    private val targetSyllable get() = consonant + target.sign

    private var roundTime = 0f
    private var solved = false
    private var roundToken = 0

    /** A wrong pick is shown in the ? slot for a moment, so the child sees what it made. */
    private var wrongPick: Matra? = null
    private var wrongAt = 0f

    private val shakeTime = FloatArray(OPTION_COUNT) { 1f }
    private val consonantBlock = RectF()
    private val slotBlock = RectF()
    private val resultBlock = RectF()
    private val speakerButton = RectF()
    private val tiles = Array(OPTION_COUNT) { RectF() }
    private val drawRect = RectF()

    init {
        newRound()
    }

    private fun newRound() {
        roundToken++
        consonant = consonants.random(random)
        target = Pick.anyExcept(pickable, target, random)
        // The look-alike (ि for ी) is always among the choices: that's the real test.
        val tricky = listOfNotNull(Barakhadi.lookAlike(target))
        val others = pickable.filter { it != target && it !in tricky }.shuffled(random).take(OPTION_COUNT - 1 - tricky.size)
        options = (listOf(target) + tricky + others).shuffled(random)
        shakeTime.fill(1f)
        solved = false
        wrongPick = null
        roundTime = 0f
        ask()
    }

    private fun ask() = speaker.say(BarakhadiWords.buildAsk(targetSyllable), Hindi.locale)

    override fun update(dt: Float) {
        roundTime += dt
        for (i in shakeTime.indices) shakeTime[i] += dt
        if (wrongPick != null && time - wrongAt > WRONG_SHOW_SECONDS) wrongPick = null
    }

    override fun drawGame(canvas: Canvas) {
        // Equation row: [क] + [?] = [की]
        val areaW = contentRight - contentLeft
        val side = min(dp(104f), (areaW - dp(96f)) / 3f)
        val signGap = dp(48f)
        val rowWidth = side * 3 + signGap * 2
        val top = contentTop + dp(8f)
        var x = width / 2f - rowWidth / 2f
        consonantBlock.set(x, top, x + side, top + side)
        x += side + signGap
        slotBlock.set(x, top, x + side, top + side)
        x += side + signGap
        resultBlock.set(x, top, x + side, top + side)

        drawLetterBlock(canvas, consonantBlock, Palette.INK, consonant, Palette.WHITE, textScale = 0.55f)
        drawText(canvas, "+", (consonantBlock.right + slotBlock.left) / 2f, slotBlock.centerY(), side * 0.5f, Palette.INK)
        drawText(canvas, "=", (slotBlock.right + resultBlock.left) / 2f, slotBlock.centerY(), side * 0.5f, Palette.INK)

        val wrong = wrongPick
        when {
            solved -> drawLetterBlock(canvas, slotBlock, Palette.GRASS, target.shown, Palette.WHITE, textScale = 0.55f)
            wrong != null -> drawLetterBlock(canvas, slotBlock, Palette.TOMATO, wrong.shown, Palette.WHITE, textScale = 0.55f)
            else -> drawLetterBlock(canvas, slotBlock, Palette.WHITE, "?", Palette.INK, textScale = 0.55f)
        }
        // The goal; after a wrong pick it briefly shows what that pick made instead.
        val shownResult = if (wrong != null) consonant + wrong.sign else targetSyllable
        drawLetterBlock(canvas, resultBlock, if (wrong != null) Palette.TOMATO else Palette.GRAPE, shownResult, Palette.WHITE, textScale = 0.5f)

        val speakerSize = dp(56f)
        speakerButton.set(width / 2f - speakerSize / 2f, resultBlock.bottom + dp(24f), width / 2f + speakerSize / 2f, resultBlock.bottom + dp(24f) + speakerSize)
        val sink = drawBlock(canvas, speakerButton, Palette.WHITE, depth = dp(5f))
        drawEmoji(canvas, "🔊", speakerButton.centerX(), speakerButton.centerY() + sink, speakerSize * 0.5f)

        // Matra choices: 2×2 in portrait, one row in landscape.
        val gridTop = speakerButton.bottom + dp(24f)
        val cols = if (width > height) OPTION_COUNT else 2
        val rows = OPTION_COUNT / cols
        val cellW = areaW / cols
        val cellH = (contentBottom - gridTop) / rows
        val half = min(cellW, cellH) * 0.4f
        options.forEachIndexed { i, matra ->
            val cx = contentLeft + cellW * (i % cols + 0.5f)
            val cy = gridTop + cellH * (i / cols + 0.5f)
            squareAt(tiles[i], cx, cy, half)
            var scale = popIn((roundTime - i * 0.07f) / 0.4f)
            if (solved) scale *= if (matra == target) 1.1f else 0.6f
            if (scale > 0.02f) {
                squareAt(drawRect, cx + shakeOffset(shakeTime[i]), cy, half * scale)
                drawLetterBlock(canvas, drawRect, Palette.TILES[i % Palette.TILES.size], matra.shown, Palette.WHITE, textScale = 0.6f)
            }
        }
    }

    override fun onTap(x: Float, y: Float) {
        if (speakerButton.contains(x, y) || resultBlock.contains(x, y)) {
            ask()
            return
        }
        if (solved) return
        val i = tiles.indexOfFirst { it.contains(x, y) }
        if (i < 0) return
        val picked = options[i]
        val targetLetter = Letter(targetSyllable, group = consonant)
        if (picked == target) {
            solved = true
            wrongPick = null
            player.correct(track, targetLetter)
            addStar()
            celebrate(slotBlock.centerX(), slotBlock.centerY())
            speaker.say(BarakhadiWords.buildRight(consonant, target, targetSyllable, random), Hindi.locale)
            val token = roundToken
            after(2.8f) { if (token == roundToken) newRound() }
        } else {
            val made = consonant + picked.sign
            player.wrong(track, targetLetter, Letter(made, group = consonant))
            Sounds.play(Sound.WRONG)
            shakeTime[i] = 0f
            wrongPick = picked
            wrongAt = time
            speaker.say(BarakhadiWords.buildWrong(made, targetSyllable), Hindi.locale)
        }
    }

    private companion object {
        const val OPTION_COUNT = 4
        const val WRONG_SHOW_SECONDS = 1.6f
    }
}
