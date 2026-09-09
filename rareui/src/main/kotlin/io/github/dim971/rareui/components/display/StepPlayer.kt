/*
 * StepPlayer.kt
 * A port of upstream's `components/ui/step-player.tsx`.
 *
 * An iOS style step track: a row of dots where the current one stretches into a bar and
 * fills left to right, with a play control beside it. The proportions are all fractions of
 * one size, so it holds together from a toolbar glyph to a hero control.
 *
 * One change of unit, recorded in docs/fidelity.md: durations are seconds here rather than
 * upstream's milliseconds, so that a duration in this library always means the same thing.
 */

package io.github.dim971.rareui.components.display

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.github.dim971.rareui.theme.RareUiColors
import io.github.dim971.rareui.theme.RareUiEasing
import io.github.dim971.rareui.theme.RareUiTheme
import io.github.dim971.rareui.theme.rareUiVisualSpring
import io.github.dim971.rareui.theme.rememberRareUiReduceMotion
import kotlin.math.roundToInt

/** One step of a [StepPlayer]. */
@Immutable
public data class StepPlayerStep(
    /** How long this step lasts, in seconds, or `null` to take the player's own duration. */
    public val duration: Double? = null,
    /** A name for the step, read out when the track is seekable. */
    public val label: String? = null,
)

/** Which side of the track the play control sits on. */
public enum class StepPlayerControlPosition {
    /** Before the track. */
    LEADING,

    /** After it, which is upstream's default. */
    TRAILING,
}

/**
 * Everything about a player's size, derived from the one number the caller gives.
 *
 * The ratios are traced from the iOS control upstream is following, and they are what makes
 * the component hold together at any size rather than only at forty-eight points.
 */
@Immutable
internal class StepPlayerMetrics(
    size: Dp,
) {
    /** The track's height, which is also the control's diameter. */
    val track: Dp = maxOf(12.dp, size)

    /** One step at rest. */
    val dot: Dp = maxOf(2.dp, (track.value * 0.115f).roundToInt().dp)

    /** And the step being played, stretched. */
    val bar: Dp = (dot.value * 8.2f).roundToInt().dp

    /** The space between two steps. */
    val gap: Dp = maxOf(2.dp, (track.value * 0.18f).roundToInt().dp)

    /** The same inset the dot leaves above and below it, so the row is centred. */
    val pad: Dp = ((track.value - dot.value) / 2).roundToInt().dp

    /** The glyph inside the control. */
    val icon: Dp = (track.value * 0.64f).roundToInt().dp
}

/** The step's change of width, from upstream's `WIDTH_SPRING`. */
private const val WIDTH_DURATION = 0.42f
private const val WIDTH_BOUNCE = 0.14f

/** The icon's morph, from `ICON_SPRING`. */
private const val ICON_DURATION = 0.32f
private const val ICON_BOUNCE = 0.22f

/** The crossfade to and from replay, from `ICON_FADE`. */
private const val ICON_FADE_MILLIS = 260

/** How small an icon starts before it fades in, from `ENTER_SCALE`. */
private const val ENTER_SCALE = 0.82f

/** The press, from upstream's `TAP_SPRING`. */
private const val TAP_DURATION = 0.25f
private const val TAP_BOUNCE = 0.3f
private const val TAP_SCALE = 0.88f

/**
 * A row of steps that fills as it plays, with a control beside it.
 *
 * ```kotlin
 * StepPlayer(
 *     steps = List(5) { StepPlayerStep() },
 *     index = step,
 *     playing = playing,
 *     onIndexChange = { step = it },
 *     onPlayingChange = { playing = it },
 * )
 * ```
 *
 * With animations turned off on the device the steps change width without springing and the
 * icons swap without morphing, but the track still fills, because the fill is the
 * information rather than the decoration.
 *
 * @param steps the steps, in order.
 * @param index the step being played.
 * @param playing whether it is playing.
 * @param onIndexChange called when the track moves to another step.
 * @param onPlayingChange called when it starts or stops.
 * @param modifier the modifier to apply.
 * @param duration how long a step with no length of its own lasts, in seconds.
 * @param loop whether to start again at the end.
 * @param size the track's height, which every other measurement is a fraction of.
 * @param showsControl whether to draw the play control.
 * @param controlPosition which side the control sits on.
 * @param seekable whether a step can be tapped to jump to it.
 * @param onComplete called when the last step finishes.
 */
