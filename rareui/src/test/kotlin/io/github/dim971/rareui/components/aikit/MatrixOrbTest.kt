/*
 * MatrixOrbTest.kt
 * The orb is a field function painted once per frame, so the field is what is worth
 * testing: its range, its continuity, and the fact that a state change crossfades rather
 * than cutting. The same claims are asserted in the iOS twin.
 */

package io.github.dim971.rareui.components.aikit

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs
import kotlin.math.sqrt

class MatrixOrbEnvelopeTest {
    @Test
    fun `the synthesised level stays inside the range upstream's constants allow`() {
        for (step in 0..4000) {
            val level = matrixOrbEnvelope(step / 100.0)
            assertTrue("fell below the floor at $step", level >= 0.22 - 1e-12)
            assertTrue("went above one at $step", level <= 1 + 1e-12)
        }
    }

    @Test
    fun `it never has a corner, which is why it reads as breathing rather than snapping`() {
        // Upstream chose two multiplied sines over the absolute value of one precisely to
        // avoid a corner at each trough. A corner shows up as a step change in the slope.
        var previous: Double? = null
        val step = 0.001
        for (index in 0 until 20000) {
            val time = index * step
            val slope = (matrixOrbEnvelope(time + step) - matrixOrbEnvelope(time)) / step
            previous?.let { assertTrue("the slope jumped at t = $time", abs(slope - it) < 0.01) }
            previous = slope
        }
    }
}

class MatrixOrbIntensityTest {
    @Test
    fun `idle breathes around a constant, within the amount upstream allows`() {
        for (step in 0..2000) {
            val value =
                matrixOrbIntensity(
                    MatrixOrbState.IDLE,
                    distance = 0.5,
                    normalisedX = 0.5,
                    normalisedY = 0.0,
                    time = step / 50.0,
                    amplitude = 1.0,
                )
            assertTrue(abs(value - 0.62) <= 0.12 + 1e-12)
        }
    }

    @Test
    fun `idle ignores the level, because there is nothing to listen to`() {
        val quiet = matrixOrbIntensity(MatrixOrbState.IDLE, 0.3, 0.3, 0.0, 1.0, 0.0)
        val loud = matrixOrbIntensity(MatrixOrbState.IDLE, 0.3, 0.3, 0.0, 1.0, 1.0)
        assertEquals(quiet, loud, 0.0)
    }

    @Test
    fun `listening sits at its floor in silence and rises with the level`() {
        val silent = matrixOrbIntensity(MatrixOrbState.LISTENING, 0.4, 0.4, 0.0, 2.0, 0.0)
        assertEquals(0.32, silent, 1e-12)

        val loud = matrixOrbIntensity(MatrixOrbState.LISTENING, 0.4, 0.4, 0.0, 2.0, 1.0)
        assertTrue(loud > silent)
        // 0.32 plus at most 0.34 + 0.38.
        assertTrue(loud <= 1.04 + 1e-12)
    }

    @Test
    fun `listening ripples outward rather than pulsing all at once`() {
        // The phase depends on distance, so two dots at different radii are not in step.
        val near = matrixOrbIntensity(MatrixOrbState.LISTENING, 0.2, 0.2, 0.0, 1.0, 1.0)
        val far = matrixOrbIntensity(MatrixOrbState.LISTENING, 0.9, 0.9, 0.0, 1.0, 1.0)
        assertNotEquals(near, far, 1e-9)
    }

    @Test
    fun `thinking is brightest where an orbiter is and dimmest far from all three`() {
        // At t = 0 the first orbiter sits at (0.62, 0), being at phase 0 and radius 0.62.
        val onTop = matrixOrbIntensity(MatrixOrbState.THINKING, 0.62, 0.62, 0.0, 0.0, 1.0)
        val away = matrixOrbIntensity(MatrixOrbState.THINKING, 1.0, -0.7, -0.7, 0.0, 1.0)
        assertTrue(onTop > away)
        assertTrue(away >= 0.26)
        // 0.26 plus 0.8 of a heat that is itself capped at one.
        assertTrue(onTop <= 1.06 + 1e-12)
    }
}

class MatrixOrbWeightsTest {
    @Test
    fun `a fresh set of weights shows exactly one state`() {
        val weights = MatrixOrbWeights.showing(MatrixOrbState.LISTENING)
        assertEquals(1.0, weights.listening, 0.0)
        assertEquals(0.0, weights.idle, 0.0)
        assertEquals(0.0, weights.thinking, 0.0)
    }

    @Test
    fun `blending converges on the state it is aimed at`() {
        val weights = MatrixOrbWeights.showing(MatrixOrbState.IDLE)
        // A second of sixtieths, at upstream's blend rate.
        repeat(60) { weights.blendToward(MatrixOrbState.THINKING, MATRIX_ORB_BLEND) }
        assertTrue(weights.thinking > 0.99)
        assertTrue(weights.idle < 0.01)
    }

    @Test
    fun `an interrupted change carries on from where it is, not from where it started`() {
        val weights = MatrixOrbWeights.showing(MatrixOrbState.IDLE)
        repeat(5) { weights.blendToward(MatrixOrbState.LISTENING, MATRIX_ORB_BLEND) }
        val partway = weights.listening
        assertTrue("the change should be mid flight", partway > 0 && partway < 1)

        // Changing course now: listening still has weight, and it decays from where it is
        // rather than being cut to zero.
        weights.blendToward(MatrixOrbState.THINKING, MATRIX_ORB_BLEND)
        assertTrue(weights.listening < partway)
        assertTrue(weights.listening > 0)
        assertTrue(weights.thinking > 0)
    }

    @Test
    fun `the weights always sum to one, so brightness does not dip during a change`() {
        val weights = MatrixOrbWeights.showing(MatrixOrbState.IDLE)
        repeat(120) { step ->
            val toward = if (step < 60) MatrixOrbState.LISTENING else MatrixOrbState.THINKING
            weights.blendToward(toward, MATRIX_ORB_BLEND)
            val total = weights.idle + weights.listening + weights.thinking
            assertTrue("the weights summed to $total", abs(total - 1) < 1e-9)
        }
    }
}

class MatrixOrbScaleTest {
    @Test
    fun `each state has its own resting size, and listening is the largest`() {
        assertEquals(0.88, matrixOrbScale(MatrixOrbState.IDLE), 0.0)
        assertEquals(1.0, matrixOrbScale(MatrixOrbState.LISTENING), 0.0)
        assertEquals(0.92, matrixOrbScale(MatrixOrbState.THINKING), 0.0)
        assertTrue(matrixOrbScale(MatrixOrbState.LISTENING) > matrixOrbScale(MatrixOrbState.THINKING))
        assertTrue(matrixOrbScale(MatrixOrbState.THINKING) > matrixOrbScale(MatrixOrbState.IDLE))
    }

    @Test
    fun `the spring is underdamped, so the orb overshoots on its way between sizes`() {
        // Critical damping for this spring would be twice the root of the stiffness, about
        // 26.8. Upstream's 26 is just below it, which is what gives the size change its
        // slight overshoot rather than a dead stop.
        val critical = 2 * sqrt(MATRIX_ORB_STIFFNESS)
        assertTrue(MATRIX_ORB_DAMPING < critical)
        assertTrue("it should be only just underdamped", MATRIX_ORB_DAMPING > critical * 0.9)
    }

    @Test
    fun `the amplitude rises faster than it falls`() {
        assertTrue(MATRIX_ORB_ATTACK > MATRIX_ORB_RELEASE)
    }
}
