/*
 * GravityLetters.kt
 * A port of upstream's `components/ui/gravity-letters.tsx`.
 *
 * Letters that fall out of your finger and pile up. Touching drops one, holding pours them,
 * and tilting the device makes the heap slide.
 */

package io.github.dim971.rareui.components.display

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.github.dim971.rareui.theme.RareUiTheme
import io.github.dim971.rareui.theme.rememberRareUiReduceMotion
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.random.Random

/** Which glyphs fall. */
public enum class GravityGlyphs {
    /** Capital letters. */
    LETTERS,

    /** Digits. */
    NUMBERS,

    /** Both. */
    BOTH,
    ;

    internal val pool: List<String>
        get() =
            when (this) {
                LETTERS -> ('A'..'Z').map { it.toString() }
                NUMBERS -> ('0'..'9').map { it.toString() }
                BOTH -> ('A'..'Z').map { it.toString() } + ('0'..'9').map { it.toString() }
            }
}

/** How long to hold before the pour starts. */
private const val HOLD_DELAY_MILLIS = 300L

/** How often it pours while held. */
private const val POUR_INTERVAL_MILLIS = 120L

/** How far a pour scatters either side of the finger, in points. */
private const val POUR_SCATTER = 8.0

/** How far the device has to lean before the pile slides, in degrees. */
private const val TILT_THRESHOLD = 10.0

/** How long the pile is left alone after an avalanche, so a wobble does not shake it apart. */
private const val AVALANCHE_REST_MILLIS = 350L

/** A frame that arrives late is capped rather than fired through the floor in one step. */
private const val LONGEST_FRAME = 1.0 / 30

/**
 * A container that letters fall into and pile up in.
 *
 * ```kotlin
 * GravityLetters(modifier = Modifier.fillMaxWidth().height(320.dp))
 * ```
 *
 * Touching it drops a glyph where you touched. Holding for a third of a second starts
 * pouring them, and dragging steers the pour. Tilting the device past ten degrees makes the
 * pile slide the way it is leaning.
 *
 * With animations turned off on the device the glyphs appear where they would have landed
 * rather than falling into place.
 *
 * @param modifier the modifier to apply. Give it a size: the pile fills whatever it is given.
 * @param glyphs which characters fall. Ignored when [items] is given.
 * @param items your own strings to drop instead of letters.
 * @param gravity the acceleration, in points per second squared.
 * @param size the glyphs' nominal size. Each one varies around it.
 * @param color the ink. Defaults to the theme's foreground.
 * @param maxGlyphs the most to keep before the oldest are forgotten.
 * @param deviceTilt whether tilting the device makes the pile slide.
 */
