/*
 * DeleteButton.kt
 * A port of upstream's `components/ui/delete-button.tsx`.
 *
 * A bin that opens into a confirmation rather than a dialogue. The tile grows sideways, a
 * recessed panel appears in the space it made, and two raised buttons rise into it.
 */

package io.github.dim971.rareui.components.inputs

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import io.github.dim971.rareui.theme.RareUiColors
import io.github.dim971.rareui.theme.RareUiEasing
import io.github.dim971.rareui.theme.RareUiTheme
import io.github.dim971.rareui.theme.rareUiSpring
import io.github.dim971.rareui.theme.rememberRareUiReduceMotion
import kotlinx.coroutines.delay

/** What the button last did. */
private enum class DeleteStatus {
    /** Nothing yet, or long enough ago that it has gone back to nothing. */
    IDLE,

    /** It deleted. The bin is replaced by a tick that draws itself on. */
    DELETED,

    /** It did not. The bin gives a small nod and stays. */
    KEPT,
}

/** The tile's width and height. */
private val Tile = 48.dp

/** How much wider the button gets when it asks. */
private val Panel = 84.dp

/** The tile's corner radius. */
private val TileRadius = 16.dp

/** The accent both the tick and the confirm button are drawn in. */
private val DeleteAccent = Color(0xFFFF5F2E)

/** How long each answer is held before the button forgets it. */
private const val HOLD_DELETED_MILLIS = 1_400L
private const val HOLD_KEPT_MILLIS = 600L

/** The nod the bin gives when the thing is kept: down to this and back. */
private const val NOD_SCALE = 0.86f
private const val NOD_MILLIS = 225

/** Upstream's `PRESS`, stiff and light so a disc snaps rather than squashing. */
private const val PRESS_STIFFNESS = 520f
private const val PRESS_DAMPING = 18f
private const val PRESS_MASS = 0.5f

/**
 * A delete button that asks first, in the space it makes for itself.
 *
 * ```kotlin
 * DeleteButton(onConfirm = { remove(item) })
 * ```
 *
 * Tapping it opens the confirmation; tapping it again, or pressing the cross, keeps the
 * thing. The answer is held for a moment and then the button goes back to being a bin.
 *
 * With animations turned off on the device everything happens without a duration: the panel
 * appears, the lid is simply open, and nothing springs.
 *
 * @param modifier the modifier to apply.
 * @param onCancel called when the thing is kept, whether by the cross or by tapping the bin
 *   again.
 * @param onConfirm called when the tick is pressed.
 */
