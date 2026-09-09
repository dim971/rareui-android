/*
 * Motion.kt
 * The bridge between Motion for React, which every upstream component animates with, and
 * Compose. Unlike SwiftUI, Compose does not take a spring the way Motion states one, so
 * the conversion is written down here once rather than at each call site. Converting by
 * hand in nineteen components is how a port drifts.
 *
 *   Motion `{ stiffness, damping, mass }`  ->  rareUiSpring(stiffness, damping, mass)
 *   Motion `{ visualDuration, bounce }`    ->  rareUiVisualSpring(duration, bounce)

 * The two have separate names on purpose. As overloads of one name they were both
 * applicable to two positional floats, Kotlin quietly resolved to the shorter one, and a
 * spring of 500 and 34 became a five hundred second duration that crawled. Two names
 * cannot be confused for one another by an overload rule.
 *   Motion `ease: [a, b, c, d]`            ->  CubicBezierEasing(a, b, c, d)
 *
 * Component specific constants do not live here. They live next to the component that
 * uses them, with a comment naming the upstream file they came from, so a number can be
 * checked against the original in one place. What lives here is the handful of curves
 * upstream reaches for again and again.
 */

package io.github.dim971.rareui.theme

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.SpringSpec
import androidx.compose.animation.core.spring
import kotlin.math.PI
import kotlin.math.sqrt

/**
 * A spring stated the way Motion for React states one.
 *
 * Motion gives a damping **coefficient**; Compose wants a damping **ratio**, which is that
 * coefficient over twice the root of the stiffness times the mass. Compose also assumes a
 * mass of one, so a heavier body is expressed by dividing the stiffness instead: the two
 * together leave the frequency and the decay exactly where Motion put them.
 *
 * @param stiffness the spring constant, as Motion states it.
 * @param damping the damping coefficient, as Motion states it.
 * @param mass the mass of the animated body. Motion's own default is one.
 * @param visibilityThreshold how close counts as arrived.
 */
public fun <T> rareUiSpring(
    stiffness: Float,
    damping: Float,
    mass: Float = 1f,
    visibilityThreshold: T? = null,
): SpringSpec<T> =
    spring(
        dampingRatio = damping / (2f * sqrt(stiffness * mass)),
        stiffness = stiffness / mass,
        visibilityThreshold = visibilityThreshold,
    )

/**
 * A spring stated the way Motion's newer form states one.
 *
 * The duration is perceptual rather than literal: it is how long the movement reads as
 * taking, not when the last thousandth of it settles. Bounce runs from zero, which does
 * not overshoot at all, to one, which never stops.
 *
 * @param durationSeconds the perceptual duration.
 * @param bounce how much it overshoots, in `0..1`.
 * @param visibilityThreshold how close counts as arrived.
 */
public fun <T> rareUiVisualSpring(
    durationSeconds: Float,
    bounce: Float,
    visibilityThreshold: T? = null,
): SpringSpec<T> {
    // Motion's own conversion: one full undamped cycle in the given duration, and a
    // damping ratio that is the complement of the bounce.
    val undampedFrequency = (2.0 * PI / durationSeconds).toFloat()
    return spring(
        dampingRatio = (1f - bounce).coerceIn(0.01f, 1f),
        stiffness = undampedFrequency * undampedFrequency,
        visibilityThreshold = visibilityThreshold,
    )
}

/** The easing curves upstream reaches for repeatedly. */
public object RareUiEasing {
    /**
     * `cubic-bezier(0.22, 1, 0.36, 1)`, upstream's most used ease out.
     *
     * It leaves fast and arrives slowly. Used by the contribution grid's cells, the
     * counter's exit, and the scroll indicator's label crossfade.
     */
    public val EaseOutQuint: Easing = CubicBezierEasing(0.22f, 1f, 0.36f, 1f)

    /**
     * `cubic-bezier(0.32, 0.72, 0, 1)`, the long settle.
     *
     * Used where something changes size rather than position: the step player's crossfade
     * and the delete button's width.
     */
    public val EaseOutSettle: Easing = CubicBezierEasing(0.32f, 0.72f, 0f, 1f)

    /**
     * `cubic-bezier(0.34, 1.1, 0.64, 1)`, an ease out that overshoots.
     *
     * The second control point sits above one on purpose. This is what throws the bin lid
     * past its open angle before it comes back.
     */
    public val EaseOutOvershoot: Easing = CubicBezierEasing(0.34f, 1.1f, 0.64f, 1f)

    /** `cubic-bezier(0.215, 0.61, 0.355, 1)`, the landing squash. */
    public val EaseOutLanding: Easing = CubicBezierEasing(0.215f, 0.61f, 0.355f, 1f)

    /** `cubic-bezier(0.65, 0, 0.35, 1)`, a symmetric ease in and out. */
    public val EaseInOut: Easing = CubicBezierEasing(0.65f, 0f, 0.35f, 1f)

    /** `cubic-bezier(0.45, 0, 0.55, 1)`, the shimmer's gentler ease in and out. */
    public val EaseInOutShimmer: Easing = CubicBezierEasing(0.45f, 0f, 0.55f, 1f)

    /** `cubic-bezier(0.4, 0.3, 0.5, 1)`, the emoji particles' rise. */
    public val EaseParticle: Easing = CubicBezierEasing(0.4f, 0.3f, 0.5f, 1f)
}
