/*
 * GitHubActivityData.kt
 * What a contribution heatmap is made of, and the arithmetic behind it, from upstream's
 * `components/ui/github-activity.tsx`.
 */

package io.github.dim971.rareui.components.display

import androidx.compose.runtime.Immutable
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import java.time.LocalDate
import kotlin.math.ceil
import kotlin.math.roundToInt

/** One day's contributions. */
@Immutable
public data class GitHubContribution(
    /** The day. */
    public val date: LocalDate,
    /** How many contributions were made. */
    public val count: Int,
    /** How dark the cell is drawn, from `0` for none to `4` for the most. */
    public val level: Int,
)

/** One repository's share of the contributions. */
@Immutable
public data class GitHubRepoContribution(
    /** The repository's name. */
    public val name: String,
    /** How many contributions went to it. */
    public val count: Int,
)

/**
 * How dark each level is drawn, as a fraction of the accent.
 *
 * Level zero is not drawn at all: the cell underneath shows through, which is what gives
 * the grid its empty days without spending a colour on them.
 *
 * @param level the day's level.
 * @return the opacity to draw the accent at.
 */
internal fun gitHubLevelOpacity(level: Int): Float =
    when (level.coerceIn(0, 4)) {
        1 -> 0.3f
        2 -> 0.52f
        3 -> 0.76f
        4 -> 1f
        else -> 0f
    }

/**
 * The gap between two cells, which grows with them.
 *
 * @param cellSize how large one cell is.
 * @return the gap, never below two points.
 */
internal fun gitHubCellGap(cellSize: Dp): Dp = maxOf(2.dp, (cellSize.value / 4).roundToInt().dp)

/**
 * How many weeks are in a given number of months.
 *
 * Never zero, because a grid of no weeks would be a grid of the whole history: upstream
 * notes that slicing the last nought weeks off an array hands back all of it.
 *
 * @param months how many months to show.
 * @return the number of weeks.
 */
internal fun gitHubWeeks(months: Int): Int = maxOf(1, ceil(months * 365.25 / 12 / 7).toInt())

/** The month names the labels above the grid are drawn from. */
internal val GitHubMonthNames: List<String> =
    listOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")
