/*
 * RareUiSpringStateTest.kt
 * The hand integrated spring the bell swings on. The same assertions run on iOS.
 */

package io.github.dim971.rareui.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs
import kotlin.math.sign

class RareUiSpringStateTest {
    private val frame = 1.0 / 60

    @Test
    fun `a spring at its target and standing still stays there`() {
        val spring = RareUiSpringState()
        spring.advance(0.0, frame, stiffness = 220.0, damping = 10.0)
        assertEquals(0.0, spring.value, 0.0)
        assertEquals(0.0, spring.velocity, 0.0)
    }

    @Test
    fun `a spring given a velocity travels and comes back`() {
        val spring = RareUiSpringState(velocity = 500.0)
        var furthest = 0.0
        repeat(600) {
            spring.advance(0.0, frame, stiffness = 220.0, damping = 10.0)
            furthest = maxOf(furthest, abs(spring.value))
        }
        assertTrue("it should have swung somewhere", furthest > 10)
        assertTrue("and come back to rest inside ten seconds", spring.hasSettled(0.0))
    }

    @Test
    fun `the bell's own spring is underdamped, which is why it keeps swinging`() {
        // Critical damping for a stiffness of 220 is about 29.7, and upstream uses 10, well
        // under it. That is the whole character of the component.
        val spring = RareUiSpringState(velocity = 400.0)
        var crossings = 0
        var previous = spring.value
        repeat(300) {
            spring.advance(0.0, frame, stiffness = 220.0, damping = 10.0)
            if (previous.sign != spring.value.sign && spring.value != 0.0) crossings++
            previous = spring.value
        }
        assertTrue("it should cross the middle several times, not stop dead", crossings > 3)
    }

    @Test
    fun `a step of no time changes nothing`() {
        val spring = RareUiSpringState(value = 5.0, velocity = 100.0)
        spring.advance(0.0, 0.0, stiffness = 220.0, damping = 10.0)
        assertEquals(5.0, spring.value, 0.0)
        assertEquals(100.0, spring.velocity, 0.0)
    }
}
