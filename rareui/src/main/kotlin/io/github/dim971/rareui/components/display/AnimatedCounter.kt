/*
 * AnimatedCounter.kt
 * A port of upstream's `components/ui/animated-counter.tsx`.
 *
 * An odometer. Each digit is a wheel of eleven faces, the ten digits plus a repeat of zero
 * so the wrap from nine back to zero lands on an identical face rather than spinning the
 * long way round. The wheel is masked top and bottom so a digit in motion fades out of the
 * window instead of being cut off by a hard edge.
 */

package io.github.dim971.rareui.components.display

import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.requiredHeight
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import io.github.dim971.rareui.core.rareUiMod
import io.github.dim971.rareui.theme.RareUiTheme
import io.github.dim971.rareui.theme.rareUiSpring
import io.github.dim971.rareui.theme.rememberRareUiReduceMotion

/**
 * A number that rolls to its new value, one digit wheel at a time.
 *
 * ```kotlin
 * AnimatedCounter(value = total)
 * AnimatedCounter(value = revenue, decimals = 2, prefix = "$")
 * ```
 *
 * The roll is direction aware: a value going up rolls its wheels up, a value coming down
 * rolls them down. Places that appear as the number grows roll in from zero rather than
 * snapping into existence, and places that disappear fade out.
 *
 * With animations turned off on the device the digits change without rolling, which is
 * what upstream does under `prefers-reduced-motion`.
 *
 * @param value the number to show.
 * @param modifier the modifier to apply.
 * @param decimals how many decimal places to show.
 * @param duration how long a roll takes, in seconds.
 * @param padStart the minimum number of whole digits, padded with leading zeros.
 * @param separator the grouping separator. Pass `""` for none.
 * @param decimalSeparator the decimal separator.
 * @param grouping how the whole digits are grouped.
 * @param prefix text shown before the number, such as a currency symbol.
 * @param suffix text shown after it, such as a unit.
 * @param style the text style to draw in.
 * @param color the ink. Defaults to the theme's foreground.
 */
@Composable
public fun AnimatedCounter(
    value: Double,
    modifier: Modifier = Modifier,
    decimals: Int = 0,
    duration: Double = 0.6,
    padStart: Int = 1,
    separator: String = ",",
    decimalSeparator: String = ".",
    grouping: CounterGrouping = CounterGrouping.WESTERN,
    prefix: String? = null,
    suffix: String? = null,
    style: TextStyle = TextStyle.Default,
    color: Color = RareUiTheme.colors.foreground,
) {
    val reduceMotion = rememberRareUiReduceMotion()
    val shape = counterShape(value, decimals, padStart, duration)
    val characters = counterFormat(shape, separator, decimalSeparator, grouping)
    val cells = counterCells(characters, shape.width)
    // A value that rounds away to nothing is not negative, however it was written.
    val negative = shape.amount < 0 && shape.scaled > 0

    // The faces each wheel started on. A place that was already there when the counter
    // appeared starts settled on its digit; one that appears later is absent from this and
    // starts from zero, so it rolls in.
    val seed =
        remember {
            cells.filterIsInstance<CounterCell.Digit>().associate { it.place to it.value }
        }

    val digits = style.copy(fontFeatureSettings = "tnum")

    Row(
        modifier =
            modifier.clearAndSetSemantics {
                // The number is drawn as a stack of wheels, which a screen reader would
                // otherwise read as eleven faces per digit.
                contentDescription = (if (negative) "-" else "") + characters
            },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        prefix?.let { BasicText(it, style = digits.copy(color = color)) }
        if (negative) BasicText("-", style = digits.copy(color = color))

        cells.forEach { cell ->
            when (cell) {
                is CounterCell.Digit ->
                    DigitWheel(
                        digit = cell.value,
                        amount = shape.amount,
                        from = seed[cell.place] ?: 0,
                        pace = shape.pace,
                        reduceMotion = reduceMotion,
                        style = digits.copy(color = color),
                    )

                is CounterCell.Mark ->
                    BasicText(cell.character.toString(), style = digits.copy(color = color))
            }
        }

        suffix?.let { BasicText(it, style = digits.copy(color = color)) }
    }
}

/**
 * The eleven faces a wheel turns through.
 *
 * Ten digits and a repeat of zero. Without the last one, the wrap from nine back to zero
 * would have to travel the whole way round the wheel instead of continuing on to an
 * identical face.
 */
private val WheelFaces = listOf(0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 0)

/**
 * The soft edge the wheel turns behind.
 *
 * The stops are upstream's, and they are eased rather than a straight ramp: a linear fade
 * of the same width reads as a hard edge. The middle stays fully opaque from 22% to 78%,
 * which is where a resting glyph sits, so a digit that is not moving is solid.
 */
private val WheelFade =
    listOf(
        0.0f to 0.0f,
        0.055f to 0.06f,
        0.11f to 0.5f,
        0.165f to 0.94f,
        0.22f to 1.0f,
        0.78f to 1.0f,
        0.835f to 0.94f,
        0.89f to 0.5f,
        0.945f to 0.06f,
        1.0f to 0.0f,
    )

