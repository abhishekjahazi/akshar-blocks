package com.aksharblocks.app

import android.content.Context
import android.graphics.Canvas
import android.graphics.RectF
import kotlin.math.min

/**
 * "Find the letter B!" / "क ढूंढो!": tap the right one of four letter blocks. With [bySound]
 * the voice says a sound instead: "Who says moo?" (animals), "Which letter says buh?" (phonics)
 */
class FindView(
    context: Context, speaker: Speaker, player: Player, private val track: Track,
    private val bySound: Boolean = false,
) : GameView(context, speaker, player) {

    override val title = if (bySound) track.lang.soundsTitle else track.lang.findTitle
    override val skyColor = Palette.MINT

    private val lang = track.lang
    private var target: Letter? = null
    private var options = emptyList<Letter>()
    private val tiles = Array(OPTION_COUNT) { RectF() }
    private val drawRect = RectF()

    /** Seconds since each tile started shaking (≥ 0.5 means still). */
    private val shakeTime = FloatArray(OPTION_COUNT) { 1f }
    private var roundTime = 0f
    private var solvedIndex = -1
    private var solvedAt = 0f

    private val targetBlock = RectF()
    private val speakerButton = RectF()

    init {
        newRound()
    }

    private fun newRound() {
        // Smart practice: letters this child gets wrong come up more often, with their usual mix-ups.
        val stats = player.stats(track)
        // The sounds game only asks about animals that have a sound.
        val pool = if (bySound) track.letters.filter { it.sound != null } else track.letters
        val next = Coach.pickTarget(pool, stats, random, except = target)
        target = next
        // By sound, two letters that sound the same (C and K: "kuh") can't both be offered.
        val choicePool = if (bySound) track.choicePool(next).filter { it == next || it.sound != next.sound } else track.choicePool(next)
        options = Coach.choices(next, choicePool, stats, OPTION_COUNT, random)
        shakeTime.fill(1f)
        solvedIndex = -1
        roundTime = 0f
        askForTarget()
    }

    private fun askForTarget() {
        target?.let { speaker.say(if (bySound) lang.soundAsk(it) else lang.find(it), lang.locale) }
    }

    override fun update(dt: Float) {
        roundTime += dt
        for (i in shakeTime.indices) shakeTime[i] += dt
    }

    override fun drawGame(canvas: Canvas) {
        val target = target ?: return

        // Prompt row: the letter to find, and a button to hear it again.
        val promptHeight = dp(112f)
        val side = dp(88f)
        val promptY = contentTop + side / 2f
        val cx = width / 2f
        squareAt(targetBlock, cx - dp(4f) - side / 2f, promptY, side / 2f)
        // By sound, showing the letter would give the answer away: the voice says the sound instead.
        drawLetterBlock(canvas, targetBlock, Theme.prompt, if (bySound) "?" else track.prompt(target), Palette.WHITE)
        squareAt(speakerButton, cx + dp(12f) + side * 0.4f, promptY, side * 0.34f)
        val sink = drawBlock(canvas, speakerButton, Palette.WHITE, depth = dp(5f))
        drawEmoji(canvas, "🔊", speakerButton.centerX(), speakerButton.centerY() + sink, side * 0.36f)

        // Answer tiles: 2×2 in portrait, a single row in landscape.
        val top = contentTop + promptHeight
        val cols = if (width > height) OPTION_COUNT else 2
        val rows = OPTION_COUNT / cols
        val cellW = (contentRight - contentLeft) / cols
        val cellH = (contentBottom - top) / rows
        val half = min(cellW, cellH) * 0.4f

        options.forEachIndexed { i, letter ->
            val tileX = contentLeft + cellW * (i % cols + 0.5f)
            val tileY = top + cellH * (i / cols + 0.5f)
            squareAt(tiles[i], tileX, tileY, half)

            var scale = popIn((roundTime - i * 0.07f) / 0.4f)
            if (solvedIndex >= 0) {
                val fade = ((roundTime - solvedAt) / 0.3f).coerceIn(0f, 1f)
                scale *= if (i == solvedIndex) 1f + 0.12f * fade else 1f - fade
            }
            if (scale > 0.02f) {
                squareAt(drawRect, tileX + shakeOffset(shakeTime[i]), tileY, half * scale)
                drawLetterBlock(canvas, drawRect, Palette.TILES[i % Palette.TILES.size], letter.symbol, Palette.WHITE)
            }
        }
    }

    override fun onTap(x: Float, y: Float) {
        val target = target ?: return
        if (speakerButton.contains(x, y) || targetBlock.contains(x, y)) {
            askForTarget()
            return
        }
        if (solvedIndex >= 0) return

        val i = tiles.indexOfFirst { it.contains(x, y) }
        if (i < 0) return
        if (options[i] == target) {
            solvedIndex = i
            player.correct(track, target)
            solvedAt = roundTime
            addStar()
            celebrate(tiles[i].centerX(), tiles[i].centerY())
            speaker.say(if (bySound) lang.soundRight(target, random) else lang.found(target, random), lang.locale)
            after(2f) { newRound() }
        } else {
            player.wrong(track, target, options[i])
            Sounds.play(Sound.WRONG)
            shakeTime[i] = 0f
            speaker.say(if (bySound) lang.soundWrong(options[i], target) else lang.notThis(options[i], target), lang.locale)
        }
    }

    private companion object {
        const val OPTION_COUNT = 4
    }
}
