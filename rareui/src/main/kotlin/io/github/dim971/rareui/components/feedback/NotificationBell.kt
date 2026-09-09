/*
 * NotificationBell.kt
 * A port of upstream's `components/ui/notification-bell.tsx`.
 *
 * A bell that rings when the count goes up. Nothing has to be tapped: the arrival of a
 * notification is the event, and the harder it arrives the further the bell swings.
 */

package io.github.dim971.rareui.components.feedback

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.dim971.rareui.core.RareUiSpringState
import io.github.dim971.rareui.theme.RareUiColors
import io.github.dim971.rareui.theme.RareUiTheme
import io.github.dim971.rareui.theme.rareUiSpring
import io.github.dim971.rareui.theme.rememberRareUiReduceMotion
import kotlinx.coroutines.delay
import kotlin.math.min

/** The colour of a [NotificationBell]'s badge. */
public enum class NotificationBellColor {
    /** The system red, and the default. */
    RED,

    /** The system orange. */
    ORANGE,

    /** The system green. */
    GREEN,

    /** The system blue. */
    BLUE,

    /** The system violet. */
    VIOLET,
    ;

    internal fun resolve(colors: RareUiColors): Color =
        when (this) {
            RED -> colors.red
            ORANGE -> colors.orange
            GREEN -> colors.green
            BLUE -> colors.blue
            VIOLET -> colors.violet
        }
}

/** How a [NotificationBell] shows that there is something waiting. */
public enum class NotificationBellVariant {
    /** A badge with the number in it. */
    COUNT,

    /** A small dot, with no number. */
    DOT,
}

/** The icon's size as a fraction of the button's. */
private const val ICON_SCALE = 0.56f

/** The badge's size as a fraction of the button's, with a number in it. */
private const val BADGE_SCALE = 0.38f

/** And without one. */
private const val DOT_SCALE = 0.22f

/** The badge's type size as a fraction of the button's size. */
private const val FONT_SCALE = 0.21f

/** The padding either side of the number, as a fraction of the button's size. */
private const val PAD_SCALE = 0.09f

/** How far out the badge sits. One would put it exactly on the button's edge. */
private const val ORBIT = 0.9

/** The badge's arrival, from upstream's `ENTER_SPRING`. */
private const val ENTER_STIFFNESS = 600f
private const val ENTER_DAMPING = 20f

/** Upstream's `SWING_SPRING`: low damping, so the bell keeps swinging for a while. */
private const val SWING_STIFFNESS = 220.0
private const val SWING_DAMPING = 10.0

/** Upstream's `CLAPPER_SPRING`. */
private const val CLAPPER_STIFFNESS = 300.0
private const val CLAPPER_DAMPING = 14.0

/** How long a frame is allowed to represent, so a late one does not fire the bell round. */
private const val LONGEST_FRAME = 0.05

/**
 * How long the frame loop keeps running after the last ring.
 *
 * Motion stops its own loop once the spring reaches rest. There is no equivalent signal
 * here, so the loop is wound down after long enough for this spring to have settled from
 * its hardest possible push, which is under two seconds.
 */
private const val SETTLE_MILLIS = 2_500L

/** Upstream writes the press as `active:scale-90`. */
private const val PRESSED_SCALE = 0.9f

/**
 * A bell that swings when its count goes up.
 *
 * ```kotlin
 * NotificationBell(count = unread)
 * NotificationBell(count = unread, variant = NotificationBellVariant.DOT)
 * ```
 *
 * The ring is a push rather than a destination: the bell is given a velocity in the
 * direction it is already travelling, so notifications arriving in quick succession make it
 * swing harder rather than resetting it. Five at once is as hard as it gets.
 *
 * With animations turned off on the device the bell does not swing and the badge fades
 * rather than springing.
 *
 * @param count how many notifications are waiting. A rise rings the bell.
 * @param modifier the modifier to apply.
 * @param max the largest number to write out. Above it the badge reads `99+`.
 * @param variant whether to show the number or only a dot.
 * @param size the button's width and height. Everything else is a fraction of it.
 * @param color the badge's colour.
 * @param onClick what to do when the bell is tapped.
 */
