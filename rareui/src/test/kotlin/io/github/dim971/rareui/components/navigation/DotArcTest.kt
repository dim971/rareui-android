/*
 * DotArcTest.kt
 * The dot's arc, checked against the `arc()` path upstream hands to Motion in
 * `components/ui/bounce-sidebar.tsx`. The same assertions run on iOS.
 */

package io.github.dim971.rareui.components.navigation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs

class DotArcTest {
    @Test
    fun `the swing is the same size whatever the distance, once past the cap`() {
        // strength is 14 / distance, so strength * distance is 14 for every hop long enough
        // that the 0.8 ceiling does not bite. The arc is a flourish, not a measure of how
        // far the dot has come.
        for (distance in listOf(20.0, 40.0, 80.0, 200.0, 1000.0)) {
            assertEquals(14.0, abs(bounceDotArcOffset(distance)), 1e-9)
        }
    }

    @Test
    fun `a very short hop is capped, so the dot does not swing further than it travels`() {
        // The cap bites below 17.5 points, which is where 14 / distance passes 0.8.
        assertEquals(0.8 * 10, abs(bounceDotArcOffset(10.0)), 1e-9)
        assertEquals(0.8 * 4, abs(bounceDotArcOffset(4.0)), 1e-9)
        assertEquals(14.0, abs(bounceDotArcOffset(17.5)), 1e-9)
    }

    @Test
    fun `the swing is on the same side going up as going down, so the dot retraces its path`() {
        // Upstream turns counter-clockwise going down and clockwise going up, which are the
        // same curve travelled in opposite directions.
        assertEquals(bounceDotArcOffset(60.0), bounceDotArcOffset(-60.0), 0.0)
        assertTrue("the swing is to the left, away from the labels", bounceDotArcOffset(60.0) < 0)
    }

    @Test
    fun `a dot that is not going anywhere does not swing`() {
        assertEquals(0.0, bounceDotArcOffset(0.0), 0.0)
        assertEquals(0.0, bounceDotArcOffset(Double.NaN), 0.0)
        assertEquals(0.0, bounceDotArcOffset(Double.POSITIVE_INFINITY), 0.0)
    }

    @Test
    fun `the swing peaks halfway across and is nothing at either end`() {
        val span = 100.0
        val widest = abs(bounceDotArcOffset(span)) / 2

        assertEquals(0.0, bounceDotSideways(0.0, 0.0, span), 1e-9)
        assertEquals(0.0, bounceDotSideways(span, 0.0, span), 1e-9)
        assertEquals(widest, abs(bounceDotSideways(span / 2, 0.0, span)), 1e-9)
        assertEquals(
            bounceDotSideways(span * 0.25, 0.0, span),
            bounceDotSideways(span * 0.75, 0.0, span),
            1e-9,
        )
    }

    @Test
    fun `the dot is measured against the arc it is on even when it is dragged off the end`() {
        // An interruption can leave the dot outside the journey it is being measured
        // against. Unclamped, the parameter would go negative and throw the swing out to
        // the other side for a frame.
        assertEquals(0.0, bounceDotSideways(-40.0, 0.0, 100.0), 1e-9)
        assertEquals(0.0, bounceDotSideways(140.0, 0.0, 100.0), 1e-9)
    }

    @Test
    fun `a journey of no distance leaves the dot straight`() {
        assertEquals(0.0, bounceDotSideways(50.0, 50.0, 50.0), 0.0)
    }
}
