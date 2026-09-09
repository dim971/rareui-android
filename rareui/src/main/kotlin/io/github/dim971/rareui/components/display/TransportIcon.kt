/*
 * TransportIcon.kt
 * The play, pause and replay glyphs on StepPlayer, ported from the `TransportIcon`
 * component in upstream's `components/ui/step-player.tsx`.
 *
 * Upstream morphs play into pause with flubber, which matches the vertices of two arbitrary
 * paths and interpolates between them. There is no such thing on this platform and there
 * does not need to be: these two shapes are a triangle and a pair of bars, and splitting
 * the triangle down its middle gives two quadrilaterals that correspond to the two bars
 * corner for corner. Interpolating those is exact rather than approximate, and it is what
 * flubber would have arrived at anyway.
 *
 * Replay is left out of it, exactly as upstream leaves it out: there is no sensible vertex
 * match between an arrow curled into a circle and either of the others, so it crossfades.
 */

package io.github.dim971.rareui.components.display

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.vector.PathParser

/** The icon's own box, from upstream's `viewBox="0 0 24 24"`. */
internal const val TRANSPORT_BOX = 24f

/** The left bar of the pause icon, `M8.4 5.9 L10.4 5.9 L10.4 18.1 L8.4 18.1 Z`. */
internal val TransportPauseLeft: List<Offset> =
    listOf(Offset(8.4f, 5.9f), Offset(10.4f, 5.9f), Offset(10.4f, 18.1f), Offset(8.4f, 18.1f))

/** The right bar, `M13.6 5.9 L15.6 5.9 L15.6 18.1 L13.6 18.1 Z`. */
internal val TransportPauseRight: List<Offset> =
    listOf(Offset(13.6f, 5.9f), Offset(15.6f, 5.9f), Offset(15.6f, 18.1f), Offset(13.6f, 18.1f))

/** The left half of the play triangle `M9.8 7 L17.7 12 L9.8 17 Z`, cut down its middle. */
internal val TransportPlayLeft: List<Offset> =
    listOf(Offset(9.8f, 7f), Offset(13.75f, 9.5f), Offset(13.75f, 14.5f), Offset(9.8f, 17f))

/**
 * The right half, which is the tip. Two of its corners meet at the point, which is how a
 * four cornered shape becomes a three cornered one without a seam.
 */
internal val TransportPlayRight: List<Offset> =
    listOf(Offset(13.75f, 9.5f), Offset(17.7f, 12f), Offset(17.7f, 12f), Offset(13.75f, 14.5f))

/**
 * One quadrilateral part of the way from its pause corners to its play corners.
 *
 * @param from the corners at rest, which is pause.
 * @param to the corners at the end, which is play.
 * @param morph how far along, in `0..1`. Values outside are held at the ends rather than
 *   extrapolated, since a triangle turned inside out is not a play button.
 * @return the four corners, in the icon's own box.
 */
internal fun transportQuad(
    from: List<Offset>,
    to: List<Offset>,
    morph: Float,
): List<Offset> {
    val t = morph.coerceIn(0f, 1f)
    return from.indices.map { index ->
        Offset(
            from[index].x + (to[index].x - from[index].x) * t,
            from[index].y + (to[index].y - from[index].y) * t,
        )
    }
}

/**
 * Builds the glyph part of the way between pause and play.
 *
 * @param morph `0` is the pause bars, `1` is the play triangle.
 * @param into the path to rebuild, so a frame allocates nothing.
 */
internal fun transportPath(
    morph: Float,
    into: Path,
) {
    into.reset()
    listOf(
        transportQuad(TransportPauseLeft, TransportPlayLeft, morph),
        transportQuad(TransportPauseRight, TransportPlayRight, morph),
    ).forEach { corners ->
        into.moveTo(corners[0].x, corners[0].y)
        corners.drop(1).forEach { into.lineTo(it.x, it.y) }
        into.close()
    }
}

/**
 * The replay arrow, quoted from upstream's `REPLAY_PATH`: an arc most of the way round a
 * circle with an arrowhead turning back into it.
 */
internal fun replayPath(): Path =
    PathParser()
        .parsePathString(
            "M17.44 6.56 A7.7 7.7 0 1 1 10.66 4.42 L10.32 2.45 L14.86 4.59 L11.32 8.16 " +
                "L10.91 5.8 A6.3 6.3 0 1 0 16.45 7.55 Z",
        ).toPath()
