package com.aksharblocks.app

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Matrix
import android.graphics.RadialGradient
import android.graphics.Shader
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

/** The game's colors: bright blocks on soft glass. Text and glass colors follow the light or dark [Theme]. */
object Palette {
    /** Text and lines on glass: dark navy in the light look, near-white in the dark one. */
    val INK: Int get() = Theme.text

    /** Dark navy that never changes: on yellow, and in pictures and printouts that leave the app. */
    const val NAVY = 0xFF1D2B53.toInt()
    const val WHITE = 0xFFFFFFFF.toInt()
    const val TOMATO = 0xFFFF5A4E.toInt()
    const val SUN = 0xFFFFC53D.toInt()
    const val GRASS = 0xFF22B866.toInt()
    const val OCEAN = 0xFF2F7BFF.toInt()
    const val GRAPE = 0xFF9B5CFF.toInt()
    const val ORANGE = 0xFFFF8A1F.toInt()
    const val PINK = 0xFFFF4FA3.toInt()
    const val TEAL = 0xFF12B5A6.toInt()
    const val INDIGO = 0xFF6A4BE8.toInt()
    const val CYAN = 0xFF1098AD.toInt()
    const val OLIVE = 0xFF5C940D.toInt()
    const val EARTH = 0xFFB0602A.toInt()
    const val ROYAL = 0xFF364FC7.toInt()
    const val RUST = 0xFFD9480F.toInt()
    const val LEAF = 0xFF2B8A3E.toInt()
    const val ROSE = 0xFFD6336C.toInt()
    const val PLUM = 0xFF862E9C.toInt()
    const val JADE = 0xFF0C8599.toInt()
    const val AMBER = 0xFFE67700.toInt()

    // Each screen tints the background with one of these (light look), so each has its own feel.
    const val SKY = 0xFFCFE8FF.toInt()
    const val MINT = 0xFFD2F4E1.toInt()
    const val LILAC = 0xFFE6DCFF.toInt()
    const val ICE = 0xFFDCE8FF.toInt()
    const val PEACH = 0xFFFFE9D9.toInt()

    /** Colors for answer tiles, by position (never by letter, so color can't give the answer away). */
    val TILES = intArrayOf(OCEAN, GRASS, GRAPE, TOMATO)

    val CONFETTI = intArrayOf(TOMATO, SUN, GRASS, OCEAN, GRAPE, PINK)

    /** A darker shade of [face]; for white (glass), a quiet "not yet" color. */
    fun edgeOf(face: Int): Int {
        if (face == WHITE) return Theme.muted
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

/** Where the background blobs sit, as fractions of the screen's width and height. */
private val BLOB_SPOTS = listOf(0.05f to 0.06f, 0.98f to 0.36f, 0.04f to 0.74f, 0.92f to 1.0f)

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

    /** Called for each star, and in Learn for each new letter: progress in today's path. */
    var onPoint: (() -> Unit)? = null

    protected val random = Random.Default
    protected val density = resources.displayMetrics.density
    protected fun dp(value: Float) = value * density

    protected open val title: String = ""
    protected open val showHomeButton = true
    protected open val showStars = true

    /** Home makes the star counter a button that opens the sticker album. */
    protected open val starsTappable = false
    /** The tint for this screen's background (light look). */
    protected open val skyColor = Palette.SKY

    /** True for screens that keep a flat [skyColor] in both looks (the night-time rest screen). */
    protected open val plainBackground = false

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
        typeface = Fonts.heavy
        isFakeBoldText = Fonts.needsFakeBold
    }

    /** The phone's own heaviest font: tracing guides were measured against its letter shapes. */
    protected val systemTypeface: Typeface =
        if (Build.VERSION.SDK_INT >= 28) Typeface.create(Typeface.DEFAULT, 900, false)
        else Typeface.create("sans-serif-black", Typeface.NORMAL)