@Composable
public fun NotificationBell(
    count: Int,
    modifier: Modifier = Modifier,
    max: Int = 99,
    variant: NotificationBellVariant = NotificationBellVariant.COUNT,
    size: Dp = 48.dp,
    color: NotificationBellColor = NotificationBellColor.RED,
    onClick: (() -> Unit)? = null,
) {
    val colors = RareUiTheme.colors
    val reduceMotion = rememberRareUiReduceMotion()
    // A count below zero would ring the bell on the way back up to zero.
    val waiting = maxOf(0, count)

    val outline = remember { BellOutline() }
    val clock = remember { BellClock() }
    var frame by remember { mutableStateOf(BellFrame(0f, 0f)) }
    var ringing by remember { mutableStateOf(false) }
    var previous by remember { mutableStateOf(waiting) }

    LaunchedEffect(waiting, reduceMotion) {
        val delta = waiting - previous
        previous = waiting
        if (delta <= 0 || reduceMotion) return@LaunchedEffect
        clock.ring(delta)
        ringing = true
        // Keyed on the count, so a second notification arriving mid ring cancels this and
        // starts the wait again rather than cutting the swing short.
        delay(SETTLE_MILLIS)
        ringing = false
    }

    LaunchedEffect(ringing) {
        if (!ringing) {
            frame = BellFrame(0f, 0f)
            return@LaunchedEffect
        }
        // The loop is only wound up while the bell is actually moving. A settled spring
        // redrawn sixty times a second is sixty identical frames.
        clock.reset()
        while (true) {
            withFrameNanos { nanos -> frame = clock.advance(nanos) }
        }
    }

    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val press by animateFloatAsState(
        targetValue = if (pressed && !reduceMotion) PRESSED_SCALE else 1f,
        animationSpec = if (reduceMotion) snap() else tween(150),
        label = "bell-press",
    )

    val badgeSide = size * if (variant == NotificationBellVariant.DOT) DOT_SCALE else BADGE_SCALE
    val inset =
        bellBadgeInset(
            size = size.value.toDouble(),
            side = badgeSide.value.toDouble(),
            orbit = ORBIT,
        ).dp

    Box(
        modifier =
            modifier
                .graphicsLayer {
                    scaleX = press
                    scaleY = press
                }.semantics {
                    role = Role.Button
                    contentDescription =
                        if (waiting > 0) "Notifications, $waiting unread" else "Notifications"
                }.then(
                    if (onClick == null) {
                        Modifier
                    } else {
                        Modifier.clickable(
                            interactionSource = interaction,
                            indication = null,
                            onClick = onClick,
                        )
                    },
                ),
    ) {
        Box(
            modifier = Modifier.size(size).background(colors.surface, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Canvas(modifier = Modifier.size(size * ICON_SCALE)) {
                drawBell(outline, frame.swing, frame.clapper, colors.glyph)
            }
        }

        AnimatedVisibility(
            visible = waiting > 0,
            modifier =
                Modifier
                    .align(Alignment.TopEnd)
                    .offset(x = -inset, y = inset),
            enter =
                if (reduceMotion) {
                    fadeIn(tween(150))
                } else {
                    scaleIn(rareUiSpring(ENTER_STIFFNESS, ENTER_DAMPING)) +
                        fadeIn(rareUiSpring(ENTER_STIFFNESS, ENTER_DAMPING))
                },
            exit = if (reduceMotion) fadeOut(tween(150)) else scaleOut(tween(150)) + fadeOut(tween(150)),
        ) {
            Box(
                // The number sizes the badge rather than the other way round: a badge laid
                // out first and then given an overlay could not grow to hold three digits.
                modifier =
                    Modifier
                        .defaultMinSize(minWidth = badgeSide, minHeight = badgeSide)
                        .background(color.resolve(colors), RoundedCornerShape(percent = 50))
                        .padding(horizontal = size * PAD_SCALE),
                contentAlignment = Alignment.Center,
            ) {
                if (variant == NotificationBellVariant.COUNT) {
                    BadgeNumber(
                        count = waiting,
                        max = max,
                        fontSize = (size.value * FONT_SCALE).sp,
                        reduceMotion = reduceMotion,
                    )
                }
            }
        }
    }
}

/** The angles to draw one frame of the bell at. */
internal data class BellFrame(
    val swing: Float,
    val clapper: Float,
)

/**
 * The bell's swing and its clapper, stepped one frame at a time.
 *
 * Both are springs integrated by hand. Compose can be handed a starting velocity, which
 * SwiftUI cannot, but the clapper follows the bell's speed from one frame to the next
 * rather than heading anywhere, and that is not a target an animation can be given.
 *
 * A plain class rather than anything observable: it is read inside the frame loop and its
 * result is published as one value, so mutating it must not invalidate a composition.
 */
internal class BellClock {
    private val swing = RareUiSpringState()
    private val clapper = RareUiSpringState()
    private var last = 0L

    /** Forgets the last frame's timestamp, so the next step is not an enormous one. */
    fun reset() {
        last = 0L
    }

    /**
     * Steps both springs up to a frame.
     *
     * @param nanos the frame's timestamp.
     * @return the angles to draw the bell and its clapper at.
     */
    fun advance(nanos: Long): BellFrame {
        val elapsed = if (last == 0L) 0.0 else min((nanos - last) / 1e9, LONGEST_FRAME)
        last = nanos

        swing.advance(0.0, elapsed, SWING_STIFFNESS, SWING_DAMPING)
        clapper.advance(
            target = bellClapperLag(swing.velocity),
            elapsed = elapsed,
            stiffness = CLAPPER_STIFFNESS,
            damping = CLAPPER_DAMPING,
        )

        return BellFrame(swing.value.toFloat(), clapper.value.toFloat())
    }

    /**
     * Rings the bell.
     *
     * @param delta how many notifications arrived at once.
     */
    fun ring(delta: Int) {
        swing.velocity = bellRingVelocity(swing.velocity, delta)
    }
}
