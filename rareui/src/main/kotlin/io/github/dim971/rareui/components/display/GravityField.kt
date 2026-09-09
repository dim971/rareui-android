/*
 * GravityField.kt
 * The simulation behind GravityLetters, ported from the `retarget`, `rebuild` and `step`
 * functions in upstream's `components/ui/gravity-letters.tsx`.
 *
 * A glyph does not fall and then discover where it lands. It is told where it will land the
 * moment it is dropped, and it falls under gravity toward that place while its horizontal
 * position and its rotation are interpolated along the way. That is why the pile is stable:
 * the landing spot was reserved in the height map at the start, so nothing can arrive
 * somewhere another glyph has already taken.
 */

package io.github.dim971.rareui.components.display

import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

/** One falling or fallen glyph. */
internal class GravityBody(
    val id: Int,
    /** What is drawn. */
    val glyph: String,
    /** How big it is drawn. */
    val fontSize: Double,
    /** The glyph's own size, before it is rotated. */
    val naturalWidth: Double,
    val naturalHeight: Double,
) {
    /** Its size once rotated, which is what the height map deals in. */
    var width: Double = naturalWidth
    var height: Double = naturalHeight

    /** The offset between the two, so the drawing stays centred in its rotated box. */
    var offsetX: Double = 0.0
    var offsetY: Double = 0.0

    /** Where the current fall started. */
    var startX: Double = 0.0
    var startY: Double = 0.0

    /** Where it is. */
    var x: Double = 0.0
    var y: Double = 0.0

    /** Where it is going. */
    var targetX: Double = 0.0
    var targetY: Double = 0.0

    var velocityY: Double = 0.0

    /** The rotation it is tumbling through, before it blends into its resting angle. */
    var spin: Double = 0.0
    var spinRate: Double = 0.0

    /** What is actually drawn. */
    var rotation: Double = 0.0

    /** The angle it will come to rest at, taken from the slope it lands on. */
    var restRotation: Double = 0.0

    /** How far it drifts sideways on the way down, peaking halfway. */
    var sway: Double = 0.0

    var hasBounced: Boolean = false
    var isResting: Boolean = false
}

