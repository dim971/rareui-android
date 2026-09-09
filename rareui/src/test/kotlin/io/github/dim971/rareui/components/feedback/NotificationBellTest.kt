/*
 * NotificationBellTest.kt
 * Ringing a bell is a push rather than a destination, and the arithmetic of that push is
 * the component. Checked against `components/ui/notification-bell.tsx`; the same assertions
 * run on iOS.
 */

package io.github.dim971.rareui.components.feedback

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs
import kotlin.math.sqrt

class BellRingTest {
    @Test
    fun `one notification pushes a bell at rest`() {
        // At rest the direction is taken as negative, so the first ring swings one way and
        // is not left dependent on a velocity of exactly zero.
        val velocity = bellRingVelocity(0.0, 1)
        assertTrue(velocity != 0.0)
        assertEquals(BELL_IMPULSE * (0.7 + 0.6 / BELL_BURST), abs(velocity), 1e-9)
    }

    @Test
    fun `more arriving at once pushes harder, up to five`() {
        val one = abs(bellRingVelocity(0.0, 1))
        val three = abs(bellRingVelocity(0.0, 3))
        val five = abs(bellRingVelocity(0.0, 5))
        val fifty = abs(bellRingVelocity(0.0, 50))

        assertTrue(one < three)
        assertTrue(three < five)
        assertEquals("past five the push stops growing", five, fifty, 0.0)
        // The weight is 0.7 plus 0.6 of however far along the five the count is, so one
        // notification weighs 0.82 and five weigh the full 1.3. Nothing ever weighs 0.7:
        // that is the limit the formula approaches from below and never reaches, since a
        // ring of no notifications is not a ring.
        assertEquals(1.3 / 0.82, five / one, 1e-9)
    }

    @Test
    fun `the push goes the way the bell is already going, so a second ring adds to the first`() {
        val moving = 300.0
        assertTrue(bellRingVelocity(moving, 1) > moving)

        val returning = -300.0
        assertTrue(bellRingVelocity(returning, 1) < returning)
    }

    @Test
    fun `the bell cannot be made to spin, however many arrive`() {
        assertEquals(BELL_MAX_VELOCITY, bellRingVelocity(800.0, 5), 0.0)
        assertEquals(-BELL_MAX_VELOCITY, bellRingVelocity(-800.0, 5), 0.0)
        assertTrue(abs(bellRingVelocity(0.0, 5)) <= BELL_MAX_VELOCITY)
    }
}

class BellClapperTest {
    @Test
    fun `the clapper hangs straight when the bell is still`() {
        assertEquals(0.0, bellClapperLag(0.0), 0.0)
    }

    @Test
    fun `it trails the bell, so it leans against the direction of travel`() {
        assertTrue(bellClapperLag(200.0) < 0)
        assertTrue(bellClapperLag(-200.0) > 0)
    }

    @Test
    fun `it never leans further than its sweep, however fast the bell goes`() {
        assertEquals(-BELL_CLAPPER_SWEEP, bellClapperLag(BELL_CLAPPER_VELOCITY), 1e-9)
        assertEquals(-BELL_CLAPPER_SWEEP, bellClapperLag(100_000.0), 1e-9)
        assertEquals(BELL_CLAPPER_SWEEP, bellClapperLag(-100_000.0), 1e-9)
    }

    @Test
    fun `it is driven by speed rather than position, which is what makes it lag`() {
        // A pendulum is fastest at the bottom of its swing, which is exactly where a real
        // clapper is furthest from centre. Driving it from the angle would put it furthest
        // out at the top instead, which is backwards.
        assertTrue(abs(bellClapperLag(450.0)) > abs(bellClapperLag(50.0)))
    }
}

class BellBadgeTest {
    @Test
    fun `the badge sits just outside the button at upstream's defaults`() {
        // A 48 point bell with a count badge: 48 * 0.38 is 18.24 across, and the badge ends
        // up hanging very slightly over the edge.
        val inset = bellBadgeInset(48.0, 48 * 0.38, 0.9)
        assertTrue(inset < 0)
        assertTrue(abs(inset) < 1)
    }

    @Test
    fun `the placement is proportional, so it holds at any size`() {
        // Doubling everything doubles the inset rather than changing where the badge sits in
        // relation to the bell.
        val small = bellBadgeInset(32.0, 32 * 0.38, 0.9)
        val large = bellBadgeInset(64.0, 64 * 0.38, 0.9)
        assertEquals(2 * small, large, 1e-9)
    }

    @Test
    fun `a dot sits further out than a number, being smaller`() {
        assertTrue(bellBadgeInset(48.0, 48 * 0.22, 0.9) > bellBadgeInset(48.0, 48 * 0.38, 0.9))
    }

    @Test
    fun `an orbit of one puts the badge exactly on the button's edge`() {
        // The circle the badge is placed on has the button's own radius, scaled by the orbit.
        assertEquals(24 - 24 * sqrt(0.5), bellBadgeInset(48.0, 0.0, 1.0), 1e-9)
    }
}
