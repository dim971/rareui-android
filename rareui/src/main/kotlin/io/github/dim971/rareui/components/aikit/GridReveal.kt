/*
 * GridReveal.kt
 * A port of upstream's `components/ui/grid-reveal.tsx`.
 *
 * A placeholder that becomes a picture by dividing itself, over and over, into the picture.
 * Each split slides two halves out of where their parent was, the cells take on the average
 * colour of what is underneath them, and the photograph itself only arrives at the very end.
 *
 * One departure, recorded in docs/fidelity.md: this takes an image rather than a URL, for
 * the same reason nothing else in this library reaches the network.
 */

package io.github.dim971.rareui.components.aikit

import android.graphics.Bitmap
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.dim971.rareui.theme.rememberRareUiReduceMotion
import kotlin.math.exp
import kotlin.math.roundToInt

/** Where the gutters between cells begin closing, and where they have gone entirely. */
private const val GUTTER_FROM = 0.35
private const val GUTTER_TO = 0.75

/** Where the photograph itself begins to arrive. */
private const val PHOTO_FROM = 0.93

/** A frame that arrives late is capped rather than jumping the whole reveal at once. */
private const val LONGEST_FRAME = 0.05

/** Upstream's own smoothing rates: the split settles a little faster than the colour does. */
private const val SPLIT_RATE = 5.5
private const val COLOUR_RATE = 4.0

/** Everything one frame of the reveal needs. */
internal data class GridRevealFrame(
    val clock: Double,
    val split: Double,
    val fade: Double,
    val tint: Double,
    val dark: Boolean,
)

/**
 * The reveal's state between frames.
 *
 * Deliberately not observable. It is stepped inside a frame loop and anything watching it
 * would ask for another frame in response to the frame it is already drawing, which never
 * stops.
 */
internal class GridRevealScene {
    var root: GridRevealCell? = null
        private set
    var hasColours = false
        private set

    private var branches: List<GridRevealCell> = emptyList()
    private var clock = 0.0
    private var split = 0.0
    private var fade = 0.0
    private var last = 0L
    private var dark = false

    /**
     * Builds the subdivision and, if there is a picture, measures it.
     *
     * @param aspect the frame's width over its height.
     * @param image the picture, or `null` while there is not one.
     * @param dark whether the appearance is dark.
     */
    fun build(
        aspect: Double,
        image: ImageBitmap?,
        dark: Boolean,
    ) {
        val tree = gridRevealBuildTree(aspect)
        root = tree.root
        branches = tree.branches
        this.dark = dark
        clock = 0.0
        split = 0.0
        fade = 0.0
        last = 0L
        hasColours = false

        val pixels = image?.let { sample(it) } ?: return
        gridRevealMeasure(tree.root, pixels, GRID_REVEAL_SAMPLE)
        // Now that the picture is known, the splits are reordered so its busiest parts come
        // apart first. The times are reused, so the pacing does not change with the order.
        gridRevealOrderByDetail(tree.branches, split)
        hasColours = true
    }

    /** Sends the reveal straight to the end, for when nothing should move. */
    fun finish() {
        split = 1.0
        fade = if (hasColours) 1.0 else 0.0
    }

    /**
     * Steps the reveal up to a moment.
     *
     * @param nanos the frame's timestamp.
     * @param elapsedSinceStart how long the reveal has been running, in seconds.
     * @param progress how far along the work is, or `null` to pace it.
     * @param duration the estimate a self-paced reveal creeps against.
     * @param hasImage whether the picture has arrived.
     * @param reduceMotion whether to go straight there.
     * @return the frame to draw.
     */
    @Suppress("LongParameterList")
    fun advance(
        nanos: Long,
        elapsedSinceStart: Double,
        progress: Double?,
        duration: Double,
        hasImage: Boolean,
        reduceMotion: Boolean,
    ): GridRevealFrame {
        val elapsed = if (last == 0L) 0.0 else minOf((nanos - last) / 1e9, LONGEST_FRAME)
        last = nanos
        clock += elapsed

        val target =
            when {
                progress != null -> progress.coerceIn(0.0, 1.0)
                hasImage -> 1.0
                // Waiting: the grid creeps but stops short, leaving the arrival somewhere
                // to go.
                else ->
                    minOf(
                        GRID_REVEAL_WAIT_CAP,
                        gridRevealSelfPaced(elapsedSinceStart, duration),
                    )
            }

        if (reduceMotion) {
            split = target
            fade = if (hasColours) 1.0 else 0.0
        } else {
            // Two exponential smoothers, upstream's own rates.
            split += (target - split) * (1 - exp(-elapsed * SPLIT_RATE))
            fade += ((if (hasColours) 1.0 else 0.0) - fade) * (1 - exp(-elapsed * COLOUR_RATE))
        }

        return GridRevealFrame(
            clock = clock,
            split = split,
            fade = fade,
            tint = if (hasColours) fade else 0.0,
            dark = dark,
        )
    }

