/*
 * AnimatedCounterFormatting.kt
 * The arithmetic behind AnimatedCounter, ported from the `measure`, `format`, `group` and
 * `toCells` functions in upstream's `components/ui/animated-counter.tsx`.
 *
 * It is separated from the composable because it is the part that can be checked exactly.
 * Whether a roll feels right is a matter for the showcase; whether 12345678 groups as
 * 1,23,45,678 in Indian digits is a matter for a test. The same cases are asserted in the
 * iOS twin.
 */

package io.github.dim971.rareui.components.display

import io.github.dim971.rareui.core.rareUiClamp
import kotlin.math.abs
import kotlin.math.pow
import kotlin.math.roundToLong

/** How the digits before the decimal separator are grouped. */
public enum class CounterGrouping {
    /** Groups of three all the way up, as in `1,234,567`. */
    WESTERN,

    /** Three at the end and pairs above it, as in `12,34,567`. */
    INDIAN,
}

/** The measured shape of a value: what will actually be drawn, and how fast. */
internal data class CounterShape(
    /** The value, with anything that is not a finite number replaced by zero. */
    val amount: Double,
    /** The value as a whole number, scaled up past the decimal point. */
    val scaled: Long,
    /** How many decimal places are shown. */
    val places: Int,
    /** The roll duration, confined to something a spring can actually do. */
    val pace: Double,
    /** How many digit columns there are, before any separators. */
    val width: Int,
)

/**
 * Past this, the digits are floating point noise rather than information.
 *
 * It is JavaScript's `Number.MAX_SAFE_INTEGER`, which upstream clamps to for the same
 * reason: `String(scaled)` turns exponential above 1e21 and starts skipping integers well
 * before that.
 */
private const val MAX_SAFE_INTEGER = 9_007_199_254_740_991L

private const val MAX_DECIMALS = 15.0
private const val MAX_PAD = 24.0
private const val MIN_DURATION = 0.01
private const val MAX_DURATION = 60.0

/**
 * Works out what a value will look like before anything is laid out.
 *
 * @param value the number to show.
 * @param decimals how many decimal places to show.
 * @param padStart the minimum number of whole digits, padded with leading zeros.
 * @param duration the roll duration, in seconds.
 */
internal fun counterShape(
    value: Double,
    decimals: Int,
    padStart: Int,
    duration: Double,
): CounterShape {
    // A value that is not a number would make the previous-value comparison true forever,
    // so the counter would never roll again.
    val amount = if (value.isFinite()) value else 0.0
    val places = rareUiClamp(decimals.toDouble(), 0.0, MAX_DECIMALS).toInt()
    val pad = rareUiClamp(padStart.toDouble(), 1.0, MAX_PAD).toInt()

    val raised = (abs(amount) * 10.0.pow(places)).let { if (it.isFinite()) it else 0.0 }
    val whole = minOf(MAX_SAFE_INTEGER, raised.roundToLong())

    return CounterShape(
        amount = amount,
        scaled = whole,
        places = places,
        pace = rareUiClamp(duration, MIN_DURATION, MAX_DURATION),
        width = maxOf(whole.toString().length, places + pad),
    )
}

/**
 * Inserts a separator between groups of digits.
 *
 * @param digits the whole part, as digits only.
 * @param separator the separator to insert. An empty separator leaves the digits alone.
 * @param grouping which grouping to use.
 */
internal fun counterGroup(
    digits: String,
    separator: String,
    grouping: CounterGrouping,
): String {
    if (separator.isEmpty()) return digits
    if (grouping != CounterGrouping.INDIAN) return chunked(digits, 3, separator)

    // Indian grouping is three at the end and pairs the rest of the way up, so the trailing
    // group is split off before the pairs are counted.
    val head = digits.dropLast(3)
    if (head.isEmpty()) return digits
    return chunked(head, 2, separator) + separator + digits.takeLast(3)
}

/** Inserts a separator every [size] characters, counting from the right. */
private fun chunked(
    digits: String,
    size: Int,
    separator: String,
): String {
    val result = StringBuilder()
    digits.forEachIndexed { index, character ->
        // Never at the start, which is what upstream's `\B` in the regex is there for.
        if (index > 0 && (digits.length - index) % size == 0) result.append(separator)
        result.append(character)
    }
    return result.toString()
}

/**
 * Renders a measured shape as the string the counter will show.
 *
 * @param shape the measured shape.
 * @param separator the grouping separator.
 * @param decimalSeparator the decimal separator.
 * @param grouping which grouping to use.
 * @return the formatted digits, without any sign.
 */
internal fun counterFormat(
    shape: CounterShape,
    separator: String,
    decimalSeparator: String,
    grouping: CounterGrouping,
): String {
    val padded = shape.scaled.toString().padStart(shape.width, '0')
    val wholeDigits = padded.dropLast(shape.places)
    val whole = counterGroup(wholeDigits.ifEmpty { "0" }, separator, grouping)
    if (shape.places == 0) return whole
    return whole + decimalSeparator + padded.takeLast(shape.places)
}

/** One column of the counter. */
internal sealed interface CounterCell {
    /** The identity a column keeps across value changes. */
    val key: String

    /** A digit that rolls. The place is its distance from the right. */
    data class Digit(
        val place: Int,
        val value: Int,
    ) : CounterCell {
        override val key: String get() = "digit-$place"
    }

    /** A separator or any other character that does not roll. */
    data class Mark(
        val place: Int,
        val run: Int,
        val character: Char,
    ) : CounterCell {
        override val key: String get() = "mark-$place-$run"
    }
}

/**
 * Splits formatted digits into the columns that get drawn.
 *
 * Digits are keyed by distance from the right rather than by position, so a number gaining
 * a place shifts the existing columns along instead of tearing all of them down and
 * building new ones.
 *
 * @param characters the formatted string.
 * @param width the shape's digit count, which anchors the place numbering.
 */
internal fun counterCells(
    characters: String,
    width: Int,
): List<CounterCell> {
    val cells = mutableListOf<CounterCell>()
    var seen = 0
    // Only digits advance the place, so without the run counter a two character separator
    // would give two marks the same identity.
    var run = 0

    characters.forEach { character ->
        if (character in '0'..'9') {
            run = 0
            cells += CounterCell.Digit(place = width - seen, value = character - '0')
            seen += 1
        } else {
            cells += CounterCell.Mark(place = width - seen, run = run, character = character)
            run += 1
        }
    }
    return cells
}
