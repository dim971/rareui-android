/*
 * BounceSidebar.kt
 * A port of upstream's `components/ui/bounce-sidebar.tsx`.
 *
 * A plain list of rows with a dot in the gutter marking the current one. The bounce is not
 * a spring: the dot travels along an arc, so it swings out sideways on its way from one row
 * to the next and comes back in as it arrives.
 */

package io.github.dim971.rareui.components.navigation

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import io.github.dim971.rareui.theme.RareUiTheme
import io.github.dim971.rareui.theme.rememberRareUiReduceMotion

/** One row of a [BounceSidebar]. */
public data class BounceSidebarItem(
    /** The row's text. */
    public val label: String,
    /** Whether the row is a heading rather than something selectable. */
    public val isHeading: Boolean = false,
)

/**
 * Creates a heading, which is drawn in the dot's colour and cannot be selected.
 *
 * @param label the heading's text.
 * @return the heading row.
 */
public fun bounceSidebarHeading(label: String): BounceSidebarItem = BounceSidebarItem(label = label, isHeading = true)

/** The dot's diameter. */
private val DotSize = 6.dp

/** How far the rows are indented, which is the gutter the dot lives in. */
private val Gutter = 24.dp

/** Where the dot sits inside that gutter. */
private val DotInset = 8.dp

/**
 * How long the dot takes to cross, and on what curve. Upstream animates the arc with an
 * ease out rather than a spring, which is why it never overshoots the row.
 */
private const val CROSSING_MILLIS = 250

/** CSS's `ease-out`, which is what Motion's default `easeOut` resolves to. */
private val EaseOut = CubicBezierEasing(0f, 0f, 0.58f, 1f)

/**
 * A list of rows with a dot in the gutter that arcs from one to the next.
 *
 * ```kotlin
 * BounceSidebar(
 *     items = listOf(BounceSidebarItem("Overview"), BounceSidebarItem("Install")),
 *     selection = section,
 *     onSelect = { section = it },
 * )
 * ```
 *
 * With animations turned off on the device the dot moves without the arc and without a
 * duration.
 *
 * @param items the rows, in order. Use [bounceSidebarHeading] for a heading.
 * @param selection the index of the selected row.
 * @param onSelect called with the index of a newly selected row.
 * @param modifier the modifier to apply.
 * @param dotColor the dot's colour, which headings also take. Defaults to the theme's accent.
 */
@Composable
public fun BounceSidebar(
    items: List<BounceSidebarItem>,
    selection: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
    dotColor: Color = RareUiTheme.colors.accent,
) {
    val colors = RareUiTheme.colors
    val reduceMotion = rememberRareUiReduceMotion()
    val density = LocalDensity.current

    val centres = remember { mutableStateMapOf<Int, Float>() }
    val dotSizePx = with(density) { DotSize.toPx() }
    val dotX = with(density) { (DotInset + DotSize / 2).toPx() }

    val dotY = remember { Animatable(0f) }
    var arcStart by remember { mutableStateOf(0f) }
    var arcEnd by remember { mutableStateOf(0f) }
    // Nothing to point at until the rows have been measured, and a dot parked at the top of
    // the list in the meantime would be read as a wrong answer.
    var placed by remember { mutableStateOf(false) }

    LaunchedEffect(selection, centres[selection], reduceMotion) {
        val centre = centres[selection] ?: return@LaunchedEffect
        val destination = centre - dotSizePx / 2

        if (!placed || reduceMotion) {
            arcStart = destination
            arcEnd = destination
            dotY.snapTo(destination)
            placed = true
            return@LaunchedEffect
        }

        // Aiming from where the dot actually is, rather than from where it was last sent,
        // is what keeps the arc right when a second row is picked mid flight.
        arcStart = dotY.value
        arcEnd = destination
        dotY.animateTo(destination, tween(CROSSING_MILLIS, easing = EaseOut))
    }

    Column(
        modifier =
            modifier
                .drawBehind {
                    if (!placed) return@drawBehind
                    val sideways =
                        bounceDotSideways(
                            y = dotY.value.toDouble(),
                            start = arcStart.toDouble(),
                            end = arcEnd.toDouble(),
                        )
                    drawCircle(
                        color = dotColor,
                        radius = dotSizePx / 2,
                        center =
                            Offset(
                                x = dotX + sideways.toFloat(),
                                y = dotY.value + dotSizePx / 2,
                            ),
                    )
                }.padding(start = Gutter),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        items.forEachIndexed { index, item ->
            val isActive = index == selection
            val ink by animateColorAsState(
                targetValue = if (isActive) colors.foreground else colors.foreground.copy(alpha = 0.5f),
                animationSpec = if (reduceMotion) snap() else tween(200),
                label = "bounce-sidebar-ink",
            )

            Box(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .onGloballyPositioned { coordinates ->
                            centres[index] = coordinates.positionInParent().y + coordinates.size.height / 2f
                        },
            ) {
                if (item.isHeading) {
                    BasicText(
                        text = item.label.uppercase(),
                        style =
                            TextStyle(
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                letterSpacing = 0.14.em,
                                color = dotColor,
                            ),
                        modifier =
                            Modifier
                                // A heading opens a gap above itself, unless it is the first
                                // thing in the list.
                                .padding(top = if (index == 0) 0.dp else 28.dp, bottom = 4.dp)
                                .padding(horizontal = 4.dp)
                                .semantics { heading() },
                    )
                } else {
                    BasicText(
                        text = item.label,
                        style = TextStyle(fontSize = 14.sp, color = ink),
                        modifier =
                            Modifier
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null,
                                    onClick = { onSelect(index) },
                                ).fillMaxWidth()
                                .padding(4.dp)
                                .semantics {
                                    role = Role.Tab
                                    selected = isActive
                                },
                    )
                }
            }
        }
    }
}
