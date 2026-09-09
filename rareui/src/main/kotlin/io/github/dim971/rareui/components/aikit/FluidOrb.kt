/*
 * FluidOrb.kt
 * A port of upstream's `components/ui/fluid-orb.tsx`.
 *
 * A circle filled with something that looks like it is being stirred. Upstream renders it
 * with a WebGL fragment shader; from Android 13 this renders the same shader as AGSL, so
 * that path is the closest thing in the library to a byte for byte port. See
 * `res/raw/fluid_orb.agsl`.
 *
 * Below Android 13 there is no runtime shader to compile, so the orb falls back to a
 * drifting gradient: the same object in motion, arrived at a different way. Recorded in
 * docs/fidelity.md.
 */

package io.github.dim971.rareui.components.aikit

import android.graphics.RuntimeShader
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.hideFromAccessibility
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.github.dim971.rareui.R
import io.github.dim971.rareui.theme.rememberRareUiReduceMotion
import kotlin.math.cos
import kotlin.math.sin

/** Upstream's own blue, which the fluid settles to at its darkest. */
private val FluidOrbBlue = Color(0xFF1A73F2)

/** How fast the drift runs, from upstream's `time * 0.22`. */
private const val FLUID_ORB_PACE = 0.22

/** A runtime shader needs Android 13. */
private val PLATFORM_SHADES = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU

/**
 * A circle of slowly drifting colour.
 *
 * ```kotlin
 * FluidOrb()
 * FluidOrb(size = 160.dp, color = Color.Magenta)
 * ```
 *
 * With animations turned off on the device it renders a single still frame, taken at the
 * moment the shader would have started, which is what upstream does under
 * `prefers-reduced-motion`.
 *
 * @param modifier the modifier to apply.
 * @param size the orb's diameter.
 * @param color the colour the fluid settles to at its darkest.
 */
@Composable
public fun FluidOrb(
    modifier: Modifier = Modifier,
    size: Dp = 240.dp,
    color: Color = FluidOrbBlue,
) {
    val context = LocalContext.current
    val reduceMotion = rememberRareUiReduceMotion()

    val shader =
        remember(PLATFORM_SHADES) {
            if (!PLATFORM_SHADES) {
                null
            } else {
                runCatching {
                    RuntimeShader(
                        context.resources
                            .openRawResource(R.raw.fluid_orb)
                            .bufferedReader()
                            .use { it.readText() },
                    )
                }.getOrNull()
            }
        }

    var time by remember { mutableFloatStateOf(0f) }

    LaunchedEffect(reduceMotion) {
        if (reduceMotion) return@LaunchedEffect
        var started = 0L
        while (true) {
            val now = withFrameNanos { it }
            if (started == 0L) started = now
            time = ((now - started) / 1e9).toFloat()
        }
    }

    Canvas(
        modifier =
            modifier
                .size(size)
                .semantics { hideFromAccessibility() },
    ) {
        if (shader != null) {
            paintFluidOrb(shader, time, color)
        } else {
            paintFluidOrbFallback(time, color)
        }
    }
}

/** Draws the orb with upstream's own shader. */
@RequiresApi(Build.VERSION_CODES.TIRAMISU)
private fun DrawScope.paintFluidOrb(
    shader: RuntimeShader,
    time: Float,
    color: Color,
) {
    shader.setFloatUniform("size", size.width, size.height)
    shader.setFloatUniform("time", time)
    shader.setColorUniform("tint", color.toArgb())
    drawRect(brush = ShaderBrush(shader))
}

/**
 * Draws the orb without one.
 *
 * The shader's own drift is reused, so the fallback moves on the same clock and with the
 * same period as the real thing: a bright pool wandering across a body that darkens toward
 * the bottom. It is not the same picture and it is not meant to be. It is the same object
 * in motion.
 */
private fun DrawScope.paintFluidOrbFallback(
    time: Float,
    color: Color,
) {
    val t = time * FLUID_ORB_PACE
    val drift =
        Offset(
            (sin(t) + 0.6 * sin(t * 1.7 + 1.3)).toFloat(),
            (cos(t * 0.8) + 0.6 * cos(t * 1.3 + 2.1)).toFloat(),
        )
    val radius = size.minDimension / 2
    val centre = Offset(size.width / 2, size.height / 2)
    val white = Color(0xFFFCFFFF)
    val light =
        Color(
            red = (white.red + color.red) / 2,
            green = (white.green + color.green) / 2,
            blue = (white.blue + color.blue) / 2,
        )

    drawCircle(
        brush =
            Brush.verticalGradient(
                0f to white,
                0.4f to light,
                1f to color,
                startY = 0f,
                endY = size.height,
            ),
        radius = radius,
        center = centre,
    )
    drawCircle(
        brush =
            Brush.radialGradient(
                colors = listOf(white.copy(alpha = 0.85f), white.copy(alpha = 0f)),
                center = centre + Offset(drift.x * radius * 0.28f, drift.y * radius * 0.22f),
                radius = radius * 0.7f,
            ),
        radius = radius,
        center = centre,
    )
}
