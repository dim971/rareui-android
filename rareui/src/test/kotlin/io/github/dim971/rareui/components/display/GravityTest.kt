/*
 * GravityTest.kt
 * The height map is the whole model, so the height map is what is tested. Checked against
 * the `spanOf`, `restY`, `deposit`, `windowTop`, `groundTilt` and `findRestX` functions in
 * `components/ui/gravity-letters.tsx`. The same assertions run on iOS.
 */

package io.github.dim971.rareui.components.display

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class GravityHeightMapTest {
    private fun emptyMap() = GravityHeightMap(320.0, 200.0)

    @Test
    fun `a glyph covers the columns it overlaps and no others`() {
        // Columns are eight points wide, so a glyph from ten to thirty covers one to three.
        val bounds = emptyMap().span(10.0, 20.0)
        assertEquals(1, bounds.from)
        assertEquals(3, bounds.to)
    }

    @Test
    fun `a glyph at the very edge is clamped into the map rather than running off it`() {
        val map = emptyMap()
        assertEquals(0, map.span(-50.0, 20.0).from)
        assertEquals(map.heights.size - 1, map.span(1000.0, 20.0).to)
    }

    @Test
    fun `on an empty floor a glyph rests on the bottom`() {
        // Two hundred tall, a thirty point glyph, so its top edge sits at one seventy.
        assertEquals(170.0, emptyMap().restY(40.0, 20.0, 30.0, 0.0), 1e-9)
    }

    @Test
    fun `a glyph landing on another rests on top of it`() {
        val map = emptyMap()
        map.deposit(40.0, 20.0, 30.0, 0.0, 170.0)
        assertEquals(
            "the second should sit exactly on the first",
            140.0,
            map.restY(40.0, 20.0, 30.0, 0.0),
            1e-9,
        )
    }

    @Test
    fun `a glyph beside the pile is unaffected by it`() {
        val map = emptyMap()
        map.deposit(40.0, 20.0, 30.0, 0.0, 170.0)
        assertEquals(170.0, map.restY(120.0, 20.0, 30.0, 0.0), 1e-9)
    }

    @Test
    fun `beyond the walls the heap reads as infinitely high, so nothing slides out`() {
        val map = emptyMap()
        assertEquals(Double.POSITIVE_INFINITY, map.top(-3, 1), 0.0)
        assertEquals(Double.POSITIVE_INFINITY, map.top(0, map.heights.size), 0.0)
        assertTrue(map.top(0, 3).isFinite())
    }

    @Test
    fun `a glyph slides off a ridge and stops in a hollow`() {
        val map = emptyMap()
        // A tower on the left, nothing to the right.
        repeat(4) { map.deposit(0.0, 24.0, 30.0, 0.0, 0.0) }
        assertTrue(
            "it should have moved off the tower",
            map.restX(4.0, 24.0, 30.0, 296.0, 1.0) > 4.0,
        )
    }

    @Test
    fun `a glyph on level ground stays where it is`() {
        assertEquals(100.0, emptyMap().restX(100.0, 24.0, 30.0, 296.0, 1.0), 0.0)
    }

    @Test
    fun `sliding never leaves the container`() {
        val map = emptyMap()
        for (bias in listOf(-1.0, 1.0)) {
            val landed = map.restX(400.0, 24.0, 30.0, 296.0, bias)
            assertTrue(landed in 0.0..296.0)
        }
    }

    @Test
    fun `the ground's slope is read from the difference between the two halves`() {
        val map = emptyMap()
        // Building up the left half only, so the ground under a wide glyph slopes down to
        // the right and the glyph should lean that way.
        repeat(3) { map.deposit(0.0, 40.0, 30.0, 0.0, 0.0) }
        assertTrue("higher on the left reads as positive", map.groundTilt(0.0, 80.0) > 0)
        assertEquals("level ground has no slope", 0.0, map.groundTilt(200.0, 80.0), 0.0)
    }

    @Test
    fun `a leaning glyph touches down on one corner rather than along its base`() {
        val map = emptyMap()
        val level = map.restY(40.0, 30.0, 30.0, 0.0)
        val leaning = map.restY(40.0, 30.0, 30.0, 20.0)
        // Leaning puts one corner lower than the base would be, so the box sits higher.
        assertTrue(leaning > level)
    }

    @Test
    fun `an eager slide gives way sooner than a reluctant one`() {
        val map = emptyMap()
        repeat(2) { map.deposit(0.0, 24.0, 30.0, 0.0, 0.0) }
        val reluctant = map.restX(4.0, 24.0, 30.0, 296.0, 1.0, eager = 1.0)
        val eager = map.restX(4.0, 24.0, 30.0, 296.0, 1.0, eager = GRAVITY_EAGER_SLOPE)
        assertTrue(eager >= reluctant)
    }
}

class GravityFieldTest {
    private fun field() = GravityField(320.0, 200.0, Random(4))

    @Test
    fun `an empty field has settled`() {
        assertTrue(field().isSettled)
    }

    @Test
    fun `a dropped glyph is in motion until it lands`() {
        val field = field()
        field.drop("A", 28.0, 20.0, 28.0, 100.0, 50)
        assertFalse(field.isSettled)

        // Two seconds at sixty frames is far more than a two hundred point fall needs.
        repeat(120) { field.step(1.0 / 60, 800.0) }
        assertTrue(field.isSettled)
        assertEquals(1, field.bodies.size)
    }

    @Test
    fun `a glyph comes to rest inside the container`() {
        val field = field()
        val random = Random(9)
        repeat(12) { field.drop("M", 28.0, 24.0, 28.0, random.nextDouble(0.0, 290.0), 50) }
        repeat(240) { field.step(1.0 / 60, 800.0) }

        for (body in field.bodies) {
            assertTrue(body.x >= -1)
            assertTrue(body.x + body.width <= 321)
            assertTrue(body.y + body.height <= 201)
        }
    }

    @Test
    fun `the oldest glyphs are forgotten past the limit`() {
        val field = field()
        repeat(20) { field.drop("X", 20.0, 16.0, 20.0, 100.0, 5) }
        assertEquals(5, field.count)
        // And the five kept are the five most recent, which is what the identifiers say.
        assertEquals(listOf(15, 16, 17, 18, 19), field.bodies.map { it.id })
    }
}
