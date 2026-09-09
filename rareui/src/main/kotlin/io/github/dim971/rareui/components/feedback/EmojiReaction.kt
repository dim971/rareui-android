/*
 * EmojiReaction.kt
 * A port of upstream's `components/ui/emoji-reaction.tsx`.
 *
 * A button that opens a bar of emoji. Picking one sends five copies of it drifting up the
 * screen, and holding it down keeps sending them.
 *
 * One departure, recorded in docs/fidelity.md: upstream loads Apple's emoji artwork from a
 * CDN, because a browser on Windows would otherwise draw something else entirely. Here the
 * emoji are text and the system draws them, which is what every other Android application
 * does and removes a network dependency from a component with no other reason to touch the
 * network.
 */

package io.github.dim971.rareui.components.feedback

import android.os.Build
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.layout.layout
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.github.dim971.rareui.theme.RareUiTheme
import io.github.dim971.rareui.theme.rareUiSpring
import io.github.dim971.rareui.theme.rememberRareUiReduceMotion
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** Which edge of the trigger an [EmojiReaction]'s bar lines up with. */
public enum class EmojiReactionAlign {
    /** The bar's leading edge meets the trigger's. */
    LEADING,

    /** The bar is centred over the trigger. */
    CENTER,

    /** The bar's trailing edge meets the trigger's. */
    TRAILING,
}

/** How large an [EmojiReaction] is drawn. */
public enum class EmojiReactionSize(
    /** The trigger's width and height. */
    internal val trigger: Dp,
    /** The trigger's icon size. */
    internal val icon: Dp,
    /** The size of an emoji in the bar. */
    internal val emoji: Dp,
    /** The space between two emoji in the bar. */
    internal val spacing: Dp,
    /** The padding inside the bar. */
    internal val padding: Dp,
) {
    /** The smallest. */
    SMALL(trigger = 32.dp, icon = 16.dp, emoji = 26.dp, spacing = 2.dp, padding = 4.dp),

    /** The default. */
    MEDIUM(trigger = 40.dp, icon = 20.dp, emoji = 34.dp, spacing = 4.dp, padding = 6.dp),

    /** The largest. */
    LARGE(trigger = 48.dp, icon = 24.dp, emoji = 42.dp, spacing = 6.dp, padding = 8.dp),
}

/** The five upstream ships with, as the glyphs their names describe. */
public val RareUiDefaultEmojis: List<String> =
    listOf("🥰", "🤩", "😕", "🥺", "😄")

/** The bar's arrival, from upstream's `BAR_SPRING`. */
private const val BAR_STIFFNESS = 520f
private const val BAR_DAMPING = 30f

/** The space between the trigger and the bar. */
private val BarGap = 16.dp

/** Blur needs a render effect, which arrived in Android 12. */
private val PLATFORM_BLURS = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S

/**
 * A button that opens a bar of emoji, and sends the one you pick drifting up the screen.
 *
 * ```kotlin
 * EmojiReaction(onReact = { emoji -> post(emoji) })
 * ```
 *
 * Holding an emoji down keeps sending copies, a little under twice a second.
 *
 * With animations turned off on the device the bar fades in rather than springing and
 * nothing is sent flying, which is what upstream does under `prefers-reduced-motion`.
 *
 * The copies rise out of the component's own bounds, so an ancestor that clips will cut
 * them off. That is true of the SwiftUI port inside a scroll view as well.
 *
 * @param modifier the modifier to apply.
 * @param emojis the emoji to offer. Defaults to upstream's five.
 * @param size how large to draw it.
 * @param align which edge of the trigger the bar lines up with.
 * @param onReact called with the emoji picked, once per copy sent.
 */
