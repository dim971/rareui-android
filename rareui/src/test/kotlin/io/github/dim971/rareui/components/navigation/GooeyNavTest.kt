/*
 * GooeyNavTest.kt
 * The seam is the component, so the seam's geometry is what is tested. The same claims are
 * asserted in the iOS twin.
 */

package io.github.dim971.rareui.components.navigation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class GooeyNeckWaistTest {
    @Test
    fun `touching tiles have a seam at full height, so they read as one block`() {
        assertEquals(GOOEY_NECK_HEIGHT, gooeyNeckWaist(0.0, 20.0), 0.0)
    }

    @Test
    fun `the seam has thinned to nothing at 22 percent of the separation`() {
        val span = 20.0
        assertEquals(0.0, gooeyNeckWaist(span * GOOEY_NECK_BREAK, span), 1e-9)
    }

    @Test
    fun `the waist only ever narrows as the gap opens`() {
        var previous = Double.POSITIVE_INFINITY
        for (step in 0..100) {
            val waist = gooeyNeckWaist(step / 10.0, 20.0)
            assertTrue(waist < previous)
            previous = waist
        }
    }

    @Test
    fun `the break is a fraction of the separation, so a wider bar stretches further`() {
        // Both are at the point of breaking, at very different absolute distances.
        assertEquals(0.0, gooeyNeckWaist(14 * GOOEY_NECK_BREAK, 14.0), 1e-9)
        assertEquals(0.0, gooeyNeckWaist(44 * GOOEY_NECK_BREAK, 44.0), 1e-9)
        // At the same absolute gap, the wider bar still has a seam and the narrow one does not.
        assertTrue(gooeyNeckWaist(5.0, 14.0) < 0)
        assertTrue(gooeyNeckWaist(5.0, 44.0) > 0)
    }
}

class GooeyNeckGeometryTest {
    @Test
    fun `a closed seam has no geometry, because the tiles are touching instead`() {
        assertNull(gooeyNeckGeometry(0.0, 20.0, 40.0))
        assertNull(gooeyNeckGeometry(-1.0, 20.0, 40.0))
    }

    @Test
    fun `a seam past its breaking point has none either`() {
        // 22 percent of 20 is 4.4, so anything beyond that has parted.
        assertNull(gooeyNeckGeometry(4.4, 20.0, 40.0))
        assertNull(gooeyNeckGeometry(12.0, 20.0, 40.0))
    }

    @Test
    fun `a stretched seam sits in the gap it belongs in and nowhere else`() {
        val gap = 2.0
        val seam = gooeyNeckGeometry(gap, 20.0, 40.0)!!
        assertEquals(20.0 - gap, seam.start, 1e-9)
        assertEquals(20.0, seam.end, 1e-9)
        assertEquals(20.0 - gap / 2, seam.middle, 1e-9)
    }

    @Test
    fun `the seam spans the full height of the tile it is stretched between`() {
        val seam = gooeyNeckGeometry(1.0, 20.0, 40.0)!!
        assertEquals(0.0, seam.top, 1e-9)
        assertEquals(40.0, seam.bottom, 1e-9)
    }

    @Test
    fun `the control points walk toward the middle and then past it as the gap opens`() {
        // At rest they sit at the edges and the seam is a solid block. As the gap opens
        // they walk inward, meet halfway, and then cross: that crossing is the pinch, and
        // it is what makes the waist thin to nothing rather than merely narrow.
        val narrow = gooeyNeckGeometry(0.5, 20.0, 40.0)!!
        val wide = gooeyNeckGeometry(4.0, 20.0, 40.0)!!
        assertTrue(narrow.upperControl < wide.upperControl)
        assertTrue(narrow.lowerControl > wide.lowerControl)
        assertTrue("they should have crossed by now", wide.upperControl > wide.lowerControl)
    }

    @Test
    fun `the two control points are always symmetric about the middle of the tile`() {
        // Their heights sum to the tile's, at every gap. If they ever stopped doing that
        // the seam would be lopsided and the two tiles would look like different objects.
        for (step in 1..43) {
            val seam = gooeyNeckGeometry(step / 10.0, 20.0, 40.0) ?: continue
            assertEquals(40.0, seam.upperControl + seam.lowerControl, 1e-9)
        }
    }

    @Test
    fun `nonsense input has no geometry rather than nonsense coordinates`() {
        assertNull(gooeyNeckGeometry(Double.NaN, 20.0, 40.0))
        assertNull(gooeyNeckGeometry(2.0, Double.NaN, 40.0))
        assertNull(gooeyNeckGeometry(2.0, 0.0, 40.0))
    }

    @Test
    fun `the seam can be placed to the left of the origin, where the gap actually is`() {
        val seam = gooeyNeckGeometry(2.0, 20.0, 40.0, left = -20.0)!!
        assertEquals(-2.0, seam.start, 1e-9)
        assertEquals(0.0, seam.end, 1e-9)
    }
}

class GooeySeamOpeningTest {
    @Test
    fun `the ends of the bar are always open, there being nothing beyond them`() {
        assertTrue(gooeyIsSeamOpen(0, active = 1, count = 3))
        assertTrue(gooeyIsSeamOpen(3, active = 1, count = 3))
    }

    @Test
    fun `the two seams either side of the selected tile open, and no others`() {
        // Five tiles, the middle one selected: seams 2 and 3 are its own.
        val open = (0..5).filter { gooeyIsSeamOpen(it, active = 2, count = 5) }
        assertEquals(listOf(0, 2, 3, 5), open)
    }

    @Test
    fun `selecting an end tile opens one inner seam, not two`() {
        assertEquals(listOf(0, 1, 3), (0..3).filter { gooeyIsSeamOpen(it, 0, 3) })
        assertEquals(listOf(0, 2, 3), (0..3).filter { gooeyIsSeamOpen(it, 2, 3) })
    }

    @Test
    fun `a single tile is open on both sides and joined to nothing`() {
        assertEquals(listOf(0, 1), (0..1).filter { gooeyIsSeamOpen(it, 0, 1) })
    }

    @Test
    fun `a seam that is neither an end nor beside the selection stays shut`() {
        assertFalse(gooeyIsSeamOpen(1, active = 3, count = 5))
        assertFalse(gooeyIsSeamOpen(2, active = 4, count = 5))
    }
}
