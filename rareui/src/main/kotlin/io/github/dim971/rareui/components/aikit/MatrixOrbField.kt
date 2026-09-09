/*
 * MatrixOrbField.kt
 * The maths behind MatrixOrb, ported from the `envelope` and `intensityOf` functions and
 * the frame loop in upstream's `components/ui/matrix-orb.tsx`.
 *
 * All of it is pure: given a state, a dot's position and a time, it says how bright that
 * dot should be. The composable does nothing but paint the answers. The same claims are
 * asserted in the iOS twin.
 */

package io.github.dim971.rareui.components.aikit

import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.sin

/** What the orb is doing. */
public enum class MatrixOrbState {
    /** Waiting. A slow breath travels out from the middle. */
    IDLE,

    /** Taking something in. Rings ripple outward in time with the level. */
    LISTENING,

    /** Working on something. Three hot spots orbit under the dots. */
    THINKING,
}

/**
 * The size the orb settles at in each state.
 *
 * Idle sits back, listening comes forward, thinking is between the two. The change is
 * carried by a spring rather than a transition, so the orb overshoots slightly on its way
 * between them.
 */
internal fun matrixOrbScale(state: MatrixOrbState): Double =
    when (state) {
        MatrixOrbState.IDLE -> 0.88
        MatrixOrbState.LISTENING -> 1.0
        MatrixOrbState.THINKING -> 0.92
    }

/** The spring that carries the orb between its resting sizes. */
internal const val MATRIX_ORB_STIFFNESS: Double = 180.0

/** The damping on that spring. */
internal const val MATRIX_ORB_DAMPING: Double = 26.0

/** How fast the amplitude follows a level that is rising. */
internal const val MATRIX_ORB_ATTACK: Double = 0.22

/**
 * How fast it follows one that is falling. Slower than the attack, so the orb holds a peak
 * for a moment rather than snapping back the instant a sound stops.
 */
internal const val MATRIX_ORB_RELEASE: Double = 0.08

/** How fast one state's influence gives way to another's, per sixtieth of a second. */
internal const val MATRIX_ORB_BLEND: Double = 0.16

/** One of the three hot spots that circle under the dots while the orb is thinking. */
internal data class MatrixOrbOrbiter(
    /** How far from the middle it orbits, in normalised units where the edge is one. */
    val radius: Double,
    /** How fast it goes round, in radians per second. A negative speed goes the other way. */
    val speed: Double,
    /** Where on its circle it starts. */
    val phase: Double,
    /** How wide its glow is. */
    val spread: Double,
)

/**
 * The three orbiters, at radii, speeds and phases that do not share a common period.
 *
 * That is the point of them: with one turning backward and none of the three in step, the
 * pattern never visibly repeats, so the orb reads as thinking rather than looping.
 */
internal val MatrixOrbOrbiters: List<MatrixOrbOrbiter> =
    listOf(
        MatrixOrbOrbiter(radius = 0.62, speed = 2.2, phase = 0.0, spread = 0.42),
        MatrixOrbOrbiter(radius = 0.4, speed = -1.7, phase = 2.1, spread = 0.36),
        MatrixOrbOrbiter(radius = 0.8, speed = 1.15, phase = 4.0, spread = 0.34),
    )

/**
 * A stand in for a microphone level, for when the caller has no real one to give.
 *
 * Two sine waves of unrelated periods, multiplied. Upstream notes that taking the absolute
 * value of a single wave instead would put a corner at every trough, and a corner reads as
 * a snap rather than as breathing.
 *
 * @param time seconds since the orb started animating.
 * @return a level in `0.22..1`.
 */
internal fun matrixOrbEnvelope(time: Double): Double {
    val slow = 0.5 + 0.5 * sin(time * 0.62 + 0.4)
    val fast = 0.5 + 0.5 * sin(time * 1.9 + 1.1)
    return 0.22 + 0.78 * (0.45 + 0.55 * slow) * fast
}

/**
 * How bright one dot should be, for one state, at one moment.
 *
 * @param state the state being asked about, which is not necessarily the orb's current
 *   one: while it changes, every state that still has weight is asked and the answers are
 *   mixed.
 * @param distance the dot's distance from the middle, where the edge of the circle is one.
 * @param normalisedX the dot's x position, in `-1..1`.
 * @param normalisedY the dot's y position, in `-1..1`.
 * @param time seconds since the orb started animating.
 * @param amplitude the smoothed level, in `0..1`.
 * @return a brightness, before it is confined to `0..1`.
 */
internal fun matrixOrbIntensity(
    state: MatrixOrbState,
    distance: Double,
    normalisedX: Double,
    normalisedY: Double,
    time: Double,
    amplitude: Double,
): Double =
    when (state) {
        MatrixOrbState.LISTENING -> {
            // A ring travelling outward: the phase depends on distance, so the whole field
            // does not pulse at once.
            val ripple = 0.5 + 0.5 * sin(distance * 4.2 - time * 3)
            0.32 + amplitude * (0.34 + 0.38 * ripple)
        }

        MatrixOrbState.THINKING -> {
            var heat = 0.0
            MatrixOrbOrbiters.forEach { orbiter ->
                val angle = time * orbiter.speed + orbiter.phase
                val dx = normalisedX - cos(angle) * orbiter.radius
                val dy = normalisedY - sin(angle) * orbiter.radius
                heat += exp(-(dx * dx + dy * dy) / (orbiter.spread * orbiter.spread))
            }
            0.26 + 0.8 * minOf(1.0, heat)
        }

        // A slow breath, travelling inward rather than outward.
        MatrixOrbState.IDLE -> 0.62 + 0.12 * sin(time * 1.05 - distance * 2.4)
    }

/**
 * How much of each state is currently showing.
 *
 * The orb does not switch states, it crossfades between them, so interrupting a change
 * halfway blends from whatever is on screen rather than from where the change started.
 */
internal data class MatrixOrbWeights(
    var idle: Double,
    var listening: Double,
    var thinking: Double,
) {
    internal operator fun get(state: MatrixOrbState): Double =
        when (state) {
            MatrixOrbState.IDLE -> idle
            MatrixOrbState.LISTENING -> listening
            MatrixOrbState.THINKING -> thinking
        }

    private operator fun set(
        state: MatrixOrbState,
        value: Double,
    ) {
        when (state) {
            MatrixOrbState.IDLE -> idle = value
            MatrixOrbState.LISTENING -> listening = value
            MatrixOrbState.THINKING -> thinking = value
        }
    }

    /**
     * Moves each weight a step toward showing only [state].
     *
     * @param state the state being moved toward.
     * @param step how far to move, in `0..1`.
     */
    internal fun blendToward(
        state: MatrixOrbState,
        step: Double,
    ) {
        MatrixOrbState.entries.forEach { candidate ->
            this[candidate] += ((if (candidate == state) 1.0 else 0.0) - this[candidate]) * step
        }
    }

    internal companion object {
        internal fun showing(state: MatrixOrbState): MatrixOrbWeights =
            MatrixOrbWeights(
                idle = if (state == MatrixOrbState.IDLE) 1.0 else 0.0,
                listening = if (state == MatrixOrbState.LISTENING) 1.0 else 0.0,
                thinking = if (state == MatrixOrbState.THINKING) 1.0 else 0.0,
            )
    }
}

/** Everything the painter needs for one frame. */
internal data class MatrixOrbFrame(
    val time: Double,
    val amplitude: Double,
    val scale: Double,
    val weights: MatrixOrbWeights,
)

/** Two pi, which the dots are drawn around. */
internal const val MATRIX_ORB_TAU: Double = 2 * PI
