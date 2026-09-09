/*
 * EmojiReactionParts.kt
 * The pieces EmojiReaction is assembled from: the bar, one emoji in it, the trigger's face
 * and one copy on its way up.
 */

package io.github.dim971.rareui.components.feedback

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.github.dim971.rareui.theme.RareUiColors
import io.github.dim971.rareui.theme.RareUiEasing
import io.github.dim971.rareui.theme.rareUiSpring
import kotlinx.coroutines.delay
import androidx.compose.ui.graphics.Path as GraphicsPath

/** The lift a pointer gives an emoji, and the squash a press gives it. */
private const val HOVER_SCALE = 1.28f
private const val PRESS_SCALE = 0.92f
private const val ARRIVING_SCALE = 0.4f

/** Each emoji's own arrival, which is stiffer than the bar's so they pop rather than drift in. */
private const val EMOJI_STIFFNESS = 800f
private const val EMOJI_DAMPING = 25f

/** The stagger down the bar, in milliseconds. */
private const val EMOJI_ENTRY_DELAY = 40L
private const val EMOJI_ENTRY_STAGGER = 35L

/**
 * One emoji in the bar, with its arrival, its pointer lift and its press.
 *
 * @param emoji the glyph.
 * @param size how large the component is drawn.
 * @param index where in the bar it sits, which sets its arrival delay.
 * @param reduceMotion whether to appear without springing.
 * @param onHoldStart called when it is pressed, with its centre in the bar's own space.
 * @param onHoldEnd called when the press ends.
 */
@Composable
internal fun EmojiButton(
    emoji: String,
    size: EmojiReactionSize,
    index: Int,
    reduceMotion: Boolean,
    onHoldStart: (Offset) -> Unit,
    onHoldEnd: () -> Unit,
) {
    val density = LocalDensity.current
    val interaction = remember { MutableInteractionSource() }
    val hovering by interaction.collectIsHoveredAsState()

    var arrived by remember { mutableStateOf(reduceMotion) }
    var pressing by remember { mutableStateOf(false) }
    var centre by remember { mutableStateOf(Offset.Zero) }

    LaunchedEffect(reduceMotion) {
        if (reduceMotion) {
            arrived = true
            return@LaunchedEffect
        }
        delay(EMOJI_ENTRY_DELAY + index * EMOJI_ENTRY_STAGGER)
        arrived = true
    }

    val scale by animateFloatAsState(
        targetValue =
            when {
                pressing -> PRESS_SCALE
                hovering && !reduceMotion -> HOVER_SCALE
                arrived -> 1f
                else -> ARRIVING_SCALE
            },
        animationSpec = if (reduceMotion) snap() else rareUiSpring(EMOJI_STIFFNESS, EMOJI_DAMPING),
        label = "emoji-scale",
    )
    val lift by animateFloatAsState(
        targetValue = if (hovering && !reduceMotion) -4f else 0f,
        animationSpec = if (reduceMotion) snap() else rareUiSpring(EMOJI_STIFFNESS, EMOJI_DAMPING),
        label = "emoji-lift",
    )
    val fade by animateFloatAsState(
        targetValue = if (arrived) 1f else 0f,
        animationSpec = if (reduceMotion) snap() else rareUiSpring(EMOJI_STIFFNESS, EMOJI_DAMPING),
        label = "emoji-fade",
    )

    BasicText(
        text = emoji,
        style = TextStyle(fontSize = with(density) { size.emoji.toSp() }),
        modifier =
            Modifier
                .padding(4.dp)
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                    translationY = lift * this.density
                    alpha = fade
                }.onGloballyPositioned { coordinates ->
                    centre =
                        coordinates.positionInParent() +
                        Offset(coordinates.size.width / 2f, coordinates.size.height / 2f)
                }.hoverable(interaction)
                // A press has to start the repeat and a release has to stop it, which a
                // plain clickable cannot say, so the gesture is written out.
                .pointerInput(emoji) {
                    awaitPointerEventScope {
                        while (true) {
                            awaitPointerEvent()
                            if (!pressing) {
                                pressing = true
                                onHoldStart(centre)
                            } else if (currentEvent.changes.none { it.pressed }) {
                                pressing = false
                                onHoldEnd()
                            }
                        }
                    }
                }.semantics {
                    role = Role.Button
                    contentDescription = emoji
                    onClick {
                        onHoldStart(centre)
                        onHoldEnd()
                        true
                    }
                },
    )
}