@Composable
public fun DeleteButton(
    modifier: Modifier = Modifier,
    onCancel: (() -> Unit)? = null,
    onConfirm: (() -> Unit)? = null,
) {
    val colors = RareUiTheme.colors
    val reduceMotion = rememberRareUiReduceMotion()
    val outlines = remember { BinOutlines() }

    var open by remember { mutableStateOf(false) }
    var status by remember { mutableStateOf(DeleteStatus.IDLE) }
    val nod = remember { Animatable(1f) }
    val tick = remember { Animatable(0f) }

    val width by androidx.compose.animation.core.animateDpAsState(
        targetValue = if (open) Tile + Panel else Tile,
        animationSpec =
            if (reduceMotion) snap() else tween(620, easing = RareUiEasing.EaseOutSettle),
        label = "delete-width",
    )
    val wallTop by animateFloatAsState(
        targetValue = if (open) BIN_WALL_TOP_OPEN else BIN_WALL_TOP,
        animationSpec =
            if (reduceMotion) snap() else tween(560, easing = RareUiEasing.EaseOutSettle),
        label = "delete-walls",
    )
    val lid by animateFloatAsState(
        targetValue = if (open) BIN_LID_OPEN else 0f,
        // An overshooting curve, so the lid is thrown a little past its open angle before
        // it settles back onto it.
        animationSpec =
            if (reduceMotion) snap() else tween(600, easing = RareUiEasing.EaseOutOvershoot),
        label = "delete-lid",
    )

    LaunchedEffect(status, reduceMotion) {
        when (status) {
            DeleteStatus.IDLE -> {
                tick.snapTo(0f)
                nod.snapTo(1f)
            }

            DeleteStatus.DELETED -> {
                nod.snapTo(1f)
                if (reduceMotion) {
                    tick.snapTo(1f)
                } else {
                    tick.animateTo(1f, tween(450, easing = RareUiEasing.EaseOutSettle))
                }
                delay(HOLD_DELETED_MILLIS)
                status = DeleteStatus.IDLE
            }

            DeleteStatus.KEPT -> {
                tick.snapTo(0f)
                if (!reduceMotion) {
                    nod.animateTo(NOD_SCALE, tween(NOD_MILLIS, easing = RareUiEasing.EaseOutSettle))
                    nod.animateTo(1f, tween(NOD_MILLIS, easing = RareUiEasing.EaseOutSettle))
                }
                delay(HOLD_KEPT_MILLIS)
                status = DeleteStatus.IDLE
            }
        }
    }

    Box(
        modifier =
            modifier
                .width(width)
                .height(Tile)
                .background(colors.surface, RoundedCornerShape(TileRadius)),
        contentAlignment = Alignment.CenterStart,
    ) {
        AnimatedVisibility(
            visible = open,
            modifier = Modifier.align(Alignment.CenterEnd),
            // The panel waits a beat before it arrives, so the tile has already started
            // widening and the panel appears in a space rather than pushing one open.
            enter =
                if (reduceMotion) {
                    fadeIn(snap())
                } else {
                    fadeIn(tween(440, delayMillis = 140, easing = RareUiEasing.EaseOutSettle)) +
                        slideInHorizontally(
                            tween(440, delayMillis = 140, easing = RareUiEasing.EaseOutSettle),
                        ) { -6 }
                },
            exit =
                if (reduceMotion) {
                    fadeOut(snap())
                } else {
                    fadeOut(tween(200)) + slideOutHorizontally(tween(200)) { -6 }
                },
        ) {
            ConfirmationPanel(
                colors = colors,
                outlines = outlines,
                reduceMotion = reduceMotion,
                onConfirm = {
                    open = false
                    status = DeleteStatus.DELETED
                    onConfirm?.invoke()
                },
                onCancel = {
                    open = false
                    status = DeleteStatus.KEPT
                    onCancel?.invoke()
                },
            )
        }

        TileTrigger(
            open = open,
            status = status,
            colors = colors,
            outlines = outlines,
            wallTop = wallTop,
            lid = lid,
            nod = { nod.value },
            tick = { tick.value },
            reduceMotion = reduceMotion,
            onClick = {
                if (open) {
                    open = false
                    status = DeleteStatus.KEPT
                    onCancel?.invoke()
                } else {
                    status = DeleteStatus.IDLE
                    open = true
                }
            },
        )
    }
}

/** The tile itself: a bin, or the tick that replaces it once something has gone. */
@Composable
@Suppress("LongParameterList")
private fun TileTrigger(
    open: Boolean,
    status: DeleteStatus,
    colors: RareUiColors,
    outlines: BinOutlines,
    wallTop: Float,
    lid: Float,
    nod: () -> Float,
    tick: () -> Float,
    reduceMotion: Boolean,
    onClick: () -> Unit,
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val press by animateFloatAsState(
        targetValue = if (pressed && !reduceMotion) 0.94f else 1f,
        animationSpec =
            if (reduceMotion) snap() else rareUiSpring(PRESS_STIFFNESS, PRESS_DAMPING, PRESS_MASS),
        label = "delete-press",
    )
    val showsTick = status == DeleteStatus.DELETED

    Canvas(
        modifier =
            Modifier
                .size(Tile)
                .graphicsLayer {
                    scaleX = press
                    scaleY = press
                }.clickable(
                    interactionSource = interaction,
                    indication = null,
                    onClick = onClick,
                ).semantics {
                    role = Role.Button
                    contentDescription = if (open) "Delete. Confirming" else "Delete"
                },
    ) {
        // The bin is drawn at twenty points inside the twenty-four point box it was
        // authored in, so the lid can swing outside the glyph without being clipped.
        val icon = 20.dp.toPx()
        val factor = icon / BIN_BOX
        translate(left = (size.width - icon) / 2, top = (size.height - icon) / 2) {
            if (showsTick) {
                scale(factor, pivot = Offset.Zero) {
                    drawPath(
                        path = outlines.tickUpTo(tick()),
                        color = DeleteAccent,
                        style = roundStroke(2.5.dp.toPx() / factor),
                    )
                }
            } else {
                scale(nod(), pivot = Offset(icon / 2, icon / 2)) {
                    scale(factor, pivot = Offset.Zero) {
                        binWallsPath(wallTop, outlines.walls)
                        drawPath(
                            path = outlines.walls,
                            color = colors.glyph,
                            style = roundStroke(2.dp.toPx() / factor),
                        )
                        rotate(
                            degrees = lid,
                            pivot = Offset(BinLidHinge.x * BIN_BOX, BinLidHinge.y * BIN_BOX),
                        ) {
                            drawPath(
                                path = outlines.lid,
                                color = colors.glyph,
                                style = roundStroke(2.dp.toPx() / factor),
                            )
                        }
                    }
                }
            }
        }
    }
}

