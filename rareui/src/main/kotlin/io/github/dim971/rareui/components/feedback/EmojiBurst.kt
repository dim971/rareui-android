/*
 * EmojiBurst.kt
 * The copies that fly off when an emoji is picked, ported from `makeParticles` and
 * `BurstEmoji` in upstream's `components/ui/emoji-reaction.tsx`.
 *
 * Upstream animates six values at once with different keyframe times and different easings
 * on each, all sharing one clock. That is reproduced here by animating a single progress
 * from nothing to everything and reading each value out of it, which is what Motion is
 * doing underneath anyway.
 */

package io.github.dim971.rareui.components.feedback

import androidx.compose.animation.core.Easing
import androidx.compose.ui.geometry.Offset
import kotlin.random.Random

/** How many copies leave on each react. */
internal const val EMOJI_BURST_COUNT = 5

/** How far up they go, in points. */
internal const val EMOJI_RISE = 450f

/** How far to either side they start. */
internal const val EMOJI_LAUNCH_SPREAD = 6f

/** How far they wander on the way up. */
internal const val EMOJI_CLIMB_SPREAD = 78f

/** The most that may be in the air at once, so holding a reaction down cannot fill the screen. */
internal const val EMOJI_MAX_PARTICLES = 60

/** How often a held reaction repeats, in milliseconds. */
internal const val EMOJI_HOLD_INTERVAL_MILLIS = 550L

/** How long apart the five copies of one burst leave, in seconds. */
internal const val EMOJI_STAGGER = 0.25f

/**
 * Reads a value out of a keyframe track.
 *
 * Motion states a track as a list of values and a list of times to reach them at, with an
 * easing applied between each pair rather than across the whole track. This is that.
 *
 * @param values the values, in order.
 * @param times when each is reached, in `0..1`. Must be the same length as [values].
 * @param progress how far through the animation is, in `0..1`.
 * @param ease the easing applied between one keyframe and the next.
 * @return the value at that point.
 */
internal fun emojiKeyframe(
    values: FloatArray,
    times: FloatArray,
    progress: Float,
    ease: Easing,
): Float {
    if (values.size < 2 || values.size != times.size) return values.firstOrNull() ?: 0f
    if (progress <= times.first()) return values.first()
    if (progress >= times.last()) return values.last()

    for (index in 0 until values.size - 1) {
        if (progress > times[index + 1]) continue
        val span = times[index + 1] - times[index]
        // Two keyframes at the same time are a step change, not a division by nothing.
        if (span <= 0f) return values[index + 1]
        val eased = ease.transform((progress - times[index]) / span)
        return values[index] + (values[index + 1] - values[index]) * eased
    }
    return values.last()
}

/** One emoji on its way up. */
internal data class EmojiParticle(
    /** Its own identity, so a held reaction never draws two copies as one. */
    val id: Int,
    /** The emoji itself. */
    val glyph: String,
    /** Where in the bar it was launched from. */
    val origin: Offset,
    /** How far to one side it starts, in points. */
    val launch: Float,
    /** How far it wanders sideways on the way up. */
    val drift: Float,
    /** How far it turns, in degrees. */
    val tilt: Float,
    /** How far it rises, in points. */
    val travel: Float,
    /** The size it settles at, as a multiple of the emoji's own. */
    val scale: Float,
    /** How blurred it is by the end, as a fraction of its size. */
    val blurRatio: Float,
    /** When it starts fading out. */
    val fadeAt: Float,
    /** How long the whole flight takes, in seconds. */
    val duration: Float,
    /** How long after the burst this one leaves. */
    val delay: Float,
)

/**
 * Makes one burst, five copies leaving a quarter of a second apart.
 *
 * @param glyph the emoji picked.
 * @param seed the first identifier to use.
 * @param origin where in the bar the emoji sits.
 * @param random the source of the randomness, so a test can pin it.
 * @return the particles.
 */
internal fun emojiBurst(
    glyph: String,
    seed: Int,
    origin: Offset,
    random: Random = Random,
): List<EmojiParticle> =
    List(EMOJI_BURST_COUNT) { index ->
        // One number decides which side of the bar this copy leans to and how far it
        // wanders, so its launch and its climb agree rather than fighting each other.
        val lane = random.nextDouble(-1.0, 1.0).toFloat()
        val direction = if (lane < 0) -1f else 1f

        EmojiParticle(
            id = seed + index,
            glyph = glyph,
            origin = origin,
            launch = lane * EMOJI_LAUNCH_SPREAD,
            drift = lane * EMOJI_CLIMB_SPREAD,
            tilt = random.nextDouble(1.0, 4.0).toFloat() * direction,
            travel = EMOJI_RISE * random.nextDouble(0.86, 1.0).toFloat(),
            scale = random.nextDouble(0.78, 1.05).toFloat(),
            blurRatio = random.nextDouble(0.18, 0.3).toFloat(),
            fadeAt = random.nextDouble(0.55, 0.88).toFloat(),
            duration = random.nextDouble(1.4, 1.8).toFloat(),
            delay = index * EMOJI_STAGGER,
        )
    }
