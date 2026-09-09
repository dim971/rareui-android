/*
 * ScrollProgress.kt
 * A port of upstream's `components/ui/scroll-progress.tsx`.
 *
 * A floating pill that shows how far down a page you are and which part of it you are
 * reading. Tapping it opens into a list of the parts.
 *
 * Three differences in shape, recorded in docs/fidelity.md. Upstream listens to the
 * window's scroll itself and finds sections by their element id; a Compose component cannot
 * reach into somebody else's scroll state, so this takes the progress and the current
 * section as values and reports a tap back, which is the same division of labour Compose's
 * own scrolling API uses and makes the component work over anything that scrolls. The pill
 * is opaque and lifted by a shadow rather than sitting on a blur of what is behind it,
 * which Compose has no way to ask for and which reads as a fault rather than as glass when
 * the blur is missing. And its corners are plain rounded ones rather than the squircle
 * upstream reaches for CSS's very new `corner-shape` to get.
 */

package io.github.dim971.rareui.components.navigation

import android.os.Build
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.EnterExitState
import androidx.compose.animation.core.animateDp
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.dim971.rareui.theme.RareUiColors
import io.github.dim971.rareui.theme.RareUiEasing
import io.github.dim971.rareui.theme.RareUiTheme
import io.github.dim971.rareui.theme.rareUiSpring
import io.github.dim971.rareui.theme.rareUiVisualSpring
import io.github.dim971.rareui.theme.rememberRareUiReduceMotion
import kotlinx.coroutines.delay

/** One part of the page a [ScrollProgress] can point at. */
@Immutable
public data class ScrollProgressSection(
    /** The section's identifier, which is what a selection reports back. */
    public val id: String,
    /** The name shown for it. */
    public val label: String,
)

/** The pill's change of size, from upstream's `SIZE_SPRING`. */
private const val SIZE_DURATION = 0.5f
private const val SIZE_BOUNCE = 0.16f

/** The corner radius closed, and once it has opened into a list. */
private val ClosedRadius = 16.dp
private val OpenRadius = 26.dp

/** A row's corner radius. */
private val RowRadius = 14.dp

/** How blurred each layer is at the far end of the crossing. */
private val LayerBlur = 4.dp

/** Blur needs a render effect, which arrived in Android 12. */
private val PLATFORM_BLURS = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S

/**
 * The name to show for whatever is being read.
 *
 * A selection that names nothing in the list falls back to the first section rather than to
 * an empty pill, which is what happens on the way into a page before anything has scrolled.
 *
 * @param sections the parts of the page.
 * @param selection the section being read.
 * @return the label to show.
 */
internal fun scrollProgressLabel(
    sections: List<ScrollProgressSection>,
    selection: String?,
): String =
    sections.firstOrNull { it.id == selection }?.label
        ?: sections.firstOrNull()?.label
        ?: ""

/**
 * A floating pill showing reading progress, which opens into the page's sections.
 *
 * ```kotlin
 * ScrollProgress(
 *     sections = sections,
 *     progress = read,
 *     selection = section,
 *     onSelect = { scrollTo(it) },
 * )
 * ```
 *
 * With animations turned off on the device the pill changes size and swaps its contents
 * without a duration.
 *
 * @param sections the parts of the page, in order.
 * @param progress how far down the page the reader is, in `0..1`.
 * @param selection the section being read. Set it as the reader scrolls.
 * @param modifier the modifier to apply.
 * @param onSelect called when a section is picked, so the host can scroll to it.
 */
