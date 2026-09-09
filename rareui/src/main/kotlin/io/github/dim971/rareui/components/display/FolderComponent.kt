/*
 * FolderComponent.kt
 * A port of upstream's `components/ui/folder-component.tsx`.
 *
 * A folder whose flap tips back and whose contents fan out of it. There are three states
 * rather than two: at rest, under a pointer, and open. Under a finger only the first and
 * the last are reachable, which is what a tap toggles between.
 *
 * Two departures, recorded in docs/fidelity.md. The inner shadows upstream builds out of a
 * blur and a composite are left out: Compose has no inner shadow, and the hairline borders
 * carry the same separation without one. And the cards behind the flap are blurred only
 * from Android 12, where there is a render effect to blur with; below that they show
 * through sharp, which is what upstream looks like with backdrop filters unsupported.
 */

package io.github.dim971.rareui.components.display

import android.os.Build
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import io.github.dim971.rareui.theme.rareUiSpring
import io.github.dim971.rareui.theme.rememberRareUiReduceMotion
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** The drawing's own size, which everything else is stated in. */
private val FolderWidth = 321.dp
private val FolderHeight = 270.dp

/** The flap, which is shorter than the folder so the cards show above it. */
private val FlapHeight = 241.dp

/** The cards' spring, from upstream. */
private const val CARD_STIFFNESS = 120f
private const val CARD_DAMPING = 13f

/** The flap's, which is damped very slightly harder. */
private const val FLAP_DAMPING = 14f

/**
 * Upstream's `perspective: 800px` against a flap drawn three hundred and twenty one points
 * wide.
 *
 * Compose scales the camera's distance by the display's density, so the number here is in
 * points and eight hundred of them is eight hundred. Setting it in the units the default of
 * eight suggests puts the camera a few pixels from the flap and tears it apart.
 */
private const val FLAP_CAMERA = 800f

/** How much of the flap the cards are blurred by, where the platform can blur at all. */
private val FlapBlur = 6.dp

/** Blur needs a render effect, which arrived in Android 12. */
private val PLATFORM_BLURS = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S

/**
 * The folder's front flap, quoted from upstream's `FLAP_PATH`.
 *
 * A rounded rectangle with a step cut into its top edge, which is the tab a paper folder
 * has, drawn as one path rather than assembled from parts.
 */
internal object FolderFlapShape : Shape {
    private const val VIEW_WIDTH = 321f
    private const val VIEW_HEIGHT = 241f

    private val outline: Path =
        PathParser()
            .parsePathString(
                "M0 25C0 11.1929 11.1929 0 25 0H136.084C143.044 0 149.689 2.90139 154.42 " +
                    "8.00608L178.08 33.5343C182.811 38.639 189.456 41.5404 196.416 41.5404H296C309.807 " +
                    "41.5404 321 52.7333 321 66.5404V216C321 229.807 309.807 241 296 241H25C11.1929 " +
                    "241 0 229.807 0 216V25Z",
            ).toPath()

    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density,
    ): Outline {
        val scaled = Path()
        scaled.addPath(outline)
        // Scaled rather than fitted, because the flap is stretched to whatever it is given
        // and its step has to stay where the drawing puts it.
        val matrix =
            androidx.compose.ui.graphics
                .Matrix()
        matrix.scale(size.width / VIEW_WIDTH, size.height / VIEW_HEIGHT, 1f)
        scaled.transform(matrix)
        return Outline.Generic(scaled)
    }
}

/**
 * A folder whose flap tips back and whose contents fan out of it.
 *
 * ```kotlin
 * FolderComponent(color = FolderColor.BLUE, size = FolderSize.LARGE)
 * ```
 *
 * Tapping it opens and closes it. A pointer passing over it lifts the cards part of the
 * way, which is a state a finger never reaches and upstream never reaches either.
 *
 * With animations turned off on the device the states change without springing.
 *
 * @param modifier the modifier to apply.
 * @param color which of the three folders to draw.
 * @param size how large to draw it.
 */
@Composable
public fun FolderComponent(
    modifier: Modifier = Modifier,
    color: FolderColor = FolderColor.BLACK,
    size: FolderSize = FolderSize.MEDIUM,
) {
    val reduceMotion = rememberRareUiReduceMotion()
    val palette = remember(color) { color.palette }

    val interaction = remember { MutableInteractionSource() }
    val hovering by interaction.collectIsHoveredAsState()
    var open by remember { mutableStateOf(false) }

    // Leaving closes it, exactly as upstream's mouse leave does, so a folder never stays
    // open behind the pointer.
    LaunchedEffect(hovering) { if (!hovering) open = false }

    val state =
        when {
            open -> FolderState.OPEN
            hovering -> FolderState.HOVERING
            else -> FolderState.REST
        }

    val flapAngle by animateFloatAsState(
        targetValue = state.flapAngle,
        animationSpec = if (reduceMotion) snap() else rareUiSpring(CARD_STIFFNESS, FLAP_DAMPING),
        label = "folder-flap",
    )

    Box(
        modifier =
            modifier
                .size(FolderWidth * size.scale, FolderHeight * size.scale)
                .hoverable(interaction)
                .clickable(
                    interactionSource = interaction,
                    indication = null,
                    onClick = { open = !open },
                ).semantics {
                    role = Role.Button
                    contentDescription = if (open) "Folder. Open" else "Folder. Closed"
                },
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier =
                Modifier
                    // Required rather than merely asked for. The folder is drawn at its own
                    // size and then scaled, and a plain size would be clamped by the
                    // smaller box the scaling leaves behind, squashing the drawing before
                    // it had a chance to shrink.
                    .requiredSize(FolderWidth, FolderHeight)
                    .graphicsLayer {
                        scaleX = size.scale
                        scaleY = size.scale
                    },
            contentAlignment = Alignment.Center,
        ) {
            Box(
                modifier =
                    Modifier
                        .requiredSize(FolderWidth, FolderHeight)
                        .background(palette.back, RoundedCornerShape(25.dp)),
            )

            FolderCards(state, palette, reduceMotion)

            // Upstream blurs whatever is behind the flap, which is what turns the cards
            // under it into soft shapes while the tips above it stay sharp. Drawing the
            // cards a second time, blurred and clipped to the flap's own outline, is the
            // same effect by a different route: a backdrop filter has no counterpart here.
            if (PLATFORM_BLURS) {
                FlapLayer(flapAngle) {
                    Box(
                        // Sized to the flap before it is clipped by it, or the outline
                        // would be stretched over whatever the cards inside happen to
                        // measure and the blur would spill past the fold.
                        modifier = Modifier.requiredSize(FolderWidth, FlapHeight).clip(FolderFlapShape),
                        contentAlignment = Alignment.Center,
                    ) {
                        Box(
                            modifier =
                                Modifier
                                    .requiredSize(FolderWidth, FolderHeight)
                                    .offset(y = (-16).dp)
                                    .blur(FlapBlur),
                            contentAlignment = Alignment.Center,
                        ) {
                            FolderCards(state, palette, reduceMotion)
                        }
                    }
                }
            }

            FlapLayer(flapAngle) {
                Box(
                    modifier =
                        Modifier
                            .requiredSize(FolderWidth, FlapHeight)
                            .background(palette.flapFill.copy(alpha = palette.flapOpacity), FolderFlapShape)
                            .border(1.dp, palette.flapStroke, FolderFlapShape),
                )
            }
        }
    }
}

