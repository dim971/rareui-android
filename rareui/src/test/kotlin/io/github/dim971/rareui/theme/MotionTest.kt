/*
 * MotionTest.kt
 * The conversion from the way Motion for React states a spring to the way Compose does.
 * The numbers are worked by hand from the definitions, so the test is a second derivation
 * rather than a recording of whatever the code happened to produce.
 */

package io.github.dim971.rareui.theme

import org.junit.Assert.assertEquals
import org.junit.Test
import kotlin.math.PI

class MotionTest {
    @Test
    fun `a Motion spring becomes a damping ratio and the same stiffness`() {
        // 34 / (2 * sqrt(500)) = 0.7603
        val spring = rareUiSpring<Float>(stiffness = 500f, damping = 34f)
        assertEquals(0.76026f, spring.dampingRatio, 1e-4f)
        assertEquals(500f, spring.stiffness, 0f)
    }

    @Test
    fun `a heavier body is expressed by dividing the stiffness, since Compose assumes a mass of one`() {
        // 34 / (2 * sqrt(420 * 0.7)) = 0.9915, and the stiffness Compose sees is 420 / 0.7.
        val spring = rareUiSpring<Float>(stiffness = 420f, damping = 34f, mass = 0.7f)
        assertEquals(0.99147f, spring.dampingRatio, 1e-4f)
        assertEquals(600f, spring.stiffness, 1e-3f)
    }

    @Test
    fun `two positional floats are a stiffness and a damping, not a duration and a bounce`() {
        // This is the whole reason the two conversions have separate names. As overloads
        // both were applicable here, Kotlin resolved to the shorter signature, and a spring
        // of 500 and 34 became a five hundred second duration that never visibly arrived.
        val spring = rareUiSpring<Float>(500f, 34f)
        assertEquals(500f, spring.stiffness, 0f)
    }

    @Test
    fun `a visual duration becomes one undamped cycle of that length`() {
        val spring = rareUiVisualSpring<Float>(durationSeconds = 0.6f, bounce = 0.18f)
        val frequency = (2.0 * PI / 0.6).toFloat()
        assertEquals(frequency * frequency, spring.stiffness, 1e-3f)
    }

    @Test
    fun `bounce is the complement of the damping ratio`() {
        assertEquals(0.82f, rareUiVisualSpring<Float>(0.6f, 0.18f).dampingRatio, 1e-6f)
        assertEquals(1f, rareUiVisualSpring<Float>(0.6f, 0f).dampingRatio, 1e-6f)
    }

    @Test
    fun `a bounce outside its range is brought back into it rather than producing a spring that never stops`() {
        // A damping ratio of zero oscillates for ever and a negative one grows without
        // bound, so both ends are clamped.
        assertEquals(0.01f, rareUiVisualSpring<Float>(0.6f, 2f).dampingRatio, 1e-6f)
        assertEquals(1f, rareUiVisualSpring<Float>(0.6f, -1f).dampingRatio, 1e-6f)
    }
}