    /**
     * Reduces a picture to a small square of pixels, which is all the averages need.
     *
     * Cropped to fill rather than squashed, so the averages line up with what the finished
     * picture will actually show.
     */
    private fun sample(image: ImageBitmap): IntArray? {
        val side = GRID_REVEAL_SAMPLE
        val source =
            runCatching { image.asAndroidBitmap() }.getOrNull() ?: return null

        val scale =
            maxOf(side.toFloat() / source.width, side.toFloat() / source.height)
        val width = (source.width * scale).roundToInt().coerceAtLeast(1)
        val height = (source.height * scale).roundToInt().coerceAtLeast(1)

        val stretched = Bitmap.createScaledBitmap(source, width, height, true)
        val square =
            Bitmap.createBitmap(
                stretched,
                ((width - side) / 2).coerceAtLeast(0),
                ((height - side) / 2).coerceAtLeast(0),
                minOf(side, width),
                minOf(side, height),
            )

        val pixels = IntArray(side * side)
        square.getPixels(pixels, 0, side, 0, 0, minOf(side, square.width), minOf(side, square.height))
        return pixels
    }
}

/**
 * A picture that reveals itself by subdividing.
 *
 * ```kotlin
 * GridReveal(image = photo, caption = "Generating")
 * ```
 *
 * Give it a [progress] and it follows that. Leave it out and it paces itself, creeping
 * toward nine tenths and holding there until the picture is ready, so a load that takes
 * longer than expected still looks like it is working.
 *
 * With animations turned off on the device it draws the finished picture without revealing
 * it.
 *
 * @param modifier the modifier to apply.
 * @param image the picture. Until it is given, the grid stays grey and keeps working.
 * @param progress how far along the work is, in `0..1`. Leave it out to pace the reveal.
 * @param aspect the frame's width over its height.
 * @param caption a line shown over the grid while it works.
 * @param estimatedDuration how long the work is expected to take, in seconds.
 */
@Composable
@Suppress("LongParameterList")
public fun GridReveal(
    modifier: Modifier = Modifier,
    image: ImageBitmap? = null,
    progress: Double? = null,
    aspect: Float = 1f,
    caption: String? = null,
    estimatedDuration: Double = 6.0,
) {
    val dark = isSystemInDarkTheme()
    val reduceMotion = rememberRareUiReduceMotion()
    val scene = remember { GridRevealScene() }
    // The scene is a plain object, so what the canvas watches is the frame it publishes.
    var painted by remember { mutableStateOf(GridRevealFrame(0.0, 0.0, 0.0, 0.0, dark)) }

    LaunchedEffect(image, aspect, dark, reduceMotion) {
        scene.build(aspect.toDouble(), image, dark)
        if (reduceMotion) {
            scene.finish()
            painted = scene.advance(0L, 0.0, 1.0, estimatedDuration, image != null, true)
            return@LaunchedEffect
        }

        var started = 0L
        while (true) {
            val now = withFrameNanos { it }
            if (started == 0L) started = now
            painted =
                scene.advance(
                    nanos = now,
                    elapsedSinceStart = (now - started) / 1e9,
                    progress = progress,
                    duration = estimatedDuration,
                    hasImage = image != null,
                    reduceMotion = false,
                )
        }
    }

    Box(
        modifier =
            modifier
                .aspectRatio(aspect)
                .clip(RoundedCornerShape(16.dp))
                .semantics { if (caption != null) contentDescription = caption },
        contentAlignment = Alignment.BottomCenter,
    ) {
        Canvas(modifier = Modifier.matchParentSize()) {
            val root = scene.root ?: return@Canvas
            paintGridReveal(root, painted, image, scene.hasColours)
        }

        if (caption != null) {
            BasicText(
                text = caption,
                style = TextStyle(fontSize = 13.sp, fontWeight = FontWeight.Medium, color = Color.White),
                modifier =
                    Modifier
                        .padding(12.dp)
                        .background(Color.Black.copy(alpha = 0.45f), RoundedCornerShape(percent = 50))
                        .padding(horizontal = 12.dp, vertical = 7.dp),
            )
        }
    }
}

/** One rectangle being painted, partway between its parent's place and its own. */
private class GridRevealPatch(
    var left: Double,
    var top: Double,
    var width: Double,
    var height: Double,
    var red: Double,
    var green: Double,
    var blue: Double,
    var tone: Double,
) {
    /** This patch moved a fraction of the way toward a cell's own place and colour. */
    fun blended(
        cell: GridRevealCell,
        t: Double,
        size: Size,
    ): GridRevealPatch {
        fun mix(
            from: Double,
            to: Double,
        ) = from + (to - from) * t
        return GridRevealPatch(
            left = mix(left, cell.x * size.width),
            top = mix(top, cell.y * size.height),
            width = mix(width, cell.width * size.width),
            height = mix(height, cell.height * size.height),
            red = mix(red, cell.red),
            green = mix(green, cell.green),
            blue = mix(blue, cell.blue),
            tone = mix(tone, cell.tone),
        )
    }
}