/** Motion's `{ visualDuration: pace, bounce: 0.18 }`. */
private const val WHEEL_BOUNCE = 0.18f

/**
 * Upstream draws each face in a box `1.5em` tall, which leaves enough air above and below
 * the glyph that the mask's fade never touches it. A text line box is closer to `1.2em`,
 * so it is opened out by this much to put the glyph back in the clear.
 * See docs/fidelity.md.
 */
private const val LINE_BOX_TO_FACE_BOX = 1.25f

/** One digit of the counter, drawn as a wheel that turns to the digit it should show. */
@Composable
private fun DigitWheel(
    digit: Int,
    amount: Double,
    from: Int,
    pace: Double,
    reduceMotion: Boolean,
    style: TextStyle,
) {
    val measurer = rememberTextMeasurer()
    val density = LocalDensity.current
    // The caller chooses the font, so the face box is measured rather than assumed.
    val faceHeightPx =
        remember(style, density) {
            measurer.measure("0", style).size.height * LINE_BOX_TO_FACE_BOX
        }
    val faceHeight = with(density) { faceHeightPx.toDp() }

    val position = remember { Animatable(from.toFloat()) }
    var goal by remember { mutableStateOf(from.toDouble()) }
    var lastAmount by remember { mutableStateOf(amount) }

    LaunchedEffect(digit, amount, reduceMotion) {
        val heading = if (amount >= lastAmount) 1.0 else -1.0
        lastAmount = amount

        if (reduceMotion) {
            goal = digit.toDouble()
            position.snapTo(digit.toFloat())
            return@LaunchedEffect
        }

        // Aiming from where the wheel actually is, rather than from where it was sent, is
        // what keeps a value that changes several times inside one roll from queueing up a
        // backlog of turns. Compose gives the live value directly, which SwiftUI does not.
        goal = wheelGoal(goal, position.value.toDouble(), digit, heading)
        position.animateTo(
            targetValue = goal.toFloat(),
            animationSpec = rareUiSpring(pace.toFloat(), WHEEL_BOUNCE),
        )
    }

    Box(
        modifier =
            Modifier
                .height(faceHeight)
                .clipToBounds()
                // The mask needs somewhere of its own to erase from. Without an offscreen
                // layer, DstIn composites against whatever is already on the canvas and
                // paints bands rather than removing them.
                .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
                .drawWithContent {
                    drawContent()
                    // The mask covers the window the column is seen through, which does not
                    // move: it is the faces behind it that slide past.
                    drawRect(
                        brush =
                            Brush.verticalGradient(
                                colorStops =
                                    WheelFade
                                        .map { (stop, alpha) ->
                                            stop to Color.Black.copy(alpha = alpha)
                                        }.toTypedArray(),
                            ),
                        blendMode = BlendMode.DstIn,
                    )
                },
        contentAlignment = Alignment.TopCenter,
    ) {
        Column(
            modifier =
                Modifier
                    .wrapContentHeight(align = Alignment.Top, unbounded = true)
                    .graphicsLayer {
                        // The offset wraps with the position, which is why the eleventh face
                        // exists: at the moment the wrap happens the face leaving the top and
                        // the face arriving are the same glyph, so nothing jumps.
                        translationY = -rareUiMod(position.value.toDouble(), 10.0).toFloat() * faceHeightPx
                    },
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            WheelFaces.forEach { face ->
                Box(
                    // Required rather than merely preferred: a face is exactly this tall,
                    // whatever the text inside it would rather be.
                    modifier = Modifier.requiredHeight(faceHeight),
                    contentAlignment = Alignment.Center,
                ) {
                    BasicText(face.toString(), style = style.copy(textAlign = TextAlign.Center))
                }
            }
        }
    }
}

/**
 * Works out where a wheel should be aimed so that it lands showing [digit].
 *
 * The wheel's position is not confined to `0..<10`: it counts turns, and only its remainder
 * decides which face is showing. Aiming therefore means adding just enough to reach the
 * next occurrence of the face in the direction of travel, which is what keeps a rising
 * number rolling upward past nine into zero rather than spinning backward.
 *
 * @param goal where the wheel is currently aimed.
 * @param position where the wheel actually is, which is behind the goal while it moves.
 * @param digit the face it should end up showing.
 * @param heading `1` to turn up, `-1` to turn down.
 * @return the new aim, unchanged when the wheel is already heading at that face.
 */
internal fun wheelGoal(
    goal: Double,
    position: Double,
    digit: Int,
    heading: Double,
): Double {
    val target = digit.toDouble()
    // Re-aim only when the face has actually changed. Without this a reversal alone would
    // send every wheel the long way round to the face it is already showing.
    if (rareUiMod(goal, 10.0) == target) return goal

    return if (heading < 0) {
        position - rareUiMod(position - target, 10.0)
    } else {
        position + rareUiMod(target - position, 10.0)
    }
}
