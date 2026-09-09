/*
 * MatrixOrb.kt
 * A port of upstream's `components/ui/matrix-orb.tsx`.
 *
 * A grid of dots clipped to a circle, each one's size driven by a field that depends on
 * what the orb is doing. Upstream paints it into a 2D canvas on a frame loop; this does the
 * same with Compose's Canvas driven by withFrameNanos, which is the same arrangement: one
 * draw call per frame, no composition to reconcile.
 */

package io.github.dim971.rareui.components.aikit

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.dim971.rareui.theme.RareUiTheme
import io.github.dim971.rareui.theme.rememberRareUiReduceMotion
import kotlin.math.exp
import kotlin.math.pow
import kotlin.math.sqrt

/**
 * A grid of dots that breathes, ripples or thinks.
 *
 * ```kotlin
 * MatrixOrb(state = MatrixOrbState.LISTENING)
 * MatrixOrb(state = MatrixOrbState.LISTENING, level = microphone.level)
 * ```
 *
 * Give it a [level] and the listening state follows it. Leave it out and it synthesises
 * one, so the orb has something to do while nothing is actually listening.
 *
 * With animations turned off on the device it paints a single still frame, which is what
 * upstream does when `prefers-reduced-motion` is set.
 *
 * @param modifier the modifier to apply.
 * @param state what the orb is doing.
 * @param level a level in `0..1`, such as a microphone's. Leave it out to have one synthesised.
 * @param size the orb's width and height.
 * @param color the dots' colour. Defaults to the theme's accent.
 * @param dots how many dots across the grid is. Never goes below three.
 * @param labels replacements for the status text under the orb.
 */
@Composable
public fun MatrixOrb(
    modifier: Modifier = Modifier,
    state: MatrixOrbState = MatrixOrbState.IDLE,
    level: Float? = null,
    size: Dp = 240.dp,
    color: Color = RareUiTheme.colors.accent,
    dots: Int = 11,
    labels: Map<MatrixOrbState, String> = emptyMap(),
) {
    val reduceMotion = rememberRareUiReduceMotion()
    val density = LocalDensity.current
    val displayScale = density.density

    val clock = remember { MatrixOrbClock() }
    var frame by remember {
        mutableStateOf(
            MatrixOrbFrame(
                time = 0.0,
                amplitude = level?.toDouble() ?: matrixOrbEnvelope(0.0),
                scale = matrixOrbScale(state),
                weights = MatrixOrbWeights.showing(state),
            ),
        )
    }

    LaunchedEffect(reduceMotion) {
        if (reduceMotion) return@LaunchedEffect
        // The loop retargets rather than restarting when the state changes, which is what
        // lets an interrupted change blend from whatever is on screen.
        while (true) {
            withFrameNanos { nanos -> frame = clock.advance(nanos, state, level?.toDouble()) }
        }
    }

    LaunchedEffect(state, level, reduceMotion) {
        clock.retarget(state, level?.toDouble())
        if (reduceMotion) {
            frame =
                MatrixOrbFrame(
                    time = 0.0,
                    amplitude = level?.toDouble() ?: matrixOrbEnvelope(0.0),
                    scale = matrixOrbScale(state),
                    weights = MatrixOrbWeights.showing(state),
                )
        }
    }

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Canvas(modifier = Modifier.size(size)) {
            paintMatrixOrb(
                frame = frame,
                dots = dots,
                color = color,
                displayScale = displayScale,
                width = this.size.width,
                height = this.size.height,
            ) { centre, radius ->
                drawCircle(color = color, radius = radius, center = centre)
            }
        }

        BasicText(
            text = labels[state] ?: matrixOrbLabel(state),
            style = TextStyle(fontSize = 14.sp, color = RareUiTheme.colors.foreground.copy(alpha = 0.7f)),
            modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
        )
    }
}

/** The default status text under the orb. */
internal fun matrixOrbLabel(state: MatrixOrbState): String =
    when (state) {
        MatrixOrbState.IDLE -> "Idle"
        MatrixOrbState.LISTENING -> "Listening"
        MatrixOrbState.THINKING -> "Thinking"
    }

/**
 * Walks the grid and hands each visible dot to [dot].
 *
 * Separated from the drawing so the geometry can be read on its own, and so a test can walk
 * the same grid without a canvas.
 */
