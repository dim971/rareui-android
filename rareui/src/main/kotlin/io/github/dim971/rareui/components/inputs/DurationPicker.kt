/*
 * DurationPicker.kt
 * A port of upstream's `components/ui/duration-picker.tsx`.
 *
 * Three touching rounded panels, an hours field, a minutes field and a pen. Pressing the
 * pen separates them, opens the fields for editing and turns the pen into a tick.
 *
 * Two departures, recorded in docs/fidelity.md. The panels have plain rounded corners
 * rather than the squircle upstream builds with figma-squircle at full corner smoothing,
 * which Compose has no shape for. And the lean the contents take on as the picker opens is
 * taken from the direction of travel rather than from the gap's own velocity, which comes
 * out the same at both ends and slightly gentler in between.
 */

package io.github.dim971.rareui.components.inputs

import androidx.compose.animation.core.Animatable
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
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.dim971.rareui.core.RareUiPathMorph
import io.github.dim971.rareui.theme.RareUiColors
import io.github.dim971.rareui.theme.RareUiTheme
import io.github.dim971.rareui.theme.rareUiSpring
import io.github.dim971.rareui.theme.rememberRareUiReduceMotion

/** A length of time, as hours and minutes. */
@Immutable
public data class DurationValue(
    /** Whole hours. */
    public val hours: Int = 0,
    /** Whole minutes. */
    public val minutes: Int = 0,
)

/**
 * Confines a typed number to what the field will accept.
 *
 * Anything that is not a number at all becomes zero, which is what upstream's `|| 0` does
 * and what stops a half typed minus sign emptying the field.
 *
 * @param raw the number typed.
 * @param limit the largest the field accepts.
 * @return the number the field will hold.
 */
internal fun durationClamp(
    raw: Int,
    limit: Int,
): Int = raw.coerceIn(0, limit)

/** How far the segments separate. */
private val OpenGap = 8.dp

/** The outer corner radius. */
private val PickerRadius = 12.dp

/** Every segment's height. */
private val PickerHeight = 48.dp

/** How wide a field opens to while it is being edited. */
private val EditingFieldWidth = 44.dp

/** The separation's spring, from upstream's `GAP_SPRING`. */
private const val GAP_STIFFNESS = 200f
private const val GAP_DAMPING = 28f

/** The fields' width, from `WIDTH_SPRING`. */
private const val WIDTH_STIFFNESS = 250f
private const val WIDTH_DAMPING = 31f

/** The pen becoming a tick, from `ICON_SPRING`. */
private const val ICON_STIFFNESS = 200f
private const val ICON_DAMPING = 28f

/** The shake on an out of range entry, from `ERROR_SPRING`. Stiff and barely damped. */
private const val ERROR_STIFFNESS = 700f
private const val ERROR_DAMPING = 9f

/** How far the contents lean as the picker opens. */
private const val SWAY = 3f

/** The pen and the tick, quoted from upstream's `PEN_PATH` and `TICK_PATH`. */
private const val PEN_PATH =
    "M3.78181 16.3092L3 21L7.69086 20.2182C8.50544 20.0825 9.25725 19.6956 9.84119 " +
        "19.1116L20.4198 8.53288C21.1934 7.75922 21.1934 6.5049 20.4197 5.73126L18.2687 " +
        "3.58024C17.495 2.80658 16.2406 2.80659 15.4669 3.58027L4.88841 14.159C4.30447 " +
        "14.7429 3.91757 15.4947 3.78181 16.3092Z"
private const val TICK_PATH =
    "M7.959 20.513L1.592 12.872L3.128 11.592L8.041 17.487L20.947 3.587L22.413 4.948L7.959 20.513Z"

/** The nib line across the pen, drawn in the surface colour so it reads as a gap. */
private const val NIB_PATH = "M14 6L18 10"

/** The box the three glyphs were drawn in. */
private val IconBox = Size(24f, 24f)

/**
 * Three touching panels that separate to be edited.
 *
 * ```kotlin
 * DurationPicker(value = duration, onValueChange = { duration = it }, onConfirm = ::schedule)
 * ```
 *
 * With animations turned off on the device the segments separate without springing and the
 * pen becomes a tick without morphing.
 *
 * @param value the duration.
 * @param onValueChange called with the duration as it is edited.
 * @param modifier the modifier to apply.
 * @param maxHours the largest number of hours accepted.
 * @param maxMinutes the largest number of minutes accepted.
 * @param hoursLabel the word after the hours field.
 * @param minutesLabel the word after the minutes field.
 * @param enabled whether the picker can be edited.
 * @param onConfirm called with the duration when the tick is pressed.
 */
