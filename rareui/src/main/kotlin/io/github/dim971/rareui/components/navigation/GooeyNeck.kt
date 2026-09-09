/*
 * GooeyNeck.kt
 * The seam between two tiles of GooeyNav, ported from the `neckPath` function in upstream's
 * `components/ui/gooey-nav.tsx`.
 *
 * Despite the component's name there is no blur and no colour matrix filter anywhere in
 * this. The seam is an explicitly drawn pair of concave quadratics pinching toward a waist,
 * which is both cheaper than a filter and exactly reproducible.
 */

package io.github.dim971.rareui.components.navigation

import androidx.compose.ui.graphics.Path

/**
 * The nominal height the seam is drawn against before it is stretched to the tile.
 *
 * Upstream draws into an SVG viewBox of this height with `preserveAspectRatio="none"`, so
 * the number itself never reaches the screen. It is kept because the waist is expressed as
 * a fraction of it.
 */
internal const val GOOEY_NECK_HEIGHT: Double = 100.0

/**
 * How far the gap can open before the seam has thinned to nothing.
 *
 * A fraction of the separation, so a wider nav stretches its seams further before they
 * break rather than snapping at the same absolute distance.
 */
internal const val GOOEY_NECK_BREAK: Double = 0.22

/**
 * The waist of the seam at a given gap, in the nominal height's units.
 *
 * It starts at the full height when the tiles are touching, and reaches zero at
 * [GOOEY_NECK_BREAK] of the separation, which is where the seam parts.
 *
 * @param gap how far apart the two tiles are, in pixels.
 * @param span the separation the nav is configured with, in pixels.
 * @return the waist, or a value at or below zero once the seam has broken.
 */
internal fun gooeyNeckWaist(
    gap: Double,
    span: Double,
): Double = GOOEY_NECK_HEIGHT * (1 - gap / (span * GOOEY_NECK_BREAK))

/**
 * Whether the seam at [seam], which sits before tile [seam], is open.
 *
 * The two ends of the bar are always open, since there is nothing beyond them to join to,
 * and so are the two seams either side of the selected tile. Everything else stays closed,
 * which is what makes the unselected tiles read as one continuous block.
 *
 * @param seam the seam's index. Seam `n` sits immediately before tile `n`, so a bar of
 *   three tiles has four seams, `0` through `3`.
 * @param active the index of the selected tile.
 * @param count how many tiles there are.
 */
internal fun gooeyIsSeamOpen(
    seam: Int,
    active: Int,
    count: Int,
): Boolean = seam == 0 || seam == count || seam - 1 == active || seam == active

/**
 * The seam's six points, once it has been worked out where they go.
 *
 * Geometry rather than a path, because a path on this platform is an Android object and an
 * Android object in a plain unit test is a stub that answers every question with zero.
 * The numbers are what is worth checking; turning them into a path is one line at the call
 * site.
 */
internal data class GooeyNeckGeometry(
    /** The left edge, where the previous tile ends. */
    val start: Double,
    /** The right edge, where this tile begins. */
    val end: Double,
    /** Halfway between them, where both control points sit. */
    val middle: Double,
    /** The tile's top edge. */
    val top: Double,
    /** Its bottom edge. */
    val bottom: Double,
    /** Where the upper curve's control point sits, pulled down toward the waist. */
    val upperControl: Double,
    /** Where the lower curve's sits, pulled up toward it. */
    val lowerControl: Double,
)

/**
 * The seam drawn in the gap a tile leaves to its left.
 *
 * The shape is two concave quadratic curves, one along the top and one along the bottom,
 * whose control points move toward each other as the gap opens. At rest they sit at the
 * edges and the seam is a solid block joining the tiles; fully open they have met in the
 * middle and there is nothing left to draw.
 *
 * @param gap how far apart the two tiles are, in pixels.
 * @param span the separation, which is also the drawing's width.
 * @param height the tile's height, which the nominal height is stretched to.
 * @param left where the drawing starts, in pixels.
 * @param top where the tile's top edge is, in pixels.
 * @return the seam's points, or `null` when there is nothing to draw.
 */
internal fun gooeyNeckGeometry(
    gap: Double,
    span: Double,
    height: Double,
    left: Double = 0.0,
    top: Double = 0.0,
): GooeyNeckGeometry? {
    // A gap that is not a positive number would otherwise emit a path full of nonsense
    // coordinates. Closed seams pull the tiles together instead, so there is nothing to
    // draw here at all.
    if (!gap.isFinite() || !span.isFinite() || gap <= 0 || span <= 0) return null

    val waist = gooeyNeckWaist(gap, span)
    if (waist <= 0) return null

    // The viewBox is stretched to the tile's height, so the waist scales with it.
    val scale = height / GOOEY_NECK_HEIGHT
    val start = left + span - gap

    return GooeyNeckGeometry(
        start = start,
        end = left + span,
        middle = start + gap / 2,
        top = top,
        bottom = top + height,
        upperControl = top + (GOOEY_NECK_HEIGHT - waist) * scale,
        lowerControl = top + waist * scale,
    )
}

/** Turns the seam's points into something that can be filled. */
internal fun GooeyNeckGeometry.toPath(): Path {
    val path = Path()
    path.moveTo(start.toFloat(), top.toFloat())
    path.quadraticTo(middle.toFloat(), upperControl.toFloat(), end.toFloat(), top.toFloat())
    path.lineTo(end.toFloat(), bottom.toFloat())
    path.quadraticTo(middle.toFloat(), lowerControl.toFloat(), start.toFloat(), bottom.toFloat())
    path.close()
    return path
}
