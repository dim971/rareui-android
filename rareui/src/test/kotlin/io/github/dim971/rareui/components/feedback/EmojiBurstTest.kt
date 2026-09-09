/*
 * EmojiBurstTest.kt
 * The burst is six values animating at once on different schedules, which is exactly the
 * kind of thing that is wrong by a frame and stays wrong. Checked against
 * `components/ui/emoji-reaction.tsx`; the same assertions run on iOS.
 */

package io.github.dim971.rareui.components.feedback

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.ui.geometry.Offset
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs
import kotlin.math.sign
import kotlin.random.Random

class EmojiKeyframeTest {
    @Test
    fun `a track starts on its first value and finishes on its last`() {
        val values = floatArrayOf(0f, 1f, 1f, 0f)
        val times = floatArrayOf(0f, 0.03f, 0.7f, 1f)
        assertEquals(0f, emojiKeyframe(values, times, 0f, LinearEasing), 0f)
        assertEquals(0f, emojiKeyframe(values, times, 1f, LinearEasing), 0f)
    }

    @Test
    fun `it holds the value between two keyframes that share it`() {
        // The opacity track is nothing, then fully on, then held, then nothing. The held
        // stretch is most of the flight and has to stay solid throughout.
        val opacity = floatArrayOf(0f, 1f, 1f, 0f)
        val times = floatArrayOf(0f, 0.03f, 0.7f, 1f)
        var step = 0.05f
        while (step <= 0.65f) {
            assertEquals(1f, emojiKeyframe(opacity, times, step, LinearEasing), 0f)
            step += 0.05f
        }
    }

    @Test
    fun `the easing is applied between one keyframe and the next, not across the track`() {
        // Halfway between the first two keyframes of a two point track, an ease out is
        // already past the middle.
        val easeOut = CubicBezierEasing(0f, 0f, 0.58f, 1f)
        assertTrue(emojiKeyframe(floatArrayOf(0f, 1f), floatArrayOf(0f, 1f), 0.5f, easeOut) > 0.5f)
    }

    @Test
    fun `progress outside the track's own times is held at the ends`() {
        // The blur track does not start until twelve percent of the way through.
        val blur = floatArrayOf(0f, 0f, 8f)
        val times = floatArrayOf(0f, 0.12f, 1f)
        assertEquals(0f, emojiKeyframe(blur, times, 0.05f, LinearEasing), 0f)
        assertEquals(8f, emojiKeyframe(blur, times, 2f, LinearEasing), 0f)
        assertEquals(0f, emojiKeyframe(blur, times, -1f, LinearEasing), 0f)
    }

    @Test
    fun `two keyframes at the same moment are a step rather than a division by nothing`() {
        // A zero length segment has no progress to divide by. The value steps across it
        // instead, and nothing downstream ever sees a NaN.
        val values = floatArrayOf(0f, 1f, 2f, 3f)
        val times = floatArrayOf(0f, 0.5f, 0.5f, 1f)
        var step = 0f
        while (step <= 1f) {
            val value = emojiKeyframe(values, times, step, LinearEasing)
            assertTrue("the track went to nothing at $step", value.isFinite())
            step += 0.05f
        }
        assertTrue(emojiKeyframe(values, times, 0.4f, LinearEasing) < 1f)
        assertTrue(emojiKeyframe(values, times, 0.6f, LinearEasing) > 2f)
    }

    @Test
    fun `a track that does not make sense yields its first value rather than throwing`() {
        assertEquals(5f, emojiKeyframe(floatArrayOf(5f), floatArrayOf(0f), 0.5f, LinearEasing), 0f)
        assertEquals(1f, emojiKeyframe(floatArrayOf(1f, 2f), floatArrayOf(0f), 0.5f, LinearEasing), 0f)
        assertEquals(0f, emojiKeyframe(floatArrayOf(), floatArrayOf(), 0.5f, LinearEasing), 0f)
    }
}

class EmojiBurstTest {
    @Test
    fun `a burst is five copies leaving a quarter of a second apart`() {
        val burst = emojiBurst("🎉", seed = 0, origin = Offset.Zero, random = Random(1))
        assertEquals(EMOJI_BURST_COUNT, burst.size)
        assertEquals(listOf(0f, 0.25f, 0.5f, 0.75f, 1f), burst.map { it.delay })
        assertEquals("each copy needs its own identity", burst.size, burst.map { it.id }.toSet().size)
    }

    @Test
    fun `every copy stays inside the bounds upstream's ranges allow`() {
        val random = Random(7)
        for (seed in 0 until 400 step EMOJI_BURST_COUNT) {
            for (particle in emojiBurst("🔥", seed, Offset(10f, 20f), random)) {
                assertTrue(abs(particle.launch) <= EMOJI_LAUNCH_SPREAD)
                assertTrue(abs(particle.drift) <= EMOJI_CLIMB_SPREAD)
                assertTrue(abs(particle.tilt) >= 1f && abs(particle.tilt) <= 4f)
                assertTrue(particle.travel >= EMOJI_RISE * 0.86f && particle.travel <= EMOJI_RISE)
                assertTrue(particle.scale >= 0.78f && particle.scale <= 1.05f)
                assertTrue(particle.blurRatio >= 0.18f && particle.blurRatio <= 0.3f)
                assertTrue(particle.fadeAt >= 0.55f && particle.fadeAt <= 0.88f)
                assertTrue(particle.duration >= 1.4f && particle.duration <= 1.8f)
            }
        }
    }

    @Test
    fun `a copy leans the same way it wanders, rather than fighting itself`() {
        // One random number decides the side, so the sideways launch and the climb drift
        // always share a sign. Two independent draws would make copies that set off one way
        // and then come back across the bar.
        val random = Random(11)
        for (seed in 0 until 200 step EMOJI_BURST_COUNT) {
            for (particle in emojiBurst("👋", seed, Offset.Zero, random)) {
                if (particle.launch == 0f) continue
                assertEquals(particle.launch.sign, particle.drift.sign, 0f)
                assertEquals(particle.launch.sign, particle.tilt.sign, 0f)
            }
        }
    }

    @Test
    fun `the identifiers march forward, so a held reaction never reuses one`() {
        val random = Random(13)
        val first = emojiBurst("a", 0, Offset.Zero, random).map { it.id }.toSet()
        val second = emojiBurst("a", EMOJI_BURST_COUNT, Offset.Zero, random).map { it.id }.toSet()
        assertTrue(first.intersect(second).isEmpty())
    }
}