@Composable
public fun ScrollProgress(
    sections: List<ScrollProgressSection>,
    progress: Float,
    selection: String?,
    modifier: Modifier = Modifier,
    onSelect: ((String) -> Unit)? = null,
) {
    val colors = RareUiTheme.colors
    val reduceMotion = rememberRareUiReduceMotion()
    var open by remember { mutableStateOf(false) }

    val radius by animateDpAsState(
        targetValue = if (open) OpenRadius else ClosedRadius,
        animationSpec = if (reduceMotion) snap() else rareUiVisualSpring(SIZE_DURATION, SIZE_BOUNCE),
        label = "scroll-progress-radius",
    )
    val shape = RoundedCornerShape(radius)

    AnimatedContent(
        targetState = open,
        modifier =
            modifier
                // Opaque, and lifted off the page by a shadow rather than by a blur of
                // what is behind it. A translucent panel with nothing blurring underneath
                // reads as a fault rather than as glass.
                .shadow(6.dp, shape)
                .background(colors.background, shape)
                .border(1.dp, colors.border, shape),
        transitionSpec = {
            if (reduceMotion) {
                fadeIn(snap()) togetherWith fadeOut(snap())
            } else {
                val crossing = tween<Float>(240, easing = RareUiEasing.EaseInOut)
                fadeIn(crossing) togetherWith fadeOut(crossing)
            }
        },
        label = "scroll-progress",
    ) { showingList ->
        // The two layers cross under a blur rather than a straight fade, which is what stops
        // the swap reading as one thing simply being replaced by another.
        val smear by transition.animateDp(label = "scroll-progress-blur") { state ->
            if (state == EnterExitState.Visible || reduceMotion || !PLATFORM_BLURS) 0.dp else LayerBlur
        }

        Box(modifier = if (smear > 0.dp) Modifier.blur(smear) else Modifier) {
            if (showingList) {
                SectionList(
                    sections = sections,
                    selection = selection,
                    colors = colors,
                    reduceMotion = reduceMotion,
                    onPick = { id ->
                        open = false
                        onSelect?.invoke(id)
                    },
                )
            } else {
                ProgressPill(
                    label = scrollProgressLabel(sections, selection),
                    progress = progress,
                    colors = colors,
                    reduceMotion = reduceMotion,
                    onOpen = { open = true },
                )
            }
        }
    }
}