@Composable
public fun EmojiReaction(
    modifier: Modifier = Modifier,
    emojis: List<String> = RareUiDefaultEmojis,
    size: EmojiReactionSize = EmojiReactionSize.MEDIUM,
    align: EmojiReactionAlign = EmojiReactionAlign.CENTER,
    onReact: ((String) -> Unit)? = null,
) {
    val colors = RareUiTheme.colors
    val reduceMotion = rememberRareUiReduceMotion()
    val density = LocalDensity.current
    val scope = rememberCoroutineScope()

    var open by remember { mutableStateOf(false) }
    var last by remember { mutableStateOf<String?>(null) }
    var placeAbove by remember { mutableStateOf(true) }
    var seed by remember { mutableIntStateOf(0) }
    val particles = remember { mutableStateListOf<EmojiParticle>() }
    var holding by remember { mutableStateOf<Job?>(null) }

    DisposableEffect(Unit) {
        onDispose { holding?.cancel() }
    }

    fun react(
        emoji: String,
        origin: Offset,
    ) {
        last = emoji
        onReact?.invoke(emoji)
        if (reduceMotion) return

        seed += EMOJI_BURST_COUNT
        // Holding a reaction down for long enough would otherwise fill the screen, and the
        // oldest copies are the ones nearest the top and about to fade anyway.
        particles += emojiBurst(glyph = emoji, seed = seed, origin = origin)
        while (particles.size > EMOJI_MAX_PARTICLES) particles.removeAt(0)
    }

    Box(modifier = modifier) {
        Box(
            modifier =
                Modifier
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = { open = !open },
                    ).semantics {
                        role = Role.Button
                        contentDescription =
                            when {
                                open -> "Close reactions"
                                last != null -> "Reacted $last"
                                else -> "Add a reaction"
                            }
                    }.onGloballyPositioned { coordinates ->
                        // The bar needs somewhere to go. Where the trigger sits in the
                        // window is what decides whether that is above it or below.
                        val needed =
                            with(density) {
                                (size.emoji + size.padding * 2 + 8.dp + 26.dp + BarGap).toPx()
                            }
                        placeAbove = coordinates.positionInWindow().y - needed >= with(density) { 8.dp.toPx() }
                    },
        ) {
            EmojiTriggerFace(open = open, last = last, size = size, colors = colors)
        }

        AnimatedVisibility(
            visible = open,
            modifier =
                Modifier.layout { measurable, _ ->
                    // Measured freely and then reported as taking no room at all. In
                    // SwiftUI this is what an overlay is; here a child of a Box is part of
                    // its size unless it says otherwise, and a bar that grew the component
                    // would shove whatever sits beside it out of the way every time it
                    // opened.
                    val bar = measurable.measure(Constraints())
                    val trigger = size.trigger.roundToPx()
                    val gap = BarGap.roundToPx()
                    val x =
                        when (align) {
                            EmojiReactionAlign.LEADING -> 0
                            EmojiReactionAlign.CENTER -> (trigger - bar.width) / 2
                            EmojiReactionAlign.TRAILING -> trigger - bar.width
                        }
                    val y = if (placeAbove) -(bar.height + gap) else trigger + gap
                    layout(0, 0) { bar.place(x, y) }
                },
            enter =
                if (reduceMotion) {
                    fadeIn(tween(150))
                } else {
                    scaleIn(
                        animationSpec = rareUiSpring(BAR_STIFFNESS, BAR_DAMPING),
                        initialScale = 0.85f,
                        transformOrigin = TransformOrigin(0.5f, 1f),
                    ) + fadeIn(rareUiSpring(BAR_STIFFNESS, BAR_DAMPING))
                },
            exit =
                if (reduceMotion) {
                    fadeOut(tween(150))
                } else {
                    scaleOut(tween(150), targetScale = 0.9f, transformOrigin = TransformOrigin(0.5f, 1f)) +
                        fadeOut(tween(150))
                },
        ) {
            Box {
                EmojiBar(
                    emojis = emojis,
                    size = size,
                    align = align,
                    colors = colors,
                    reduceMotion = reduceMotion,
                    onHoldStart = { emoji, origin ->
                        react(emoji, origin)
                        holding?.cancel()
                        holding = scope.launch { repeatWhileHeld(emoji, origin, ::react) }
                    },
                    onHoldEnd = {
                        holding?.cancel()
                        holding = null
                    },
                )

                // Overlaid rather than handed to the bar, so a copy knows where in the bar
                // it started from and nothing has to be threaded back down.
                particles.forEach { particle ->
                    key(particle.id) {
                        FlyingEmoji(
                            particle = particle,
                            size = size.emoji,
                            blurs = PLATFORM_BLURS,
                            onFinish = { finished -> particles.removeAll { it.id == finished } },
                        )
                    }
                }
            }
        }
    }
}

/** Keeps sending copies for as long as an emoji is held down. */
private suspend fun repeatWhileHeld(
    emoji: String,
    origin: Offset,
    react: (String, Offset) -> Unit,
) {
    while (true) {
        delay(EMOJI_HOLD_INTERVAL_MILLIS)
        react(emoji, origin)
    }
}
