package com.aksharblocks.app

import kotlin.math.ceil
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min

/** A point in a letter's box: (0, 0) is the top-left corner, (1, 1) the bottom-right. */
data class P(val x: Float, val y: Float)

/** One pen stroke, in the order it should be written. Smooth strokes curve through their points. */
data class Stroke(val points: List<P>, val smooth: Boolean) {

    /** Evenly spread points along the stroke, for drawing and checking. */
    fun sample(perSegment: Int = 12): List<P> {
        if (points.size < 2) return points
        val out = ArrayList<P>()
        val last = points.lastIndex
        for (i in 0 until last) {
            val p1 = points[i]
            val p2 = points[i + 1]
            for (s in 0 until perSegment) {
                val t = s / perSegment.toFloat()
                out += if (smooth) {
                    catmullRom(points[max(i - 1, 0)], p1, p2, points[min(i + 2, last)], t)
                } else {
                    P(p1.x + (p2.x - p1.x) * t, p1.y + (p2.y - p1.y) * t)
                }
            }
        }
        out += points[last]
        return out
    }

    private fun catmullRom(p0: P, p1: P, p2: P, p3: P, t: Float): P {
        val t2 = t * t
        val t3 = t2 * t
        fun axis(a: Float, b: Float, c: Float, d: Float) =
            0.5f * (2 * b + (-a + c) * t + (2 * a - 5 * b + 4 * c - d) * t2 + (-a + 3 * b - 3 * c + d) * t3)
        return P(axis(p0.x, p1.x, p2.x, p3.x), axis(p0.y, p1.y, p2.y, p3.y))
    }
}

object Tracing {

    /** How thick the letter's shape is, as a share of the box (radius). */
    const val TARGET_RADIUS = 0.06f

    /** How thick the child's crayon is (radius). A little wider than the target, to be forgiving. */
    const val CRAYON_RADIUS = 0.085f

    /** Share of the letter that must be covered. Filled letter shapes are bigger, so they need less. */
    const val PASS_WITH_STROKES = 0.8f
    const val PASS_WITH_SHAPE = 0.7f

    /** At most this share of the crayon may land off the letter (stops "scribble everywhere"). */
    const val MAX_OUTSIDE = 0.55f

    /** Every stroke on its own must be at least this well covered. */
    const val PASS_EACH_STROKE = 0.6f

    /**
     * Reads a stroke file: one letter per line, `LETTER <tab> STROKES`. Strokes are separated
     * by `;`, points by spaces, each point is `x,y` from 0 to 1. A stroke starting with `~`
     * is a curve. Blank lines and lines starting with `#` are ignored.
     */
    fun parse(text: String): Map<String, List<Stroke>> =
        text.lineSequence()
            .map { it.trimEnd('\r') }
            .filter { it.isNotBlank() && !it.startsWith("#") }
            .mapNotNull { line ->
                val columns = line.split('\t')
                if (columns.size < 2) null else columns[0].trim() to parseStrokes(columns[1])
            }
            .toMap()

    fun parseStrokes(spec: String): List<Stroke> =
        spec.split(';').map { it.trim() }.filter { it.isNotEmpty() }.map { part ->
            val smooth = part.startsWith("~")
            val points = part.removePrefix("~").trim().split(Regex("\\s+")).map { pair ->
                val (x, y) = pair.split(',').map { it.toFloat() }
                P(x, y)
            }
            Stroke(points, smooth)
        }
}

/**
 * A coarse grid over the letter's box that records where the letter is and where the
 * child has drawn, to decide when the letter is traced well enough.
 */
class TraceGrid(private val size: Int = 64) {

    private val target = BooleanArray(size * size)

    /** Each stroke's own cells, so every stroke can be checked (not just the total). */
    private val strokeTargets = ArrayList<BooleanArray>()
    private val painted = BooleanArray(size * size)

    var targetCount = 0
        private set
    var paintedCount = 0
        private set

    fun clearTarget() {
        target.fill(false)
        strokeTargets.clear()
        targetCount = 0
    }

    /** Marks a stroke of the letter, drawn with the given radius (box units). */
    fun addTargetLine(points: List<P>, radius: Float) {
        val own = BooleanArray(size * size)
        for (i in 1 until points.size) stamp(points[i - 1], points[i], radius, own)
        if (points.size == 1) stamp(points[0], points[0], radius, own)
        strokeTargets += own
        for (i in own.indices) if (own[i]) target[i] = true
        targetCount = target.count { it }
    }

    /** Uses a ready-made shape (row by row, `size × size`) as the letter. */
    fun setTargetMask(mask: BooleanArray) {
        require(mask.size == target.size)
        mask.copyInto(target)
        targetCount = target.count { it }
    }

    /** Records a crayon line from [a] to [b]. */
    fun paint(a: P, b: P, radius: Float) {
        stamp(a, b, radius, painted)
        paintedCount = painted.count { it }
    }

    fun clearPaint() {
        painted.fill(false)
        paintedCount = 0
    }

    /** Share of the letter covered by crayon, 0 to 1. */
    val coverage: Float
        get() {
            if (targetCount == 0) return 0f
            var hit = 0
            for (i in target.indices) if (target[i] && painted[i]) hit++
            return hit / targetCount.toFloat()
        }

    /**
     * The lowest coverage of any single stroke (1 when there are no separate strokes).
     * Stops a letter counting as done while one of its strokes, like the bar of an A, is missing.
     */
    val weakestStroke: Float
        get() = strokeTargets.minOfOrNull { own ->
            var total = 0
            var hit = 0
            for (i in own.indices) if (own[i]) {
                total++
                if (painted[i]) hit++
            }
            if (total == 0) 1f else hit / total.toFloat()
        } ?: 1f

    /** Share of the crayon that is off the letter, 0 to 1. */
    val outsideShare: Float
        get() {
            if (paintedCount == 0) return 0f
            var off = 0
            for (i in painted.indices) if (painted[i] && !target[i]) off++
            return off / paintedCount.toFloat()
        }

    private fun stamp(a: P, b: P, radius: Float, into: BooleanArray) {
        val length = hypot(b.x - a.x, b.y - a.y)
        val steps = max(1, ceil(length * size * 2).toInt())
        val r = radius * size
        val rr = r * r
        for (s in 0..steps) {
            val t = s / steps.toFloat()
            val cx = (a.x + (b.x - a.x) * t) * size
            val cy = (a.y + (b.y - a.y) * t) * size
            val x0 = max(0, (cx - r).toInt())
            val x1 = min(size - 1, (cx + r).toInt())
            val y0 = max(0, (cy - r).toInt())
            val y1 = min(size - 1, (cy + r).toInt())
            for (y in y0..y1) {
                val dy = y + 0.5f - cy
                for (x in x0..x1) {
                    val dx = x + 0.5f - cx
                    if (dx * dx + dy * dy <= rr) into[y * size + x] = true
                }
            }
        }
    }

    companion object {
        const val DEFAULT_SIZE = 64
    }
}
