package com.aksharblocks.app

import android.content.Context
import android.graphics.Canvas
import android.graphics.RectF
import kotlin.math.abs
import kotlin.math.min
import kotlin.math.sin

/**
 * First screen: a hello, today's games, then every section in three labelled groups
 * (letters; numbers and maths; things to explore), each card with a bar showing how much of
 * it the child knows.
 */
class HomeView(context: Context, speaker: Speaker, player: Player) : GameView(context, speaker, player) {

    var onPick: ((Track) -> Unit)? = null

    /** Grown-ups button (goes through the parent gate). */
    var onParent: (() -> Unit)? = null

    /** The child name tag was tapped. */
    var onChild: (() -> Unit)? = null

    /** The star counter was tapped: open the shop (which leads on to the sticker album). */
    var onAlbum: (() -> Unit)? = null

    /** The "today's games" card was tapped. */
    var onPath: (() -> Unit)? = null

    /** Rhymes was tapped. */
    var onRhymes: (() -> Unit)? = null

    /** "My name" was tapped: trace the child's own name. */
    var onName: (() -> Unit)? = null

    /** The World (colors, animals, fruits…) was tapped. */
    var onWorld: (() -> Unit)? = null

    /** Maths was tapped. */
    var onMaths: (() -> Unit)? = null

    /** The sun / moon button was tapped: switch between the light and dark look. */
    var onTheme: (() -> Unit)? = null

    /** Which of today's steps are done, as bits (see [Player.pathDone]). */
    var pathDone = 0

    override val starsTappable = true

    override val showHomeButton = false

    /** One card on Home: a track, or one of the other corners (World, Maths, rhymes, my name). */
    private class Section(
        val label: String,
        val subtitle: String,
        val color: Int,
        /** What the colored tile on the card shows: a few letters, or pictures. */
        val sample: List<String>,
        /** How much is known, 0 to 1; null for corners without progress. */
        val progress: Float?,
        val open: () -> Unit,
    )

    private class Group(val title: String, val hindi: String, val sections: List<Section>)

    private fun track(track: Track) = Section(
        track.label, track.subtitle, track.color, track.letters.take(SAMPLE_SIZE).map { it.symbol },
        player.knownShare(track),
    ) { onPick?.invoke(track) }

    private val groups = listOf(
        Group("Letters", "अक्षर", listOf(Track.ENGLISH, Track.LOWER, Track.SWAR, Track.VYANJAN, Track.BARAKHADI, Track.MARATHI).map(::track)),
        Group(
            "Numbers & Maths", "गिनती और गणित",
            listOf(track(Track.NUMBERS), track(Track.GINTI), Section(CommonWords.MATHS, "गणित", Palette.AMBER, listOf("1", "+", "2"), null) { onMaths?.invoke() }),
        ),
        Group(
            "Explore", "खोजो",
            listOf(
                Section(
                    CommonWords.WORLD, "दुनिया", Palette.JADE, listOf("🐄", "🥭"),
                    Track.entries.filter { it.isPictures }.map { player.knownShare(it) }.average().toFloat(),
                ) { onWorld?.invoke() },
                Section("Rhymes", "कविताएँ", Palette.RUST, listOf("🎵", "⭐"), null) { onRhymes?.invoke() },
                Section("My name", "मेरा नाम", Palette.ROYAL, NameLetters.sample(player.profile.name, SAMPLE_SIZE), null) { onName?.invoke() },
            ),
        ),
    )
    private val sections = groups.flatMap { it.sections }
    private val cards = sections.map { RectF() }
    private val wearing = player.wearing

    private val hero = RectF()
    private val playCircle = RectF()
    private val tile = RectF()
    private val bar = RectF()
    private val parentButton = RectF()
    private val themeButton = RectF()
    private val childTag = RectF()