/** The closed state: a ring, the current section's name, and somewhere to tap. */
@Composable
private fun ProgressPill(
    label: String,
    progress: Float,
    colors: RareUiColors,
    reduceMotion: Boolean,
    onOpen: () -> Unit,
) {
    Row(
        modifier =
            Modifier
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onOpen,
                ).padding(vertical = 6.dp)
                .padding(start = 8.dp, end = 16.dp)
                .semantics {
                    role = Role.Button
                    contentDescription = "Show sections. $label"
                },
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ProgressRing(progress, colors, reduceMotion)

        AnimatedContent(
            targetState = label,
            transitionSpec = {
                // A new section's name comes in on its own, rather than the old one sliding
                // out of the way of it.
                val swap = if (reduceMotion) snap<Float>() else tween(220, easing = RareUiEasing.EaseOutQuint)
                fadeIn(swap) togetherWith fadeOut(swap)
            },
            label = "scroll-progress-label",
        ) { name ->
            BasicText(
                text = name,
                style =
                    TextStyle(
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = colors.foreground,
                    ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

/** The ring around the pill's progress. */
@Composable
private fun ProgressRing(
    progress: Float,
    colors: RareUiColors,
    reduceMotion: Boolean,
) {
    // A spring on the progress rather than a straight follow, so a flick of the scroll does
    // not make the ring jump.
    val shown by animateFloatAsState(
        targetValue = progress.coerceIn(0f, 1f),
        animationSpec = if (reduceMotion) snap() else rareUiSpring(120f, 30f, 0.3f),
        label = "scroll-progress-ring",
    )

    Canvas(modifier = Modifier.size(20.dp)) {
        val stroke = 2.5.dp.toPx()
        drawCircle(
            color = colors.foreground.copy(alpha = 0.15f),
            radius = (size.minDimension - stroke) / 2,
            style = Stroke(width = stroke),
        )
        drawArc(
            color = colors.foreground,
            // Starting at the top rather than at three o'clock, as upstream's rotation of
            // the whole svg by minus ninety degrees does.
            startAngle = -90f,
            sweepAngle = 360f * shown,
            useCenter = false,
            topLeft = Offset(stroke / 2, stroke / 2),
            size = Size(size.width - stroke, size.height - stroke),
            style = Stroke(width = stroke, cap = StrokeCap.Round),
        )
    }
}

/** The opened state: every section, with one highlight that travels between them. */
@Composable
private fun SectionList(
    sections: List<ScrollProgressSection>,
    selection: String?,
    colors: RareUiColors,
    reduceMotion: Boolean,
    onPick: (String) -> Unit,
) {
    val density = LocalDensity.current
    val bounds = remember { mutableStateMapOf<String, Pair<Float, Float>>() }
    val active = bounds[selection]

    // One highlight that travels between the rows, rather than one per row fading in and
    // out, which is what upstream's shared layout id gives it.
    val highlightY by animateDpAsState(
        targetValue = with(density) { (active?.first ?: 0f).toDp() },
        animationSpec = if (reduceMotion) snap() else rareUiVisualSpring(SIZE_DURATION, SIZE_BOUNCE),
        label = "scroll-progress-highlight-y",
    )
    val highlightHeight = with(density) { (active?.second ?: 0f).toDp() }
    val highlightInk = colors.foreground.copy(alpha = 0.1f)

    Column(
        modifier =
            Modifier
                // As wide as its longest label and no wider, with every row filling that,
                // so the panel is the size of what it says rather than the size of
                // whatever it was offered.
                .width(IntrinsicSize.Max)
                .padding(6.dp)
                // Drawn behind the rows rather than placed among them, so the list stays
                // as wide as its longest label instead of being stretched to whatever the
                // highlight would fill.
                .drawBehind {
                    if (active == null) return@drawBehind
                    drawRoundRect(
                        color = highlightInk,
                        topLeft = Offset(0f, highlightY.toPx()),
                        size = Size(size.width, highlightHeight.toPx()),
                        cornerRadius = CornerRadius(RowRadius.toPx()),
                    )
                },
    ) {
        sections.forEachIndexed { index, section ->
            SectionRow(
                section = section,
                isActive = section.id == selection,
                index = index,
                colors = colors,
                reduceMotion = reduceMotion,
                onMeasured = { top, height -> bounds[section.id] = top to height },
                onPick = { onPick(section.id) },
            )
        }
    }
}

/** One row of the opened list, rising into place in its turn. */
@Composable
private fun SectionRow(
    section: ScrollProgressSection,
    isActive: Boolean,
    index: Int,
    colors: RareUiColors,
    reduceMotion: Boolean,
    onMeasured: (Float, Float) -> Unit,
    onPick: () -> Unit,
) {
    var arrived by remember { mutableStateOf(reduceMotion) }

    LaunchedEffect(reduceMotion) {
        if (reduceMotion) {
            arrived = true
            return@LaunchedEffect
        }
        delay(40L + index * 30L)
        arrived = true
    }

    val settle by animateFloatAsState(
        targetValue = if (arrived) 1f else 0f,
        animationSpec = if (reduceMotion) snap() else tween(300, easing = RareUiEasing.EaseInOut),
        label = "scroll-progress-row",
    )
    val smear = 3.dp * (1f - settle)

    Row(
        modifier =
            Modifier
                .graphicsLayer { alpha = settle }
                .offset(y = 4.dp * (1f - settle))
                .then(if (PLATFORM_BLURS && smear > 0.dp) Modifier.blur(smear) else Modifier)
                .fillMaxWidth()
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onPick,
                ).onGloballyPositioned { coordinates ->
                    onMeasured(coordinates.positionInParent().y, coordinates.size.height.toFloat())
                }.padding(horizontal = 12.dp, vertical = 8.dp)
                .semantics {
                    role = Role.Button
                    selected = isActive
                },
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier =
                Modifier
                    .size(6.dp)
                    .background(
                        if (isActive) colors.foreground else colors.foreground.copy(alpha = 0.3f),
                        CircleShape,
                    ),
        )
        BasicText(
            text = section.label,
            style =
                TextStyle(
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = if (isActive) colors.foreground else colors.foreground.copy(alpha = 0.55f),
                ),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}
