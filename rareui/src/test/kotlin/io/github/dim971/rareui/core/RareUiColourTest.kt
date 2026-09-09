/*
 * RareUiColourTest.kt
 * The colour conversions are the one piece of the theme that can be wrong quietly: a hue
 * that drifts by a degree is invisible on its own and obvious once CodeBlock has built
 * twelve tones out of it. The same assertions run on iOS.
 */

package io.github.dim971.rareui.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs

class RareUiColourTest {
    @Test
    fun `the six digit form parses, with or without its hash`() {
        val withHash = rareUiHex("#F4F4F9")
        val without = rareUiHex("F4F4F9")
        assertNotNull(withHash)
        assertEquals(withHash, without)
        assertEquals(244.0 / 255, withHash!!.red, 1e-12)
        assertEquals(244.0 / 255, withHash.green, 1e-12)
        assertEquals(249.0 / 255, withHash.blue, 1e-12)
        assertEquals(1.0, withHash.alpha, 0.0)
    }

    @Test
    fun `the three digit form repeats each digit, so f80 is ff8800`() {
        assertEquals(rareUiHex("#ff8800"), rareUiHex("#f80"))
    }

    @Test
    fun `the eight digit form carries alpha`() {
        val colour = rareUiHex("#FFFFFF14")
        assertNotNull(colour)
        assertEquals(1.0, colour!!.red, 0.0)
        assertEquals(20.0 / 255, colour.alpha, 1e-12)
    }

    @Test
    fun `a string that is not a colour parses as nothing rather than as black`() {
        assertNull(rareUiHex("#GGGGGG"))
        assertNull(rareUiHex("#FFFFF"))
        assertNull(rareUiHex(""))
        assertNull(rareUiHex("rebeccapurple"))
    }

    @Test
    fun `the primaries land on the hues they are named after`() {
        assertEquals(0.0, rareUiHex("#FF0000")!!.toHsl().hue, 1e-9)
        assertEquals(100.0, rareUiHex("#FF0000")!!.toHsl().saturation, 1e-9)
        assertEquals(50.0, rareUiHex("#FF0000")!!.toHsl().lightness, 1e-9)
        assertEquals(120.0, rareUiHex("#00FF00")!!.toHsl().hue, 1e-9)
        assertEquals(240.0, rareUiHex("#0000FF")!!.toHsl().hue, 1e-9)
    }

    @Test
    fun `a grey has no hue and no saturation to speak of`() {
        val grey = rareUiHex("#808080")!!.toHsl()
        assertEquals(0.0, grey.saturation, 1e-9)
        assertEquals(0.0, grey.hue, 0.0)
    }

    @Test
    fun `a colour survives the round trip through hue, saturation and lightness`() {
        for (hex in listOf("#F75001", "#50B1FD", "#39D353", "#262626", "#FAFAFA")) {
            val original = rareUiHex(hex)!!
            val returned = original.toHsl().toRgba()
            assertTrue(
                "$hex drifted",
                abs(original.red - returned.red) < 1e-9 &&
                    abs(original.green - returned.green) < 1e-9 &&
                    abs(original.blue - returned.blue) < 1e-9,
            )
        }
    }

    @Test
    fun `replacing the lightness keeps the hue and the saturation`() {
        val accent = rareUiHex("#F75001")!!.toHsl()
        val paler = accent.withLightness(80.0)
        assertEquals(accent.hue, paler.hue, 0.0)
        assertEquals(accent.saturation, paler.saturation, 0.0)
        assertEquals(80.0, paler.lightness, 0.0)
    }

    @Test
    fun `a lightness outside its range is brought back in rather than wrapping`() {
        val accent = rareUiHex("#F75001")!!.toHsl()
        assertEquals(100.0, accent.withLightness(140.0).lightness, 0.0)
        assertEquals(0.0, accent.withLightness(-20.0).lightness, 0.0)
    }
}