@Composable
@Suppress("LongParameterList", "CyclomaticComplexMethod")
public fun StepPlayer(
    steps: List<StepPlayerStep>,
    index: Int,
    playing: Boolean,
    onIndexChange: (Int) -> Unit,
    onPlayingChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    duration: Double = 4.0,
    loop: Boolean = false,
    size: Dp = 48.dp,
    showsControl: Boolean = true,
    controlPosition: StepPlayerControlPosition = StepPlayerControlPosition.TRAILING,
    seekable: Boolean = false,
    onComplete: (() -> Unit)? = null,
) {
    val colors = RareUiTheme.colors
    val reduceMotion = rememberRareUiReduceMotion()
    val metrics = remember(size) { StepPlayerMetrics(size) }
    val track = if (steps.isEmpty()) listOf(StepPlayerStep()) else steps
    val current = index.coerceIn(0, track.lastIndex)

    var progress by remember { mutableFloatStateOf(0f) }
    var finished by remember { mutableStateOf(false) }

    // A new step starts empty, whoever moved to it.
    LaunchedEffect(current) { progress = 0f }

    LaunchedEffect(current, playing, finished) {
        if (!playing || finished) return@LaunchedEffect
        val length = track[current].duration ?: duration
        if (length <= 0) return@LaunchedEffect

        var last = 0L
        while (true) {
            val now = withFrameNanos { it }
            if (last != 0L) {
                // Written straight rather than animated. The fill is a clock, and a spring
                // on a clock would make it lie about where the step has got to.
                progress += ((now - last) / 1e9 / length).toFloat()
            }
            last = now

            if (progress >= 1f) {
                progress = 1f
                when {
                    current < track.lastIndex -> onIndexChange(current + 1)
                    loop -> onIndexChange(0)
                    else -> {
                        finished = true
                        onPlayingChange(false)
                    }
                }
                if (current == track.lastIndex) onComplete?.invoke()
                return@LaunchedEffect
            }
        }
    }

    fun restart() {
        progress = 0f
        finished = false
        onIndexChange(0)
        onPlayingChange(true)
    }

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(metrics.gap),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (showsControl && controlPosition == StepPlayerControlPosition.LEADING) {
            TransportControl(metrics, colors, reduceMotion, finished, playing) {
                if (finished) restart() else onPlayingChange(!playing)
            }
        }

        StepTrack(
            steps = track,
            current = current,
            progress = { progress },
            metrics = metrics,
            colors = colors,
            reduceMotion = reduceMotion,
            seekable = seekable,
            onSeek = { position ->
                progress = 0f
                finished = false
                onIndexChange(position)
                onPlayingChange(true)
            },
        )

        if (showsControl && controlPosition == StepPlayerControlPosition.TRAILING) {
            TransportControl(metrics, colors, reduceMotion, finished, playing) {
                if (finished) restart() else onPlayingChange(!playing)
            }
        }
    }
}