/**
 * Places something where the flap is, tipped back by however far it is open.
 *
 * Both the flap itself and the copy of the cards blurred behind it have to sit in exactly
 * the same place, so the transform is written once.
 */
@Composable
private fun FlapLayer(
    angle: Float,
    content: @Composable () -> Unit,
) {
    Box(
        modifier =
            Modifier
                .requiredSize(FolderWidth, FlapHeight)
                .offset(y = 16.dp)
                .graphicsLayer {
                    rotationX = angle
                    cameraDistance = FLAP_CAMERA
                    // The flap hangs from its own bottom edge, which is the fold.
                    transformOrigin = TransformOrigin(0.5f, 1f)
                },
        contentAlignment = Alignment.Center,
    ) {
        content()
    }
}

/** The three sheets inside the folder, fanned out according to the state. */
@Composable
private fun FolderCards(
    state: FolderState,
    palette: FolderPalette,
    reduceMotion: Boolean,
) {
    Box(contentAlignment = Alignment.Center) {
        FolderCardPlacements.forEach { placement ->
            val pose = placement.pose(state)
            val x = remember { Animatable(pose.x) }
            val y = remember { Animatable(pose.y) }
            val turn = remember { Animatable(pose.rotation) }

            LaunchedEffect(state, reduceMotion) {
                val target = placement.pose(state)
                if (reduceMotion) {
                    x.snapTo(target.x)
                    y.snapTo(target.y)
                    turn.snapTo(target.rotation)
                    return@LaunchedEffect
                }
                // The cards leave in turn rather than together, and the one furthest from
                // the middle leaves first, so the fan opens outward. Compose has no delay
                // on a spring, so the wait is the coroutine's rather than the spec's.
                delay(placement.delay(state))
                val spec = rareUiSpring<Float>(CARD_STIFFNESS, CARD_DAMPING)
                launch { x.animateTo(target.x, spec) }
                launch { y.animateTo(target.y, spec) }
                launch { turn.animateTo(target.rotation, spec) }
            }

            Box(
                modifier =
                    Modifier.graphicsLayer {
                        translationX = x.value * density
                        translationY = y.value * density
                        rotationZ = turn.value
                    },
            ) {
                FolderCard(palette)
            }
        }
    }
}

/** The card's own drawing size, from upstream's `viewBox="0 0 164 214"`. */
private val CardWidth = 164.dp
private val CardHeight = 214.dp

/**
 * One sheet of paper inside a folder.
 *
 * A rounded card with a heading bar and nine rows of two columns of placeholder text.
 * Upstream draws each line as its own rectangle carrying a transform matrix with a skew of
 * about two thousandths of a degree, which is the residue of whatever drew the original.
 * The lines are level here, which is a difference of a fifth of a pixel across the card.
 */
@Composable
private fun FolderCard(palette: FolderPalette) {
    Box(
        modifier =
            Modifier
                .requiredSize(CardWidth, CardHeight)
                .background(palette.cardFill, RoundedCornerShape(20.dp))
                .border(1.dp, palette.cardStroke, RoundedCornerShape(20.dp)),
    ) {
        Canvas(modifier = Modifier.size(CardWidth, CardHeight)) {
            // The heading bar: wider than the body lines and twice their height.
            drawRoundRect(
                color = palette.cardLine,
                topLeft = Offset(14.1193.dp.toPx(), 31.2091.dp.toPx()),
                size = Size(134.84.dp.toPx(), 11.8892.dp.toPx()),
                cornerRadius = CornerRadius(11.8892.dp.toPx() / 2),
            )

            repeat(9) { row ->
                val top = (60.9939f + row * 14.1183f).dp.toPx()
                listOf(14.8253f, 84.4303f).forEach { left ->
                    drawRoundRect(
                        color = palette.cardLine,
                        topLeft = Offset(left.dp.toPx(), top),
                        size = Size(64.5183.dp.toPx(), 5.88276.dp.toPx()),
                        cornerRadius = CornerRadius(5.88276.dp.toPx() / 2),
                    )
                }
            }
        }
    }
}
