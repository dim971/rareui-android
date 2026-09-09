/*
 * HookRail.kt
 * One of HookSidebar's two rails, ported from the `Rail` component in upstream's
 * `components/ui/hook-sidebar.tsx`.
 *
 * A hairline running down the gutter, stopping a corner's radius short of the row it is
 * pointing at, and a quarter-round hook turning out of it toward that row.
 */

package io.github.dim971.rareui.components.navigation

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import kotlin.math.max

/**
 * Where the fainter rail starts.
 *
 * Below the active row the two rails read as one line reaching further, so the faint one
 * starts where the accent one stops. Above it the accent rail already covers the whole
 * span, so there is nothing to draw but the hook itself, and the faint rail starts a corner
 * short of the row it is pointing at.
 *
 * @param activeY the middle of the selected row, or `null` if there is not one.
 * @param hoverY the middle of the row being pointed at, or `null` if none is.
 * @param corner the hook's radius.
 * @return where the faint rail starts, measured from the top of the list.
 */
internal fun hookGhostRailStart(
    activeY: Double?,
    hoverY: Double?,
    corner: Double,
): Double {
    if (activeY == null || hoverY == null) return activeY ?: 0.0
    return if (hoverY <= activeY) max(0.0, hoverY - corner) else activeY
}

/**
 * How much hairline there is to draw between where a rail starts and the row it points at.
 *
 * The last stretch belongs to the hook, which covers it on its way round, so the line stops
 * a corner short. A rail with nowhere to go draws nothing rather than a negative length.
 *
 * @param from where the rail starts.
 * @param to the middle of the row it points at.
 * @param corner the hook's radius.
 * @return the length of the straight part, never below zero.
 */
internal fun hookRailLength(
    from: Double,
    to: Double,
    corner: Double,
): Double = max(0.0, to - corner - from)

/**
 * Draws one rail: the hairline, then the hook turning out of it.
 *
 * Upstream draws the line as a repeating gradient rather than a stroke, which comes to the
 * same thing: two points of ink, two points of nothing, all the way down.
 *
 * @param from where the rail starts, from the top of the list.
 * @param to the middle of the row it points at.
 * @param gutter how far in from the leading edge the rail runs.
 * @param corner the hook's radius.
 * @param reach how far the hook carries on toward the label after it has turned.
 * @param color the rail's colour.
 * @param alpha how much of it is showing.
 * @param dashed whether it is drawn as dashes.
 * @param hairline the stroke width.
 * @param dash the dash pattern.
 * @param path scratch geometry, reused between frames so the draw pass allocates nothing.
 */
@Suppress("LongParameterList")
internal fun DrawScope.drawHookRail(
    from: Float,
    to: Float,
    gutter: Float,
    corner: Float,
    reach: Float,
    color: Color,
    alpha: Float,
    dashed: Boolean,
    hairline: Float,
    dash: FloatArray,
    path: Path,
) {
    if (alpha <= 0f) return
    val stroke =
        Stroke(
            width = hairline,
            pathEffect = if (dashed) PathEffect.dashPathEffect(dash) else null,
        )

    val length = hookRailLength(from.toDouble(), to.toDouble(), corner.toDouble()).toFloat()
    if (length > 0f) {
        drawLine(
            color = color,
            start = Offset(gutter, from),
            end = Offset(gutter, from + length),
            strokeWidth = hairline,
            pathEffect = stroke.pathEffect,
            alpha = alpha,
        )
    }

    // Upstream's `M0.5 0a6 6 0 0 0 6 6H12`: round a quarter circle of the corner's radius,
    // then out toward the label. The turn ends level with the row's middle, which is why it
    // begins a corner above it.
    val top = to - corner
    path.reset()
    path.moveTo(gutter, top)
    path.arcTo(
        rect = Rect(gutter, top - corner, gutter + 2 * corner, top + corner),
        startAngleDegrees = 180f,
        // Negative because the arc is travelled the short way round, from pointing left to
        // pointing down, which on a canvas whose y grows downward is anticlockwise.
        sweepAngleDegrees = -90f,
        forceMoveTo = false,
    )
    path.lineTo(gutter + reach, to)
    drawPath(path = path, color = color, alpha = alpha, style = stroke)
}