@Composable
@Suppress("LongParameterList")
public fun DurationPicker(
    value: DurationValue,
    onValueChange: (DurationValue) -> Unit,
    modifier: Modifier = Modifier,
    maxHours: Int = 24,
    maxMinutes: Int = 60,
    hoursLabel: String = "Hr.",
    minutesLabel: String = "Min.",
    enabled: Boolean = true,
    onConfirm: ((DurationValue) -> Unit)? = null,
) {
    val colors = RareUiTheme.colors
    val reduceMotion = rememberRareUiReduceMotion()
    val keyboard = LocalSoftwareKeyboardController.current
    val hoursFocus = remember { FocusRequester() }

    var editing by remember { mutableStateOf(false) }
    var hoursText by remember { mutableStateOf("") }
    var minutesText by remember { mutableStateOf("") }
    val nudge = remember { Animatable(0f) }
    // Counted rather than flagged, so two rejections in a row shake twice instead of the
    // second one finding the flag already set and doing nothing.
    var rejections by remember { mutableIntStateOf(0) }

    LaunchedEffect(rejections) {
        if (rejections == 0 || reduceMotion) return@LaunchedEffect
        nudge.snapTo(6f)
        nudge.animateTo(0f, rareUiSpring(ERROR_STIFFNESS, ERROR_DAMPING))
    }

    LaunchedEffect(value) {
        hoursText = if (value.hours == 0) "" else value.hours.toString()
        minutesText = if (value.minutes == 0) "" else value.minutes.toString()
    }

    val openness by animateFloatAsState(
        targetValue = if (editing) 1f else 0f,
        animationSpec = if (reduceMotion) snap() else rareUiSpring(GAP_STIFFNESS, GAP_DAMPING),
        label = "duration-openness",
    )
    val innerRadius = PickerRadius * openness
    // The closed gap pulls in by a point, so two touching panels read as one shape rather
    // than showing a hairline between them.
    val gap = maxOf(0.dp, OpenGap * openness - (1f - openness).dp)
    val sway = SWAY * openness

    fun commit() {
        onValueChange(
            DurationValue(
                hours = durationClamp(hoursText.toIntOrNull() ?: 0, maxHours),
                minutes = durationClamp(minutesText.toIntOrNull() ?: 0, maxMinutes),
            ),
        )
    }

    Row(
        modifier = modifier.graphicsLayer { alpha = if (enabled) 1f else 0.5f },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        PickerSegment(
            leading = PickerRadius,
            trailing = innerRadius,
            padStart = 8.dp,
            padEnd = 3.dp + 9.dp * openness,
            colors = colors,
        ) {
            DurationField(
                text = hoursText,
                onTextChange = { typed ->
                    val kept = typed.filter { it.isDigit() }.take(3)
                    if (kept.toIntOrNull()?.let { it > maxHours } == true) rejections++
                    hoursText = kept
                },
                editing = editing,
                reduceMotion = reduceMotion,
                nudge = { nudge.value },
                sway = sway,
                colors = colors,
                label = hoursLabel,
                modifier = Modifier.focusRequester(hoursFocus),
            )
            SegmentLabel(hoursLabel, FontWeight.SemiBold, sway, colors)
        }

        Spacer(Modifier.width(gap))

        PickerSegment(
            leading = innerRadius,
            trailing = innerRadius,
            padStart = 9.dp * openness,
            padEnd = 3.dp + 9.dp * openness,
            colors = colors,
        ) {
            DurationField(
                text = minutesText,
                onTextChange = { typed ->
                    val kept = typed.filter { it.isDigit() }.take(3)
                    if (kept.toIntOrNull()?.let { it > maxMinutes } == true) rejections++
                    minutesText = kept
                },
                editing = editing,
                reduceMotion = reduceMotion,
                nudge = { nudge.value },
                sway = sway,
                colors = colors,
                label = minutesLabel,
            )
            SegmentLabel(minutesLabel, FontWeight.Medium, sway, colors)
        }

        Spacer(Modifier.width(gap))

        PenToggle(
            editing = editing,
            innerRadius = innerRadius,
            colors = colors,
            reduceMotion = reduceMotion,
            onClick = {
                val next = !editing
                editing = next
                if (next) {
                    hoursFocus.requestFocus()
                } else {
                    keyboard?.hide()
                    commit()
                    onConfirm?.invoke(
                        DurationValue(
                            hours = durationClamp(hoursText.toIntOrNull() ?: 0, maxHours),
                            minutes = durationClamp(minutesText.toIntOrNull() ?: 0, maxMinutes),
                        ),
                    )
                }
            },
        )
    }
}

/** One of the three panels. */
@Composable
private fun PickerSegment(
    leading: Dp,
    trailing: Dp,
    padStart: Dp,
    padEnd: Dp,
    colors: RareUiColors,
    content: @Composable () -> Unit,
) {
    Row(
        modifier =
            Modifier
                .height(PickerHeight)
                .background(
                    color = colors.surface,
                    shape =
                        RoundedCornerShape(
                            topStart = leading,
                            bottomStart = leading,
                            topEnd = trailing,
                            bottomEnd = trailing,
                        ),
                ).padding(start = padStart, end = padEnd),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        content()
    }
}

