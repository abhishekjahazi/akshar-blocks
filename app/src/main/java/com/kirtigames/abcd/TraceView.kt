package com.kirtigames.abcd

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.DashPathEffect
import android.graphics.Paint
import android.graphics.RectF
import android.view.MotionEvent
import kotlin.math.hypot
import kotlin.math.min

/**
 * Trace a letter with a finger. Letters with stroke data (English) show numbered start
 * dots and an animated finger demonstrating the stroke order; the others (Hindi, for now)
 * are traced over the letter's own shape.
 */
class TraceView(
    context: Context, speaker: Speaker, player: Player, private val track: Track,
) : GameView(context, speaker, player) {

    private val lang = track.lang
    private val letters = track.letters
    private var index = 0

    /** Stroke order for the current letter, or null to trace over its shape. */
    private var strokes: List<Stroke>? = null
    private var strokeSamples: List<List<P>> = emptyList()

    /** Size of the letter's font shape, as a share of the box (shape tracing only). */
    private var glyphScale = GLYPH_SIZE

    private val grid = TraceGrid(GRID_SIZE)

    /** What the child has drawn, in box coordinates so it survives rotation. */
    private val drawn = ArrayList<MutableList<P>>()
    private var drawing = false
    private var solved = false

    /** A touch that started on the letter while celebrating; ignored so it can't flip letters. */
    private var ignoringTouch = false

    /** Changes whenever a new letter is shown, so a pending "next letter" can tell it's stale. */
    private var letterToken = 0

    /** When the stroke demonstration starts (game time), or null when it isn't playing. */
    private var demoStart: Float? = null

    private val card = RectF()
    private val box = RectF()
    private val prevButton = RectF()
    private val clearButton = RectF()
    private val demoButton = RectF()
    private val nextButton = RectF()

    private val dashPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        color = GUIDE_DASH
    }
    private val glyphPaint = Paint(textPaint).apply { textAlign = Paint.Align.CENTER }

    override val title get() = lang.traceTitle(index + 1, letters.size)
    override val skyColor = Palette.PEACH

    init {
        showLetter(0)
    }

    private fun showLetter(i: Int) {
        letterToken++
        index = (i + letters.size) % letters.size
        val letter = letters[index]
        strokes = Content.strokes(track, letter)
        strokeSamples = strokes?.map { it.sample() }.orEmpty()

        grid.clearTarget()
        val known = strokes
        if (known != null) {
            strokeSamples.forEach { grid.addTargetLine(it, Tracing.TARGET_RADIUS) }
        } else {
            grid.setTargetMask(glyphMask(letter.symbol))
        }
        clearDrawing()
        solved = false
        demoStart = if (known != null) time + DEMO_DELAY else null
        speaker.say(lang.traceAsk(letter), lang.locale)
    }

    private fun clearDrawing() {
        drawn.clear()
        grid.clearPaint()
    }

    /** Renders the letter's shape onto the check grid, using the same font and centering as the screen. */
    private fun glyphMask(symbol: String): BooleanArray {
        val size = GRID_SIZE
        val paint = Paint(glyphPaint).apply { textSize = size * GLYPH_SIZE }
        val width = paint.measureText(symbol) / size
        glyphScale = if (width > MAX_GLYPH_WIDTH) GLYPH_SIZE * MAX_GLYPH_WIDTH / width else GLYPH_SIZE
        paint.textSize = size * glyphScale

        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val metrics = paint.fontMetrics
        Canvas(bitmap).drawText(symbol, size / 2f, size / 2f - (metrics.ascent + metrics.descent) / 2f, paint)
        val pixels = IntArray(size * size)
        bitmap.getPixels(pixels, 0, size, 0, 0, size, size)
        bitmap.recycle()
        return BooleanArray(pixels.size) { (pixels[it] ushr 24) > 100 }
    }

    // --- Layout and drawing -----------------------------------------------------

    private fun layout() {
        val button = min(dp(72f), (contentRight - contentLeft - dp(48f)) / 4f)
        val buttonTop = contentBottom - button - dp(8f)
        val buttons = if (strokes != null) listOf(prevButton, clearButton, demoButton, nextButton)
        else listOf(prevButton, clearButton, nextButton)
        val rowWidth = button * buttons.size + dp(16f) * (buttons.size - 1)
        var x = width / 2f - rowWidth / 2f
        for (b in buttons) {
            b.set(x, buttonTop, x + button, buttonTop + button)
            x += button + dp(16f)
        }
        if (strokes == null) demoButton.setEmpty()

        card.set(contentLeft, contentTop, contentRight, buttonTop - dp(28f))
        val side = min(card.width(), card.height()) - dp(40f)
        box.set(card.centerX() - side / 2f, card.centerY() - side / 2f, card.centerX() + side / 2f, card.centerY() + side / 2f)
    }

    override fun drawGame(canvas: Canvas) {
        layout()
        drawBlock(canvas, card, Palette.WHITE, radius = dp(36f), depth = dp(10f), pressable = false)

        val color = track.colorFor(index)
        if (strokes != null) drawStrokeGuide(canvas, if (solved) color else GUIDE_FILL) else drawShapeGuide(canvas, if (solved) color else GUIDE_FILL)

        // The child's crayon.
        strokePaint.color = color
        strokePaint.strokeWidth = Tracing.CRAYON_RADIUS * 2f * box.width()
        strokePaint.strokeJoin = Paint.Join.ROUND
        for (line in drawn) drawLine(canvas, line, strokePaint)

        drawDemo(canvas, color)

        drawArrowBlock(canvas, prevButton, Palette.SUN, pointsRight = false)
        drawArrowBlock(canvas, nextButton, Palette.SUN, pointsRight = true)
        val clearSink = drawBlock(canvas, clearButton, Palette.WHITE, depth = dp(6f))
        drawEmoji(canvas, "🔄", clearButton.centerX(), clearButton.centerY() + clearSink, clearButton.height() * 0.45f)
        if (!demoButton.isEmpty) {
            val demoSink = drawBlock(canvas, demoButton, Palette.WHITE, depth = dp(6f))
            drawEmoji(canvas, "👆", demoButton.centerX(), demoButton.centerY() + demoSink, demoButton.height() * 0.45f)
        }
    }

    /** Thick pale strokes, a dashed middle line, and numbered dots where each stroke starts. */
    private fun drawStrokeGuide(canvas: Canvas, fill: Int) {
        strokePaint.color = fill
        strokePaint.strokeWidth = Tracing.TARGET_RADIUS * 2f * box.width()
        strokePaint.strokeJoin = Paint.Join.ROUND
        strokeSamples.forEach { drawLine(canvas, it, strokePaint) }

        if (solved) return
        dashPaint.strokeWidth = dp(3f)
        dashPaint.pathEffect = DashPathEffect(floatArrayOf(dp(2f), dp(10f)), 0f)
        strokeSamples.forEach { drawLine(canvas, it, dashPaint) }

        val dot = box.width() * 0.045f
        strokeSamples.forEachIndexed { i, points ->
            // When strokes start at the same spot (like both sides of an A), move this
            // number a little way along its own stroke so the numbers don't cover each other.
            val first = points.first()
            val shared = (0 until i).any { j -> strokeSamples[j].first().let { hypot(it.x - first.x, it.y - first.y) < 0.08f } }
            val at = if (shared) points[(points.lastIndex * 0.18f).toInt()] else first
            val start = toScreenX(at.x) to toScreenY(at.y)
            fillPaint.color = Palette.INK
            canvas.drawCircle(start.first, start.second, dot, fillPaint)
            drawText(canvas, "${i + 1}", start.first, start.second, dot * 1.3f, Palette.WHITE)
        }
    }

    /** The letter itself in a pale color with a dashed outline. */
    private fun drawShapeGuide(canvas: Canvas, fill: Int) {
        val letter = letters[index]
        glyphPaint.textSize = box.width() * glyphScale
        val metrics = glyphPaint.fontMetrics
        val baseline = box.centerY() - (metrics.ascent + metrics.descent) / 2f
        glyphPaint.style = Paint.Style.FILL
        glyphPaint.color = fill
        glyphPaint.pathEffect = null
        canvas.drawText(letter.symbol, box.centerX(), baseline, glyphPaint)
        if (solved) return
        glyphPaint.style = Paint.Style.STROKE
        glyphPaint.strokeWidth = dp(2.5f)
        glyphPaint.color = GUIDE_DASH
        glyphPaint.pathEffect = DashPathEffect(floatArrayOf(dp(8f), dp(7f)), 0f)
        canvas.drawText(letter.symbol, box.centerX(), baseline, glyphPaint)
        glyphPaint.style = Paint.Style.FILL
        glyphPaint.pathEffect = null
    }

    /** A finger that draws each stroke in order, showing where to start and which way to go. */
    private fun drawDemo(canvas: Canvas, color: Int) {
        val start = demoStart ?: return
        if (time < start) return
        val progress = (time - start) / STROKE_SECONDS
        val strokeIndex = progress.toInt()
        if (strokeIndex >= strokeSamples.size) {
            demoStart = null
            return
        }
        val fraction = progress - strokeIndex

        // Strokes already shown stay faintly colored.
        strokePaint.color = color
        strokePaint.alpha = 110
        strokePaint.strokeWidth = Tracing.TARGET_RADIUS * 2f * box.width()
        for (i in 0 until strokeIndex) drawLine(canvas, strokeSamples[i], strokePaint)
        val points = strokeSamples[strokeIndex]
        val upTo = (fraction * points.lastIndex).toInt().coerceIn(0, points.lastIndex)
        drawLine(canvas, points.subList(0, upTo + 1), strokePaint)
        strokePaint.alpha = 255

        val tip = points[upTo]
        val x = toScreenX(tip.x)
        val y = toScreenY(tip.y)
        fillPaint.color = Palette.WHITE
        canvas.drawCircle(x, y, box.width() * 0.05f, fillPaint)
        fillPaint.color = color
        canvas.drawCircle(x, y, box.width() * 0.035f, fillPaint)
    }

    private fun drawLine(canvas: Canvas, points: List<P>, paint: Paint) {
        if (points.isEmpty()) return
        path.reset()
        path.moveTo(toScreenX(points[0].x), toScreenY(points[0].y))
        if (points.size == 1) path.lineTo(toScreenX(points[0].x) + 0.1f, toScreenY(points[0].y))
        for (i in 1 until points.size) path.lineTo(toScreenX(points[i].x), toScreenY(points[i].y))
        canvas.drawPath(path, paint)
    }

    private fun toScreenX(x: Float) = box.left + x * box.width()
    private fun toScreenY(y: Float) = box.top + y * box.height()
    private fun toBox(x: Float, y: Float) = P((x - box.left) / box.width(), (y - box.top) / box.height())

    // --- Touch ------------------------------------------------------------------

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (showingReveal) return super.onTouchEvent(event)
        if (event.actionMasked == MotionEvent.ACTION_DOWN) {
            val onLetter = card.contains(event.x, event.y)
            ignoringTouch = solved && onLetter
            drawing = !solved && onLetter
        }
        if (ignoringTouch) {
            val action = event.actionMasked
            if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL) ignoringTouch = false
            return true
        }
        if (!drawing) return super.onTouchEvent(event)

        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                demoStart = null
                val p = toBox(event.x, event.y)
                drawn += mutableListOf(p)
                grid.paint(p, p, Tracing.CRAYON_RADIUS)
            }
            MotionEvent.ACTION_MOVE -> {
                val line = drawn.lastOrNull() ?: return true
                for (h in 0 until event.historySize) addPoint(line, toBox(event.getHistoricalX(h), event.getHistoricalY(h)))
                addPoint(line, toBox(event.x, event.y))
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                drawing = false
                check()
            }
        }
        return true
    }

    private fun addPoint(line: MutableList<P>, p: P) {
        grid.paint(line.last(), p, Tracing.CRAYON_RADIUS)
        line += p
    }

    /** After each stroke: done if enough of the letter is covered without scribbling everywhere. */
    private fun check() {
        val pass = if (strokes != null) Tracing.PASS_WITH_STROKES else Tracing.PASS_WITH_SHAPE
        val passed = grid.coverage >= pass &&
            grid.weakestStroke >= Tracing.PASS_EACH_STROKE &&
            grid.outsideShare <= Tracing.MAX_OUTSIDE
        if (passed) {
            solved = true
            addStar()
            celebrate(box.centerX(), box.centerY(), 48)
            speaker.say(lang.traceDone(letters[index], random), lang.locale)
            val token = letterToken
            after(2.4f) { if (token == letterToken) showLetter(index + 1) }
        } else if (grid.outsideShare > SCRIBBLE && grid.paintedCount > grid.targetCount) {
            Sounds.play(Sound.WRONG)
            speaker.say(lang.traceAgain(), lang.locale)
            after(0.6f) { clearDrawing() }
        }
    }

    override fun onTap(x: Float, y: Float) {
        when {
            prevButton.contains(x, y) -> showLetter(index - 1)
            nextButton.contains(x, y) -> showLetter(index + 1)
            clearButton.contains(x, y) -> if (!solved) clearDrawing()
            demoButton.contains(x, y) -> if (!solved) {
                clearDrawing()
                demoStart = time
            }
        }
    }

    override fun onSwipe(towardsLeft: Boolean) = showLetter(index + if (towardsLeft) 1 else -1)

    private companion object {
        const val GRID_SIZE = TraceGrid.DEFAULT_SIZE
        const val GLYPH_SIZE = 0.8f
        const val MAX_GLYPH_WIDTH = 0.92f
        const val DEMO_DELAY = 0.8f
        const val STROKE_SECONDS = 1.3f

        /** Share of crayon off the letter that counts as scribbling, which clears the drawing. */
        const val SCRIBBLE = 0.7f

        const val GUIDE_FILL = 0xFFE6ECF8.toInt()
        const val GUIDE_DASH = 0xFF9AAAD0.toInt()
    }
}