    /** Glass cards and colored blocks; their soft shadows come from a shadow layer. */
    private val blockPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val rimPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE }
    private val blockMatrix = Matrix()
    private val blockShaders = HashMap<Int, LinearGradient>()
    private val gradientShaders = HashMap<Long, LinearGradient>()
    private val blobPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val blobShaders = HashMap<Int, RadialGradient>()
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
        point()
    }

    /** One step of progress that earns no star (a new letter in Learn). */
    protected fun point() {
        onPoint?.invoke()
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
     * Draws a card whose face is [rect], floating on a soft shadow ([depth] sets how high).
     * [Palette.WHITE] means glass: see-through, with a bright rim. Any other color is a glossy
     * block, lighter at the top-left. Pressed cards dip a little and shrink. Returns how far the
     * face moved down, so callers can move what they draw on top by the same amount.
     */
    protected fun drawBlock(
        canvas: Canvas, rect: RectF, face: Int,
        radius: Float = min(rect.width(), rect.height()) * 0.24f,
        depth: Float = dp(7f),
        pressable: Boolean = true,
    ): Float {
        val pressed = pressable && isPressed(rect)
        val sink = if (pressed) depth * 0.3f else 0f
        val lift = if (pressed) depth * 0.4f else depth
        blockRect.set(rect)
        if (pressed) {
            val inset = min(rect.width(), rect.height()) * 0.03f
            blockRect.inset(inset, inset)
        }
        blockRect.offset(0f, sink)
        rimPaint.strokeWidth = dp(1.2f)
        if (face == Palette.WHITE) {
            blockPaint.shader = null
            blockPaint.color = Theme.glass
            blockPaint.setShadowLayer(lift * 1.8f, 0f, lift * 0.7f, Theme.shadow)
            canvas.drawRoundRect(blockRect, radius, radius, blockPaint)
            rimPaint.color = Theme.glassRim
        } else {
            blockPaint.color = Palette.WHITE
            blockPaint.shader = glossFor(face, blockRect)
            blockPaint.setShadowLayer(lift * 1.6f, 0f, lift * 0.75f, (face and 0x00FFFFFF) or if (Theme.dark) 0x80000000.toInt() else 0x66000000)
            canvas.drawRoundRect(blockRect, radius, radius, blockPaint)
            rimPaint.color = 0x4DFFFFFF
        }
        blockPaint.clearShadowLayer()
        blockPaint.shader = null
        val half = rimPaint.strokeWidth / 2f
        blockRect.inset(half, half)
        canvas.drawRoundRect(blockRect, radius - half, radius - half, rimPaint)
        return sink
    }

    /** Like [drawBlock] with a colored face, but shading diagonally from [from] to [to] (the big "Today's games" card). */
    protected fun drawGradientBlock(
        canvas: Canvas, rect: RectF, from: Int, to: Int, radius: Float, depth: Float = dp(8f),
    ): Float {
        val pressed = isPressed(rect)
        val sink = if (pressed) depth * 0.3f else 0f
        val lift = if (pressed) depth * 0.4f else depth
        blockRect.set(rect)
        if (pressed) blockRect.inset(rect.height() * 0.03f, rect.height() * 0.03f)
        blockRect.offset(0f, sink)
        val shader = gradientShaders.getOrPut(from.toLong() shl 32 or (to.toLong() and 0xFFFFFFFFL)) {
            LinearGradient(0f, 0f, 1f, 1f, from, to, Shader.TileMode.CLAMP)
        }
        blockMatrix.setScale(blockRect.width(), blockRect.height())
        blockMatrix.postTranslate(blockRect.left, blockRect.top)
        shader.setLocalMatrix(blockMatrix)
        blockPaint.color = Palette.WHITE
        blockPaint.shader = shader
        blockPaint.setShadowLayer(lift * 1.8f, 0f, lift * 0.8f, (to and 0x00FFFFFF) or 0x66000000)
        canvas.drawRoundRect(blockRect, radius, radius, blockPaint)
        blockPaint.clearShadowLayer()
        blockPaint.shader = null
        rimPaint.strokeWidth = dp(1.2f)
        rimPaint.color = 0x59FFFFFF
        val half = rimPaint.strokeWidth / 2f
        blockRect.inset(half, half)
        canvas.drawRoundRect(blockRect, radius - half, radius - half, rimPaint)
        return sink
    }

    /** Draws [text] starting at [x] (not centered), vertically centered on [cy], shrunk to fit [maxWidth]. */
    protected fun drawTextLeft(canvas: Canvas, text: String, x: Float, cy: Float, size: Float, color: Int, maxWidth: Float) {
        textPaint.textAlign = Paint.Align.LEFT
        textPaint.textSize = size
        textPaint.color = color
        val measured = textPaint.measureText(text)
        if (measured > maxWidth) textPaint.textSize = size * maxWidth / measured
        val metrics = textPaint.fontMetrics
        canvas.drawText(text, x, cy - (metrics.ascent + metrics.descent) / 2f, textPaint)
        textPaint.textAlign = Paint.Align.CENTER
    }

    /** A diagonal shine for a colored block: a lighter tint at the top-left, [face] at the bottom-right. */
    private fun glossFor(face: Int, rect: RectF): Shader {
        val shader = blockShaders.getOrPut(face) {
            val light = Color.rgb(
                Color.red(face) + (255 - Color.red(face)) * 30 / 100,
                Color.green(face) + (255 - Color.green(face)) * 30 / 100,
                Color.blue(face) + (255 - Color.blue(face)) * 30 / 100,
            )
            LinearGradient(0f, 0f, 1f, 1f, light, face, Shader.TileMode.CLAMP)
        }
        blockMatrix.setScale(rect.width(), rect.height())
        blockMatrix.postTranslate(rect.left, rect.top)
        shader.setLocalMatrix(blockMatrix)
        return shader
    }

    /**
     * The background: the theme's base color with four big soft color blobs that drift
     * slowly, which is what makes the glass cards look like glass.
     */
    private fun drawBackground(canvas: Canvas) {
        canvas.drawColor(if (plainBackground) skyColor else Theme.background)
        if (plainBackground) return
        val colors = Theme.blobs(skyColor)
        val w = width.toFloat()
        val h = height.toFloat()
        val base = maxOf(w, h) * 0.42f
        for (i in colors.indices) {
            val (fx, fy) = BLOB_SPOTS[i]
            val drift = if (motionEnabled) 1f else 0f
            val cx = w * fx + sin(time * 0.35f + i * 1.7f) * dp(26f) * drift
            val cy = h * fy + cos(time * 0.28f + i * 2.3f) * dp(30f) * drift
            val r = base * (if (i % 2 == 0) 1f else 0.85f)
            val shader = blobShaders.getOrPut(colors[i]) {
                val c = colors[i]
                RadialGradient(
                    0f, 0f, 1f,
                    intArrayOf(c, (c and 0x00FFFFFF) or ((Color.alpha(c) / 2) shl 24), c and 0x00FFFFFF),
                    floatArrayOf(0f, 0.45f, 1f), Shader.TileMode.CLAMP,
                )
            }
            blockMatrix.setScale(r, r)
            blockMatrix.postTranslate(cx, cy)
            shader.setLocalMatrix(blockMatrix)
            blobPaint.shader = shader
            canvas.drawCircle(cx, cy, r, blobPaint)
        }
    }

    /** A block with one letter (or short text) centered on it. */
    protected fun drawLetterBlock(
        canvas: Canvas, rect: RectF, face: Int, text: String, textColor: Int,
        textScale: Float = 0.66f, depth: Float = dp(7f),
    ) {
        // Pictures sit on white: a red square on a green block would muddle a lesson on colors.
        val sink = drawBlock(canvas, rect, if (Art.has(text)) Palette.WHITE else face, depth = depth)
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
        fillPaint.color = Palette.NAVY
        canvas.drawPath(path, fillPaint)
    }

    /** Draws [text] centered on ([cx], [cy]), shrinking it to fit [maxWidth]. */
    protected fun drawText(
        canvas: Canvas, text: String, cx: Float, cy: Float, size: Float, color: Int,
        maxWidth: Float = Float.MAX_VALUE,
    ) {
        // Picture tracks put a picture where letters go (on blocks, cards and menus).
        if (Art.has(text)) {
            drawEmoji(canvas, text, cx, cy, min(size, maxWidth))
            return
        }
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
                fillPaint.color = Theme.prompt
                canvas.drawCircle(cx, by, r, fillPaint)
                drawText(canvas, badge(i), cx, by, r * 1.3f, Palette.WHITE, r * 1.8f)
            }
        }
    }

    /** The child's animal at [size], centered on ([cx], [cy]), dressed in what it's [wearing]. */
    protected fun drawAvatar(canvas: Canvas, avatar: String, wearing: List<Outfit>, cx: Float, cy: Float, size: Float) {
        drawEmoji(canvas, avatar, cx, cy, size)
        for (slot in Slot.entries) {
            val outfit = wearing.firstOrNull { it.slot == slot } ?: continue
            when (slot) {
                Slot.NECK -> drawEmoji(canvas, outfit.emoji, cx, cy + size * 0.58f, size * 0.46f)
                Slot.EYES -> drawEmoji(canvas, outfit.emoji, cx, cy - size * 0.1f, size * 0.62f)
                Slot.HEAD -> drawEmoji(canvas, outfit.emoji, cx, cy - size * 0.5f, size * 0.52f)
                Slot.HAND -> drawEmoji(canvas, outfit.emoji, cx + size * 0.5f, cy + size * 0.12f, size * 0.42f)
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

        drawBackground(canvas)
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
        drawBlock(canvas, revealCard, Theme.card, radius = dp(36f), depth = dp(10f), pressable = false)
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
            // Stars left to spend in the shop (stickers go by all stars ever earned).
            val count = player.spendableStars.toString()
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