/**
 * The bar of emoji, and the tail that points back at the trigger.
 *
 * @param emojis the glyphs to offer.
 * @param size how large the component is drawn.
 * @param align which edge of the trigger the bar lines up with.
 * @param colors the palette.
 * @param reduceMotion whether the emoji appear without springing.
 * @param onHoldStart called when one is pressed, with its centre in the bar's own space.
 * @param onHoldEnd called when the press ends.
 * @param modifier the modifier to apply.
 */
@Composable
@Suppress("LongParameterList")
internal fun EmojiBar(
    emojis: List<String>,
    size: EmojiReactionSize,
    align: EmojiReactionAlign,
    colors: RareUiColors,
    reduceMotion: Boolean,
    onHoldStart: (String, Offset) -> Unit,
    onHoldEnd: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.semantics { contentDescription = "Pick a reaction" },
        horizontalAlignment =
            when (align) {
                EmojiReactionAlign.LEADING -> Alignment.Start
                EmojiReactionAlign.CENTER -> Alignment.CenterHorizontally
                EmojiReactionAlign.TRAILING -> Alignment.End
            },
    ) {
        Row(
            modifier =
                Modifier
                    .background(colors.surface, RoundedCornerShape(percent = 50))
                    .padding(size.padding),
            horizontalArrangement = Arrangement.spacedBy(size.spacing),
        ) {
            emojis.forEachIndexed { index, emoji ->
                EmojiButton(
                    emoji = emoji,
                    size = size,
                    index = index,
                    reduceMotion = reduceMotion,
                    onHoldStart = { onHoldStart(emoji, it) },
                    onHoldEnd = onHoldEnd,
                )
            }
        }

        // Two circles of falling size, which is how a message bubble points at whoever
        // sent it.
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = size.padding + size.emoji / 2 - 6.dp)
                    .graphicsLayer { translationY = -4 * density },
            horizontalAlignment =
                when (align) {
                    EmojiReactionAlign.LEADING -> Alignment.Start
                    EmojiReactionAlign.CENTER -> Alignment.CenterHorizontally
                    EmojiReactionAlign.TRAILING -> Alignment.End
                },
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Box(Modifier.size(12.dp).background(colors.surface, CircleShape))
            Box(Modifier.size(6.dp).background(colors.surface, CircleShape))
        }
    }
}

/** The curves the flight's six values are read out on, from upstream's per value easings. */
private val FlightEaseOut: Easing = CubicBezierEasing(0f, 0f, 0.58f, 1f)
private val FlightEaseInOut: Easing = CubicBezierEasing(0.42f, 0f, 0.58f, 1f)

/**
 * One copy on its way up, which removes itself when it gets there.
 *
 * @param particle what this copy is doing.
 * @param size the emoji's own size.
 * @param blurs whether the platform can blur, which it can from API 31.
 * @param onFinish called with the particle's identifier once it has arrived.
 */
@Composable
internal fun FlyingEmoji(
    particle: EmojiParticle,
    size: Dp,
    blurs: Boolean,
    onFinish: (Int) -> Unit,
) {
    val density = LocalDensity.current
    val progress = remember { Animatable(0f) }

    LaunchedEffect(particle.id) {
        delay((particle.delay * 1000).toLong())
        // The flight's own easing is applied per value below, one curve each, so the
        // progress driving them has to be plain linear.
        progress.animateTo(1f, tween((particle.duration * 1000).toInt(), easing = LinearEasing))
        onFinish(particle.id)
    }

    val at = progress.value
    // The climb shares its curve with the rise. Giving it one of its own bends the path
    // sideways, which is what upstream's note about overriding it means.
    val climb = RareUiEasing.EaseParticle.transform(at)

    val scale =
        emojiKeyframe(
            floatArrayOf(0.6f, particle.scale * 1.15f, particle.scale, particle.scale * 0.75f),
            floatArrayOf(0f, 0.1f, 0.22f, 1f),
            at,
            FlightEaseOut,
        )
    val tilt =
        emojiKeyframe(
            floatArrayOf(0f, particle.tilt, -particle.tilt * 0.65f, particle.tilt * 0.35f),
            floatArrayOf(0f, 0.3f, 0.65f, 1f),
            at,
            FlightEaseInOut,
        )
    val smear =
        emojiKeyframe(
            floatArrayOf(0f, 0f, particle.blurRatio),
            floatArrayOf(0f, 0.12f, 1f),
            at,
            RareUiEasing.EaseParticle,
        )
    val fade =
        emojiKeyframe(
            floatArrayOf(0f, 1f, 1f, 0f),
            floatArrayOf(0f, 0.03f, particle.fadeAt, 1f),
            at,
            LinearEasing,
        )

    BasicText(
        text = particle.glyph,
        style = TextStyle(fontSize = with(density) { size.toSp() }),
        modifier =
            Modifier
                .graphicsLayer {
                    // Positioned about its own centre at the emoji it came from, then
                    // carried up and across by the flight.
                    translationX = particle.origin.x + (particle.launch + particle.drift * climb) * this.density
                    translationY = particle.origin.y - particle.travel * climb * this.density
                    scaleX = scale
                    scaleY = scale
                    rotationZ = tilt
                    // Below API 31 there is no blur to apply, so the softening it stands
                    // for is taken out of the opacity instead. See docs/fidelity.md.
                    alpha = if (blurs) fade else fade * (1f - smear)
                    transformOrigin = androidx.compose.ui.graphics.TransformOrigin.Center
                }.then(if (blurs) Modifier.blur(size * smear) else Modifier),
    )
}

