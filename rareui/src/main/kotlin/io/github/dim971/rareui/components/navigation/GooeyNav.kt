/*
 * GooeyNav.kt
 * A port of upstream's `components/ui/gooey-nav.tsx`.
 *
 * A segmented bar where the selected tile detaches from its neighbours. The seams either
 * side of it stretch and pinch until they part, which is where the name comes from, but
 * nothing here is a filter: the seam is drawn, in GooeyNeck.
 */

package io.github.dim971.rareui.components.navigation

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.dim971.rareui.theme.RareUiTheme
import io.github.dim971.rareui.theme.rareUiSpring
import io.github.dim971.rareui.theme.rememberRareUiReduceMotion

/** One tile of a [GooeyNav]. */
public data class GooeyNavItem(
    /** The text on the tile. */
    public val label: String,
)

/** How large a [GooeyNav] is drawn. */
public enum class GooeyNavSize(
    internal val horizontalPadding: Dp,
    internal val verticalPadding: Dp,
    internal val fontSize: Int,
    internal val radius: Dp,
    internal val separation: Dp,
) {
    /** The smallest, for a dense toolbar. */
    EXTRA_SMALL(8.dp, 6.dp, 11, 8.dp, 14.dp),

    /** Small. */
    SMALL(14.dp, 8.dp, 12, 10.dp, 16.dp),

    /** The default. */
    MEDIUM(20.dp, 10.dp, 14, 12.dp, 20.dp),

    /** The largest. */
    LARGE(24.dp, 12.dp, 16, 14.dp, 24.dp),
}

/**
 * A segmented bar whose selected tile detaches, stretching the seams either side of it
 * until they part.
 *
 * ```kotlin
 * GooeyNav(items = listOf("Home", "Docs", "Pricing"), selection = tab, onSelect = { tab = it })
 * ```
 *
 * With animations turned off on the device the tiles move without springing.
 *
 * @param items the tiles' labels, in order.
 * @param selection the index of the selected tile.
 * @param onSelect called with the index of a newly selected tile.
 * @param modifier the modifier to apply.
 * @param size how large to draw it.
 * @param activeColor the selected tile's fill. Defaults to the theme's accent.
 * @param activeLabelColor the selected tile's label colour.
 * @param separation how far the selected tile pulls away. Defaults to the size's own.
 * @param radius the corner radius. Defaults to the size's own.
 */
@Composable
public fun GooeyNav(
    items: List<String>,
    selection: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
    size: GooeyNavSize = GooeyNavSize.MEDIUM,
    activeColor: Color = RareUiTheme.colors.accent,
    activeLabelColor: Color = Color.White,
    separation: Dp = size.separation,
    radius: Dp = size.radius,
) {
    val colors = RareUiTheme.colors
    val reduceMotion = rememberRareUiReduceMotion()
    val active = selection.coerceIn(0, (items.size - 1).coerceAtLeast(0))

    // Damped hard enough that nothing overshoots: a tile that sprang past its resting place
    // would pull the seam back through itself.
    fun <T> travel() = if (reduceMotion) snap<T>() else rareUiSpring<T>(200f, 28f, 1f)

    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        items.forEachIndexed { index, label ->
            val isActive = index == active
            val openBefore = gooeyIsSeamOpen(index, active, items.size)
            val openAfter = gooeyIsSeamOpen(index + 1, active, items.size)

            // A closed seam pulls the next tile in by a point, so no hairline of the
            // background shows through two tiles that should read as one.
            val target =
                when {
                    index == 0 -> 0.dp
                    openBefore -> separation
                    else -> (-1).dp
                }
            val gap by animateDpAsState(target, travel(), label = "gooey-gap")

            val leading by animateDpAsState(if (openBefore) radius else 0.dp, travel(), label = "gooey-lead")
            val trailing by animateDpAsState(if (openAfter) radius else 0.dp, travel(), label = "gooey-trail")

            // Arriving at the active colours takes 400ms; leaving them takes none, so the
            // tile being left does not hold its colour while the new one takes it up.
            val colourSpec = if (reduceMotion || !isActive) snap<Color>() else tween<Color>(400)
            val fill by animateColorAsState(
                if (isActive) activeColor else colors.surface,
                colourSpec,
                label = "gooey-fill",
            )
            val ink by animateColorAsState(
                if (isActive) activeLabelColor else colors.glyph,
                colourSpec,
                label = "gooey-ink",
            )

            if (index > 0) Spacer(Modifier.width(gap))

            GooeyTile(
                label = label,
                size = size,
                fill = fill,
                ink = ink,
                leading = leading,
                trailing = trailing,
                gap = gap,
                separation = separation,
                seamStart = if (index - 1 == active) activeColor else colors.surface,
                seamEnd = if (isActive) activeColor else colors.surface,
                selected = isActive,
                onClick = { onSelect(index) },
            )
        }
    }
}

/** One tile, with the seam that reaches back toward the tile before it. */
@Composable
private fun GooeyTile(
    label: String,
    size: GooeyNavSize,
    fill: Color,
    ink: Color,
    leading: Dp,
    trailing: Dp,
    gap: Dp,
    separation: Dp,
    seamStart: Color,
    seamEnd: Color,
    selected: Boolean,
    onClick: () -> Unit,
) {
    BasicText(
        text = label,
        style = TextStyle(fontSize = size.fontSize.sp, fontWeight = FontWeight.Medium, color = ink),
        modifier =
            Modifier
                .drawBehind {
                    val leadingPx = leading.toPx()
                    val trailingPx = trailing.toPx()

                    // The tile itself, with its four corners set one at a time as the seams
                    // either side of it open and close.
                    drawPath(
                        path =
                            tilePath(
                                width = this.size.width,
                                height = this.size.height,
                                leading = leadingPx,
                                trailing = trailingPx,
                            ),
                        color = fill,
                    )

                    // And the seam, drawn back into the space the spacer opened. It takes the
                    // colour of the tile on each side of it, so it reads as the two of them
                    // stretching apart rather than as a third thing between them.
                    val gapPx = gap.toPx().toDouble()
                    val spanPx = separation.toPx().toDouble()
                    gooeyNeckGeometry(
                        gap = gapPx,
                        span = spanPx,
                        height = this.size.height.toDouble(),
                        left = -spanPx,
                    )?.let { seam ->
                        drawPath(
                            path = seam.toPath(),
                            brush =
                                Brush.horizontalGradient(
                                    colors = listOf(seamStart, seamEnd),
                                    startX = (-spanPx).toFloat(),
                                    endX = 0f,
                                ),
                        )
                    }
                }.clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onClick,
                ).padding(horizontal = size.horizontalPadding, vertical = size.verticalPadding)
                .semantics {
                    role = Role.Tab
                    this.selected = selected
                },
    )
}

/** A rectangle whose leading and trailing corners are rounded by different amounts. */
private fun tilePath(
    width: Float,
    height: Float,
    leading: Float,
    trailing: Float,
): Path {
    val path = Path()
    val lead = leading.coerceIn(0f, minOf(width, height) / 2)
    val trail = trailing.coerceIn(0f, minOf(width, height) / 2)

    path.moveTo(lead, 0f)
    path.lineTo(width - trail, 0f)
    if (trail > 0) path.quadraticTo(width, 0f, width, trail)
    path.lineTo(width, height - trail)
    if (trail > 0) path.quadraticTo(width, height, width - trail, height)
    path.lineTo(lead, height)
    if (lead > 0) path.quadraticTo(0f, height, 0f, height - lead)
    path.lineTo(0f, lead)
    if (lead > 0) path.quadraticTo(0f, 0f, lead, 0f)
    path.close()
    return path
}
