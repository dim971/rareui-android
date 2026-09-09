/*
 * RareUiTheme.kt
 * Upstream hardcodes its palette inside each component, as Tailwind classes: the same
 * `bg-[#F4F4F9] dark:bg-[#262626]` appears in a dozen files. Those values are collected
 * here once so a host application can restyle the set without forking a component, while
 * the defaults stay exactly what the originals use.
 */

package io.github.dim971.rareui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * The colours the components draw themselves in.
 *
 * Every default is the value the matching upstream component hardcodes, so a component
 * left untouched looks like its counterpart on rareui.com.
 */
@Immutable
public data class RareUiColors(
    /** The raised surface almost every component sits on. */
    public val surface: Color,
    /** The recessed surface a raised control sinks into, as in the delete button's panel. */
    public val surfaceRecessed: Color,
    /** The page behind the components. */
    public val background: Color,
    /** Text and icons at full strength. */
    public val foreground: Color,
    /** The muted grey upstream draws inactive glyphs and labels in. */
    public val glyph: Color,
    /** Hairline borders. */
    public val border: Color,
    /** The brand accent, which is what a selection is marked in. */
    public val accent: Color,
    /** The filled part of a track, as in the step player. */
    public val track: Color,
    /** The system red, used for destructive state and validation failure. */
    public val red: Color,
    /** The system orange. */
    public val orange: Color,
    /** The system green, used for validation success. */
    public val green: Color,
    /** The system blue. */
    public val blue: Color,
    /** The system violet. */
    public val violet: Color,
)

/**
 * Builds the palette for one appearance.
 *
 * @param dark whether to build the dark appearance.
 * @param accent the colour a selection is marked in.
 */
public fun rareUiColors(
    dark: Boolean,
    accent: Color = Color(0xFFFC4C01),
): RareUiColors =
    if (dark) {
        RareUiColors(
            surface = Color(0xFF262626),
            surfaceRecessed = Color(0xFF1B1B1B),
            background = Color(0xFF0A0A0A),
            foreground = Color(0xFFFAFAFA),
            glyph = Color(0xFF9B9AA7),
            border = Color(0x14FFFFFF),
            accent = accent,
            track = Color(0xFFEBEBF5),
            // The five below are Apple's system colours, which is not a coincidence:
            // upstream reaches for the iOS palette by hex in its inputs and its badge.
            red = Color(0xFFFF453A),
            orange = Color(0xFFFF9F0A),
            green = Color(0xFF30D158),
            blue = Color(0xFF0A84FF),
            violet = Color(0xFFBF5AF2),
        )
    } else {
        RareUiColors(
            surface = Color(0xFFF4F4F9),
            surfaceRecessed = Color(0xFFE7E7EF),
            background = Color(0xFFFFFFFF),
            foreground = Color(0xFF0A0A0A),
            glyph = Color(0xFF868593),
            border = Color(0x0D000000),
            accent = accent,
            track = Color(0xFF3C3C43),
            red = Color(0xFFFF3B30),
            orange = Color(0xFFFF9500),
            green = Color(0xFF34C759),
            blue = Color(0xFF007AFF),
            violet = Color(0xFFAF52DE),
        )
    }

/**
 * The palette in force, or nothing when no theme has been provided.
 *
 * Nullable on purpose: a component with no theme around it should still follow the
 * system's appearance rather than being stuck in whichever one the default was written
 * for, and a composition local's default cannot itself be composable.
 */
public val LocalRareUiColors: ProvidableCompositionLocal<RareUiColors?> = compositionLocalOf { null }

/**
 * Provides a palette to everything inside it.
 *
 * ```kotlin
 * RareUiTheme(colors = rareUiColors(dark = isSystemInDarkTheme(), accent = Color.Magenta)) {
 *     GooeyNav(items = items, selection = tab, onSelect = { tab = it })
 * }
 * ```
 *
 * @param colors the palette to use.
 * @param content what to draw with it.
 */
@Composable
public fun RareUiTheme(
    colors: RareUiColors = rareUiColors(isSystemInDarkTheme()),
    content: @Composable () -> Unit,
) {
    CompositionLocalProvider(LocalRareUiColors provides colors, content = content)
}

/** The palette the Rare UI components in this composition draw themselves with. */
public object RareUiTheme {
    /**
     * The palette in force. With no [RareUiTheme] around it, the defaults for the
     * system's current appearance.
     */
    public val colors: RareUiColors
        @Composable
        @ReadOnlyComposable
        get() = LocalRareUiColors.current ?: rareUiColors(isSystemInDarkTheme())
}