/** The row of steps, on its own capsule. */
@Composable
@Suppress("LongParameterList")
private fun StepTrack(
    steps: List<StepPlayerStep>,
    current: Int,
    progress: () -> Float,
    metrics: StepPlayerMetrics,
    colors: RareUiColors,
    reduceMotion: Boolean,
    seekable: Boolean,
    onSeek: (Int) -> Unit,
) {
    Row(
        modifier =
            Modifier
                .height(metrics.track)
                .background(colors.surface, RoundedCornerShape(percent = 50))
                .padding(horizontal = metrics.pad)
                .semantics { contentDescription = "Step ${current + 1} of ${steps.size}" },
        horizontalArrangement = Arrangement.spacedBy(metrics.gap),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        steps.forEachIndexed { position, step ->
            val isActive = position == current
            val isPast = position < current

            val width by animateDpAsState(
                targetValue = if (isActive) metrics.bar else metrics.dot,
                animationSpec =
                    if (reduceMotion) snap() else rareUiVisualSpring(WIDTH_DURATION, WIDTH_BOUNCE),
                label = "step-width",
            )

            Box(
                modifier =
                    Modifier
                        .width(width)
                        .height(metrics.dot)
                        .background(if (isPast) colors.track else colors.glyph, CircleShape)
                        .drawBehind {
                            if (!isActive) return@drawBehind
                            // Read here rather than in composition, so a filling step costs
                            // one draw a frame and no recomposition at all.
                            drawRoundRect(
                                color = colors.track,
                                size = Size(size.width * progress().coerceIn(0f, 1f), size.height),
                                cornerRadius = CornerRadius(size.height / 2),
                            )
                        }.then(
                            if (seekable) {
                                Modifier.clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null,
                                    onClick = { onSeek(position) },
                                )
                            } else {
                                Modifier
                            },
                        ).semantics {
                            contentDescription = step.label ?: "Step ${position + 1}"
                        },
            )
        }
    }
}

/** The play control, which morphs between play and pause and crossfades to replay. */
@Composable
private fun TransportControl(
    metrics: StepPlayerMetrics,
    colors: RareUiColors,
    reduceMotion: Boolean,
    finished: Boolean,
    playing: Boolean,
    onClick: () -> Unit,
) {
    val glyph = remember { Path() }
    val replay = remember { replayPath() }

    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val press by animateFloatAsState(
        targetValue = if (pressed && !reduceMotion) TAP_SCALE else 1f,
        animationSpec = if (reduceMotion) snap() else rareUiVisualSpring(TAP_DURATION, TAP_BOUNCE),
        label = "step-player-press",
    )

    val morph by animateFloatAsState(
        targetValue = if (playing) 0f else 1f,
        animationSpec = if (reduceMotion) snap() else rareUiVisualSpring(ICON_DURATION, ICON_BOUNCE),
        label = "step-player-morph",
    )
    val replayed by animateFloatAsState(
        targetValue = if (finished) 1f else 0f,
        animationSpec =
            if (reduceMotion) snap() else tween(ICON_FADE_MILLIS, easing = RareUiEasing.EaseOutSettle),
        label = "step-player-replay",
    )

    Canvas(
        modifier =
            Modifier
                .size(metrics.track)
                .graphicsLayer {
                    scaleX = press
                    scaleY = press
                }.background(colors.surface, CircleShape)
                .clickable(
                    interactionSource = interaction,
                    indication = null,
                    onClick = onClick,
                ).semantics {
                    role = Role.Button
                    contentDescription =
                        when {
                            finished -> "Replay"
                            playing -> "Pause"
                            else -> "Play"
                        }
                },
    ) {
        val box = metrics.icon.toPx()
        val factor = box / TRANSPORT_BOX
        translate(left = (size.width - box) / 2, top = (size.height - box) / 2) {
            scale(factor, pivot = Offset.Zero) {
                if (replayed < 1f) {
                    transportPath(morph, glyph)
                    scale(
                        scale = 1f - (1f - ENTER_SCALE) * replayed,
                        pivot = Offset(TRANSPORT_BOX / 2, TRANSPORT_BOX / 2),
                    ) {
                        drawPath(glyph, colors.glyph, alpha = 1f - replayed)
                    }
                }
                if (replayed > 0f) {
                    scale(
                        scale = ENTER_SCALE + (1f - ENTER_SCALE) * replayed,
                        pivot = Offset(TRANSPORT_BOX / 2, TRANSPORT_BOX / 2),
                    ) {
                        drawPath(replay, colors.glyph, alpha = replayed)
                    }
                }
            }
        }
    }
}
