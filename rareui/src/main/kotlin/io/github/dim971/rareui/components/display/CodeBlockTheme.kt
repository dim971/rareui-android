/*
 * CodeBlockTheme.kt
 * The palette CodeBlock builds out of a single colour, ported from the `buildTheme`
 * function in upstream's `components/ui/code-block.tsx`.
 *
 * This is the part of the component worth being exact about. Every colour in a syntax theme
 * is derived from one accent: its hue and saturation are kept, its lightness is replaced
 * with a value per token kind, and in a light appearance the whole ramp is flipped. Twelve
 * token kinds, one colour, no table of hand-picked shades to drift.
 */

package io.github.dim971.rareui.components.display

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import io.github.dim971.rareui.core.RareUiHsl
import io.github.dim971.rareui.core.toColor
import io.github.dim971.rareui.core.toHsl
import io.github.dim971.rareui.core.toRareUiRgba

/** A syntax theme, and the chrome around it, derived from one colour. */
@Immutable
public class CodeBlockTheme internal constructor(
    accentHsl: RareUiHsl,
    dark: Boolean,
) {
    /** The colour each token kind is drawn in. */
    public val tokens: Map<CodeTokenKind, Color>

    /** The accent, after it has been brought into a usable range. */
    public val accent: Color

    /** The panel behind the code. */
    public val background: Color

    /** The hairline around it and under its header. */
    public val border: Color

    /** The header's own slightly lifted ground. */
    public val headerBackground: Color

    /** The filename and the language, which are quieter than the code. */
    public val muted: Color

    /** The line numbers, quieter still. */
    public val gutter: Color

    /** The wash behind a highlighted line. */
    public val lineWash: Color

    init {
        val hue = accentHsl.hue
        val saturation = accentHsl.saturation

        // The accent, kept where it is legible: not so dark it disappears into the panel,
        // not so pale it stops reading as a colour.
        val usable =
            if (dark) {
                accentHsl.lightness.coerceIn(56.0, 70.0)
            } else {
                accentHsl.lightness.coerceIn(38.0, 50.0)
            }
        val accentTone = RareUiHsl(hue, saturation, usable).toColor()

        // A light appearance is the same ramp upside down, which is the whole of what
        // upstream does to invert the theme.
        fun ramp(lightness: Double) = if (dark) lightness else 100 - lightness

        fun tint(
            lightness: Double,
            tone: Double = saturation,
        ) = RareUiHsl(hue, tone, lightness).toColor()

        tokens =
            mapOf(
                CodeTokenKind.PLAIN to if (dark) Color.White else Color(0xFF171717),
                CodeTokenKind.COMMENT to tint(ramp(42.0), saturation * 0.35),
                CodeTokenKind.PUNCTUATION to tint(ramp(62.0), saturation * 0.3),
                CodeTokenKind.OPERATOR to tint(ramp(70.0), saturation * 0.4),
                CodeTokenKind.KEYWORD to accentTone,
                CodeTokenKind.STRING to tint(ramp(76.0)),
                CodeTokenKind.FUNCTION to tint(ramp(88.0), saturation * 0.5),
                CodeTokenKind.ATTRIBUTE_NAME to tint(ramp(78.0), saturation * 0.7),
                CodeTokenKind.NUMBER to tint(ramp(70.0)),
                CodeTokenKind.CLASS_NAME to tint(ramp(93.0), saturation * 0.35),
                CodeTokenKind.PROPERTY to tint(ramp(97.0), saturation * 0.15),
                CodeTokenKind.REGEX to tint(ramp(72.0), saturation * 0.6),
            )

        accent = accentTone
        // Upstream states these two in oklch, which has no counterpart here. These are the
        // same greys: a very dark neutral and a very light one.
        background = if (dark) Color(0xFF232323) else Color(0xFFFAFAFA)
        border = if (dark) Color.White.copy(alpha = 0.08f) else Color.Black.copy(alpha = 0.08f)
        headerBackground =
            if (dark) Color.White.copy(alpha = 0.03f) else Color.Black.copy(alpha = 0.03f)
        muted = if (dark) Color.White.copy(alpha = 0.6f) else Color.Black.copy(alpha = 0.6f)
        gutter = if (dark) Color.White.copy(alpha = 0.28f) else Color.Black.copy(alpha = 0.32f)
        lineWash =
            RareUiHsl(hue, saturation, if (dark) 58.0 else 45.0)
                .toColor()
                .copy(alpha = if (dark) 0.1f else 0.08f)
    }

    /**
     * The colour a token kind is drawn in.
     *
     * @param kind the kind.
     * @return its colour, falling back to plain text.
     */
    public fun color(kind: CodeTokenKind): Color = tokens[kind] ?: tokens[CodeTokenKind.PLAIN] ?: Color.Unspecified
}

/**
 * Builds a theme from one colour.
 *
 * A colour with nothing to say, such as a fully transparent one, falls back to upstream's
 * own fallback of a strong blue, which is what stops an unreadable accent producing an
 * unreadable theme.
 *
 * @param accent any colour. The whole theme is shades of it.
 * @param dark whether to build the dark appearance.
 * @return the theme.
 */
public fun codeBlockTheme(
    accent: Color,
    dark: Boolean,
): CodeBlockTheme {
    val hsl =
        if (accent == Color.Unspecified || accent.alpha == 0f) {
            RareUiHsl(211.0, 100.0, 52.0)
        } else {
            accent.toRareUiRgba().toHsl()
        }
    return CodeBlockTheme(hsl, dark)
}
