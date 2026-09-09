/*
 * FolderStates.kt
 * Where the flap and the cards sit in each of the folder's three states, from upstream's
 * `components/ui/folder-component.tsx`.
 */

package io.github.dim971.rareui.components.display

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

/** How large a [FolderComponent] is drawn. */
public enum class FolderSize(
    internal val scale: Float,
) {
    /** Two thirds of the drawing's own size. */
    SMALL(0.65f),

    /** The size it was drawn at. */
    MEDIUM(1f),

    /** A third again larger. */
    LARGE(1.35f),
}

/**
 * The colours one folder is drawn in.
 *
 * Upstream carries three of these, and each is a complete set rather than a tint: the black
 * folder holds pale cards, the white one holds dark cards, and the blue one holds pale
 * cards again.
 */
@Immutable
internal data class FolderPalette(
    val back: Color,
    val flapFill: Color,
    val flapOpacity: Float,
    val flapStroke: Color,
    val cardFill: Color,
    val cardStroke: Color,
    val cardLine: Color,
)

/** Which of upstream's three folders to draw. */
public enum class FolderColor {
    /** A black folder holding pale cards. */
    BLACK,

    /** A white folder holding dark cards. */
    WHITE,

    /** A blue folder holding pale cards. */
    BLUE,
    ;

    internal val palette: FolderPalette
        get() =
            when (this) {
                BLACK ->
                    FolderPalette(
                        back = Color.Black,
                        flapFill = Color(0xFF292929),
                        flapOpacity = 0.25f,
                        flapStroke = Color(0xFF979797),
                        cardFill = Color(0xFFF1F1F1),
                        cardStroke = Color(0xFFE0E0E0),
                        cardLine = Color(0xFFD4D4D4),
                    )

                WHITE ->
                    FolderPalette(
                        back = Color.White,
                        flapFill = Color(0xFFF5F5F5),
                        flapOpacity = 0.85f,
                        flapStroke = Color(0xFFD4D4D4),
                        cardFill = Color(0xFF262626),
                        cardStroke = Color(0xFF404040),
                        cardLine = Color(0xFF737373),
                    )

                BLUE ->
                    FolderPalette(
                        back = Color(0xFF50B1FD),
                        flapFill = Color(0xFF3A9AE8),
                        flapOpacity = 0.45f,
                        flapStroke = Color(0xFF7EC8FF),
                        cardFill = Color(0xFFF1F1F1),
                        cardStroke = Color(0xFFE0E0E0),
                        cardLine = Color(0xFFD4D4D4),
                    )
            }
}

/** Which of the three states the folder is in. */
internal enum class FolderState {
    REST,
    HOVERING,
    OPEN,
    ;

    /** How far the flap has tipped back, in degrees. */
    val flapAngle: Float
        get() =
            when (this) {
                REST -> -15f
                HOVERING -> -45f
                OPEN -> -55f
            }
}

/** Where a card sits and how far it leans, in one of the three states. */
@Immutable
internal data class FolderPose(
    val x: Float,
    val y: Float,
    val rotation: Float,
)

/**
 * Where one card sits in each state.
 *
 * The three fan out rather than stacking: one to the right leaning right, one in the middle
 * almost straight, one to the left leaning left. Opening lifts all three clear of the
 * folder and exaggerates the fan.
 */
@Immutable
internal data class FolderCardPlacement(
    val rest: FolderPose,
    val hovering: FolderPose,
    val open: FolderPose,
    val openDelay: Long,
    val hoverDelay: Long,
) {
    /**
     * Where the card sits in a given state.
     *
     * @param state the folder's state.
     * @return the card's pose.
     */
    fun pose(state: FolderState): FolderPose =
        when (state) {
            FolderState.REST -> rest
            FolderState.HOVERING -> hovering
            FolderState.OPEN -> open
        }

    /**
     * The cards leave in turn rather than together, and the one furthest from the middle
     * leaves first, so the fan opens outward.
     *
     * @param state the folder's state.
     * @return how long this card waits, in milliseconds.
     */
    fun delay(state: FolderState): Long =
        when (state) {
            FolderState.OPEN -> openDelay
            FolderState.HOVERING -> hoverDelay
            FolderState.REST -> 0
        }
}

/**
 * The three cards, in the order they are drawn: the rightmost first, so the leftmost ends up
 * on top.
 */
internal val FolderCardPlacements: List<FolderCardPlacement> =
    listOf(
        FolderCardPlacement(
            rest = FolderPose(40f, -10f, 10f),
            hovering = FolderPose(40f, -30f, 14f),
            open = FolderPose(70f, -160f, 18f),
            openDelay = 100,
            hoverDelay = 120,
        ),
        FolderCardPlacement(
            rest = FolderPose(3f, -20f, 2f),
            hovering = FolderPose(3f, -35f, -1f),
            open = FolderPose(0f, -180f, -3f),
            openDelay = 50,
            hoverDelay = 60,
        ),
        FolderCardPlacement(
            rest = FolderPose(-40f, -22f, -5f),
            hovering = FolderPose(-40f, -44f, -9f),
            open = FolderPose(-65f, -170f, -14f),
            openDelay = 0,
            hoverDelay = 0,
        ),
    )
