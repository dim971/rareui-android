/*
 * GravityHeightMap.kt
 * The pile GravityLetters' glyphs land on, ported from the `spanOf`, `restY`, `deposit`,
 * `windowTop`, `groundTilt` and `findRestX` functions in upstream's
 * `components/ui/gravity-letters.tsx`.
 *
 * There is no physics engine here and there is not one upstream either. The pile is a
 * height map: the container is divided into eight point columns, each remembering how deep
 * the heap is at that point. A glyph looking for somewhere to land asks the columns it
 * covers how high they are, and a glyph that has landed raises them. That is the whole
 * model, and it is what makes hundreds of letters cost almost nothing.
 */

package io.github.dim971.rareui.components.display

import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.roundToInt
import kotlin.math.tan

/** How wide one column of the height map is, in points. */
internal const val GRAVITY_COLUMN_WIDTH = 8.0

/**
 * How much lower a neighbouring column has to be before a glyph slides on to it, as a
 * fraction of the glyph's own height.
 */
internal const val GRAVITY_SLOPE = 0.35

/** The steepest a glyph comes to rest at, in degrees. */
internal const val GRAVITY_MAX_TILT = 26.0

/** How much of its speed a glyph keeps on its one bounce. */
internal const val GRAVITY_BOUNCE = 0.22

/** The slide threshold while the device is tilted, which makes the pile keener to move. */
internal const val GRAVITY_EAGER_SLOPE = 0.45

/** The first and last column a glyph touches. */
internal data class GravitySpan(
    val from: Int,
    val to: Int,
)

