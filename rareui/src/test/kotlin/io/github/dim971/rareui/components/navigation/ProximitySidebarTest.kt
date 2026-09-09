/*
 * ProximitySidebarTest.kt
 * How far a dash swells for a given distance, checked against the mapping in
 * `components/ui/proximity-sidebar.tsx`. The same assertions run on iOS.
 */

package io.github.dim971.rareui.components.navigation

import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ProximitySidebarTest {
    @Test
    fun `a dash under the pointer is at its full width`() {
        for (kind in ProximitySectionKind.entries) {
            assertEquals(kind.base + kind.bump, proximityDashWidth(0.dp, kind))
        }
    }

    @Test
    fun `a dash out of reach is at rest`() {
        for (kind in ProximitySectionKind.entries) {
            assertEquals(kind.base, proximityDashWidth(ProximityRadius, kind))
            assertEquals(kind.base, proximityDashWidth(500.dp, kind))
        }
    }

    @Test
    fun `the swelling is symmetric, since above and below are the same distance`() {
        for (kind in ProximitySectionKind.entries) {
            var distance = 0f
            while (distance <= 60f) {
                assertEquals(
                    proximityDashWidth(distance.dp, kind),
                    proximityDashWidth((-distance).dp, kind),
                )
                distance += 5f
            }
        }
    }

    @Test
    fun `it only ever narrows as the pointer moves away`() {
        var previous = Float.MAX_VALUE.dp
        var distance = 0f
        while (distance <= ProximityRadius.value) {
            val width = proximityDashWidth(distance.dp, ProximitySectionKind.TITLE)
            assertTrue(width <= previous)
            previous = width
            distance += 1f
        }
    }

    @Test
    fun `a dash never grows past the width they are all measured against`() {
        for (kind in ProximitySectionKind.entries) {
            assertTrue(kind.base + kind.bump <= ProximityMaxDashWidth)
        }
    }

    @Test
    fun `the outline reads as a hierarchy, a title being wider and stronger than a body line`() {
        assertTrue(ProximitySectionKind.TITLE.base > ProximitySectionKind.SUBTITLE.base)
        assertTrue(ProximitySectionKind.SUBTITLE.base > ProximitySectionKind.SECTION.base)
        assertTrue(ProximitySectionKind.SECTION.base > ProximitySectionKind.BODY.base)
        assertTrue(ProximitySectionKind.TITLE.isProminent)
        assertTrue(ProximitySectionKind.SUBTITLE.isProminent)
        assertFalse(ProximitySectionKind.SECTION.isProminent)
        assertFalse(ProximitySectionKind.BODY.isProminent)
    }
}
