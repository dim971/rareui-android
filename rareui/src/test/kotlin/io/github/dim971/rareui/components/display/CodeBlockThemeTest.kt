/*
 * CodeBlockThemeTest.kt
 * The palette a whole syntax theme is derived from. Checked against the `buildTheme`
 * function in `components/ui/code-block.tsx`; the same assertions run on iOS.
 */

package io.github.dim971.rareui.components.display

import androidx.compose.ui.graphics.Color
import io.github.dim971.rareui.core.RareUiHsl
import io.github.dim971.rareui.core.toHsl
import io.github.dim971.rareui.core.toRareUiRgba
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CodeBlockThemeTest {
    private fun theme(
        lightness: Double,
        saturation: Double = 100.0,
        dark: Boolean = true,
    ) = CodeBlockTheme(RareUiHsl(20.0, saturation, lightness), dark)

    private fun lightness(colour: Color): Double = colour.toRareUiRgba().toHsl().lightness

    @Test
    fun `every token kind has a colour of its own`() {
        val theme = theme(50.0)
        for (kind in CodeTokenKind.entries) {
            assertNotNull("$kind has no colour", theme.tokens[kind])
        }
    }

    @Test
    fun `keywords are drawn in the accent itself`() {
        val theme = theme(50.0)
        assertEquals(theme.accent, theme.color(CodeTokenKind.KEYWORD))
    }

    @Test
    fun `an accent too dark or too pale is brought into a usable range`() {
        // Otherwise a nearly black accent makes keywords invisible on a dark panel, and a
        // nearly white one stops reading as a colour at all.
        val black = theme(2.0)
        val white = theme(99.0)
        assertTrue(black.accent != white.accent)
        assertTrue(lightness(black.accent) in 55.0..71.0)
        assertTrue(lightness(white.accent) in 55.0..71.0)
    }

    @Test
    fun `the light appearance is the same ramp upside down`() {
        val dark = theme(50.0, saturation = 60.0, dark = true)
        val light = theme(50.0, saturation = 60.0, dark = false)

        // A comment is pale on a dark panel and dark on a light one.
        assertTrue(
            lightness(dark.color(CodeTokenKind.COMMENT)) <
                lightness(light.color(CodeTokenKind.COMMENT)),
        )
        assertTrue(
            lightness(dark.color(CodeTokenKind.PROPERTY)) >
                lightness(light.color(CodeTokenKind.PROPERTY)),
        )
    }

    @Test
    fun `comments and attribute names are the italic ones, as upstream sets them`() {
        assertTrue(CodeTokenKind.COMMENT.isItalic)
        assertTrue(CodeTokenKind.ATTRIBUTE_NAME.isItalic)
        assertFalse(CodeTokenKind.KEYWORD.isItalic)
        assertFalse(CodeTokenKind.STRING.isItalic)
    }

    @Test
    fun `a colour with nothing to say falls back rather than producing a grey theme`() {
        // Upstream's own fallback is a strong blue, and an accent that cannot be read is
        // exactly where a derived palette would otherwise collapse.
        val fallback = codeBlockTheme(Color.Transparent, dark = true)
        assertEquals(
            211.0,
            fallback.accent
                .toRareUiRgba()
                .toHsl()
                .hue,
            1.0,
        )
    }
}