    override fun drawGame(canvas: Canvas) {
        drawTopRow(canvas)
        val landscape = width > height * 1.1f
        val left = contentLeft
        val right = contentRight
        var y = safeTopInset + dp(76f)

        // Hello, and what to do (portrait only: landscape needs the room for cards).
        if (!landscape) {
            val appear = popIn((time - 0.1f) / 0.4f)
            if (appear > 0.01f) {
                canvas.save()
                canvas.scale(appear, appear, left, y + dp(24f))
                drawTextLeft(canvas, CommonWords.hi(player.profile.name), left + dp(4f), y + dp(14f), dp(26f), Palette.INK, right - left)
                drawTextLeft(canvas, CommonWords.WHAT_TODAY, left + dp(4f), y + dp(40f), dp(15f), Theme.subtext, right - left)
                canvas.restore()
            }
            y += dp(58f)
        }

        val heroH = if (landscape) dp(64f) else dp(92f)
        hero.set(left, y, right, y + heroH)
        drawHero(canvas)
        y = hero.bottom + dp(if (landscape) 10f else 14f)

        if (landscape) layoutLandscape(y) else layoutPortrait(y)
        drawGroups(canvas)
    }

    /** Three groups one under another, three cards to a row. */
    private fun layoutPortrait(top: Float) {
        val gap = dp(9f)
        val header = dp(26f)
        val groupGap = dp(8f)
        val rowsTotal = groups.sumOf { (it.sections.size + 2) / 3 }
        val available = contentBottom - top - header * groups.size - groupGap * (groups.size - 1) - gap * (rowsTotal - groups.size)
        val cellW = (contentRight - contentLeft - gap * 2) / 3f
        val rowH = min(available / rowsTotal, cellW * 1.1f)
        var y = top
        var index = 0
        for (group in groups) {
            groupTitles[group] = contentLeft to y + header / 2f
            y += header
            group.sections.forEachIndexed { i, _ ->
                val x = contentLeft + (cellW + gap) * (i % 3)
                val rowTop = y + (rowH + gap) * (i / 3)
                cards[index++].set(x, rowTop, x + cellW, rowTop + rowH)
            }
            y += (rowH + gap) * ((group.sections.size + 2) / 3) - gap + groupGap
        }
    }

    /** Letters in one row of six; numbers and maths beside the things to explore below it. */
    private fun layoutLandscape(top: Float) {
        val gap = dp(9f)
        val header = dp(24f)
        val cellW = (contentRight - contentLeft - gap * 5) / 6f
        val rowH = min((contentBottom - top - header * 2 - dp(8f)) / 2f, cellW * 0.9f)
        var index = 0
        groupTitles[groups[0]] = contentLeft to top + header / 2f
        var y = top + header
        groups[0].sections.forEachIndexed { i, _ ->
            val x = contentLeft + (cellW + gap) * i
            cards[index++].set(x, y, x + cellW, y + rowH)
        }
        y += rowH + dp(8f)
        for ((g, group) in groups.drop(1).withIndex()) {
            val groupLeft = contentLeft + (cellW + gap) * 3 * g
            groupTitles[group] = groupLeft to y + header / 2f
            group.sections.forEachIndexed { i, _ ->
                val x = groupLeft + (cellW + gap) * i
                cards[index++].set(x, y + header, x + cellW, y + header + rowH)
            }
        }
    }

    /** Where each group's heading goes: its left edge and middle line. */
    private val groupTitles = HashMap<Group, Pair<Float, Float>>()

    private fun drawGroups(canvas: Canvas) {
        var index = 0
        groups.forEachIndexed { g, group ->
            val (x, cy) = groupTitles[group] ?: return@forEachIndexed
            val appear = popIn((time - 0.25f - g * 0.08f) / 0.4f)
            if (appear > 0.01f) {
                textPaint.textSize = dp(17f)
                val titleWidth = textPaint.measureText(group.title)
                drawTextLeft(canvas, group.title, x + dp(4f), cy, dp(17f), Palette.INK, dp(220f))
                drawTextLeft(canvas, group.hindi, x + dp(12f) + titleWidth, cy, dp(14f), Theme.subtext, dp(160f))
            }
            for (section in group.sections) {
                drawCard(canvas, section, cards[index], index)
                index++
            }
        }
    }

