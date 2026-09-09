/*
 * StepPlayerTest.kt
 * The proportions and the morph, checked against `components/ui/step-player.tsx`.
 *
 * The iOS twin asserts the morph through the path's bounding box. A Compose `Path`
 * delegates to an Android one, which in a plain unit test is a stub answering every
 * question with zero, so the corners the path is built from are asserted instead.
 */

package io.github.dim971.rareui.components.display

import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs

class StepPlayerMetricsTest {
    @Test
    fun `everything is derived from the one size the caller gives`() {
        val metrics = StepPlayerMetrics(48.dp)
        assertEquals(48.dp, metrics.track)
        assertEquals(6.dp, metrics.dot)
        assertEquals(49.dp, metrics.bar)
        assertEquals(9.dp, metrics.gap)
        assertEquals(21.dp, metrics.pad)
        assertEquals(31.dp, metrics.icon)
    }

    @Test
    fun `the proportions hold at any size`() {
        for (size in listOf(16, 24, 32, 48, 64, 120)) {
            val metrics = StepPlayerMetrics(size.dp)
            assertTrue("the active step must be longer than a dot", metrics.bar > metrics.dot)
            assertTrue("the glyph has to fit in its button", metrics.icon < metrics.track)
            assertTrue(metrics.dot <= metrics.track)
            // The padding is the same inset the dot leaves above and below it, so the row
            // sits in the middle of the track.
            assertTrue(abs(metrics.pad.value * 2 + metrics.dot.value - metrics.track.value) <= 1f)
        }
    }

    @Test
    fun `a track too small to draw is grown to something that can be`() {
        val tiny = StepPlayerMetrics(1.dp)
        assertEquals(12.dp, tiny.track)
        assertTrue(tiny.dot >= 2.dp)
        assertTrue(tiny.gap >= 2.dp)
    }
}

class TransportMorphTest {
    @Test
    fun `at nothing it is the pause bars, at everything it is the play triangle`() {
        val pauseLeft = transportQuad(TransportPauseLeft, TransportPlayLeft, 0f)
        val playRight = transportQuad(TransportPauseRight, TransportPlayRight, 1f)

        assertEquals(TransportPauseLeft, pauseLeft)
        assertEquals(TransportPlayRight, playRight)
        // The pause bars run from 8.4 to 15.6; the triangle reaches its point at 17.7.
        assertEquals(8.4f, TransportPauseLeft.minOf { it.x }, 1e-6f)
        assertEquals(15.6f, TransportPauseRight.maxOf { it.x }, 1e-6f)
        assertEquals(17.7f, TransportPlayRight.maxOf { it.x }, 1e-6f)
    }

    @Test
    fun `the shape stays inside its own box the whole way across`() {
        var step = 0f
        while (step <= 1f) {
            val corners =
                transportQuad(TransportPauseLeft, TransportPlayLeft, step) +
                    transportQuad(TransportPauseRight, TransportPlayRight, step)
            assertTrue(corners.minOf { it.x } >= 8.4f - 1e-6f)
            assertTrue(corners.maxOf { it.x } <= 17.7f + 1e-6f)
            assertTrue(corners.minOf { it.y } >= 5.9f - 1e-6f)
            assertTrue(corners.maxOf { it.y } <= 18.1f + 1e-6f)
            step += 0.05f
        }
    }

    @Test
    fun `the morph moves the whole way rather than jumping at one end`() {
        // Each corner travels at a constant rate, so the point advances smoothly rather
        // than sitting still and then snapping.
        var previous = transportQuad(TransportPauseRight, TransportPlayRight, 0f).maxOf { it.x }
        var step = 0.05f
        while (step <= 1f) {
            val tip = transportQuad(TransportPauseRight, TransportPlayRight, step).maxOf { it.x }
            assertTrue("the point moved too far in one step", abs(tip - previous) < 0.5f)
            previous = tip
            step += 0.05f
        }
    }

    @Test
    fun `progress outside the range is held at the ends rather than extrapolated`() {
        // A triangle turned inside out is not a play button.
        assertEquals(TransportPauseLeft, transportQuad(TransportPauseLeft, TransportPlayLeft, -1f))
        assertEquals(TransportPlayLeft, transportQuad(TransportPauseLeft, TransportPlayLeft, 2f))
    }

    @Test
    fun `the tip is two corners in the same place, so four sides become three without a seam`() {
        assertEquals(TransportPlayRight[1], TransportPlayRight[2])
    }
}
