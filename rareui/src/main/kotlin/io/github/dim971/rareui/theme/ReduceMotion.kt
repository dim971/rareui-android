/*
 * ReduceMotion.kt
 * Whether the reader has asked for less movement.
 *
 * Upstream calls `useReducedMotion()`, which reads a media query. Android has no such
 * query: what it has is an animator duration scale, which a reader turns down or off in
 * the developer options or through an accessibility service. Compose respects it for its
 * own animations automatically; the components here have to read it themselves, because
 * several of them run their own clock and would otherwise keep moving after everything
 * else had stopped.
 */

package io.github.dim971.rareui.theme

import android.provider.Settings
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext

/**
 * Whether animations should be reduced to nothing.
 *
 * @return `true` when the system's animator duration scale is off.
 */
@Composable
@ReadOnlyComposable
public fun rareUiReduceMotion(): Boolean {
    val resolver = LocalContext.current.contentResolver
    return Settings.Global.getFloat(resolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f
}

/**
 * Whether animations should be reduced, read once and remembered.
 *
 * The setting cannot change without the process being able to see it, and reading a
 * content resolver on every recomposition of a component that recomposes every frame is
 * not free.
 *
 * @return `true` when the system's animator duration scale is off.
 */
@Composable
public fun rememberRareUiReduceMotion(): Boolean {
    val resolver = LocalContext.current.contentResolver
    return remember(resolver) {
        Settings.Global.getFloat(resolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f
    }
}