    /** A glass card: a colored tile with letters or pictures, the name, a second line and a progress bar. */
    private fun drawCard(canvas: Canvas, section: Section, card: RectF, i: Int) {
        val appear = popIn((time - 0.3f - i * 0.04f) / 0.4f)
        if (appear <= 0.01f) return
        canvas.save()
        canvas.scale(appear, appear, card.centerX(), card.centerY())
        val sink = drawBlock(canvas, card, Palette.WHITE, radius = dp(22f), depth = dp(6f))
        val h = card.height()
        val w = card.width()

        val tileH = min(h * 0.34f, dp(44f))
        val tileW = min(w * 0.58f, tileH * 1.45f)
        tile.set(card.centerX() - tileW / 2f, card.top + h * 0.1f + sink, card.centerX() + tileW / 2f, card.top + h * 0.1f + tileH + sink)
        drawBlock(canvas, tile, section.color, radius = tileH * 0.34f, depth = dp(3f), pressable = false)
        val pictures = section.sample.all { Art.has(it) }
        if (pictures) {
            val n = section.sample.size
            val size = min(tileH * 0.62f, tileW / n * 0.8f)
            section.sample.forEachIndexed { j, picture ->
                drawEmoji(canvas, picture, tile.left + tileW * (j + 0.5f) / n, tile.centerY(), size)
            }
        } else {
            drawText(canvas, section.sample.joinToString(""), tile.centerX(), tile.centerY(), tileH * 0.5f, Palette.WHITE, tileW * 0.86f)
        }

        drawText(canvas, section.label, card.centerX(), card.top + h * 0.6f + sink, min(h * 0.16f, dp(16f)), Palette.INK, w * 0.9f)
        drawText(canvas, section.subtitle, card.centerX(), card.top + h * 0.76f + sink, min(h * 0.12f, dp(12.5f)), Theme.subtext, w * 0.9f)

        val progress = section.progress
        if (progress != null) {
            val barW = w * 0.62f
            val barH = dp(5f)
            val barY = card.top + h * 0.9f + sink
            bar.set(card.centerX() - barW / 2f, barY - barH / 2f, card.centerX() + barW / 2f, barY + barH / 2f)
            fillPaint.color = Theme.track
            canvas.drawRoundRect(bar, barH / 2f, barH / 2f, fillPaint)
            if (progress > 0f) {
                bar.right = bar.left + barW * progress.coerceIn(0.06f, 1f)
                fillPaint.color = section.color
                canvas.drawRoundRect(bar, barH / 2f, barH / 2f, fillPaint)
            }
        }
        canvas.restore()
    }

    /** The big blue-to-purple card: today's games, how many are done, and a bobbing play button. */
    private fun drawHero(canvas: Canvas) {
        val allDone = Integer.bitCount(pathDone) >= DailyPath.STEPS
        val appear = popIn((time - 0.18f) / 0.4f)
        if (appear <= 0.01f) return
        canvas.save()
        canvas.scale(appear, appear, hero.centerX(), hero.centerY())
        val from = if (allDone) Palette.GRASS else Theme.heroFrom
        val to = if (allDone) Palette.TEAL else Theme.heroTo
        val sink = drawGradientBlock(canvas, hero, from, to, radius = dp(28f), depth = dp(8f))
        val h = hero.height()

        // The play button (a trophy once all are done) on the right, bobbing and glowing to say "start here".
        val circle = h * 0.68f
        val bob = if (!allDone && motionEnabled) abs(sin(time * 2.6f)) * dp(4f) else 0f
        val cx = hero.right - h * 0.2f - circle / 2f
        val cy = hero.centerY() + sink - bob
        if (!allDone && motionEnabled) {
            val pulse = (time * 0.6f) % 1f
            fillPaint.color = ((((1f - pulse) * 0x8C).toInt()) shl 24) or 0xFFC21A
            canvas.drawCircle(cx, cy, circle / 2f + pulse * dp(12f), fillPaint)
        }
        playCircle.set(cx - circle / 2f, cy - circle / 2f, cx + circle / 2f, cy + circle / 2f)
        drawBlock(canvas, playCircle, if (allDone) Palette.WHITE else Palette.SUN, radius = circle / 2f, depth = dp(4f), pressable = false)
        if (allDone) {
            drawEmoji(canvas, "🏆", cx, cy, circle * 0.6f)
        } else {
            val s = circle * 0.2f
            path.reset()
            path.moveTo(cx + s * 1.15f, cy)
            path.lineTo(cx - s * 0.75f, cy - s * 1.05f)
            path.lineTo(cx - s * 0.75f, cy + s * 1.05f)
            path.close()
            fillPaint.color = Palette.NAVY
            canvas.drawPath(path, fillPaint)
        }

        // "Today's games", how many are done, and a bar per step.
        val textLeft = hero.left + dp(20f)
        val textWidth = playCircle.left - dp(12f) - textLeft
        val done = Integer.bitCount(pathDone)
        val small = h < dp(80f)
        drawTextLeft(canvas, if (allDone) CommonWords.ALL_DONE_TODAY else CommonWords.TODAYS_GAMES, textLeft, hero.top + h * (if (small) 0.36f else 0.3f) + sink, min(h * 0.27f, dp(24f)), Palette.WHITE, textWidth)
        if (!small) {
            drawTextLeft(canvas, CommonWords.stepsDone(done, DailyPath.STEPS), textLeft, hero.top + h * 0.55f + sink, dp(14f), 0xEBFFFFFF.toInt(), textWidth)
        }
        val segW = min(dp(38f), (textWidth - dp(6f) * (DailyPath.STEPS - 1)) / DailyPath.STEPS)
        val segH = dp(7f)
        val segY = hero.top + h * (if (small) 0.74f else 0.78f) + sink
        for (i in 0 until DailyPath.STEPS) {
            val x = textLeft + (segW + dp(6f)) * i
            bar.set(x, segY - segH / 2f, x + segW, segY + segH / 2f)
            fillPaint.color = if (pathDone and (1 shl i) != 0) Palette.WHITE else 0x66FFFFFF
            canvas.drawRoundRect(bar, segH / 2f, segH / 2f, fillPaint)
        }
        canvas.restore()
    }