/** One of the two numbers, which grows to a fixed width while it is being edited. */
@Composable
@Suppress("LongParameterList")
private fun DurationField(
    text: String,
    onTextChange: (String) -> Unit,
    editing: Boolean,
    reduceMotion: Boolean,
    nudge: () -> Float,
    sway: Float,
    colors: RareUiColors,
    label: String,
    modifier: Modifier = Modifier,
) {
    val style =
        TextStyle(
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold,
            color = colors.foreground,
            textAlign = TextAlign.Center,
        )

    val width by animateDpAsState(
        targetValue = if (editing) EditingFieldWidth else 22.dp + 7.dp * maxOf(0, text.length - 1),
        animationSpec = if (reduceMotion) snap() else rareUiSpring(WIDTH_STIFFNESS, WIDTH_DAMPING),
        label = "duration-field-width",
    )

    BasicTextField(
        value = text,
        onValueChange = onTextChange,
        modifier =
            modifier
                .width(width)
                .graphicsLayer { translationX = (sway + nudge()) * density }
                .semantics { contentDescription = label },
        enabled = editing,
        textStyle = style,
        cursorBrush = SolidColor(colors.foreground),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        singleLine = true,
        decorationBox = { inner ->
            Box(contentAlignment = Alignment.Center) {
                if (text.isEmpty() && !editing) {
                    BasicText("0", style = style.copy(color = colors.foreground))
                }
                inner()
            }
        },
    )
}

/** The word after a field. */
@Composable
private fun SegmentLabel(
    text: String,
    weight: FontWeight,
    sway: Float,
    colors: RareUiColors,
) {
    BasicText(
        text = text,
        style =
            TextStyle(
                fontSize = 16.sp,
                fontWeight = weight,
                color = colors.glyph.copy(alpha = 0.7f),
            ),
        modifier = Modifier.graphicsLayer { translationX = sway * density },
    )
}

/** The pen that becomes a tick. */
@Composable
private fun PenToggle(
    editing: Boolean,
    innerRadius: Dp,
    colors: RareUiColors,
    reduceMotion: Boolean,
    onClick: () -> Unit,
) {
    val morph =
        remember {
            RareUiPathMorph(
                from = PathParser().parsePathString(PEN_PATH).toPath(),
                to = PathParser().parsePathString(TICK_PATH).toPath(),
            )
        }
    val nib = remember { PathParser().parsePathString(NIB_PATH).toPath() }
    val glyph = remember { Path() }

    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val press by animateFloatAsState(
        targetValue = if (pressed && !reduceMotion) 0.9f else 1f,
        animationSpec = if (reduceMotion) snap() else tween(150),
        label = "duration-press",
    )
    val progress by animateFloatAsState(
        targetValue = if (editing) 1f else 0f,
        animationSpec = if (reduceMotion) snap() else rareUiSpring(ICON_STIFFNESS, ICON_DAMPING),
        label = "duration-pen",
    )

    Canvas(
        modifier =
            Modifier
                .size(PickerHeight)
                .graphicsLayer {
                    scaleX = press
                    scaleY = press
                }.background(
                    color = colors.surface,
                    shape =
                        RoundedCornerShape(
                            topStart = innerRadius,
                            bottomStart = innerRadius,
                            topEnd = PickerRadius,
                            bottomEnd = PickerRadius,
                        ),
                ).clickable(
                    interactionSource = interaction,
                    indication = null,
                    onClick = onClick,
                ).semantics {
                    role = Role.Button
                    contentDescription = if (editing) "Save duration" else "Edit duration"
                },
    ) {
        val box = 18.dp.toPx()
        val glyphSize = Size(box, box)
        translate(left = (size.width - box) / 2, top = (size.height - box) / 2) {
            morph.path(progress, glyph, glyphSize, IconBox)
            drawPath(glyph, colors.glyph)
            if (progress > 0f) {
                // A stroke that grows as the tick arrives, so the finished mark is heavier
                // than the pen it came from.
                drawPath(
                    path = glyph,
                    color = colors.glyph.copy(alpha = progress),
                    style =
                        Stroke(
                            width = 2.5f * progress * box / IconBox.width,
                            cap = StrokeCap.Round,
                            join = StrokeJoin.Round,
                        ),
                )
            }
            if (progress < 0.4f) {
                // The pen's nib line, which fades out over the first two fifths of the
                // morph, before the shape has changed enough for a gap across it to look
                // wrong.
                val factor = box / IconBox.width
                scale(factor, pivot = Offset.Zero) {
                    drawPath(
                        path = nib,
                        color = colors.surface.copy(alpha = 1f - progress / 0.4f),
                        style = Stroke(width = 1.5f / factor, cap = StrokeCap.Round),
                    )
                }
            }
        }
    }
}
