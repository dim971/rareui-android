package io.github.dim971.rareui.showcase.entries

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.drag
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import io.github.dim971.rareui.components.display.AnimatedCounter
import io.github.dim971.rareui.components.display.CounterGrouping
import io.github.dim971.rareui.showcase.CatalogEntry
import io.github.dim971.rareui.showcase.Demo
import kotlin.math.roundToInt

val animatedCounterEntry: CatalogEntry =
    CatalogEntry(
        name = "Animated Counter",
        summary = "An odometer. Each digit is a wheel that rolls to its new face.",
        demos =
            listOf(
                Demo(
                    title = "Drag the ruler",
                    note =
                        "Upstream's own demonstration, down to its numbers: forty-one ticks over a " +
                            "hundred and fifty thousand, and a dash that is a tick rather than an " +
                            "overlay, so it lands dead on one. The ruler is the demonstration and " +
                            "not the component: upstream draws it on the page, out of a range " +
                            "input and a row of spans.",
                    code =
                        "AnimatedCounter(value = value, duration = 0.5,\n" +
                            "    grouping = CounterGrouping.INDIAN, prefix = \"$\")",
                ) { CounterRulerDemo() },
                Demo(
                    title = "Up and down",
                    note = "The roll follows the value: up when it grows, down when it shrinks.",
                    code = "AnimatedCounter(value = total)",
                ) { CounterPlayground(start = 1234.0, step = 111.0) },
                Demo(
                    title = "Currency",
                    note = "Two decimal places, with a symbol in front.",
                    code = """AnimatedCounter(value = revenue, decimals = 2, prefix = "$")""",
                ) { CounterPlayground(start = 4820.5, step = 137.25, decimals = 2, prefix = "$") },
                Demo(
                    title = "Indian grouping",
                    note = "Three at the end, pairs above it: 12,34,567 rather than 1,234,567.",
                    code = "AnimatedCounter(value = population, grouping = CounterGrouping.INDIAN)",
                ) {
                    CounterPlayground(start = 1_234_567.0, step = 111_111.0, grouping = CounterGrouping.INDIAN)
                },
                Demo(
                    title = "Padded",
                    note = "A minimum width, so the number never changes size. Useful for a timer.",
                    code = """AnimatedCounter(value = count, padStart = 6, separator = "")""",
                ) { CounterPlayground(start = 42.0, step = 7.0, padStart = 6, separator = "") },
            ),
    ) {
        AnimatedCounter(value = 1234.0, style = TextStyle(fontSize = 18.sp, fontWeight = FontWeight.SemiBold))
    }

@Composable
private fun CounterPlayground(
    start: Double,
    step: Double,
    decimals: Int = 0,
    padStart: Int = 1,
    separator: String = ",",
    grouping: CounterGrouping = CounterGrouping.WESTERN,
    prefix: String? = null,
) {
    var value by remember { mutableStateOf(start) }

    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        AnimatedCounter(
            value = value,
            decimals = decimals,
            padStart = padStart,
            separator = separator,
            grouping = grouping,
            prefix = prefix,
            style = TextStyle(fontSize = 36.sp, fontWeight = FontWeight.SemiBold),
        )
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Button(onClick = { value -= step }) { Text("Less") }
            Button(onClick = { value += step }) { Text("More") }
            Button(onClick = { value = start }) { Text("Reset") }
        }
    }
}

/** How far the ruler goes, from upstream's `MAX`. */
private const val RULER_MAX = 150_000.0

/** How many ticks it is drawn with, from `TICKS`. */
private const val RULER_TICKS = 41

/** How long the counter takes to roll, from `ROLL`. */
private const val RULER_ROLL = 0.5

/** Upstream's `ACCENT`, which is the theme's accent by another name. */
private val RulerAccent = Color(0xFFFC4C01)

/**
 * Upstream's own demonstration of the counter: a ruler you drag.
 *
 * It lives here rather than in the library because it lives on the page rather than in the
 * component upstream, where it is a range input with a row of spans over it. The dash is
 * one of the ticks rather than something drawn on top of them, which is what makes it land
 * exactly on a tick instead of between two.
 */
@Composable
private fun CounterRulerDemo() {
    var value by remember { mutableStateOf(12_480.0) }
    var width by remember { mutableIntStateOf(0) }
    // The ruler only changes when the dash crosses a tick, not on every pixel of the drag.
    val marker = ((value / RULER_MAX) * (RULER_TICKS - 1)).roundToInt().coerceIn(0, RULER_TICKS - 1)

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(56.dp),
    ) {
        AnimatedCounter(
            value = value,
            duration = RULER_ROLL,
            grouping = CounterGrouping.INDIAN,
            prefix = "$",
            style =
                TextStyle(
                    fontSize = 56.sp,
                    fontWeight = FontWeight.Medium,
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = (-0.02).em,
                ),
        )

        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .height(32.dp)
                    .onSizeChanged { width = it.width }
                    .pointerInput(Unit) {
                        fun setFrom(x: Float) {
                            if (width <= 0) return
                            value = (x / width * RULER_MAX).coerceIn(0.0, RULER_MAX)
                        }
                        awaitEachGesture {
                            val down = awaitFirstDown()
                            setFrom(down.position.x)
                            drag(down.id) { change ->
                                setFrom(change.position.x)
                                change.consume()
                            }
                        }
                    }.semantics {
                        contentDescription = "Counter value"
                    },
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom,
        ) {
            repeat(RULER_TICKS) { index -> RulerTick(index, marker) }
        }
    }
}

/** One tick of the ruler, which grows and darkens as the dash passes it. */
@Composable
private fun RulerTick(
    index: Int,
    marker: Int,
) {
    val dark = isSystemInDarkTheme()
    val passed = index < marker
    val isMarker = index == marker

    // Upstream's two tick colours, which are not the theme's: the passed ones take the
    // track colour and the ones still ahead take a pale grey of their own.
    val ahead = if (dark) Color(0xFF3C3C43) else Color(0xFFE7E7EF)
    val behind = if (dark) Color(0xFFEBEBF5) else Color(0xFF3C3C43)

    val height by animateDpAsState(
        targetValue =
            if (isMarker) {
                28.dp
            } else if (passed) {
                20.dp
            } else {
                14.dp
            },
        animationSpec = tween(200),
        label = "ruler-tick-height",
    )
    val ink by animateColorAsState(
        targetValue =
            if (isMarker) {
                RulerAccent
            } else if (passed) {
                behind
            } else {
                ahead
            },
        animationSpec = tween(200),
        label = "ruler-tick-ink",
    )

    Box(
        modifier =
            Modifier
                .width(if (isMarker) 3.dp else 2.dp)
                .height(height)
                .background(ink, RoundedCornerShape(percent = 50)),
    )
}