/** The stroke every part of this icon is drawn with. */
private fun roundStroke(width: Float) = Stroke(width = width, cap = StrokeCap.Round, join = StrokeJoin.Round)

/** The recessed panel, with the two raised discs in it. */
@Composable
private fun ConfirmationPanel(
    colors: RareUiColors,
    outlines: BinOutlines,
    reduceMotion: Boolean,
    onConfirm: () -> Unit,
    onCancel: () -> Unit,
) {
    Row(
        modifier =
            Modifier
                .width(Panel)
                .height(Tile)
                .drawBehind {
                    // The notch, pointing back at the tile the panel opened out of.
                    val notch = Path()
                    val width = 6.dp.toPx()
                    val height = 10.dp.toPx()
                    notch.moveTo(0f, (size.height - height) / 2)
                    notch.lineTo(-width, size.height / 2)
                    notch.lineTo(0f, (size.height + height) / 2)
                    notch.close()
                    drawPath(notch, colors.surfaceRecessed)
                }.background(colors.surfaceRecessed, RoundedCornerShape(TileRadius)),
        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RaisedDisc(
            label = "Confirm delete",
            icon = outlines.tick,
            tint = DeleteAccent,
            colors = colors,
            reduceMotion = reduceMotion,
            onClick = onConfirm,
        )
        RaisedDisc(
            label = "Cancel",
            icon = outlines.cross,
            tint = colors.glyph,
            colors = colors,
            reduceMotion = reduceMotion,
            onClick = onCancel,
        )
    }
}

/** One of the two buttons in the panel: a raised disc with an icon stroked on it. */
@Composable
private fun RaisedDisc(
    label: String,
    icon: Path,
    tint: Color,
    colors: RareUiColors,
    reduceMotion: Boolean,
    onClick: () -> Unit,
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val press by animateFloatAsState(
        targetValue = if (pressed && !reduceMotion) 0.84f else 1f,
        animationSpec =
            if (reduceMotion) snap() else rareUiSpring(PRESS_STIFFNESS, PRESS_DAMPING, PRESS_MASS),
        label = "delete-disc-press",
    )

    Canvas(
        modifier =
            Modifier
                .size(28.dp)
                .graphicsLayer {
                    scaleX = press
                    scaleY = press
                }
                // The lift: a soft shadow under the disc, which is what makes it read as
                // sitting proud of the recess rather than printed on it.
                .shadow(2.dp, CircleShape)
                .background(colors.surface, CircleShape)
                .clickable(
                    interactionSource = interaction,
                    indication = null,
                    onClick = onClick,
                ).semantics {
                    role = Role.Button
                    contentDescription = label
                },
    ) {
        val glyph = 14.dp.toPx()
        val factor = glyph / BIN_BOX
        translate(left = (size.width - glyph) / 2, top = (size.height - glyph) / 2) {
            scale(factor, pivot = Offset.Zero) {
                drawPath(path = icon, color = tint, style = roundStroke(3.5.dp.toPx() / factor))
            }
        }
    }
}
