/*
 * RareUiSpringState.kt
 * A spring integrated by hand.
 *
 * Compose's springs are excellent and are used everywhere in this library that they fit.
 * They do not fit in one place: when the spring's output feeds a draw call rather than a
 * view property, and its target never changes while its velocity does. The bell needs
 * exactly that, because ringing it is a push rather than a new destination, and its clapper
 * follows the bell's speed from one frame to the next rather than its angle.
 */

package io.github.dim971.rareui.core

import kotlin.math.abs

/** A damped spring, stepped one frame at a time. */
public class RareUiSpringState(
    /** Where the spring is. */
    public var value: Double = 0.0,
    /** How fast it is moving, in units per second. */
    public var velocity: Double = 0.0,
) {
    /**
     * Steps the spring toward a target.
     *
     * Semi-implicit Euler, which is what Motion itself uses: the velocity is updated first
     * and the position is then moved by the new velocity. It stays stable at frame rate
     * where the explicit form drifts outward.
     *
     * @param target where it is heading.
     * @param elapsed how long has passed, in seconds.
     * @param stiffness the spring constant, as Motion states it.
     * @param damping the damping coefficient, as Motion states it.
     * @param mass the mass of the body on the spring.
     */
    public fun advance(
        target: Double,
        elapsed: Double,
        stiffness: Double,
        damping: Double,
        mass: Double = 1.0,
    ) {
        if (elapsed <= 0 || mass <= 0) return
        velocity += (-stiffness * (value - target) - damping * velocity) / mass * elapsed
        value += velocity * elapsed
    }

    /**
     * Whether the spring has stopped moving in any way worth drawing.
     *
     * @param target where it was heading.
     * @param restDelta how close counts as arrived.
     * @return whether it has settled.
     */
    public fun hasSettled(
        target: Double,
        restDelta: Double = 0.01,
    ): Boolean = abs(value - target) < restDelta && abs(velocity) < restDelta * 10
}
