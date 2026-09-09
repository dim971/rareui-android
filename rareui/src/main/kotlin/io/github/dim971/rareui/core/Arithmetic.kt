/*
 * Arithmetic.kt
 * Small numeric helpers shared across components, each matching the JavaScript operator it
 * stands in for rather than the Kotlin one that looks like it.
 */

package io.github.dim971.rareui.core

/**
 * A true modulo, always returning a value with the sign of the divisor.
 *
 * Kotlin's `%` and JavaScript's both take the sign of the dividend, so `-1 % 10` is `-1` in
 * each. Upstream writes `((n % m) + m) % m` wherever it needs a wheel to wrap, and this is
 * that. The counter's digit wheels and the matrix orb's ripples both depend on it staying
 * positive across the wrap.
 *
 * @param value the dividend.
 * @param modulus the divisor. Must not be zero.
 * @return [value] reduced into `0 until modulus`.
 */
public fun rareUiMod(
    value: Double,
    modulus: Double,
): Double {
    val remainder = value % modulus
    return (remainder + modulus) % modulus
}

/**
 * Confines a value to a range, treating a value that is not finite as the low end.
 *
 * The fallback matters: upstream reaches for this to keep a caller supplied duration or
 * count usable, and a NaN that slipped through would otherwise poison every comparison
 * downstream of it.
 *
 * @param value the value to confine.
 * @param low the lowest acceptable value.
 * @param high the highest acceptable value.
 * @return the confined value.
 */
public fun rareUiClamp(
    value: Double,
    low: Double,
    high: Double,
): Double {
    if (!value.isFinite()) return low
    return minOf(high, maxOf(low, value))
}
