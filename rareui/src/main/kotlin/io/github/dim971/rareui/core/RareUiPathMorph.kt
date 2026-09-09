/*
 * RareUiPathMorph.kt
 * Turning one outline into another.
 *
 * Upstream reaches for flubber in two places: a pen becoming a tick, and a play triangle
 * becoming a pause bar. The second is two shapes whose corners correspond, and it is
 * written out by hand where it is used. The first is not: a pen and a tick have nothing in
 * common, and the only honest way to move between them is the way flubber does it, by
 * walking both outlines, taking the same number of points along each, and sliding every
 * point to its opposite number.
 *
 * Two things make that look like a morph rather than a scramble. The points have to be
 * evenly spaced along the outline rather than at its corners, and the two rings have to be
 * turned until they line up, or the shape twists inside out on its way across.
 */

package io.github.dim971.rareui.core

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathMeasure

/**
 * How many points each outline is reduced to.
 *
 * Ninety-six is far more than an icon needs to look smooth and few enough that lining the
 * two rings up is instant.
 */
public const val RAREUI_MORPH_SAMPLES: Int = 96

/**
 * Reduces an outline to evenly spaced points along its length.
 *
 * @param path the outline.
 * @param samples how many points to take.
 * @return the points, in order, or nothing for an empty outline.
 */
public fun rareUiPathRing(
    path: Path,
    samples: Int = RAREUI_MORPH_SAMPLES,
): List<Offset> {
    val measure = PathMeasure()
    measure.setPath(path, false)
    val length = measure.length
    if (length <= 0f || samples <= 0) return emptyList()

    return List(samples) { index ->
        measure.getPosition(length * index / samples)
    }
}

/**
 * Turns one ring until it lines up with the other.
 *
 * Without this the two outlines are joined at whatever point each happens to start from,
 * and the shape twists through itself on the way across.
 *
 * @param ring the ring to turn.
 * @param reference the ring to line it up with.
 * @return the turned ring, or the ring unchanged when the two cannot be compared.
 */
public fun rareUiAlignRing(
    ring: List<Offset>,
    reference: List<Offset>,
): List<Offset> {
    if (ring.isEmpty() || ring.size != reference.size) return ring

    var bestOffset = 0
    var bestCost = Float.MAX_VALUE
    for (offset in ring.indices) {
        var cost = 0f
        for (index in ring.indices) {
            val candidate = ring[(index + offset) % ring.size]
            val dx = candidate.x - reference[index].x
            val dy = candidate.y - reference[index].y
            cost += dx * dx + dy * dy
            // Nothing to learn from finishing a rotation that is already worse.
            if (cost >= bestCost) break
        }
        if (cost < bestCost) {
            bestCost = cost
            bestOffset = offset
        }
    }

    return List(ring.size) { ring[(it + bestOffset) % ring.size] }
}

/**
 * The ring partway between two others.
 *
 * @param start the ring at the beginning.
 * @param end the ring at the end, already lined up with [start].
 * @param progress how far across, in `0..1`. Values outside are held at the ends.
 * @return the points to draw, or nothing when the two rings cannot be compared.
 */
public fun rareUiInterpolateRing(
    start: List<Offset>,
    end: List<Offset>,
    progress: Float,
): List<Offset> {
    if (start.isEmpty() || start.size != end.size) return emptyList()
    val t = progress.coerceIn(0f, 1f)
    return List(start.size) { index ->
        Offset(
            start[index].x + (end[index].x - start[index].x) * t,
            start[index].y + (end[index].y - start[index].y) * t,
        )
    }
}

/** A morph between two outlines, sampled once and interpolated thereafter. */
public class RareUiPathMorph(
    from: Path,
    to: Path,
    samples: Int = RAREUI_MORPH_SAMPLES,
) {
    private val start: List<Offset> = rareUiPathRing(from, samples)
    private val end: List<Offset> =
        rareUiAlignRing(rareUiPathRing(to, samples), start)

    /**
     * Rebuilds the outline partway between the two, fitted into a box.
     *
     * @param progress how far across, in `0..1`.
     * @param into the path to rebuild, so a frame allocates nothing.
     * @param size the box to draw into.
     * @param viewBox the box the two outlines were drawn in.
     */
    public fun path(
        progress: Float,
        into: Path,
        size: Size,
        viewBox: Size,
    ) {
        into.reset()
        val points = rareUiInterpolateRing(start, end, progress)
        if (points.isEmpty()) return

        val scale = minOf(size.width / viewBox.width, size.height / viewBox.height)
        val left = (size.width - viewBox.width * scale) / 2
        val top = (size.height - viewBox.height * scale) / 2

        points.forEachIndexed { index, point ->
            val x = left + point.x * scale
            val y = top + point.y * scale
            if (index == 0) into.moveTo(x, y) else into.lineTo(x, y)
        }
        into.close()
    }
}
