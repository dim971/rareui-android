/*
 * DotArc.kt
 * The path BounceSidebar's dot takes, ported from the `arc()` path upstream hands to Motion
 * in `components/ui/bounce-sidebar.tsx`.
 */

package io.github.dim971.rareui.components.navigation

import kotlin.math.abs
import kotlin.math.min

/**
 * How far sideways the dot swings on its way across a given distance.
 *
 * Upstream asks Motion for `arc({ strength: min(0.8, 14 / distance) })`. The cap only bites
 * on very short hops; everywhere else the strength is inversely proportional to the
 * distance, so `strength * distance` is a constant 14 and the swing is the same size
 * whether the dot is moving one row or ten. That is the point of it: the arc is a flourish,
 * not a measure of how far the dot has come.
 *
 * @param span the signed distance the dot is travelling, positive downward.
 * @return the control point's sideways offset. Half of it is the widest the swing gets.
 */
internal fun bounceDotArcOffset(span: Double): Double {
    val distance = abs(span)
    if (!(distance > 0) || !distance.isFinite()) return 0.0
    val strength = min(0.8, 14 / distance)

    // Upstream turns counter-clockwise going down and clockwise going up, which are the
    // same curve travelled in opposite directions: the dot retraces its own path rather
    // than swinging out on the other side coming back. Both put the swing on the left,
    // away from the labels.
    return -strength * distance
}

/**
 * Where the dot sits sideways, given how far down the journey it has got.
 *
 * The vertical position is the animated value and the sideways offset is derived from it.
 * That works because the arc is a quadratic curve whose control point sits level with the
 * middle of the journey: the vertical component is then exactly linear in the curve's own
 * parameter, so the parameter can be recovered from the height alone.
 *
 * @param y where the dot currently is.
 * @param start the top of the arc's journey.
 * @param end the bottom of it.
 * @return the sideways offset to draw the dot at.
 */
internal fun bounceDotSideways(
    y: Double,
    start: Double,
    end: Double,
): Double {
    val span = end - start
    // Clamped because an interruption can leave the dot momentarily outside the arc it is
    // being measured against, and an unclamped parameter would throw the swing the wrong
    // way for a frame.
    val progress = if (abs(span) < 1e-9) 1.0 else ((y - start) / span).coerceIn(0.0, 1.0)
    return 2 * progress * (1 - progress) * bounceDotArcOffset(span)
}
