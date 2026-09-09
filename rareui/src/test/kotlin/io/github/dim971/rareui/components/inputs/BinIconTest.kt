/*
 * BinIconTest.kt
 * The bin's walls are redrawn rather than transformed, which is the one part of this
 * component that is geometry rather than animation. Checked against
 * `components/ui/delete-button.tsx`.
 *
 * The iOS twin asserts the same things through the path's bounding box. A Compose `Path`
 * delegates to an Android one, which in a plain unit test is a stub that answers every
 * question with zero, so the numbers the path is built from are asserted instead.
 */

package io.github.dim971.rareui.components.inputs

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BinIconTest {
    @Test
    fun `a shut bin is taller than an open one`() {
        // The walls start lower down as the lid lifts, so the bin appears to sink into
        // itself and the lid swings clear of it rather than through it.
        assertTrue(binWallHeight(BIN_WALL_TOP_OPEN) < binWallHeight(BIN_WALL_TOP))
        assertTrue(BIN_WALL_TOP_OPEN > BIN_WALL_TOP)
    }

    @Test
    fun `the base does not move as the bin opens`() {
        // Only the top of the walls travels. A base that moved would make the bin slide
        // down the tile rather than sink into itself.
        assertEquals(22f, BIN_BASE, 0f)
        assertEquals(binWallHeight(BIN_WALL_TOP), BIN_BASE - BIN_WALL_TOP, 0f)
    }

    @Test
    fun `the walls stay inside the icon's own box at every height`() {
        var top = BIN_WALL_TOP
        while (top <= BIN_WALL_TOP_OPEN) {
            assertTrue(binWallHeight(top) > 0f)
            assertTrue(top + binWallHeight(top) <= BIN_BOX)
            top += 0.5f
        }
        assertTrue(BIN_LEFT >= 0f)
        assertTrue(BIN_RIGHT <= BIN_BOX)
    }

    @Test
    fun `the base is as wide as upstream draws it`() {
        // From five to nineteen in the icon's own coordinates, corners included.
        assertEquals(5f, BIN_LEFT, 0f)
        assertEquals(19f, BIN_RIGHT, 0f)
    }

    @Test
    fun `the corners fit inside the walls they round`() {
        // Twice the radius has to fit across the bin and up from the base, or the two
        // quarter arcs would overlap and the path would double back on itself.
        assertTrue(2 * BIN_CORNER < BIN_RIGHT - BIN_LEFT)
        assertTrue(2 * BIN_CORNER < binWallHeight(BIN_WALL_TOP_OPEN))
    }

    @Test
    fun `the hinge is at the left end of the rim, not the middle of the icon`() {
        // Turning about the middle would make the lid pivot inside the bin rather than
        // swing off the back of it.
        assertEquals(3f / 24, BinLidHinge.x, 1e-6f)
        assertEquals(6f / 24, BinLidHinge.y, 1e-6f)
        assertTrue(BinLidHinge.x < 0.5f)
    }

    @Test
    fun `the lid opens backwards`() {
        // A positive angle would swing it forward over the bin's mouth, which is where the
        // rubbish is going.
        assertTrue(BIN_LID_OPEN < 0f)
    }
}