    /** Grown-ups lock, the light / dark switch, then the playing child's animal and name. */
    private fun drawTopRow(canvas: Canvas) {
        val size = dp(52f)
        val barY = topBarCenter
        parentButton.set(contentLeft, barY - size / 2f, contentLeft + size, barY + size / 2f)
        val lockSink = drawBlock(canvas, parentButton, Palette.WHITE, radius = dp(18f), depth = dp(5f))
        drawEmoji(canvas, "🔒", parentButton.centerX(), parentButton.centerY() + lockSink, size * 0.42f)

        themeButton.set(parentButton.right + dp(8f), barY - size / 2f, parentButton.right + dp(8f) + size, barY + size / 2f)
        val themeSink = drawBlock(canvas, themeButton, Palette.WHITE, radius = dp(18f), depth = dp(5f))
        drawEmoji(canvas, if (Theme.dark) "☀️" else "🌙", themeButton.centerX(), themeButton.centerY() + themeSink, size * 0.44f)

        // The child's animal, dressed in what they bought in the shop, then their name.
        val name = player.profile.name
        val face = dp(32f)
        textPaint.textSize = dp(20f)
        val maxWidth = (starsRect.left.takeIf { it > 0f } ?: (width * 0.7f)) - themeButton.right - dp(16f)
        val tagWidth = min(textPaint.measureText(name) + face + dp(40f), maxWidth)
        childTag.set(themeButton.right + dp(8f), barY - size / 2f, themeButton.right + dp(8f) + tagWidth, barY + size / 2f)
        val tagSink = drawBlock(canvas, childTag, Palette.WHITE, radius = size / 2f, depth = dp(5f))
        val faceX = childTag.left + dp(12f) + face / 2f
        drawAvatar(canvas, player.profile.avatar, wearing, faceX, childTag.centerY() + tagSink, face)
        val textLeft = faceX + face / 2f + dp(6f)
        drawTextLeft(canvas, name, textLeft, childTag.centerY() + tagSink, dp(20f), Palette.INK, childTag.right - dp(14f) - textLeft)
    }

    override fun onTap(x: Float, y: Float) {
        when {
            parentButton.contains(x, y) -> onParent?.invoke()
            themeButton.contains(x, y) -> {
                Sounds.play(Sound.TAP)
                onTheme?.invoke()
            }
            childTag.contains(x, y) -> onChild?.invoke()
            starsRect.contains(x, y) -> onAlbum?.invoke()
            hero.contains(x, y) -> {
                Sounds.play(Sound.TAP)
                onPath?.invoke()
            }
            else -> {
                val index = cards.indexOfFirst { it.contains(x, y) }
                if (index >= 0) {
                    Sounds.play(Sound.TAP)
                    sections[index].open()
                }
            }
        }
    }

    private companion object {
        const val SAMPLE_SIZE = 3
    }
}
