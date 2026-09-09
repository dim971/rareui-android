/*
 * BadgeNumber.kt
 * The number in NotificationBell's badge, ported from the `DigitColumn` component in
 * upstream's `components/ui/notification-bell.tsx`.
 *
 * Each place is its own column, and each column is a spring. What makes this different from
 * an ordinary odometer is what a column is driven by: not the digit it should show, but the
 * whole number at that place. A count going from 39 to 40 moves the units column from 39 to
 * 40 and the tens column from 3 to 4, and taking each modulo ten afterwards turns those
 * into the right glyphs. The column therefore always knows which way to turn, because it is
 * following the count rather than a digit that has wrapped.
 */

package io.github.dim971.rareui.components.feedback

import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
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
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import io.github.dim971.rareui.core.rareUiMod
import io.github.dim971.rareui.theme.rareUiSpring
import kotlin.math.abs
import kotlin.math.roundToInt

/** How many tiles are kept above and below the one showing. */
private const val WINDOW = 3

/** How far behind the spring is allowed to fall before it is moved up to catch it. */
private const val LAG = 2f

/** The widest the mask fades, as a fraction of the column. */
private const val FADE = 0.34f

/** The speed at which it reaches that width. */
private const val FADE_VELOCITY = 9f

/** Upstream's `COLUMN_SPRING`. */
private const val COLUMN_STIFFNESS = 400f
private const val COLUMN_DAMPING = 30f
private const val COLUMN_MASS = 0.9f

/**
 * The number inside the badge, one rolling column per place.
 *
 * @param count how many notifications are waiting.
 * @param max the largest number to write out. Above it the badge reads `99+`.
 * @param fontSize the type size, which is also the height of one tile.
 * @param reduceMotion whether to change the number without rolling it.
 */
@Composable
internal fun BadgeNumber(
    count: Int,
    max: Int,
    fontSize: TextUnit,
    reduceMotion: Boolean,
) {
    val style =
        TextStyle(
            fontSize = fontSize,
            fontWeight = FontWeight.SemiBold,
            color = Color.White,
            // Digits have to share a width or the columns shift as the number changes.
            fontFeatureSettings = "tnum",
        )

    if (count > max) {
        BasicText(text = "$max+", style = style)
        return
    }

    Row {
        val places = count.toString().length
        repeat(places) { index ->
            val place = places - 1 - index
            BadgeDigit(
                value = count / POWERS_OF_TEN[place.coerceAtMost(POWERS_OF_TEN.lastIndex)],
                style = style,
                reduceMotion = reduceMotion,
            )
        }
    }
}

/** Enough powers of ten to cover any count a badge can hold. */
private val POWERS_OF_TEN = intArrayOf(1, 10, 100, 1_000, 10_000, 100_000, 1_000_000)

/**
 * One place of the badge's number.
 *
 * @param value the whole number at this place, not the digit: 4 for the tens column of 42.
 * @param style the type the digits are set in.
 * @param reduceMotion whether to change the digit without rolling it.
 */
@Composable
private fun BadgeDigit(
    value: Int,
    style: TextStyle,
    reduceMotion: Boolean,
) {
    val density = LocalDensity.current
    val tileHeight = with(density) { style.fontSize.toDp() }
    val tileHeightPx = with(density) { tileHeight.toPx() }

    val position = remember { Animatable(value.toFloat()) }

    LaunchedEffect(value, reduceMotion) {
        if (reduceMotion) {
            position.snapTo(value.toFloat())
            return@LaunchedEffect
        }

        // On a jump larger than the window, the column is moved up close first, so there are
        // still digits between here and there to roll past. Without it the spring travels
        // through a stretch of tiles that were never drawn.
        val gap = value - position.value
        if (abs(gap) > LAG) position.snapTo(value - if (gap < 0) -LAG else LAG)

        position.animateTo(
            targetValue = value.toFloat(),
            animationSpec = rareUiSpring(COLUMN_STIFFNESS, COLUMN_DAMPING, COLUMN_MASS),
        )
    }

    // A window tall enough to fade at rest would show the next digit peeking out, so the
    // fade rides the speed instead: sharp when still, soft while rolling. Compose reports
    // the spring's velocity directly, which SwiftUI does not.
    val edge = minOf(FADE, abs(position.velocity) / FADE_VELOCITY * FADE)

    Box(
        modifier =
            Modifier
                .height(tileHeight)
                .clipToBounds()
                // The mask needs somewhere of its own to erase from. Without an offscreen
                // layer, DstIn composites against whatever is already on the canvas.
                .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
                .drawWithContent {
                    drawContent()
                    // The mask covers the window the column is seen through, which does not
                    // move: it is the tiles behind it that slide past.
                    drawRect(
                        brush =
                            Brush.verticalGradient(
                                0f to Color.Transparent,
                                edge to Color.Black,
                                (1f - edge) to Color.Black,
                                1f to Color.Transparent,
                            ),
                        blendMode = BlendMode.DstIn,
                    )
                },
        contentAlignment = Alignment.Center,
    ) {
        for (offset in -WINDOW..WINDOW) {
            val tile = position.value.roundToInt() + offset
            BasicText(
                text = rareUiMod(tile.toDouble(), 10.0).toInt().toString(),
                style = style,
                modifier =
                    Modifier.graphicsLayer {
                        translationY = (tile - position.value) * tileHeightPx
                    },
            )
        }
    }
}
