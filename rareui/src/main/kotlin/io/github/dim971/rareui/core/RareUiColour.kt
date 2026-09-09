/*
 * RareUiColour.kt
 * Colour arithmetic, kept free of the framework so it can be unit tested without a device.
 * CodeBlock derives an entire syntax theme from one accent by way of HSL, so the
 * conversions here are load-bearing rather than convenience.
 */

package io.github.dim971.rareui.core

import androidx.compose.ui.graphics.Color
import kotlin.math.abs

/** A colour in the sRGB space, with each component in `0..1`. */
public data class RareUiRgba(
    /** The red component, in `0..1`. */
    public val red: Double,
    /** The green component, in `0..1`. */
    public val green: Double,
    /** The blue component, in `0..1`. */
    public val blue: Double,
    /** The alpha component, in `0..1`. */
    public val alpha: Double = 1.0,
)

/**
 * A colour expressed as hue, saturation and lightness.
 *
 * CodeBlock builds its whole palette by taking one accent colour here, walking the
 * lightness up and down a ramp, and converting back. Working in HSL rather than sRGB is
 * what keeps the derived tones reading as the same hue.
 */
public data class RareUiHsl(
    /** The hue, in degrees, `0 until 360`. */
    public val hue: Double,
    /** The saturation, as a percentage, `0..100`. */
    public val saturation: Double,
    /** The lightness, as a percentage, `0..100`. */
    public val lightness: Double,
    /** The alpha component, in `0..1`. */
    public val alpha: Double = 1.0,
)

/**
 * Parses a CSS style hex string.
 *
 * Accepts `RGB`, `RGBA`, `RRGGBB` and `RRGGBBAA`, with or without a leading `#`. Returns
 * `null` for anything else, including strings of the right length holding characters that
 * are not hex digits.
 *
 * @param hex the hex string, for example `"#F4F4F9"`.
 * @return the colour, or `null` when the string is not one.
 */
public fun rareUiHex(hex: String): RareUiRgba? {
    val text = hex.trim().removePrefix("#")
    if (text.isEmpty() || !text.all { it.isDigit() || it.lowercaseChar() in 'a'..'f' }) return null

    // The three and four digit forms repeat each digit, so `#f80` is `#ff8800`.
    val expanded =
        when (text.length) {
            3, 4 -> text.map { "$it$it" }.joinToString("")
            6, 8 -> text
            else -> return null
        }

    val value = expanded.toLong(16)
    val shifted = if (expanded.length == 8) value else (value shl 8) or 0xFF

    return RareUiRgba(
        red = (shifted shr 24 and 0xFF) / 255.0,
        green = (shifted shr 16 and 0xFF) / 255.0,
        blue = (shifted shr 8 and 0xFF) / 255.0,
        alpha = (shifted and 0xFF) / 255.0,
    )
}

/**
 * This colour converted to hue, saturation and lightness.
 *
 * @return the same colour in HSL.
 */
public fun RareUiRgba.toHsl(): RareUiHsl {
    val maximum = maxOf(red, green, blue)
    val minimum = minOf(red, green, blue)
    val lightness = (maximum + minimum) / 2
    val delta = maximum - minimum

    if (delta <= 0) return RareUiHsl(0.0, 0.0, lightness * 100, alpha)

    // Saturation folds around mid lightness: the same delta means more saturation near the
    // ends of the range than it does in the middle.
    val saturation = delta / (1 - abs(2 * lightness - 1))

    val hue =
        when (maximum) {
            red -> 60 * (((green - blue) / delta) % 6)
            green -> 60 * ((blue - red) / delta + 2)
            else -> 60 * ((red - green) / delta + 4)
        }

    return RareUiHsl(
        hue = if (hue < 0) hue + 360 else hue,
        saturation = saturation * 100,
        lightness = lightness * 100,
        alpha = alpha,
    )
}

/**
 * This colour converted back to sRGB.
 *
 * @return the same colour in RGB.
 */
public fun RareUiHsl.toRgba(): RareUiRgba {
    val s = saturation / 100
    val l = lightness / 100
    val chroma = (1 - abs(2 * l - 1)) * s
    val sector = ((hue % 360) + 360) % 360 / 60
    val second = chroma * (1 - abs(sector % 2 - 1))
    val lift = l - chroma / 2

    // The hue circle is six sectors wide. In each one, one channel is at full chroma, one
    // is off, and the third ramps between them.
    val (r, g, b) =
        when {
            sector < 1 -> Triple(chroma, second, 0.0)
            sector < 2 -> Triple(second, chroma, 0.0)
            sector < 3 -> Triple(0.0, chroma, second)
            sector < 4 -> Triple(0.0, second, chroma)
            sector < 5 -> Triple(second, 0.0, chroma)
            else -> Triple(chroma, 0.0, second)
        }

    return RareUiRgba(r + lift, g + lift, b + lift, alpha)
}

/**
 * This colour with its lightness replaced.
 *
 * @param lightness the new lightness, as a percentage. Clamped to `0..100`.
 * @return a colour of the same hue and saturation at the given lightness.
 */
public fun RareUiHsl.withLightness(lightness: Double): RareUiHsl = copy(lightness = lightness.coerceIn(0.0, 100.0))

/** This colour as a Compose one. */
public fun RareUiRgba.toColor(): Color =
    Color(
        red = red.toFloat().coerceIn(0f, 1f),
        green = green.toFloat().coerceIn(0f, 1f),
        blue = blue.toFloat().coerceIn(0f, 1f),
        alpha = alpha.toFloat().coerceIn(0f, 1f),
    )

/** This colour as a Compose one, by way of sRGB. */
public fun RareUiHsl.toColor(): Color = toRgba().toColor()

/** A Compose colour as components this file can work with. */
public fun Color.toRareUiRgba(): RareUiRgba =
    RareUiRgba(red.toDouble(), green.toDouble(), blue.toDouble(), alpha.toDouble())