/** The pile, and everything falling into it. */
internal class GravityField(
    width: Double,
    height: Double,
    private val random: Random = Random,
) {
    private var containerWidth = width
    private var containerHeight = height
    private var map = GravityHeightMap(width, height)
    private var nextId = 0

    private val store = mutableListOf<GravityBody>()

    /** Everything in the field, oldest first. */
    val bodies: List<GravityBody> get() = store

    /** Which way the pile is leaning, from the device being tilted. Zero when it is level. */
    var wind: Double = 0.0

    /** Whether anything is still moving, which is what decides if the clock needs to run. */
    val isSettled: Boolean get() = store.all { it.isResting }

    /** How many glyphs there are. */
    val count: Int get() = store.size

    /**
     * Drops a glyph in.
     *
     * @param glyph what to draw.
     * @param fontSize how big to draw it.
     * @param glyphWidth the glyph's natural width at that size.
     * @param glyphHeight its natural height.
     * @param x where to drop it from, horizontally.
     * @param limit the most glyphs to keep. The oldest are dropped past it.
     */
    @Suppress("LongParameterList")
    fun drop(
        glyph: String,
        fontSize: Double,
        glyphWidth: Double,
        glyphHeight: Double,
        x: Double,
        limit: Int,
    ) {
        val body =
            GravityBody(
                id = nextId,
                glyph = glyph,
                fontSize = fontSize,
                naturalWidth = glyphWidth,
                naturalHeight = glyphHeight,
            )
        nextId++

        // A squarer glyph tumbles faster than a tall thin one, which is upstream's
        // squareness factor: it is the difference between a dropped O and a dropped I.
        val squareness = minOf(1.0, glyphHeight / maxOf(1.0, glyphWidth))
        body.spinRate = random.nextDouble(-1.0, 1.0) * (50 + 130 * squareness)
        body.startX = x
        body.x = x
        body.targetX = x

        retarget(body)
        // Dropping it just clear of whatever it is going to land on, rather than always
        // from the top, so a deep pile does not make every new glyph fall further.
        body.y = minOf(body.y, body.targetY - 24)
        body.startY = body.y
        store += body

        if (store.size > limit) {
            repeat(store.size - limit) { store.removeAt(0) }
            rebuild()
        }
    }

    /**
     * Steps every falling glyph forward.
     *
     * @param elapsed how long has passed, in seconds.
     * @param gravity the acceleration, in points per second squared.
     */
    fun step(
        elapsed: Double,
        gravity: Double,
    ) {
        store.forEach { if (!it.isResting) advance(it, elapsed, gravity) }
    }

    /**
     * Rebuilds the pile from scratch, which is what a resize or an avalanche needs.
     *
     * @param sliding whether resting glyphs should look for somewhere lower to go.
     */
    fun rebuild(sliding: Boolean = false) {
        map = GravityHeightMap(containerWidth, containerHeight)

        // Deepest first, so the bottom of the pile is rebuilt before anything resting on it.
        store.sortedByDescending { it.y }.forEach { body ->
            body.spin = body.rotation

            if (!body.isResting) {
                retarget(body)
                return@forEach
            }

            val rest = map.restY(body.x, body.width, body.height, body.restRotation)
            var falls = body.y < rest - 1

            if (!falls && sliding) {
                val probe =
                    map.restX(
                        x = body.x,
                        width = body.naturalWidth,
                        glyphHeight = body.naturalHeight,
                        maxX = maxOf(containerWidth - body.naturalWidth, 0.0),
                        bias = if (wind == 0.0) 1.0 else wind,
                        eager = GRAVITY_EAGER_SLOPE,
                    )
                val candidateX =
                    (probe - body.offsetX).coerceIn(0.0, maxOf(containerWidth - body.width, 0.0))
                val candidateY =
                    map.restY(candidateX, body.width, body.height, body.restRotation)
                falls = candidateY > body.y + 2
                if (falls) body.spinRate = random.nextDouble(-40.0, 40.0)
            }

            if (falls) {
                body.velocityY = 0.0
                retarget(body)
            } else {
                map.deposit(body.x, body.width, body.height, body.restRotation, body.y)
            }
        }
    }

    /**
     * Notes a new container size and settles the pile into it.
     *
     * @param width the new width.
     * @param height the new height.
     */
    fun resize(
        width: Double,
        height: Double,
    ) {
        if (width <= 0 || height <= 0) return
        if (width == containerWidth && height == containerHeight) return
        containerWidth = width
        containerHeight = height
        rebuild()
    }

    /** Empties the field. */
    fun clear() {
        store.clear()
        map = GravityHeightMap(containerWidth, containerHeight)
    }

    /** Works out where a glyph is going and reserves the space for it. */
    private fun retarget(body: GravityBody) {
        // A tilted device decides which way ties break; a level one tosses a coin.
        val bias =
            if (wind != 0.0) {
                wind
            } else if (random.nextBoolean()) {
                -1.0
            } else {
                1.0
            }
        val seekX =
            map.restX(
                x = body.x,
                width = body.naturalWidth,
                glyphHeight = body.naturalHeight,
                maxX = maxOf(containerWidth - body.naturalWidth, 0.0),
                bias = bias,
                eager = if (wind != 0.0) GRAVITY_EAGER_SLOPE else 1.0,
            )

        val squareness = minOf(1.0, body.naturalHeight / maxOf(1.0, body.naturalWidth))
        val jitter = random.nextDouble(-1.0, 1.0) * (2 + 8 * squareness)
        val restRotation =
            (map.groundTilt(seekX, body.naturalWidth) + jitter)
                .coerceIn(-GRAVITY_MAX_TILT, GRAVITY_MAX_TILT)

        // A rotated glyph needs a bigger box, and the height map only deals in boxes.
        val radians = abs(restRotation) * PI / 180
        body.restRotation = restRotation
        body.width = body.naturalWidth * cos(radians) + body.naturalHeight * sin(radians)
        body.height = body.naturalWidth * sin(radians) + body.naturalHeight * cos(radians)
        body.offsetX = (body.width - body.naturalWidth) / 2
        body.offsetY = (body.height - body.naturalHeight) / 2

        val targetX =
            (seekX - body.offsetX).coerceIn(0.0, maxOf(containerWidth - body.width, 0.0))
        val targetY = map.restY(targetX, body.width, body.height, restRotation)
        map.deposit(targetX, body.width, body.height, restRotation, targetY)

        body.startX = body.x
        body.startY = minOf(body.y, targetY)
        body.targetX = targetX
        body.targetY = targetY
        // The further it falls the more it wanders, up to a point.
        body.sway = random.nextDouble(-1.0, 1.0) * minOf(12.0, (targetY - body.startY) * 0.05)
        body.hasBounced = false
        body.isResting = false
    }

    private fun advance(
        body: GravityBody,
        elapsed: Double,
        gravity: Double,
    ) {
        body.velocityY += gravity * elapsed
        body.y += body.velocityY * elapsed
        body.spin += body.spinRate * elapsed

        val total = body.targetY - body.startY
        val progress = if (total > 0) minOf((body.y - body.startY) / total, 1.0) else 1.0

        // The horizontal move eases out while the fall accelerates, so a glyph arrives over
        // its landing spot well before it reaches it rather than sliding in sideways at the
        // last moment.
        body.x =
            body.startX + (body.targetX - body.startX) * progress * (2 - progress) +
            sin(progress * PI) * body.sway

        // The tumble gives way to the resting angle late, so the glyph is still turning most
        // of the way down and then settles quickly.
        val blend = progress * progress * progress
        body.rotation = body.spin * (1 - blend) + body.restRotation * blend

        if (body.y < body.targetY) return

        body.x = body.targetX
        body.y = body.targetY
        body.rotation = body.restRotation

        val rebound = body.velocityY * GRAVITY_BOUNCE
        // One bounce only, and only if it would be worth seeing. Upstream compares the
        // rebound's square against the gravity so the test scales with the setting.
        if (!body.hasBounced && rebound * rebound > gravity * 6) {
            body.hasBounced = true
            body.velocityY = -rebound
            body.startX = body.targetX
            body.startY = body.targetY - rebound * rebound / (2 * maxOf(gravity, 1.0))
            body.sway = 0.0
            body.spinRate *= 0.4
            return
        }

        body.velocityY = 0.0
        body.spinRate = 0.0
        body.isResting = true
    }
}
