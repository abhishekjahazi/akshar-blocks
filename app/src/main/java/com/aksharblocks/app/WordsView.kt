package com.aksharblocks.app

import android.content.Context
import android.graphics.Canvas
import android.graphics.RectF
import kotlin.math.min

/**
 * First words: a picture (🐱) and empty slots; the child taps the letter blocks in order to
 * build the word (c-a-t, ज-ल). Each right letter jumps into its slot and is said; a finished
 * word is sounded out letter by letter, then as a whole.
 */
class WordsView(
    context: Context, speaker: Speaker, player: Player, private val track: Track,
) : GameView(context, speaker, player) {

    /** A letter block to pick from; [used] once it's been placed in a slot. */
    private class Tile(val letter: String) {
        var used = false
        var shake = 1f
    }

    override val title = track.lang.wordsTitle
    override val skyColor = Palette.ICE

    private val lang = track.lang
    private var word: Word = track.words.first()
    private var tiles: List<Tile> = emptyList()

    /** How many letters of the word are in place. */
    private var placed = 0

    /** While sounding out a finished word: the slot lit up now (or -1). */
    private var lit = -1
    private var done = false
    private var roundTime = 0f
    private var roundToken = 0

    private val picture = RectF()
    private val speakerButton = RectF()
    private val slots = List(MAX_LETTERS) { RectF() }
    private val tileRects = List(MAX_LETTERS + EXTRA_TILES) { RectF() }
    private val drawRect = RectF()

    init {
        newWord(first = true)
    }

    private fun newWord(first: Boolean = false) {
        roundToken++
        val pool = track.words.filter { canDraw(it.emoji) }.ifEmpty { track.words }
        word = if (first) pool.random(random) else Pick.anyExcept(pool, word, random)
        // The word's letters plus a couple of others to choose from.
        val alphabet = track.letters.map { tileLetter(it.symbol) }.distinct()
        // बारहखड़ी words: the wrong blocks are the same consonants with other matras (की for कि),
        // so the child has to look at the matra, not just the letter.
        val tricky = if (track == Track.BARAKHADI) {
            val groups = word.letters.mapNotNull { block -> track.letters.firstOrNull { it.symbol == block }?.group }.toSet()
            track.letters.filter { it.group in groups }.map { it.symbol }
        } else {
            emptyList()
        }
        val extras = (tricky.filter { it !in word.letters }.shuffled(random) + alphabet.filter { it !in word.letters }.shuffled(random))
            .distinct().take(EXTRA_TILES)
        tiles = (word.letters + extras).shuffled(random).map { Tile(it) }
        placed = 0
        lit = -1
        done = false
        roundTime = 0f
        ask()
    }

    /** English words are built from small letters; Hindi letters are used as they are. */
    private fun tileLetter(symbol: String) = if (track == Track.ENGLISH) symbol.lowercase() else symbol

    private fun ask() = speaker.say(lang.wordAsk(word), lang.locale)

    private fun letterName(letter: String) = lang.name(Letter(letter))

    /** The progress report tracks letters as the track shows them (capitals for English). */
    private fun reportLetter(letter: String) = Letter(if (track == Track.ENGLISH) letter.uppercase() else letter)

    override fun update(dt: Float) {
        roundTime += dt
        tiles.forEach { it.shake += dt }
    }

    override fun drawGame(canvas: Canvas) {
        val areaW = contentRight - contentLeft
        val areaH = contentBottom - contentTop
        val landscape = width > height

        // Picture, with 🔊 to hear the word again.
        val pictureSide = min(areaH * (if (landscape) 0.38f else 0.3f), areaW * 0.5f)
        picture.set(width / 2f - pictureSide / 2f, contentTop, width / 2f + pictureSide / 2f, contentTop + pictureSide)
        val appear = popIn(roundTime / 0.4f)
        canvas.save()
        canvas.scale(appear, appear, picture.centerX(), picture.centerY())
        val sink = drawBlock(canvas, picture, Palette.WHITE, radius = dp(28f), depth = dp(8f))
        drawEmoji(canvas, word.emoji, picture.centerX(), picture.centerY() + sink, pictureSide * 0.62f)
        canvas.restore()
        val speakerSize = dp(52f)
        speakerButton.set(picture.right + dp(16f), picture.centerY() - speakerSize / 2f, picture.right + dp(16f) + speakerSize, picture.centerY() + speakerSize / 2f)
        val speakerSink = drawBlock(canvas, speakerButton, Palette.WHITE, depth = dp(5f))
        drawEmoji(canvas, "🔊", speakerButton.centerX(), speakerButton.centerY() + speakerSink, speakerSize * 0.5f)

        // Slots for the word's letters.
        val n = word.letters.size
        val slotSide = min(dp(92f), (areaW - dp(12f) * (n - 1)) / n)
        val slotsTop = picture.bottom + dp(if (landscape) 16f else 28f)
        var x = width / 2f - (slotSide * n + dp(12f) * (n - 1)) / 2f
        for (i in 0 until n) {
            val slot = slots[i]
            slot.set(x, slotsTop, x + slotSide, slotsTop + slotSide)
            if (i < placed) {
                val face = if (i == lit) Palette.SUN else track.colorFor(i)
                val ink = if (i == lit) Palette.INK else Palette.WHITE
                drawLetterBlock(canvas, slot, face, word.letters[i], ink, textScale = 0.62f)
            } else {
                // An empty slot: a pale outline where the next letter goes.
                strokePaint.color = if (i == placed) Palette.OCEAN else Palette.edgeOf(Palette.WHITE)
                strokePaint.strokeWidth = dp(if (i == placed) 4f else 3f)
                canvas.drawRoundRect(slot, slotSide * 0.22f, slotSide * 0.22f, strokePaint)
            }
            x += slotSide + dp(12f)
        }

        // Letter blocks to choose from, up to five a row.
        val tilesTop = slotsTop + slotSide + dp(if (landscape) 20f else 36f)
        val perRow = min(tiles.size, 5)
        val rows = (tiles.size + perRow - 1) / perRow
        val tileSide = min(dp(84f), min((areaW - dp(14f) * (perRow - 1)) / perRow, (contentBottom - tilesTop - dp(14f) * (rows - 1)) / rows))
        tiles.forEachIndexed { i, tile ->
            val row = i / perRow
            val inRow = min(perRow, tiles.size - row * perRow)
            val col = i % perRow
            val left = width / 2f - (tileSide * inRow + dp(14f) * (inRow - 1)) / 2f + col * (tileSide + dp(14f))
            val top = tilesTop + row * (tileSide + dp(14f))
            tileRects[i].set(left, top, left + tileSide, top + tileSide)
            if (tile.used) return@forEachIndexed
            val scale = popIn((roundTime - 0.25f - i * 0.05f) / 0.35f)
            if (scale <= 0.02f) return@forEachIndexed
            squareAt(drawRect, tileRects[i].centerX() + shakeOffset(tile.shake), tileRects[i].centerY(), tileSide / 2f * scale)
            drawLetterBlock(canvas, drawRect, Palette.TILES[i % Palette.TILES.size], tile.letter, Palette.WHITE, textScale = 0.62f)
        }
    }

    override fun onTap(x: Float, y: Float) {
        if (speakerButton.contains(x, y) || picture.contains(x, y)) {
            if (!done) ask()
            return
        }
        if (done) return
        val i = tileRects.indexOfFirst { it.contains(x, y) }
        if (i < 0 || i >= tiles.size || tiles[i].used) return
        val tile = tiles[i]
        val needed = word.letters[placed]
        if (tile.letter == needed) {
            tile.used = true
            placed++
            Sounds.play(Sound.POP)
            speaker.say("${letterName(tile.letter)}!", lang.locale)
            if (placed == word.letters.size) finish()
        } else {
            tile.shake = 0f
            Sounds.play(Sound.WRONG)
            player.wrong(track, reportLetter(needed), reportLetter(tile.letter))
            speaker.say("${letterName(tile.letter)}!", lang.locale)
        }
    }

    /** Sounds the word out: each letter lights up and is said, then the whole word and praise. */
    private fun finish() {
        done = true
        val token = roundToken
        val step = SOUND_OUT_SECONDS
        word.letters.forEachIndexed { i, letter ->
            after(0.8f + i * step) {
                if (token == roundToken) {
                    lit = i
                    speaker.say("${letterName(letter)}!", lang.locale)
                }
            }
        }
        val end = 0.8f + word.letters.size * step
        after(end) {
            if (token == roundToken) {
                lit = -1
                addStar()
                word.letters.distinct().forEach { player.correct(track, reportLetter(it)) }
                celebrate(picture.centerX(), picture.centerY(), 40)
                speaker.say(lang.wordDone(word, random), lang.locale)
            }
        }
        after(end + 2.8f) { if (token == roundToken) newWord() }
    }

    private companion object {
        const val MAX_LETTERS = 6
        const val EXTRA_TILES = 2
        const val SOUND_OUT_SECONDS = 0.75f
    }
}