@Composable
@Suppress("LongParameterList")
public fun GravityLetters(
    modifier: Modifier = Modifier,
    glyphs: GravityGlyphs = GravityGlyphs.LETTERS,
    items: List<String>? = null,
    gravity: Double = 800.0,
    size: Dp = 28.dp,
    color: Color = RareUiTheme.colors.foreground,
    maxGlyphs: Int = 200,
    deviceTilt: Boolean = true,
) {
    val density = LocalDensity.current
    val measurer = rememberTextMeasurer()
    val reduceMotion = rememberRareUiReduceMotion()
    val scope = rememberCoroutineScope()

    val field = remember { GravityField(0.0, 0.0) }
    val measurements = remember { mutableMapOf<String, Pair<Double, Double>>() }
    // The field is a plain object so that stepping it does not invalidate a composition.
    // One counter published per frame is what the canvas redraws from.
    var frame by remember { mutableIntStateOf(0) }
    var lean by remember { mutableIntStateOf(0) }

    val pool = items ?: glyphs.pool

    fun measure(
        glyph: String,
        fontSize: Double,
    ): Pair<Double, Double> =
        measurements.getOrPut("$glyph@${fontSize.toInt()}") {
            val measured =
                measurer.measure(
                    text = glyph,
                    style = TextStyle(fontSize = with(density) { fontSize.toFloat().toSp() }),
                )
            measured.size.width.toDouble() to measured.size.height.toDouble()
        }

    fun drop(at: Double) {
        val glyph = pool.randomElement()
        // Each glyph is a little bigger or smaller than the nominal size, which is what
        // stops a pile of the same letter looking like a printed line.
        val fontSize = with(density) { size.toPx() } * Random.nextDouble(0.8, 1.2)
        val (width, height) = measure(glyph, fontSize)

        field.drop(
            glyph = glyph,
            fontSize = fontSize,
            glyphWidth = width,
            glyphHeight = height,
            x = at - width / 2,
            limit = maxGlyphs,
        )

        if (reduceMotion) {
            // Nothing falls, so the pile is simply built. Stepping it once at a large
            // interval lands everything on the spot it was already assigned.
            field.step(10.0, gravity)
        }
        frame++
    }

    LaunchedEffect(gravity, reduceMotion) {
        if (reduceMotion) return@LaunchedEffect
        var last = 0L
        while (isActive) {
            val now = withFrameNanos { it }
            val elapsed = if (last == 0L) 0.0 else minOf((now - last) / 1e9, LONGEST_FRAME)
            last = now
            if (!field.isSettled) {
                field.step(elapsed, gravity)
                frame++
            } else {
                // Nothing is moving, so the clock is allowed to stop until something is.
                last = 0L
            }
        }
    }

    TiltWatcher(enabled = deviceTilt && !reduceMotion) { degrees -> lean = degrees.toInt() }

    LaunchedEffect(lean) {
        if (abs(lean) <= TILT_THRESHOLD) {
            field.wind = 0.0
            return@LaunchedEffect
        }
        field.wind = if (lean < 0) -1.0 else 1.0
        field.rebuild(sliding = true)
        frame++
        // Upstream will not start another avalanche for a third of a second, so a wobbling
        // device does not shake the pile apart.
        delay(AVALANCHE_REST_MILLIS)
    }

    Box(
        modifier =
            modifier
                .onSizeChanged { field.resize(it.width.toDouble(), it.height.toDouble()) }
                .pointerInput(pool, gravity, size) {
                    awaitEachGesture {
                        val down = awaitFirstDown()
                        drop(down.position.x.toDouble())
                        // Where the next copy lands, which a drag steers.
                        var target = down.position.x.toDouble()

                        val pour =
                            scope.launch {
                                delay(HOLD_DELAY_MILLIS)
                                while (true) {
                                    // A little scatter either side, so a held pour makes a
                                    // heap rather than a tower.
                                    drop(target + Random.nextDouble(-POUR_SCATTER, POUR_SCATTER))
                                    delay(POUR_INTERVAL_MILLIS)
                                }
                            }

                        try {
                            while (true) {
                                val event = awaitPointerEvent()
                                event.changes.firstOrNull()?.let { target = it.position.x.toDouble() }
                                if (event.changes.none { it.pressed }) break
                            }
                        } finally {
                            pour.cancel()
                        }
                    }
                }.semantics {
                    contentDescription = "Falling letters, ${field.count} on the pile"
                },
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            // Read so the canvas is invalidated once a frame while anything is moving.
            @Suppress("UNUSED_EXPRESSION")
            frame

            field.bodies.forEach { body ->
                paintGravityBody(body, measurer, color)
            }
        }
    }
}

/** Draws one glyph where the simulation has put it. */
private fun DrawScope.paintGravityBody(
    body: GravityBody,
    measurer: TextMeasurer,
    color: Color,
) {
    val left = (body.x + body.offsetX).toFloat()
    val top = (body.y + body.offsetY).toFloat()
    val centre = Offset(left + body.naturalWidth.toFloat() / 2, top + body.naturalHeight.toFloat() / 2)

    rotate(degrees = body.rotation.toFloat(), pivot = centre) {
        drawText(
            textMeasurer = measurer,
            text = body.glyph,
            topLeft = Offset(left, top),
            style =
                TextStyle(
                    fontSize = body.fontSize.toFloat().toSp(),
                    fontWeight = FontWeight.SemiBold,
                    color = color,
                ),
        )
    }
}

/** Picks one at random, or a fallback for an empty pool. */
private fun List<String>.randomElement(): String = randomOrNull() ?: "A"

/**
 * Reports how far the device is leaning, in degrees, positive when it is tipped right.
 *
 * The sensor reports which way is up rather than which way is down, so the sign is turned
 * over: tipping the right edge down puts a negative number on the x axis, and the pile
 * should slide right.
 *
 * @param enabled whether to listen at all.
 * @param onLean called with the lean, about ten times a second.
 */
@Composable
private fun TiltWatcher(
    enabled: Boolean,
    onLean: (Double) -> Unit,
) {
    val context = LocalContext.current

    DisposableEffect(enabled, context) {
        if (!enabled) return@DisposableEffect onDispose {}

        val manager = context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
        val sensor = manager?.getDefaultSensor(Sensor.TYPE_GRAVITY)
        if (manager == null || sensor == null) return@DisposableEffect onDispose {}

        val listener =
            object : SensorEventListener {
                override fun onSensorChanged(event: SensorEvent) {
                    onLean(-event.values[0] / SensorManager.GRAVITY_EARTH * 90.0)
                }

                override fun onAccuracyChanged(
                    sensor: Sensor?,
                    accuracy: Int,
                ) = Unit
            }

        manager.registerListener(listener, sensor, 100_000)
        onDispose { manager.unregisterListener(listener) }
    }
}
