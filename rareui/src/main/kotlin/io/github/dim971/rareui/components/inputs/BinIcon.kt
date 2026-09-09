/*
 * BinIcon.kt
 * The bin on DeleteButton, ported from the animated path template and the lid group in
 * upstream's `components/ui/delete-button.tsx`.
 *
 * The lid is a real rotation about the hinge at the back left corner. The walls are not:
 * they are redrawn shorter as the lid opens, so the bin appears to sink into itself while
 * the lid swings clear of it. Upstream does that by interpolating a number into the path's
 * `d` string; here the path is built from the same number, which comes to the same shape
 * without producing a new string sixty times a second.
 */

package io.github.dim971.rareui.components.inputs

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.vector.PathParser

/** The box upstream draws the icon in. */
internal const val BIN_BOX = 24f

/** Where the base of the bin sits. */
internal const val BIN_BASE = 22f

/** The left and right walls. */
internal const val BIN_LEFT = 5f
internal const val BIN_RIGHT = 19f

/** The corner radius at the base, from the arcs in upstream's path. */
internal const val BIN_CORNER = 2f

/** Where the walls start when the bin is shut. */
internal const val BIN_WALL_TOP = 6f

/** And when it is open, which is what makes the bin appear to sink as the lid lifts. */
internal const val BIN_WALL_TOP_OPEN = 13.5f

/** How far the lid swings, in degrees. Negative, because it opens backwards. */
internal const val BIN_LID_OPEN = -35f

/** The hinge the lid turns about, at the back left of the rim, as a fraction of the box. */
internal val BinLidHinge: Offset = Offset(3f / BIN_BOX, 6f / BIN_BOX)

/**
 * How tall the walls are for a given starting height.
 *
 * @param top where the walls start, in the icon's own box.
 * @return the height from there down to the base.
 */
internal fun binWallHeight(top: Float): Float = BIN_BASE - top

/**
 * Builds the walls of the bin, drawn from a given height down.
 *
 * Upstream's template is `M19 {top}v{20 - top}a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V{top}`: down
 * the right wall, round the bottom right corner, across the base, round the bottom left,
 * and back up the left wall.
 *
 * @param top where the walls start, in the icon's own box.
 * @param into the path to rebuild, so a frame allocates nothing.
 */
internal fun binWallsPath(
    top: Float,
    into: Path,
) {
    into.reset()
    into.moveTo(BIN_RIGHT, top)
    into.lineTo(BIN_RIGHT, BIN_BASE - BIN_CORNER)
    into.arcTo(
        rect =
            Rect(
                BIN_RIGHT - 2 * BIN_CORNER,
                BIN_BASE - 2 * BIN_CORNER,
                BIN_RIGHT,
                BIN_BASE,
            ),
        startAngleDegrees = 0f,
        sweepAngleDegrees = 90f,
        forceMoveTo = false,
    )
    into.lineTo(BIN_LEFT + BIN_CORNER, BIN_BASE)
    into.arcTo(
        rect =
            Rect(
                BIN_LEFT,
                BIN_BASE - 2 * BIN_CORNER,
                BIN_LEFT + 2 * BIN_CORNER,
                BIN_BASE,
            ),
        startAngleDegrees = 90f,
        sweepAngleDegrees = 90f,
        forceMoveTo = false,
    )
    into.lineTo(BIN_LEFT, top)
}

/** The lid, the tick and the cross, parsed once from upstream's own path data. */
internal class BinOutlines {
    /** The rim across the top and the handle above it. */
    val lid: Path =
        PathParser().parsePathString("M3 6h18M8 6V4a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2").toPath()

    /** The tick that replaces the bin once something has been deleted. */
    val tick: Path = PathParser().parsePathString("M4 12.5 9.5 18 20 7").toPath()

    /** The cross on the cancel button. */
    val cross: Path = PathParser().parsePathString("M6 6 18 18M18 6 6 18").toPath()

    /** Scratch geometry the walls are rebuilt into each frame. */
    val walls: Path = Path()

    /** And the part of the tick that has been drawn so far. */
    val drawnTick: Path = Path()
    private val measure = PathMeasure()

    /**
     * The first [fraction] of the tick.
     *
     * @param fraction how much of it to take, in `0..1`.
     * @return the partial path, rebuilt in place.
     */
    fun tickUpTo(fraction: Float): Path {
        drawnTick.reset()
        if (fraction <= 0f) return drawnTick
        measure.setPath(tick, false)
        measure.getSegment(0f, measure.length * fraction.coerceAtMost(1f), drawnTick, true)
        return drawnTick
    }
}
