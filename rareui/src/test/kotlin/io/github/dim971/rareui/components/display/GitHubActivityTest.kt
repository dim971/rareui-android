/*
 * GitHubActivityTest.kt
 * The arithmetic behind the heatmap, checked against `components/ui/github-activity.tsx`.
 * The same assertions run on iOS, plus one for the month labels.
 */

package io.github.dim971.rareui.components.display

import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class GitHubLevelTest {
    @Test
    fun `an empty day is not drawn at all`() {
        // The cell underneath shows through, which is how the grid gets its empty days
        // without spending a colour on them.
        assertEquals(0f, gitHubLevelOpacity(0), 0f)
    }

    @Test
    fun `each level is darker than the last, and the top one is the accent itself`() {
        var previous = -1f
        for (level in 0..4) {
            val opacity = gitHubLevelOpacity(level)
            assertTrue(opacity > previous)
            previous = opacity
        }
        assertEquals(1f, gitHubLevelOpacity(4), 0f)
    }

    @Test
    fun `a level outside the range is brought back into it`() {
        assertEquals(gitHubLevelOpacity(0), gitHubLevelOpacity(-3), 0f)
        assertEquals(gitHubLevelOpacity(4), gitHubLevelOpacity(99), 0f)
    }
}

class GitHubMeasurementTest {
    @Test
    fun `the gap grows with the cells but never disappears`() {
        assertEquals(3.dp, gitHubCellGap(11.dp))
        assertEquals(5.dp, gitHubCellGap(20.dp))
        // A gap of nothing would turn the grid into a solid block.
        assertEquals(2.dp, gitHubCellGap(1.dp))
        assertEquals(2.dp, gitHubCellGap(0.dp))
    }

    @Test
    fun `a year is about fifty-three weeks`() {
        assertEquals(53, gitHubWeeks(12))
        assertEquals(27, gitHubWeeks(6))
        assertEquals(5, gitHubWeeks(1))
    }

    @Test
    fun `no months still means one week, not the whole history`() {
        // Upstream notes the trap: slicing the last nought weeks off an array hands back
        // all of it, so a grid of no months would silently become a grid of everything.
        assertEquals(1, gitHubWeeks(0))
        assertEquals(1, gitHubWeeks(-5))
    }
}

class GitHubMonthLabelTest {
    /** A year of empty days starting on the given date, cut into weeks. */
    private fun weeks(
        from: LocalDate,
        days: Int,
    ): List<List<GitHubContribution>> =
        (0 until days)
            .map { GitHubContribution(from.plusDays(it.toLong()), count = 0, level = 0) }
            .chunked(7)

    @Test
    fun `the first column always carries its month`() {
        val grid = weeks(LocalDate.of(2026, 3, 2), 70)
        assertEquals("Mar", gitHubMonthLabel(grid, 0))
    }

    @Test
    fun `only the first week of a month is labelled`() {
        val grid = weeks(LocalDate.of(2026, 1, 5), 200)
        val labelled = grid.indices.count { gitHubMonthLabel(grid, it) != null }
        // Roughly one label a month across seven months, never one a week.
        assertTrue(labelled in 5..8)
        assertNull(gitHubMonthLabel(grid, 1))
    }

    @Test
    fun `a month with too little of itself on screen goes unlabelled`() {
        // Two weeks of February is narrower than the word naming it, so it is left out.
        val grid = weeks(LocalDate.of(2026, 1, 5), 45)
        assertNull(gitHubMonthLabel(grid, grid.lastIndex))
    }

    @Test
    fun `a column that is not there has no label rather than throwing`() {
        assertNull(gitHubMonthLabel(emptyList(), 0))
        assertNull(gitHubMonthLabel(weeks(LocalDate.of(2026, 1, 5), 7), 4))
    }
}