/** Draws one frame of the reveal. */
private fun DrawScope.paintGridReveal(
    root: GridRevealCell,
    frame: GridRevealFrame,
    image: ImageBitmap?,
    hasColours: Boolean,
) {
    // The gutters recess into this rather than cutting through to whatever is behind the
    // component, so the grid reads as one object with grooves in it. A shade darker than
    // the cells, so a gutter looks like a groove cut into the surface rather than a hole
    // through it. Upstream multiplies the ground by 0.92.
    drawRect(
        color =
            gridRevealShade(
                grey = gridRevealGrey(root.tone, frame.dark, frame.clock),
                red = root.red,
                green = root.green,
                blue = root.blue,
                tint = frame.tint,
                dim = 0.92,
            ),
    )

    val soft = 1 - gridRevealSmoothstep(GUTTER_FROM, GUTTER_TO, frame.split)
    walkGridReveal(
        root,
        GridRevealPatch(
            0.0,
            0.0,
            size.width.toDouble(),
            size.height.toDouble(),
            root.red,
            root.green,
            root.blue,
            root.tone,
        ),
        frame,
        soft,
    )

    if (image == null || frame.fade <= 0) return
    val photo =
        if (hasColours) {
            gridRevealSmoothstep(PHOTO_FROM, 1.0, frame.split) * frame.fade
        } else {
            frame.fade
        }
    if (photo <= 0.002) return

    // Cropped to fill rather than letterboxed, so the picture arrives where the averages
    // said it would be.
    val scale = maxOf(size.width / image.width, size.height / image.height)
    val width = (image.width * scale).roundToInt()
    val height = (image.height * scale).roundToInt()
    drawImage(
        image = image,
        dstOffset = IntOffset(((size.width - width) / 2).roundToInt(), ((size.height - height) / 2).roundToInt()),
        dstSize = IntSize(width, height),
        alpha = photo.toFloat(),
    )
}

/** Walks the tree, painting the leaves that have not come apart yet. */
private fun DrawScope.walkGridReveal(
    cell: GridRevealCell,
    patch: GridRevealPatch,
    frame: GridRevealFrame,
    soft: Double,
) {
    val first = cell.first
    val second = cell.second
    if (first == null || second == null || frame.split < cell.splitAt) {
        paintGridRevealPatch(patch, frame, soft)
        return
    }

    // The children start on their parent's rectangle and slide into their own, so a split
    // looks like one thing coming apart rather than two things appearing.
    val progress = ((frame.split - cell.splitAt) / GRID_REVEAL_MORPH).coerceIn(0.0, 1.0)
    val t = 1 - (1 - progress) * (1 - progress) * (1 - progress)
    walkGridReveal(first, patch.blended(first, t, size), frame, soft)
    walkGridReveal(second, patch.blended(second, t, size), frame, soft)
}

/** Paints one cell, inset by however open the gutters are. */
private fun DrawScope.paintGridRevealPatch(
    patch: GridRevealPatch,
    frame: GridRevealFrame,
    soft: Double,
) {
    // Whole pixels, so two neighbouring cells stay flush and no seam shows between them.
    val x = patch.left.roundToInt().toFloat()
    val y = patch.top.roundToInt().toFloat()
    val width = (patch.left + patch.width).roundToInt().toFloat() - x
    val height = (patch.top + patch.height).roundToInt().toFloat() - y

    val gutter = (soft * 2).toFloat()
    // Only interior edges are inset, so the outer silhouette stays the frame.
    val left = if (x <= 0f) 0f else gutter
    val top = if (y <= 0f) 0f else gutter
    val innerWidth = width - left - if (x + width >= size.width) 0f else gutter
    val innerHeight = height - top - if (y + height >= size.height) 0f else gutter
    if (innerWidth <= 0f || innerHeight <= 0f) return

    drawRoundRect(
        color =
            gridRevealShade(
                grey = gridRevealGrey(patch.tone, frame.dark, frame.clock),
                red = patch.red,
                green = patch.green,
                blue = patch.blue,
                tint = frame.tint,
            ),
        topLeft = Offset(x + left, y + top),
        size = Size(innerWidth, innerHeight),
        cornerRadius = CornerRadius(minOf(innerWidth, innerHeight) * 0.12f * soft.toFloat()),
    )
}

/**
 * Mixes a cell's placeholder grey toward the colour of what is underneath it.
 *
 * @param grey the placeholder level, `0..255`.
 * @param red the cell's average red, `0..255`.
 * @param green its average green.
 * @param blue its average blue.
 * @param tint how far the colours have arrived, in `0..1`.
 * @param dim a factor applied afterwards, which is how the ground is darkened.
 * @return the colour to fill with.
 */
@Suppress("LongParameterList")
internal fun gridRevealShade(
    grey: Double,
    red: Double,
    green: Double,
    blue: Double,
    tint: Double,
    dim: Double = 1.0,
): Color =
    Color(
        red = (((grey + (red - grey) * tint) / 255) * dim).toFloat().coerceIn(0f, 1f),
        green = (((grey + (green - grey) * tint) / 255) * dim).toFloat().coerceIn(0f, 1f),
        blue = (((grey + (blue - grey) * tint) / 255) * dim).toFloat().coerceIn(0f, 1f),
    )
