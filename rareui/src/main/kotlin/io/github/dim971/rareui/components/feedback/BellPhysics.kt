/*
 * BellPhysics.kt
 * The arithmetic of ringing a bell, from upstream's `components/ui/notification-bell.tsx`.
 * Ringing is a push rather than a destination, and this is the push.
 */

package io.github.dim971.rareui.components.feedback

import kotlin.math.min
import kotlin.math.sqrt

/** How hard one notification pushes, in degrees per second. */
internal const val BELL_IMPULSE = 500.0

/** However many arrive at once, the bell is never pushed harder than this. */
internal const val BELL_MAX_VELOCITY = 900.0

/** The number of notifications at which the push stops growing. */
internal const val BELL_BURST = 5.0

/** How far the clapper trails the bell at full tilt, in degrees. */
internal const val BELL_CLAPPER_SWEEP = 13.0

/** The bell speed at which it reaches that. */
internal const val BELL_CLAPPER_VELOCITY = 450.0

/**
 * The speed to give the bell when notifications arrive.
 *
 * The push is added to whatever the bell is already doing, in the direction it is already
 * going, so a second notification arriving mid swing adds to the ring rather than fighting
 * it. More arriving at once pushes harder, up to a point: five is as hard as it gets, and
 * the result is capped so the bell cannot be made to spin.
 *
 * @param current how fast the bell is going now, in degrees per second.
 * @param delta how many notifications arrived at once.
 * @return the bell's new speed.
 */
internal fun bellRingVelocity(
    current: Double,
    delta: Int,
): Double {
    val weight = 0.7 + 0.6 * min(delta.toDouble(), BELL_BURST) / BELL_BURST
    // At rest the direction is taken as negative, so the first ring swings one way rather
    // than depending on a velocity of exactly zero.
    val along = if (current > 1) 1.0 else -1.0
    val pushed = current + along * BELL_IMPULSE * weight
    return pushed.coerceIn(-BELL_MAX_VELOCITY, BELL_MAX_VELOCITY)
}

/**
 * Where the clapper is trying to be, given how fast the bell is going.
 *
 * It is driven by the bell's speed rather than by its position, which is what makes it
 * trail behind rather than move with it: the bell is fastest at the bottom of its swing,
 * which is exactly where the clapper is furthest out.
 *
 * @param swingVelocity the bell's speed, in degrees per second.
 * @return the clapper's angle relative to the bell, in degrees.
 */
internal fun bellClapperLag(swingVelocity: Double): Double =
    -BELL_CLAPPER_SWEEP * (swingVelocity / BELL_CLAPPER_VELOCITY).coerceIn(-1.0, 1.0)

/**
 * How far the badge is inset from the button's corner.
 *
 * Placing it on a circle rather than at a fixed offset keeps it in the same relation to the
 * button's edge whatever the size. A negative result means it hangs outside, which is where
 * it usually sits.
 *
 * @param size the button's width and height.
 * @param side the badge's own height.
 * @param orbit how far out to place it. One puts it exactly on the button's edge.
 * @return the inset from the top and from the trailing edge.
 */
internal fun bellBadgeInset(
    size: Double,
    side: Double,
    orbit: Double,
): Double = size / 2 - (orbit * size * sqrt(0.5)) / 2 - side / 2
