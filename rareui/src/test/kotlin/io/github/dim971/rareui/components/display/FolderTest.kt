/*
 * FolderTest.kt
 * Where the cards sit in each of the folder's three states, checked against
 * `components/ui/folder-component.tsx`. The same assertions run on iOS.
 */

package io.github.dim971.rareui.components.display

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs

class FolderStateTest {
    @Test
    fun `the flap tips further back at every step`() {
        assertTrue(FolderState.REST.flapAngle > FolderState.HOVERING.flapAngle)
        assertTrue(FolderState.HOVERING.flapAngle > FolderState.OPEN.flapAngle)
        // All three tip backward, so the folder never leans toward the reader.
        assertTrue(FolderState.REST.flapAngle < 0f)
    }

    @Test
    fun `hovering is most of the way to open, so the two do not read as separate ideas`() {
        val travel = FolderState.REST.flapAngle - FolderState.OPEN.flapAngle
        val hovered = FolderState.REST.flapAngle - FolderState.HOVERING.flapAngle
        assertTrue(hovered / travel > 0.7f)
    }
}

class FolderCardTest {
    @Test
    fun `the cards lift clear of the folder when it opens`() {
        for (placement in FolderCardPlacements) {
            val rest = placement.pose(FolderState.REST).y
            val hovering = placement.pose(FolderState.HOVERING).y
            val open = placement.pose(FolderState.OPEN).y
            assertTrue("hovering should lift them", hovering < rest)
            assertTrue("opening should lift them further", open < hovering)
            assertTrue("and clear of the folder entirely", open < -150f)
        }
    }

    @Test
    fun `the fan opens outward, so the three do not end up on top of each other`() {
        // The first leans right and sits right, the last leans left and sits left, and the
        // middle one is nearly straight. Opening exaggerates all of it.
        val open = FolderCardPlacements.map { it.pose(FolderState.OPEN) }
        assertTrue(open[0].x > open[1].x)
        assertTrue(open[1].x > open[2].x)
        assertTrue(open[0].rotation > open[1].rotation)
        assertTrue(open[1].rotation > open[2].rotation)

        for (placement in FolderCardPlacements) {
            assertTrue(
                abs(placement.pose(FolderState.OPEN).rotation) >=
                    abs(placement.pose(FolderState.REST).rotation),
            )
        }
    }

    @Test
    fun `the cards leave in turn rather than together`() {
        val delays = FolderCardPlacements.map { it.delay(FolderState.OPEN) }
        assertTrue("they should not all leave at once", delays.toSet().size > 1)
        // The one nearest the front leaves last, so the fan opens from the back forward.
        assertTrue(delays[0] > delays[2])
    }

    @Test
    fun `closing happens all at once, with nothing held back`() {
        for (placement in FolderCardPlacements) {
            assertEquals(0L, placement.delay(FolderState.REST))
        }
    }

    @Test
    fun `each of the three folders is a complete set of colours rather than a tint`() {
        // The black folder holds pale cards and the white one holds dark cards, so a
        // palette cannot be derived from the folder's own colour.
        val black = FolderColor.BLACK.palette
        val white = FolderColor.WHITE.palette
        assertTrue(black.cardFill.luminance() > black.back.luminance())
        assertTrue(white.cardFill.luminance() < white.back.luminance())
    }

    private fun androidx.compose.ui.graphics.Color.luminance(): Float = red * 0.299f + green * 0.587f + blue * 0.114f
}
