/*
 * ProximitySidebar.kt
 * A port of upstream's `components/ui/proximity-sidebar.tsx`.
 *
 * A minimap of a page, drawn as a column of dashes, where the dash nearest the pointer
 * swells and its neighbours swell a little less. It is a document outline you read with the
 * cursor rather than with your eyes.
 *
 * A phone has no pointer, and upstream already knows it: alongside the proximity effect it
 * swells the dash for whichever section is being read. That is the behaviour under a
 * finger, and the proximity effect appears when a pointer does, with a mouse, a trackpad or
 * a stylus. Recorded in docs/fidelity.md.
 */

package io.github.dim971.rareui.components.navigation

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.snap
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.github.dim971.rareui.theme.RareUiTheme
import io.github.dim971.rareui.theme.rareUiSpring
import io.github.dim971.rareui.theme.rememberRareUiReduceMotion
import kotlin.math.abs
import kotlin.math.min

/** How prominent a section is in the outline. */
public enum class ProximitySectionKind(
    /** The dash's width at rest. */
    internal val base: Dp,
    /** How much wider it gets when the pointer is on it. */
    internal val bump: Dp,
    /** Whether the dash is drawn at full strength or muted. */
    internal val isProminent: Boolean,
) {
    /** The page's own title. */
    TITLE(base = 40.dp, bump = 70.dp, isProminent = true),

    /** A heading under it. */
    SUBTITLE(base = 36.dp, bump = 64.dp, isProminent = true),

    /** A heading under that. */
    SECTION(base = 30.dp, bump = 56.dp, isProminent = false),

    /** Anything below. */
    BODY(base = 24.dp, bump = 50.dp, isProminent = false),
}

/** One entry in a [ProximitySidebar]. */
@Immutable
public data class ProximitySection(
    /** The section's identifier, which is what a selection reports back. */
    public val id: String,
    /** The name, read out by a screen reader since the dash itself carries no text. */
    public val label: String,
    /** How prominent it is. */
    public val kind: ProximitySectionKind = ProximitySectionKind.BODY,
)

/** Which side of the page the outline sits on. */
public enum class ProximitySidebarSide {
    /** On the left, growing rightward. */
    LEADING,

    /** On the right, growing leftward. */
    TRAILING,
}

/** How far from a dash the pointer still reaches it. */
internal val ProximityRadius = 40.dp

/** The width every dash is measured against, so they scale rather than resize. */
internal val ProximityMaxDashWidth = 110.dp

/** The space between two dashes. */
private val DashSpacing = 8.dp

/** The spring that smooths the swelling, from upstream's own. */
private const val SETTLE_STIFFNESS = 320f
private const val SETTLE_DAMPING = 34f
private const val SETTLE_MASS = 0.7f

/**
 * How wide a dash should be, given how far the pointer is from it.
 *
 * Upstream maps the distance over `[-radius, 0, radius]` to `[base, base + bump, base]` and
 * clamps outside that, so a dash is at rest until the pointer comes within reach and then
 * swells smoothly to its full width as the pointer arrives on it.
 *
 * @param distance how far the pointer is from the dash's middle. Sign does not matter.
 * @param kind the dash's kind, which sets both ends of the range.
 * @return the width.
 */
internal fun proximityDashWidth(
    distance: Dp,
    kind: ProximitySectionKind,
): Dp {
    val reach = min(1f, abs(distance.value) / ProximityRadius.value)
    return kind.base + kind.bump * (1f - reach)
}

/**
 * A page outline drawn as dashes, which swell as the pointer passes them.
 *
 * ```kotlin
 * ProximitySidebar(sections = outline, selection = reading, onSelect = { scrollTo(it) })
 * ```
 *
 * With animations turned off on the device the dashes change width without springing.
 *
 * @param sections the page's sections, in order.
 * @param modifier the modifier to apply.
 * @param side which side of the page it sits on.
 * @param selection the section being read, which is the one that swells when there is no
 *   pointer.
 * @param onSelect called when a dash is tapped, so the host can scroll to it.
 */
@Composable
public fun ProximitySidebar(
    sections: List<ProximitySection>,
    modifier: Modifier = Modifier,
    side: ProximitySidebarSide = ProximitySidebarSide.LEADING,
    selection: String? = null,
    onSelect: ((String) -> Unit)? = null,
) {
    val colors = RareUiTheme.colors
    val reduceMotion = rememberRareUiReduceMotion()
    val density = LocalDensity.current

    val centres = remember { mutableStateMapOf<Int, Float>() }
    var pointerY by remember { mutableStateOf<Float?>(null) }

    val edge =
        if (side == ProximitySidebarSide.LEADING) Alignment.CenterStart else Alignment.CenterEnd

    Column(
        modifier =
            modifier
                .width(ProximityMaxDashWidth)
                .pointerInput(Unit) {
                    awaitPointerEventScope {
                        while (true) {
                            val event = awaitPointerEvent()
                            pointerY =
                                when (event.type) {
                                    PointerEventType.Exit -> null
                                    // A finger produces these too, and following it is the
                                    // right answer: a touch that is dragging down the
                                    // outline is a pointer for as long as it lasts.
                                    PointerEventType.Enter,
                                    PointerEventType.Move,
                                    ->
                                        event.changes
                                            .firstOrNull()
                                            ?.position
                                            ?.y

                                    PointerEventType.Release -> null
                                    else -> pointerY
                                }
                        }
                    }
                }.semantics { contentDescription = "Page outline" },
        verticalArrangement = Arrangement.spacedBy(DashSpacing),
    ) {
        sections.forEachIndexed { index, section ->
            val pointer = pointerY
            val centre = centres[index]
            val target =
                when {
                    // No pointer to follow, so the dash for whatever is being read swells
                    // instead. Upstream does the same where there is no cursor.
                    pointer == null || centre == null ->
                        if (pointer == null && section.id == selection) {
                            section.kind.base + section.kind.bump
                        } else {
                            section.kind.base
                        }

                    else ->
                        proximityDashWidth(
                            distance = with(density) { (pointer - centre).toDp() },
                            kind = section.kind,
                        )
                }

            val width by animateDpAsState(
                targetValue = target,
                animationSpec =
                    if (reduceMotion) {
                        snap()
                    } else {
                        rareUiSpring(SETTLE_STIFFNESS, SETTLE_DAMPING, SETTLE_MASS)
                    },
                label = "proximity-dash",
            )

            Box(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .height(DashSpacing)
                        .onGloballyPositioned { coordinates ->
                            centres[index] = coordinates.positionInParent().y + coordinates.size.height / 2f
                        }.clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = { onSelect?.invoke(section.id) },
                        ).semantics {
                            role = Role.Button
                            contentDescription = section.label
                            selected = section.id == selection
                        },
                contentAlignment = edge,
            ) {
                Box(
                    modifier =
                        Modifier
                            .width(width)
                            .height(1.dp)
                            .background(
                                if (section.kind.isProminent) {
                                    colors.foreground
                                } else {
                                    colors.glyph.copy(alpha = 0.4f)
                                },
                            ),
                )
            }
        }
    }
}