/** A heap of glyphs, remembered as how deep it is at each column across the container. */
internal class GravityHeightMap(
    width: Double,
    /** How tall the container is, which is what depths are measured up from. */
    val height: Double,
) {
    val heights: DoubleArray =
        DoubleArray(maxOf(1, ceil(width / GRAVITY_COLUMN_WIDTH).toInt()))

    /**
     * Which columns a glyph of the given width covers.
     *
     * @param x the glyph's left edge.
     * @param width its width.
     * @return the first and last column it touches.
     */
    fun span(
        x: Double,
        width: Double,
    ): GravitySpan {
        val from = maxOf(0, floor(x / GRAVITY_COLUMN_WIDTH).toInt())
        val to =
            minOf(
                heights.size - 1,
                maxOf(from, ceil((x + width) / GRAVITY_COLUMN_WIDTH).toInt() - 1),
            )
        return GravitySpan(from, to)
    }

    /**
     * The highest the heap reaches across a run of columns.
     *
     * Returns infinity when the run falls off either end, which is how a glyph at the edge
     * of the container is stopped from sliding out of it: there is nothing lower beyond the
     * wall, so it reads as infinitely high.
     *
     * @param from the first column.
     * @param to the last.
     * @return the greatest depth in that run.
     */
    fun top(
        from: Int,
        to: Int,
    ): Double {
        if (from < 0 || to >= heights.size) return Double.POSITIVE_INFINITY
        var highest = 0.0
        for (column in from..maxOf(from, to)) highest = maxOf(highest, heights[column])
        return highest
    }

    /**
     * Where a glyph would come to rest, given how far it is leaning.
     *
     * A leaning glyph touches the heap on one corner rather than along its base, so the
     * column under that corner is what stops it and the rest of its width hangs over
     * whatever is beside it.
     *
     * @param x the glyph's left edge.
     * @param width its width once rotated.
     * @param glyphHeight its height once rotated.
     * @param rotation how far it is leaning, in degrees.
     * @return the vertical position of its top edge at rest.
     */
    fun restY(
        x: Double,
        width: Double,
        glyphHeight: Double,
        rotation: Double,
    ): Double {
        val bounds = span(x, width)
        val tangent = tan(kotlin.math.abs(rotation) * PI / 180)
        var lowest = Double.POSITIVE_INFINITY

        for (column in bounds.from..bounds.to) {
            val centre = ((column + 0.5) * GRAVITY_COLUMN_WIDTH - x).coerceIn(0.0, width)
            val edge =
                minOf((if (rotation >= 0) width - centre else centre) * tangent, glyphHeight - 1)
            lowest = minOf(lowest, height - heights[column] - glyphHeight + edge)
        }
        return lowest
    }

    /**
     * Raises the heap to account for a glyph that has landed.
     *
     * @param x the glyph's left edge.
     * @param width its width once rotated.
     * @param glyphHeight its height once rotated.
     * @param rotation how far it is leaning, in degrees.
     * @param y where its top edge came to rest.
     */
    fun deposit(
        x: Double,
        width: Double,
        glyphHeight: Double,
        rotation: Double,
        y: Double,
    ) {
        val bounds = span(x, width)
        val tangent = tan(kotlin.math.abs(rotation) * PI / 180)

        for (column in bounds.from..bounds.to) {
            val centre = ((column + 0.5) * GRAVITY_COLUMN_WIDTH - x).coerceIn(0.0, width)
            // The opposite corner to the one restY measured, so a leaning glyph raises the
            // heap under its high side as well as its low one.
            val edge =
                minOf((if (rotation >= 0) centre else width - centre) * tangent, glyphHeight - 1)
            heights[column] = maxOf(heights[column], height - y - edge)
        }
    }

    /**
     * How steeply the heap slopes under a glyph, in degrees.
     *
     * Comparing the left half of the span with the right half. A glyph landing on a slope
     * leans to match it, which is what makes a pile of letters look like a pile rather than
     * like a stack.
     *
     * @param x the glyph's left edge.
     * @param width its width.
     * @return the slope, in degrees, positive when the heap is higher on the left.
     */
    fun groundTilt(
        x: Double,
        width: Double,
    ): Double {
        val bounds = span(x, width)
        if (bounds.to <= bounds.from) return 0.0

        val middle = ceil((bounds.from + bounds.to) / 2.0).toInt()
        val left = top(bounds.from, middle - 1)
        val right = top(middle, bounds.to)
        if (!left.isFinite() || !right.isFinite()) return 0.0

        val run = maxOf((bounds.to - bounds.from + 1) / 2.0 * GRAVITY_COLUMN_WIDTH, 1.0)
        return atan2(left - right, run) * 180 / PI
    }

    /**
     * Walks a glyph sideways until it finds somewhere it will not slide off.
     *
     * Sixty-four steps is upstream's own cap and it is generous: a glyph reaches its
     * resting place in a handful, and the cap only matters for a heap so uneven that a
     * glyph would otherwise wander for ever.
     *
     * @param x where the glyph is now.
     * @param width its unrotated width.
     * @param glyphHeight its unrotated height.
     * @param maxX the furthest right it may go.
     * @param bias which way to break a tie, `-1` for left and `1` for right.
     * @param eager a factor on the slide threshold. Below one the pile slides more readily.
     * @return where it settles.
     */
    @Suppress("LongParameterList")
    fun restX(
        x: Double,
        width: Double,
        glyphHeight: Double,
        maxX: Double,
        bias: Double,
        eager: Double = 1.0,
    ): Double {
        var current = x.coerceIn(0.0, maxOf(0.0, maxX))
        val drop = glyphHeight * GRAVITY_SLOPE * eager
        val step = maxOf(GRAVITY_COLUMN_WIDTH, (width / 3).roundToInt().toDouble())

        repeat(64) {
            val bounds = span(current, width)
            val columns = bounds.to - bounds.from + 1
            val here = top(bounds.from, bounds.to)
            val toLeft = here - top(bounds.from - columns, bounds.from - 1)
            val toRight = here - top(bounds.to + 1, bounds.to + columns)

            val next =
                when {
                    toLeft > drop && toRight > drop && kotlin.math.abs(toLeft - toRight) <= 1 ->
                        // A ridge with equal drops either side, so the tie is broken by the
                        // bias: whichever way the wind is blowing, or a coin toss when it
                        // is still.
                        if (bias < 0) maxOf(current - step, 0.0) else minOf(current + step, maxX)

                    toLeft > drop && toLeft >= toRight -> maxOf(current - step, 0.0)
                    toRight > drop -> minOf(current + step, maxX)
                    else -> current
                }

            if (next == current) return current
            current = next
        }
        return current
    }
}