/** The side of the box upstream's smile icon is drawn in. */
private const val SMILE_VIEW_BOX = 24f

/**
 * The face on the trigger before anything has been picked, quoted from upstream's
 * `SmileIcon`: an open circle, two eyes, a smile and a sparkle off the top right.
 *
 * Parsed once and kept, since a path built from a string in a draw pass is a string parsed
 * on every frame.
 */
internal class SmileOutline {
    val strokes: GraphicsPath =
        PathParser()
            .parsePathString("M21 12a9 9 0 1 1-9-9M8 13.9a4.7 4.7 0 0 0 8 0M19 2.5v5M21.5 5h-5")
            .toPath()

    val eyes: GraphicsPath =
        GraphicsPath().apply {
            fillType = PathFillType.NonZero
            addOval(Rect(8.9f - 0.7f, 10f - 0.7f, 8.9f + 0.7f, 10f + 0.7f))
            addOval(Rect(15.1f - 0.7f, 10f - 0.7f, 15.1f + 0.7f, 10f + 0.7f))
        }
}

/**
 * Draws the smile, filling the whole of this scope.
 *
 * @param outline the parsed paths.
 * @param color the ink.
 * @param strokeWidth the line width, in pixels, which does not grow with the icon.
 */
internal fun DrawScope.drawSmile(
    outline: SmileOutline,
    color: Color,
    strokeWidth: Float,
) {
    val factor = size.minDimension / SMILE_VIEW_BOX
    scale(factor, pivot = Offset.Zero) {
        // The stroke is divided back out so it stays the same weight whatever the icon's
        // size, which is what stroking the scaled path in points does on the other platform.
        drawPath(
            path = outline.strokes,
            color = color,
            style = Stroke(width = strokeWidth / factor, cap = StrokeCap.Round),
        )
        drawPath(path = outline.eyes, color = color)
    }
}

/**
 * Draws the cross the trigger shows while the bar is open.
 *
 * @param color the ink.
 * @param strokeWidth the line width, in pixels.
 */
internal fun DrawScope.drawCross(
    color: Color,
    strokeWidth: Float,
) {
    val inset = size.minDimension * 0.2f
    drawLine(
        color = color,
        start = Offset(inset, inset),
        end = Offset(size.width - inset, size.height - inset),
        strokeWidth = strokeWidth,
        cap = StrokeCap.Round,
    )
    drawLine(
        color = color,
        start = Offset(size.width - inset, inset),
        end = Offset(inset, size.height - inset),
        strokeWidth = strokeWidth,
        cap = StrokeCap.Round,
    )
}

/**
 * What the trigger shows: a cross while the bar is open, the last reaction once one has been
 * picked, and a face before that.
 *
 * @param open whether the bar is showing.
 * @param last the last emoji picked, if there is one.
 * @param size how large the component is drawn.
 * @param colors the palette.
 */
@Composable
internal fun EmojiTriggerFace(
    open: Boolean,
    last: String?,
    size: EmojiReactionSize,
    colors: RareUiColors,
) {
    val density = LocalDensity.current
    val outline = remember { SmileOutline() }
    val ink = colors.foreground.copy(alpha = 0.6f)

    Box(
        modifier = Modifier.size(size.trigger).background(colors.surface, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        when {
            open ->
                Canvas(Modifier.size(size.icon * 0.8f)) {
                    drawCross(ink, strokeWidth = 2.dp.toPx())
                }

            last != null ->
                BasicText(
                    text = last,
                    style = TextStyle(fontSize = with(density) { (size.emoji * 0.72f).toSp() }),
                )

            else ->
                Canvas(Modifier.size(size.icon)) {
                    drawSmile(outline, ink, strokeWidth = 1.7.dp.toPx())
                }
        }
    }
}
