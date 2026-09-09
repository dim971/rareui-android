/*
 * HookSidebar.kt
 * A port of upstream's `components/ui/hook-sidebar.tsx`.
 *
 * A list with a rail down its left edge that stops at the current row and turns into it
 * with a small quarter-round hook. Pointing at another row runs a second, fainter rail out
 * to that one, so the list shows both where you are and where you are about to be.
 */

package io.github.dim971.rareui.components.navigation

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
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
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
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
import androidx.compose.ui.unit.sp
import io.github.dim971.rareui.theme.RareUiTheme
import io.github.dim971.rareui.theme.rareUiSpring
import io.github.dim971.rareui.theme.rememberRareUiReduceMotion

/** The radius of the hook, which is also how far above a row's middle the rail stops. */
private val HookCorner = 6.dp

/** How far the hook carries on toward the label after it has turned, from upstream's `width="12"`. */
private val HookReach = 12.dp

/** The gutter the rail runs down, in from the leading edge. */
private val HookGutter = 2.dp

/** Upstream's `transparent 0 2px, currentColor 2px 4px`. */
private val HookDash = 2.dp

/** The spring both rails travel on, from upstream's `stiffness: 420, damping: 34, mass: 0.7`. */
private const val RAIL_STIFFNESS = 420f
private const val RAIL_DAMPING = 34f
private const val RAIL_MASS = 0.7f

/** How faint the second rail is, from upstream's `opacity-30`. */
private const val GHOST_ALPHA = 0.3f

/**
 * A list marked by a rail that hooks into the current row.
 *
 * ```kotlin
 * HookSidebar(
 *     items = listOf("Overview", "Install", "Theming"),
 *     selection = section,
 *     onSelect = { section = it },
 * )
 * ```
 *
 * The second rail follows a pointer, so it appears with a mouse, a trackpad or a stylus
 * and simply never appears under a finger. Upstream has the same behaviour for the same
 * reason.
 *
 * With animations turned off on the device the rails move without springing.
 *
 * @param items the rows, in order.
 * @param selection the index of the selected row.
 * @param onSelect called with the index of a newly selected row.
 * @param modifier the modifier to apply.
 * @param label a heading above the list.
 * @param color the active rail's colour. Defaults to the theme's accent.
 * @param dashed whether the rails are drawn as dashes.
 */
@Composable
public fun HookSidebar(
    items: List<String>,
    selection: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
    label: String? = null,
    color: Color = RareUiTheme.colors.accent,
    dashed: Boolean = true,
) {
    val colors = RareUiTheme.colors
    val reduceMotion = rememberRareUiReduceMotion()
    val density = LocalDensity.current

    val centres = remember { mutableStateMapOf<Int, Float>() }
    var hovered by remember { mutableStateOf<Int?>(null) }

    val activeY = centres[selection]
    val hoverY = hovered?.let { centres[it] }
    val ghostShowing = hovered != null && hovered != selection && hoverY != null

    val travel = rareUiSpring<Float>(RAIL_STIFFNESS, RAIL_DAMPING, RAIL_MASS)
    val fade = if (reduceMotion) snap() else tween<Float>(200)

    // The rails keep animating to their last known target while a row is being measured, so
    // a list that reflows does not throw them back to the top and spring in again.
    val activeTo by animateFloatAsState(activeY ?: 0f, if (reduceMotion) snap() else travel, label = "hook-active")
    val activeAlpha by animateFloatAsState(if (activeY != null) 1f else 0f, fade, label = "hook-active-alpha")
    val ghostFrom by animateFloatAsState(
        hookGhostRailStart(
            activeY = activeY?.toDouble(),
            hoverY = hoverY?.toDouble(),
            corner = with(density) { HookCorner.toPx() }.toDouble(),
        ).toFloat(),
        if (reduceMotion) snap() else travel,
        label = "hook-ghost-from",
    )
    val ghostTo by animateFloatAsState(hoverY ?: 0f, if (reduceMotion) snap() else travel, label = "hook-ghost-to")
    val ghostAlpha by animateFloatAsState(if (ghostShowing) GHOST_ALPHA else 0f, fade, label = "hook-ghost-alpha")

    Column(modifier = modifier) {
        if (label != null) {
            BasicText(
                text = label.uppercase(),
                style =
                    TextStyle(
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        letterSpacing = 0.5.sp,
                        color = colors.foreground,
                    ),
                modifier =
                    Modifier
                        .padding(start = 2.dp, bottom = 12.dp)
                        .semantics { heading() },
            )
        }

        Column(
            modifier =
                Modifier.drawWithCache {
                    // The hook's geometry is rebuilt every frame but its storage is not: a
                    // rail moving on a spring would otherwise allocate a path per frame.
                    val path = Path()
                    val gutter = HookGutter.toPx()
                    val corner = HookCorner.toPx()
                    val reach = HookReach.toPx()
                    val hairline = 1.dp.toPx()
                    val dash = floatArrayOf(HookDash.toPx(), HookDash.toPx())

                    onDrawBehind {
                        drawHookRail(
                            from = ghostFrom,
                            to = ghostTo,
                            gutter = gutter,
                            corner = corner,
                            reach = reach,
                            color = colors.foreground,
                            alpha = ghostAlpha,
                            dashed = dashed,
                            hairline = hairline,
                            dash = dash,
                            path = path,
                        )
                        drawHookRail(
                            from = 0f,
                            to = activeTo,
                            gutter = gutter,
                            corner = corner,
                            reach = reach,
                            color = color,
                            alpha = activeAlpha,
                            dashed = dashed,
                            hairline = hairline,
                            dash = dash,
                            path = path,
                        )
                    }
                },
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            items.forEachIndexed { index, item ->
                val isActive = index == selection
                val interaction = remember { MutableInteractionSource() }
                val isHovered by interaction.collectIsHoveredAsState()
                val ink by animateColorAsState(
                    targetValue = if (isActive) colors.foreground else colors.foreground.copy(alpha = 0.5f),
                    animationSpec = if (reduceMotion) snap() else tween(200),
                    label = "hook-sidebar-ink",
                )

                LaunchedEffect(isHovered) {
                    hovered =
                        when {
                            isHovered -> index
                            hovered == index -> null
                            else -> hovered
                        }
                }

                Box(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .onGloballyPositioned { coordinates ->
                                centres[index] = coordinates.positionInParent().y + coordinates.size.height / 2f
                            }.hoverable(interaction)
                            .clickable(
                                interactionSource = interaction,
                                indication = null,
                                onClick = { onSelect(index) },
                            ).padding(start = 20.dp, end = 8.dp, top = 6.dp, bottom = 6.dp)
                            .semantics {
                                role = Role.Tab
                                selected = isActive
                            },
                ) {
                    BasicText(text = item, style = TextStyle(fontSize = 14.sp, color = ink))
                }
            }
        }
    }
}
