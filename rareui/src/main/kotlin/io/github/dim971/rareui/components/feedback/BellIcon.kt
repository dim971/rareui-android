/*
 * BellIcon.kt
 * The bell itself, quoted from the two SVG paths in upstream's
 * `components/ui/notification-bell.tsx`.
 */

package io.github.dim971.rareui.components.feedback

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.vector.PathParser

/** The side of the box upstream's two paths are drawn in. */
private const val VIEW_BOX = 18f

/** The bell hangs from its crown. Turning it about the middle would look like a spin. */
private const val CROWN_Y = 0.12f

/** The dome reads through the clapper at this opacity, which is upstream's `opacity-55`. */
private const val BODY_OPACITY = 0.55f

/**
 * The two outlines and the point the clapper turns about.
 *
 * Parsed once and kept, because a path built from a string in a draw pass is a string
 * parsed sixty times a second.
 */
internal class BellOutline {
    /** The dome and the rim. */
    val body: Path =
        PathParser()
            .parsePathString(
                "M3.5 6.5C3.5 3.46279 5.96279 1 9 1C12.0372 1 14.5 3.46279 14.5 6.5V10.75C14.5 " +
                    "11.4408 15.0592 12 15.75 12C16.1642 12 16.5 12.3358 16.5 12.75C16.5 13.1642 " +
                    "16.1642 13.5 15.75 13.5H2.25C1.83579 13.5 1.5 13.1642 1.5 12.75C1.5 12.3358 " +
                    "1.83579 12 2.25 12C2.94079 12 3.5 11.4408 3.5 10.75V6.5Z",
            ).toPath()

    /** The clapper, hanging below the rim. */
    val tongue: Path =
        PathParser()
            .parsePathString(
                "M10.2 15H7.80099C7.64999 15 7.50799 15.068 7.41299 15.185C7.31799 15.302 " +
                    "7.28099 15.456 7.31199 15.603C7.48499 16.425 8.17999 17 9.00099 17C9.82199 17 " +
                    "10.517 16.425 10.69 15.603C10.721 15.456 10.684 15.302 10.589 15.185C10.494 " +
                    "15.068 10.351 15 10.2 15Z",
            ).toPath()

    /**
     * The top middle of the clapper, which it turns about rather than turning about the
     * bell's own pivot. This is upstream's `transformBox: fill-box` with an origin of
     * 50% 0%.
     */
    val tongueAnchor: Offset =
        tongue.getBounds().let { Offset(it.center.x, it.top) }
}

/**
 * Draws the bell at the given angles, filling the whole of this scope.
 *
 * @param outline the parsed paths.
 * @param swing how far the bell has swung, in degrees.
 * @param clapper how far the clapper has swung relative to it, in degrees.
 * @param color the ink both parts are drawn in.
 */
internal fun DrawScope.drawBell(
    outline: BellOutline,
    swing: Float,
    clapper: Float,
    color: Color,
) {
    // Everything below is stated in the eighteen point box the paths were authored in, so
    // the pivots read as they do in the original rather than in pixels.
    scale(size.minDimension / VIEW_BOX, pivot = Offset.Zero) {
        rotate(swing, pivot = Offset(VIEW_BOX / 2, VIEW_BOX * CROWN_Y)) {
            drawPath(outline.body, color.copy(alpha = color.alpha * BODY_OPACITY))
            rotate(clapper, pivot = outline.tongueAnchor) {
                drawPath(outline.tongue, color)
            }
        }
    }
}
