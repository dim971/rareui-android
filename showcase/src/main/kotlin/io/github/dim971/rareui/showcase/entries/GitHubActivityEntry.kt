package io.github.dim971.rareui.showcase.entries

import androidx.compose.foundation.layout.width
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import io.github.dim971.rareui.components.display.GitHubActivity
import io.github.dim971.rareui.components.display.GitHubContribution
import io.github.dim971.rareui.components.display.GitHubRepoContribution
import io.github.dim971.rareui.showcase.CatalogEntry
import io.github.dim971.rareui.showcase.Demo
import java.time.LocalDate
import kotlin.random.Random

val gitHubActivityEntry: CatalogEntry =
    CatalogEntry(
        name = "GitHub Activity",
        summary = "A contribution heatmap whose footer lifts up and turns into a ranked list.",
        demos =
            listOf(
                Demo(
                    title = "A year of it",
                    note =
                        "The grid draws itself left to right, a column at a time, and the month " +
                            "labels wait for it to finish and then resolve out of a blur.",
                    code = "GitHubActivity(contributions = days, repos = repositories)",
                ) {
                    GitHubActivity(
                        contributions = SampleActivityYear,
                        repos = SampleActivityRepos,
                        showsMonths = true,
                    )
                },
                Demo(
                    title = "A ramp of your own",
                    note =
                        "One colour shaded five ways is the default. Four colours set the four " +
                            "levels that have something in them, which is how GitHub's own scale " +
                            "is stated.",
                    code =
                        "GitHubActivity(contributions = days,\n" +
                            "    accentScale = listOf(Color(0xFF0E4429), Color(0xFF006D32),\n" +
                            "        Color(0xFF26A641), Color(0xFF39D353)))",
                ) {
                    GitHubActivity(
                        contributions = SampleActivityYear,
                        accentScale =
                            listOf(
                                Color(0xFF0E4429),
                                Color(0xFF006D32),
                                Color(0xFF26A641),
                                Color(0xFF39D353),
                            ),
                        months = 6,
                    )
                },
                Demo(
                    title = "Any accent, any size",
                    code =
                        "GitHubActivity(contributions = days, accent = Color(0xFFFC4C01),\n" +
                            "    cellSize = 8.dp, months = 6)",
                ) {
                    GitHubActivity(
                        contributions = SampleActivityYear,
                        repos = SampleActivityRepos,
                        accent = Color(0xFFFC4C01),
                        cellSize = 8.dp,
                        months = 6,
                    )
                },
            ),
    ) {
        GitHubActivity(
            contributions = SampleActivityYear.takeLast(70),
            cellSize = 5.dp,
            months = 2,
            modifier = Modifier.width(100.dp),
        )
    }

/** A year of plausible looking activity, so the demo has something to draw. */
private val SampleActivityYear: List<GitHubContribution> =
    run {
        val random = Random(20260909)
        val start = LocalDate.now().minusDays(364)

        List(365) { day ->
            val date = start.plusDays(day.toLong())
            // Weekends are quieter, and there are stretches of nothing at all, which is
            // what makes a real year of activity look like one.
            val weekend = date.dayOfWeek.value >= 6
            val quiet = weekend || random.nextInt(5) == 0
            val count = if (quiet) random.nextInt(3) else random.nextInt(15)

            val level =
                when {
                    count == 0 -> 0
                    count <= 2 -> 1
                    count <= 6 -> 2
                    count <= 10 -> 3
                    else -> 4
                }
            GitHubContribution(date = date, count = count, level = level)
        }
    }

private val SampleActivityRepos =
    listOf(
        GitHubRepoContribution("rareui-ios", 412),
        GitHubRepoContribution("rareui-android", 287),
        GitHubRepoContribution("drawably-ios", 143),
        GitHubRepoContribution("openclaude", 61),
    )