internal inline fun paintMatrixOrb(
    frame: MatrixOrbFrame,
    dots: Int,
    color: Color,
    displayScale: Float,
    width: Float,
    height: Float,
    dot: (Offset, Float) -> Unit,
) {
    val grid = maxOf(3, dots)
    val half = (grid - 1) / 2.0
    val extent = minOf(width, height)
    val spacing = extent * 0.74 / (grid - 1)
    val maxRadius = spacing * 0.6
    val centreX = width / 2.0
    val centreY = height / 2.0

    for (row in 0 until grid) {
        for (column in 0 until grid) {
            val normalisedX = (column - half) / half
            val normalisedY = (row - half) / half
            val distance = sqrt(normalisedX * normalisedX + normalisedY * normalisedY)
            // 1.12 rather than the square's own 1.41 corner is what rounds the outline.
            if (distance > 1.12) continue

            var blended = 0.0
            MatrixOrbState.entries.forEach { candidate ->
                val weight = frame.weights[candidate]
                if (weight < 0.001) return@forEach
                blended += weight *
                    matrixOrbIntensity(
                        candidate,
                        distance,
                        normalisedX,
                        normalisedY,
                        frame.time,
                        frame.amplitude,
                    )
            }

            val intensity = minOf(1.0, maxOf(0.0, blended))
            val radius = maxRadius * exp(-distance * distance * 1.7) * intensity * frame.scale
            // Anything under half a device pixel comes out as haze rather than a dot.
            if (radius * displayScale < 0.5) continue

            dot(
                Offset(
                    (centreX + (column - half) * spacing * frame.scale).toFloat(),
                    (centreY + (row - half) * spacing * frame.scale).toFloat(),
                ),
                radius.toFloat(),
            )
        }
    }
}

/**
 * The orb's simulation, stepped once per frame.
 *
 * A plain class rather than anything observable: the frame loop is already asking for a new
 * frame, and an observation on top of that would ask for a second one.
 */
internal class MatrixOrbClock {
    private var time = 0.0
    private var amplitude = 0.0
    private var scale: Double? = null
    private var velocity = 0.0
    private var weights: MatrixOrbWeights? = null
    private var last: Long? = null

    /** Notes a change of state without stepping, so a paused loop still knows where it is going. */
    internal fun retarget(
        state: MatrixOrbState,
        level: Double?,
    ) {
        if (weights == null) weights = MatrixOrbWeights.showing(state)
        if (scale == null) scale = matrixOrbScale(state)
        if (amplitude == 0.0) amplitude = level ?: matrixOrbEnvelope(0.0)
    }

    /**
     * Steps the simulation up to a frame and reports what to paint.
     *
     * @param nanos the frame's timestamp.
     * @param state what the orb is doing, which the loop retargets toward rather than
     *   restarting for.
     * @param level the caller's level, if there is one.
     */
    internal fun advance(
        nanos: Long,
        state: MatrixOrbState,
        level: Double?,
    ): MatrixOrbFrame {
        var currentWeights = weights ?: MatrixOrbWeights.showing(state)
        var currentScale = scale ?: matrixOrbScale(state)

        // A frame that arrives late, because the app was in the background or the main
        // thread was busy, is capped rather than integrated in one enormous step.
        val elapsed = last?.let { minOf((nanos - it) / 1_000_000_000.0, 0.05) } ?: 0.0
        last = nanos
        time += elapsed

        val target = levelAt(level, time)
        // Rising follows quickly and falling follows slowly, so a peak is held for a moment
        // instead of dropping the instant the level does.
        val rate = if (target > amplitude) MATRIX_ORB_ATTACK else MATRIX_ORB_RELEASE
        amplitude += (target - amplitude) * (1 - (1 - rate).pow(elapsed * 60))

        currentWeights.blendToward(state, 1 - (1 - MATRIX_ORB_BLEND).pow(elapsed * 60))

        // The spring is integrated by hand rather than handed to Compose, because its
        // output feeds a draw call rather than a property.
        velocity += (
            -MATRIX_ORB_STIFFNESS * (currentScale - matrixOrbScale(state)) -
                MATRIX_ORB_DAMPING * velocity
        ) * elapsed
        currentScale += velocity * elapsed

        weights = currentWeights
        scale = currentScale
        return MatrixOrbFrame(time, amplitude, currentScale, currentWeights.copy())
    }

    /**
     * The level to follow: the caller's when there is a usable one, a synthesised one
     * otherwise. A level that is not a number would stick in the smoother forever.
     */
    private fun levelAt(
        given: Double?,
        at: Double,
    ): Double {
        if (given == null || !given.isFinite()) return matrixOrbEnvelope(at)
        return minOf(1.0, maxOf(0.0, given))
    }
}
