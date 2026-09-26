package com.aksharblocks.app

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Typeface
import android.os.SystemClock
import android.view.MotionEvent
import android.view.View
import android.os.Build
import android.provider.Settings
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.min
import kotlin.math.sin
import kotlin.random.Random

/** The game's colors. Everything is drawn as flat toy blocks in these. */
object Palette {
    const val INK = 0xFF1D2B53.toInt()
    const val WHITE = 0xFFFFFFFF.toInt()
    const val TOMATO = 0xFFFF5A4E.toInt()
    const val SUN = 0xFFFFC53D.toInt()
    const val GRASS = 0xFF22B866.toInt()
    const val OCEAN = 0xFF2F7BFF.toInt()
    const val GRAPE = 0xFF9B5CFF.toInt()
    const val ORANGE = 0xFFFF8A1F.toInt()
    const val PINK = 0xFFFF4FA3.toInt()
    const val TEAL = 0xFF12B5A6.toInt()

    // Light screen backgrounds, one per game so each has its own feel.
    const val SKY = 0xFFCFE8FF.toInt()
    const val MINT = 0xFFD2F4E1.toInt()
    const val LILAC = 0xFFE6DCFF.toInt()
    const val ICE = 0xFFDCE8FF.toInt()
    const val PEACH = 0xFFFFE9D9.toInt()

    /** Colors for answer tiles, by position (never by letter, so color can't give the answer away). */
    val TILES = intArrayOf(OCEAN, GRASS, GRAPE, TOMATO)

    val CONFETTI = intArrayOf(TOMATO, SUN, GRASS, OCEAN, GRAPE, PINK)

    /** The darker "side" of a block. */
    fun edgeOf(face: Int): Int {
        if (face == WHITE) return 0xFFB5C4E3.toInt()
        return Color.rgb(
            (Color.red(face) * 0.72f).toInt(),
            (Color.green(face) * 0.72f).toInt(),
            (Color.blue(face) * 0.72f).toInt(),
        )
    }
}

private val TITLE_TILT = floatArrayOf(-6f, 4f, -3f, 5f)
private const val REVEAL_SECONDS = 4.5f

/** Artwork is drawn this much larger than the font size it replaces, to match emoji glyphs. */
private const val ART_SCALE = 1.1f

/** Widest the game content gets (dp); wider screens center it. */
private const val MAX_CONTENT_DP = 960f

/**
 * Base class for every screen. Runs the animation loop and draws the
 * background, the top bar (home button, title, stars) and confetti. Touches
 * become [onPress] (finger down), [onTap] (finger up without moving) or
 * [onSwipe]; while a finger is down, any block under it is drawn pressed in.
 */
