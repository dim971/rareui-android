/*
 * RareUiPathMorphTest.kt
 * The stand-in for flubber. It has to end up exactly on both outlines and stay a sensible
 * shape everywhere in between, or a pen turning into a tick turns inside out on the way.
 *
 * The iOS twin samples real paths and asserts the result's bounding box. Sampling here goes
 * through a `PathMeasure`, which delegates to an Android one and is a stub in a plain unit
 * test, so the rings are built by hand and the two parts that decide whether a morph works,
 * lining the rings up and interpolating between them, are asserted directly.
 */

package io.github.dim971.rareui.core

import androidx.compose.ui.geometry.Offset
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.cos
import kotlin.math.sin

class RareUiPathMorphTest {
    /** A ring of points around a circle, starting at a given angle. */
    private fun ring(
        count: Int,
        radius: Float,
        from: Float = 0f,
        centre: Offset = Offset(12f, 12f),
    ): List<Offset> =
        List(count) { index ->
            val angle = from + 2f * Math.PI.toFloat() * index / count
            Offset(centre.x + radius * cos(angle), centre.y + radius * sin(angle))
        }

    @Test
    fun `a ring already lined up is left where it is`() {
        val reference = ring(16, 8f)
        assertEquals(reference, rareUiAlignRing(reference, reference))
    }

    @Test
    fun `a ring that starts elsewhere is turned until it lines up`() {
        // The same circle sampled from a quarter of the way round. Joined as they are, every
        // point would travel a quarter of the circumference and the shape would rotate
        // through itself; turned, nothing moves at all.
        val reference = ring(16, 8f)
        val turned = ring(16, 8f, from = 2f * Math.PI.toFloat() / 4)
        val aligned = rareUiAlignRing(turned, reference)

        aligned.forEachIndexed { index, point ->
            assertEquals(reference[index].x, point.x, 1e-3f)
            assertEquals(reference[index].y, point.y, 1e-3f)
        }
    }

    @Test
    fun `lining up keeps every point, only its place in the order`() {
        val reference = ring(12, 8f)
        val turned = ring(12, 8f, from = 1f)
        val aligned = rareUiAlignRing(turned, reference)

        assertEquals(turned.size, aligned.size)
        assertEquals(turned.toSet(), aligned.toSet())
    }

    @Test
    fun `two rings that cannot be compared are left alone rather than throwing`() {
        val short = ring(4, 8f)
        assertEquals(short, rareUiAlignRing(short, ring(9, 8f)))
        assertEquals(emptyList<Offset>(), rareUiAlignRing(emptyList(), emptyList()))
    }

    @Test
    fun `at either end the interpolation is the ring it started or finished on`() {
        val start = ring(12, 8f)
        val end = ring(12, 3f)
        assertEquals(start, rareUiInterpolateRing(start, end, 0f))
        assertEquals(end, rareUiInterpolateRing(start, end, 1f))
    }

    @Test
    fun `halfway across it is halfway between the two`() {
        val start = ring(12, 8f)
        val end = ring(12, 4f)
        rareUiInterpolateRing(start, end, 0.5f).forEachIndexed { index, point ->
            assertEquals((start[index].x + end[index].x) / 2, point.x, 1e-4f)
            assertEquals((start[index].y + end[index].y) / 2, point.y, 1e-4f)
        }
    }

    @Test
    fun `the outline keeps its size rather than collapsing partway across`() {
        // A morph whose two rings are not lined up pinches through itself in the middle and
        // the shape all but disappears. Watching the width is a cheap way to catch it.
        val start = ring(24, 8f)
        val end = rareUiAlignRing(ring(24, 6f, from = 1.3f), start)

        var step = 0.05f
        while (step <= 0.95f) {
            val points = rareUiInterpolateRing(start, end, step)
            val width = points.maxOf { it.x } - points.minOf { it.x }
            assertTrue("it went flat at $step", width > 8f)
            step += 0.05f
        }
    }

    @Test
    fun `progress outside the range is held at the ends`() {
        val start = ring(8, 8f)
        val end = ring(8, 2f)
        assertEquals(start, rareUiInterpolateRing(start, end, -1f))
        assertEquals(end, rareUiInterpolateRing(start, end, 2f))
    }

    @Test
    fun `an empty ring yields nothing rather than throwing`() {
        assertEquals(emptyList<Offset>(), rareUiInterpolateRing(emptyList(), ring(8, 8f), 0.5f))
        assertEquals(emptyList<Offset>(), rareUiInterpolateRing(ring(8, 8f), emptyList(), 0.5f))
    }
}