abstract class GameView(
    context: Context,
    protected val speaker: Speaker,
    /** The child playing now: their stars and results. */
    protected val player: Player,
) : View(context) {

    var onHome: (() -> Unit)? = null

    protected val random = Random.Default
    protected val density = resources.displayMetrics.density
    protected fun dp(value: Float) = value * density

    protected open val title: String = ""
    protected open val showHomeButton = true
    protected open val showStars = true

    /** Home makes the star counter a button that opens the sticker album. */
    protected open val starsTappable = false
    protected open val skyColor = Palette.SKY

    /** Seconds since this screen appeared. */
    protected var time = 0f
        private set

    /** False when the phone's "remove animations" setting is on. */
    protected val motionEnabled =
        if (Build.VERSION.SDK_INT >= 26) ValueAnimator.areAnimatorsEnabled()
        else Settings.Global.getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) != 0f

    // Space taken by notches, camera holes and any visible system bars.
    private var safeLeft = 0f
    private var safeTop = 0f
    private var safeRight = 0f
    private var safeBottom = 0f

    /** The area below the top bar that screens lay their content out in. */
    protected val contentTop get() = safeTop + dp(88f)
    protected val contentBottom get() = height - safeBottom - dp(20f)
    // On tablets the content stays a comfortable width, centered, instead of stretching edge to edge.
    private val sideSpace get() = maxOf(dp(16f), (width - safeLeft - safeRight - dp(MAX_CONTENT_DP)) / 2f)
    protected val contentLeft get() = safeLeft + sideSpace
    protected val contentRight get() = width - safeRight - sideSpace
    protected val safeTopInset get() = safeTop

    /** Vertical center of the top bar (home button, title, stars). */
    protected val topBarCenter get() = safeTop + dp(42f)

    protected val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textAlign = Paint.Align.CENTER
        typeface = if (Build.VERSION.SDK_INT >= 28) Typeface.create(Typeface.DEFAULT, 900, false)
        else Typeface.create("sans-serif-black", Typeface.NORMAL)
    }
    protected val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    protected val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
    }
    protected val path = Path()

    private val blockRect = RectF()
    private val homeRect = RectF()
    protected val starsRect = RectF()
    private val artRect = RectF()
    private val artPaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
    private val revealCard = RectF()

    /** A sticker just earned, shown big over the game until tapped or timed out. */
    private var reveal: Sticker? = null
    private var revealAt = 0f

    /** True while the "New sticker!" card is on screen; games should ignore touches then. */
    protected val showingReveal get() = reveal != null
    private val titleRect = RectF()

    private class Particle(
        var x: Float, var y: Float, var vx: Float, var vy: Float,
        val color: Int, val size: Float, var rotation: Float, val spin: Float, var life: Float,
    )

    private class Timer(var remaining: Float, val action: () -> Unit)

    private val particles = ArrayList<Particle>()
    private val timers = ArrayList<Timer>()
    private var lastFrameMs = 0L

    private var downX = 0f
    private var downY = 0f
    private var touching = false
    private var touchX = 0f
    private var touchY = 0f
    private var homePressed = false

    // --- Hooks for subclasses -------------------------------------------------

    protected open fun update(dt: Float) {}
    protected abstract fun drawGame(canvas: Canvas)
    protected open fun onPress(x: Float, y: Float) {}
    protected open fun onTap(x: Float, y: Float) {}

    /** [towardsLeft] is true when the finger moved right-to-left (i.e. "next"). */
    protected open fun onSwipe(towardsLeft: Boolean) {}

    // --- Helpers for subclasses -----------------------------------------------

    /** Runs [action] after [seconds] of game time. */
    protected fun after(seconds: Float, action: () -> Unit) {
        timers += Timer(seconds, action)
    }

    /** Gives the child a star; the total is kept between visits. */
    protected fun addStar() {
        val unlockedBefore = Stickers.unlocked(player.stars)
        player.addStar()
        Sounds.play(Sound.RIGHT)
        val unlockedNow = Stickers.unlocked(player.stars)
        if (unlockedNow > unlockedBefore) {
            val sticker = Stickers.all[unlockedNow - 1]
            reveal = sticker
            revealAt = time
            after(0.5f) { if (reveal == sticker) Sounds.play(Sound.FANFARE) }
            // Let the game's own praise finish first.
            after(1.6f) { if (reveal == sticker) speaker.say(CommonWords.newSticker(sticker)) }
        }
    }

    /** Bursts confetti out from ([x], [y]). */
    protected fun celebrate(x: Float, y: Float, count: Int = 36) {
        repeat(count) {
            val angle = random.nextFloat() * 2f * Math.PI.toFloat()
            val speed = dp(150f + random.nextFloat() * 350f)
            particles += Particle(
                x = x, y = y,
                vx = cos(angle) * speed,
                vy = sin(angle) * speed - dp(250f),
                color = Palette.CONFETTI[random.nextInt(Palette.CONFETTI.size)],
                size = dp(6f + random.nextFloat() * 6f),
                rotation = random.nextFloat() * 360f,
                spin = (random.nextFloat() - 0.5f) * 720f,
                life = 1.2f + random.nextFloat() * 0.6f,
            )
        }
    }

    protected fun distance(x1: Float, y1: Float, x2: Float, y2: Float) = hypot(x1 - x2, y1 - y2)

    /** Side-to-side wobble for a wrong answer; [t] is seconds since it started. */
    protected fun shakeOffset(t: Float): Float =
        if (t >= 0.5f) 0f else sin(t * 50f) * dp(10f) * (1f - t / 0.5f)

    /** 0 → 1 with a small overshoot ("pop in"); [t] is 0..1 progress. */
    protected fun popIn(t: Float): Float {
        if (!motionEnabled) return 1f
        val x = t.coerceIn(0f, 1f) - 1f
        return 1f + 2.70158f * x * x * x + 1.70158f * x * x
    }

    /** True while a finger is held down inside [rect]. */
    protected fun isPressed(rect: RectF) = touching && rect.contains(touchX, touchY)

    /**
     * Draws a toy block whose top face is [rect], with its darker side showing
     * below. Pressed blocks sink down onto their side. Returns how far the face
     * sank, so callers can move what they draw on top by the same amount.
     */
    protected fun drawBlock(
        canvas: Canvas, rect: RectF, face: Int,
        radius: Float = min(rect.width(), rect.height()) * 0.24f,
        depth: Float = dp(7f),
        pressable: Boolean = true,
    ): Float {
        val sink = if (pressable && isPressed(rect)) depth * 0.75f else 0f
        fillPaint.color = Palette.edgeOf(face)
        blockRect.set(rect)
        blockRect.offset(0f, depth)
        canvas.drawRoundRect(blockRect, radius, radius, fillPaint)
        fillPaint.color = face
        blockRect.set(rect)
        blockRect.offset(0f, sink)
        canvas.drawRoundRect(blockRect, radius, radius, fillPaint)
        return sink
    }

    /** A block with one letter (or short text) centered on it. */
    protected fun drawLetterBlock(
        canvas: Canvas, rect: RectF, face: Int, text: String, textColor: Int,
        textScale: Float = 0.66f, depth: Float = dp(7f),
    ) {
        val sink = drawBlock(canvas, rect, face, depth = depth)
        drawText(canvas, text, rect.centerX(), rect.centerY() + sink, rect.height() * textScale, textColor, rect.width() * 0.86f)
    }

    /** A block with a triangle pointing left or right. */
    protected fun drawArrowBlock(canvas: Canvas, rect: RectF, face: Int, pointsRight: Boolean) {
        val sink = drawBlock(canvas, rect, face)
        val cx = rect.centerX()
        val cy = rect.centerY() + sink
        val s = min(rect.width(), rect.height()) * 0.22f
        val d = if (pointsRight) 1f else -1f
        path.reset()
        path.moveTo(cx + d * s, cy)
        path.lineTo(cx - d * s * 0.7f, cy - s)
        path.lineTo(cx - d * s * 0.7f, cy + s)
        path.close()
        fillPaint.color = Palette.INK
        canvas.drawPath(path, fillPaint)
    }

    /** Draws [text] centered on ([cx], [cy]), shrinking it to fit [maxWidth]. */
    protected fun drawText(
        canvas: Canvas, text: String, cx: Float, cy: Float, size: Float, color: Int,
        maxWidth: Float = Float.MAX_VALUE,
    ) {
        textPaint.textSize = size
        textPaint.color = color
        val measured = textPaint.measureText(text)
        if (measured > maxWidth) textPaint.textSize = size * maxWidth / measured
        val metrics = textPaint.fontMetrics
        canvas.drawText(text, cx, cy - (metrics.ascent + metrics.descent) / 2f, textPaint)
    }

    /** Draws a picture: the bundled artwork when there is one, otherwise the phone's emoji font. */
    protected fun drawEmoji(canvas: Canvas, emoji: String, cx: Float, cy: Float, size: Float) {
        val side = size * ART_SCALE
        val bitmap = Art.bitmap(emoji, side)
        if (bitmap == null) {
            drawText(canvas, emoji, cx, cy, size, Color.BLACK)
            return
        }
        artRect.set(cx - side / 2f, cy - side / 2f, cx + side / 2f, cy + side / 2f)
        canvas.drawBitmap(bitmap, null, artRect, artPaint)
    }

    private val glyphCache = HashMap<String, Boolean>()

    /** False for emoji this phone's font can't draw (older Android versions lack newer emoji). */
    protected fun canDraw(emoji: String?): Boolean =
        emoji != null && glyphCache.getOrPut(emoji) { Art.has(emoji) || textPaint.hasGlyph(emoji) }

    /** Draws [word] centered, with its first [headLength] characters in [highlight]. */
    protected fun drawWord(
        canvas: Canvas, word: String, headLength: Int, highlight: Int,
        cx: Float, cy: Float, size: Float, maxWidth: Float,
    ) {
        textPaint.textSize = size
        var total = textPaint.measureText(word)
        if (total > maxWidth) {
            textPaint.textSize = size * maxWidth / total
            total = maxWidth
        }
        val first = word.substring(0, headLength.coerceIn(1, word.length))
        val metrics = textPaint.fontMetrics
        val baseline = cy - (metrics.ascent + metrics.descent) / 2f
        val left = cx - total / 2f
        textPaint.textAlign = Paint.Align.LEFT
        textPaint.color = highlight
        canvas.drawText(first, left, baseline, textPaint)
        textPaint.color = Palette.INK
        canvas.drawText(word.substring(first.length), left + textPaint.measureText(first), baseline, textPaint)
        textPaint.textAlign = Paint.Align.CENTER
    }

    /**
     * A row of tilted letter blocks that drop in one after another when the screen opens.
     * [rects] receives each block's resting position, for tapping.
     */
    protected fun drawTitleBlocks(
        canvas: Canvas, symbols: List<String>, colors: List<Int>, top: Float, bottom: Float, rects: List<RectF>,
    ) {
        val count = symbols.size
        val gap = dp(10f)
        val maxSide = (width * 0.86f - gap * (count - 1)) / count
        val side = minOf(maxSide, (bottom - top) * 0.8f, dp(130f))
        var left = width / 2f - (side * count + gap * (count - 1)) / 2f
        val cy = (top + bottom) / 2f

        symbols.forEachIndexed { i, symbol ->
            val drop = popIn((time - i * 0.1f) / 0.45f)
            rects[i].set(left, cy - side / 2f, left + side, cy + side / 2f)
            titleRect.set(rects[i])
            titleRect.offset(0f, -(1f - drop) * dp(220f))
            canvas.save()
            canvas.rotate(TITLE_TILT[i % TITLE_TILT.size], titleRect.centerX(), titleRect.centerY())
            drawLetterBlock(canvas, titleRect, colors[i % colors.size], symbol, Palette.WHITE, depth = dp(10f))
            canvas.restore()
            left += side + gap
        }
    }

    /**
     * [count] copies of [emoji] in rows of five (a "ten frame", as counting is taught), centered
     * in [area]. The first [counted] get a number badge from [badge], the latest one bigger.
     */
    protected fun drawCountedObjects(
        canvas: Canvas, area: RectF, count: Int, emoji: String,
        counted: Int = 0, badge: (Int) -> String = { "${it + 1}" },
    ) {
        if (count <= 0) return
        val cols = min(count, 5)
        val rows = (count + 4) / 5
        val cell = min(area.width() / 5f, area.height() / 2f)
        val left = area.centerX() - cols * cell / 2f
        val top = area.centerY() - rows * cell / 2f
        for (i in 0 until count) {
            val cx = left + cell * (i % 5 + 0.5f)
            val cy = top + cell * (i / 5 + 0.5f)
            val latest = i == counted - 1
            drawEmoji(canvas, emoji, cx, cy, cell * if (latest) 0.78f else 0.62f)
            if (i < counted) {
                val r = cell * 0.17f
                val by = cy + cell * 0.36f
                fillPaint.color = Palette.INK
                canvas.drawCircle(cx, by, r, fillPaint)
                drawText(canvas, badge(i), cx, by, r * 1.3f, Palette.WHITE, r * 1.8f)
            }
        }
    }

    /** Sets [out] to a square of half-size [half] centered on ([cx], [cy]). */
    protected fun squareAt(out: RectF, cx: Float, cy: Float, half: Float): RectF {
        out.set(cx - half, cy - half, cx + half, cy + half)
        return out
    }

    // --- View plumbing ----------------------------------------------------------

    init {
        // Keep content clear of notches, camera holes and any visible system bars (all Android versions).
        ViewCompat.setOnApplyWindowInsetsListener(this) { _, insets ->
            val safe = insets.getInsets(WindowInsetsCompat.Type.displayCutout() or WindowInsetsCompat.Type.systemBars())
            safeLeft = safe.left.toFloat()
            safeTop = safe.top.toFloat()
            safeRight = safe.right.toFloat()
            safeBottom = safe.bottom.toFloat()
            insets
        }
    }

    override fun onDraw(canvas: Canvas) {
        val now = SystemClock.uptimeMillis()
        // Clamp so a pause (or the first frame) doesn't make things jump.
        val dt = if (lastFrameMs == 0L) 0f else ((now - lastFrameMs) / 1000f).coerceIn(0f, 0.05f)
        lastFrameMs = now
        time += dt

        runTimers(dt)
        update(dt)
        updateParticles(dt)

        canvas.drawColor(skyColor)
        drawGame(canvas)
        drawParticles(canvas)
        drawTopBar(canvas)
        drawReveal(canvas)

        postInvalidateOnAnimation()
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        lastFrameMs = 0L
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (reveal != null) {
            // Any tap closes the sticker card; nothing underneath reacts.
            if (event.actionMasked == MotionEvent.ACTION_UP && time - revealAt > 0.6f) reveal = null
            return true
        }
        touchX = event.x
        touchY = event.y
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                touching = true
                downX = event.x
                downY = event.y
                homePressed = showHomeButton && homeRect.contains(event.x, event.y)
                if (!homePressed) onPress(event.x, event.y)
            }
            MotionEvent.ACTION_UP -> {
                touching = false
                val dx = event.x - downX
                val dy = event.y - downY
                if (homePressed) {
                    if (homeRect.contains(event.x, event.y)) {
                        performClick()
                        onHome?.invoke()
                    }
                } else if (abs(dx) > dp(60f) && abs(dx) > abs(dy)) {
                    onSwipe(towardsLeft = dx < 0f)
                } else {
                    performClick()
                    onTap(downX, downY)
                }
                homePressed = false
            }
            MotionEvent.ACTION_CANCEL -> {
                touching = false
                homePressed = false
            }
        }
        return true
    }

    override fun performClick(): Boolean {
        super.performClick()
        return true
    }

    private fun runTimers(dt: Float) {
        if (timers.isEmpty()) return
        val due = ArrayList<Timer>()
        val iterator = timers.iterator()
        while (iterator.hasNext()) {
            val timer = iterator.next()
            timer.remaining -= dt
            if (timer.remaining <= 0f) {
                iterator.remove()
                due += timer
            }
        }
        due.forEach { it.action() }
    }

    private fun updateParticles(dt: Float) {
        val gravity = dp(900f)
        val iterator = particles.iterator()
        while (iterator.hasNext()) {
            val p = iterator.next()
            p.vy += gravity * dt
            p.x += p.vx * dt
            p.y += p.vy * dt
            p.rotation += p.spin * dt
            p.life -= dt
            if (p.life <= 0f) iterator.remove()
        }
    }

    private fun drawParticles(canvas: Canvas) {
        for (p in particles) {
            fillPaint.color = p.color
            fillPaint.alpha = (min(1f, p.life / 0.4f) * 255).toInt()
            canvas.save()
            canvas.translate(p.x, p.y)
            canvas.rotate(p.rotation)
            canvas.drawRect(-p.size / 2f, -p.size / 3f, p.size / 2f, p.size / 3f, fillPaint)
            canvas.restore()
        }
        fillPaint.alpha = 255
    }

    /** The "New sticker!" card: the game dims, the sticker pops up big. Closes itself after a while. */
    private fun drawReveal(canvas: Canvas) {
        val sticker = reveal ?: return
        val age = time - revealAt
        if (age > REVEAL_SECONDS) {
            reveal = null
            return
        }
        fillPaint.color = 0x99000000.toInt()
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), fillPaint)

        val side = min(width, height) * 0.72f
        revealCard.set(width / 2f - side / 2f, height / 2f - side * 0.6f, width / 2f + side / 2f, height / 2f + side * 0.6f)
        val scale = popIn(age / 0.5f)
        canvas.save()
        canvas.scale(scale, scale, revealCard.centerX(), revealCard.centerY())
        drawBlock(canvas, revealCard, Palette.WHITE, radius = dp(36f), depth = dp(10f), pressable = false)
        drawText(canvas, "New sticker!", revealCard.centerX(), revealCard.top + revealCard.height() * 0.16f, side * 0.1f, Palette.INK, side * 0.9f)
        drawEmoji(canvas, sticker.emoji, revealCard.centerX(), revealCard.centerY() + side * 0.02f, side * 0.5f)
        drawText(canvas, sticker.name, revealCard.centerX(), revealCard.bottom - revealCard.height() * 0.14f, side * 0.09f, Palette.GRAPE, side * 0.9f)
        canvas.restore()
    }

    private fun drawTopBar(canvas: Canvas) {
        val barY = topBarCenter
        val size = dp(52f)
        if (showHomeButton) {
            homeRect.set(safeLeft + dp(16f), barY - size / 2f, safeLeft + dp(16f) + size, barY + size / 2f)
            val sink = drawBlock(canvas, homeRect, Palette.WHITE, depth = dp(5f))
            drawEmoji(canvas, "🏠", homeRect.centerX(), homeRect.centerY() + sink, size * 0.55f)
        }
        if (title.isNotEmpty()) {
            val maxWidth = width - safeLeft - safeRight - dp(250f)
            drawText(canvas, title, width / 2f, barY, dp(24f), Palette.INK, maxWidth)
        }
        if (showStars) {
            val count = player.stars.toString()
            textPaint.textSize = dp(24f)
            val star = dp(26f)
            val pillWidth = star + dp(8f) + textPaint.measureText(count) + dp(28f)
            val right = width - safeRight - dp(16f)
            starsRect.set(right - pillWidth, barY - size / 2f, right, barY + size / 2f)
            val sink = drawBlock(canvas, starsRect, Palette.WHITE, depth = dp(5f), pressable = starsTappable)
            val starX = starsRect.left + dp(14f) + star / 2f
            drawEmoji(canvas, "⭐", starX, barY + sink, star / ART_SCALE)
            textPaint.textAlign = Paint.Align.LEFT
            textPaint.color = Palette.INK
            val metrics = textPaint.fontMetrics
            canvas.drawText(count, starX + star / 2f + dp(8f), barY + sink - (metrics.ascent + metrics.descent) / 2f, textPaint)
            textPaint.textAlign = Paint.Align.CENTER
        }
    }
}
