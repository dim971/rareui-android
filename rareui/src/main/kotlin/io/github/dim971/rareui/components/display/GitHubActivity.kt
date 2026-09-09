/*
 * GitHubActivity.kt
 * A port of upstream's `components/ui/github-activity.tsx`.
 *
 * A contribution heatmap on a card, with a footer that lifts up over the grid and turns
 * into a ranked list of the repositories the contributions went to.
 *
 * Three deliberate departures, recorded in docs/fidelity.md. This takes its data as values
 * and never touches the network: upstream can fetch a year of contributions from a public
 * API given a username, which is convenient on a page and wrong in a component library,
 * since a view that makes its own requests cannot be tested, cannot be previewed offline
 * and gives an application no say over caching or failure. The year is browsable rather
 * than trimmed, because a component on a phone is as wide as it is given and dropping
 * months to fit would quietly change what it says. And the avatars cross fade between the
 * two states rather than travelling between them, since the shared element API that would
 * carry them is still experimental and a published library should not depend on one.
 */

package io.github.dim971.rareui.components.display

import android.os.Build
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.dim971.rareui.theme.RareUiColors
import io.github.dim971.rareui.theme.RareUiEasing
import io.github.dim971.rareui.theme.RareUiTheme
import io.github.dim971.rareui.theme.rareUiVisualSpring
import io.github.dim971.rareui.theme.rememberRareUiReduceMotion
import kotlinx.coroutines.flow.first
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

/** How many avatars the collapsed footer shows before it stops. */
private const val STACK_LIMIT = 3

/** How far apart two columns start appearing, in milliseconds. */
private const val COLUMN_STAGGER = 12

/** How long a cell takes to appear. */
private const val CELL_FADE = 200

/** How small a cell starts. */
private const val CELL_ARRIVING_SCALE = 0.4f

/** How long the month labels take to resolve out of their blur, once the grid has drawn. */
private const val LABEL_SETTLE = 450

/** How blurred they start, in points. */
private val LabelBlur = 6.dp

/** The panel's spring, from upstream's `SPRING`. */
private const val PANEL_DURATION = 0.62f
private const val PANEL_BOUNCE = 0.2f

/** GitHub's own green, which upstream uses as the default accent. */
private val GitHubGreen = Color(0xFF39D353)

/** Room under the grid for the footer, which sits over the card rather than in it. */
private val FooterRoom = 52.dp

/**
 * A contribution heatmap with a footer that opens into a ranked list.
 *
 * ```kotlin
 * GitHubActivity(contributions = days, repos = repositories)
 * ```
 *
 * With animations turned off on the device the cells appear without sweeping in and the
 * footer opens without springing.
 *
 * @param contributions one entry per day, oldest first.
 * @param modifier the modifier to apply.
 * @param repos the repositories the contributions went to, in whatever order you want them ranked.
 * @param accent the colour the cells are drawn in, shaded by level. Defaults to GitHub's green.
 * @param accentScale one colour per level, for a ramp of your own rather than one colour
 *   shaded five ways. Four colours are the four levels that have something in them; five or
 *   more set the empty level too. Wins over [accent] when it is not empty.
 * @param expanded whether the footer is open. Leave it out and the component keeps its own.
 * @param onExpandedChange called when the footer is opened or closed.
 * @param cellSize how large one cell is.
 * @param months how many months to show.
 * @param label the footer's wording.
 * @param showsMonths whether to label the months above the grid.
 */
@Composable
public fun GitHubActivity(
    contributions: List<GitHubContribution>,
    modifier: Modifier = Modifier,
    repos: List<GitHubRepoContribution> = emptyList(),
    accent: Color = GitHubGreen,
    accentScale: List<Color> = emptyList(),
    cellSize: Dp = 11.dp,
    months: Int = 12,
    label: String = "Top contributions in:",
    showsMonths: Boolean = false,
    expanded: Boolean? = null,
    onExpandedChange: ((Boolean) -> Unit)? = null,
) {
    val colors = RareUiTheme.colors
    val reduceMotion = rememberRareUiReduceMotion()
    val gap = gitHubCellGap(cellSize)

    val weeks =
        remember(contributions, months) {
            contributions
                .chunked(7)
                .takeLast(gitHubWeeks(months))
        }

    // One clock for the whole sweep rather than one timer per column. Each column reads its
    // own share out of it inside a graphics layer, so a year of cells arriving costs one
    // animation and no recomposition.
    val sweep = remember { Animatable(0f) }
    val total = weeks.size * COLUMN_STAGGER + CELL_FADE
    var swept by remember { mutableStateOf(reduceMotion) }

    LaunchedEffect(weeks.size, reduceMotion) {
        if (reduceMotion) {
            sweep.snapTo(1f)
            swept = true
            return@LaunchedEffect
        }
        sweep.snapTo(0f)
        swept = false
        sweep.animateTo(1f, tween(total, easing = LinearEasing))
        swept = true
    }

    val settle by animateFloatAsState(
        targetValue = if (swept) 1f else 0f,
        animationSpec = if (reduceMotion) snap() else tween(LABEL_SETTLE, easing = RareUiEasing.EaseOutQuint),
        label = "github-labels",
    )

    val scroll = rememberScrollState()
    LaunchedEffect(weeks.size) {
        // The most recent weeks are the ones worth seeing first, and they are on the right.
        // Waiting for the grid to have been measured is the point of the flow: asked before
        // that, the scroll state says there is nowhere to go and the year opens on its
        // oldest end.
        scroll.scrollTo(snapshotFlow { scroll.maxValue }.first { it > 0 })
    }

    Box(modifier = modifier) {
        Column(
            modifier =
                Modifier
                    .background(colors.background, RoundedCornerShape(16.dp))
                    .border(1.dp, colors.border, RoundedCornerShape(16.dp))
                    .horizontalScroll(scroll)
                    .padding(16.dp)
                    .padding(bottom = if (repos.isEmpty()) 0.dp else FooterRoom)
                    .semantics { contentDescription = "Contribution activity" },
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (showsMonths) {
                MonthLabels(
                    weeks = weeks,
                    cellSize = cellSize,
                    gap = gap,
                    settle = { settle },
                    colors = colors,
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(gap)) {
                weeks.forEachIndexed { column, week ->
                    Column(
                        modifier =
                            Modifier.graphicsLayer {
                                // Each column starts a little after the one before, so the
                                // year draws itself left to right rather than all at once.
                                val arrived =
                                    (((sweep.value * total) - column * COLUMN_STAGGER) / CELL_FADE)
                                        .coerceIn(0f, 1f)
                                val eased = RareUiEasing.EaseOutQuint.transform(arrived)
                                alpha = eased
                                scaleX = CELL_ARRIVING_SCALE + (1f - CELL_ARRIVING_SCALE) * eased
                                scaleY = scaleX
                            },
                        verticalArrangement = Arrangement.spacedBy(gap),
                    ) {
                        week.forEach { day ->
                            ContributionCell(day, cellSize, accent, accentScale, colors)
                        }
                    }
                }
            }
        }

        // No repositories means nothing to rank, and a footer that says so is worse than no
        // footer. Upstream always shows it because it always has data to put in it.
        if (repos.isNotEmpty()) {
            ActivityFooter(
                repos = repos,
                label = label,
                colors = colors,
                reduceMotion = reduceMotion,
                expanded = expanded,
                onExpandedChange = onExpandedChange,
                modifier = Modifier.align(Alignment.BottomCenter).padding(12.dp),
            )
        }
    }
}

/** One day of the grid. */
@Composable
private fun ContributionCell(
    day: GitHubContribution,
    cellSize: Dp,
    accent: Color,
    accentScale: List<Color>,
    colors: RareUiColors,
) {
    val noun = if (day.count == 1) "contribution" else "contributions"
    val shape = RoundedCornerShape(3.dp)

    Box(
        modifier =
            Modifier
                .size(cellSize)
                .background(colors.foreground.copy(alpha = 0.08f), shape)
                .background(
                    if (accentScale.isEmpty()) {
                        accent.copy(alpha = gitHubLevelOpacity(day.level))
                    } else {
                        gitHubLevelInk(day.level, accentScale)
                    },
                    shape,
                ).semantics {
                    contentDescription =
                        "${day.count} $noun on ${day.date.format(GitHubDayFormat)}"
                },
    )
}

/** The date the accessibility label reads out. */
private val GitHubDayFormat: DateTimeFormatter = DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)

/** Blur needs a render effect, which arrived in Android 12. */
private val PLATFORM_BLURS = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S

/**
 * The month names above the grid.
 *
 * They wait for the grid to finish drawing itself, then resolve out of a blur rather than
 * fading, so they read as settling onto the year rather than being switched on over it.
 */
@Composable
private fun MonthLabels(
    weeks: List<List<GitHubContribution>>,
    cellSize: Dp,
    gap: Dp,
    settle: () -> Float,
    colors: RareUiColors,
) {
    val blur = LabelBlur * (1f - settle())

    Row(
        modifier =
            Modifier
                .graphicsLayer { alpha = settle() }
                .then(if (PLATFORM_BLURS) Modifier.blur(blur) else Modifier),
        horizontalArrangement = Arrangement.spacedBy(gap),
        verticalAlignment = Alignment.Bottom,
    ) {
        weeks.indices.forEach { column ->
            Box(modifier = Modifier.width(cellSize)) {
                gitHubMonthLabel(weeks, column)?.let { name ->
                    BasicText(
                        text = name,
                        style =
                            TextStyle(
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Medium,
                                color = colors.glyph,
                            ),
                        // A column is eleven points wide and a month's name is not. The
                        // label is measured at its natural width and allowed to run over
                        // the columns it names, rather than being wrapped down the page.
                        modifier = Modifier.wrapContentWidth(Alignment.Start, unbounded = true),
                    )
                }
            }
        }
    }
}

/**
 * The label above one column, if it deserves one.
 *
 * Only the first week of a month is labelled, and only when the month has at least three
 * weeks on screen: any fewer and the word is wider than the run it names.
 *
 * @param weeks the grid, in columns.
 * @param column which column to label.
 * @return the month's name, or `null` for a column that carries no label.
 */
internal fun gitHubMonthLabel(
    weeks: List<List<GitHubContribution>>,
    column: Int,
): String? {
    val month =
        weeks
            .getOrNull(column)
            ?.firstOrNull()
            ?.date
            ?.monthValue ?: return null
    if (column == 0) return GitHubMonthNames[month - 1]

    val previous = weeks[column - 1].firstOrNull()?.date?.monthValue
    if (previous == month) return null

    val remaining =
        weeks.drop(column).takeWhile { week -> week.firstOrNull()?.date?.monthValue == month }
    return if (remaining.size >= 3) GitHubMonthNames[month - 1] else null
}

/** The footer, which is either a line of avatars or the ranked list they open into. */
@Composable
@Suppress("LongParameterList")
private fun ActivityFooter(
    repos: List<GitHubRepoContribution>,
    label: String,
    colors: RareUiColors,
    reduceMotion: Boolean,
    expanded: Boolean?,
    onExpandedChange: ((Boolean) -> Unit)?,
    modifier: Modifier = Modifier,
) {
    // Controlled when the caller says so, and keeping its own when it does not, which is
    // the same choice upstream offers through `open` and `defaultOpen`.
    var uncontrolled by remember { mutableStateOf(false) }
    val open = expanded ?: uncontrolled

    fun setOpen(next: Boolean) {
        uncontrolled = next
        onExpandedChange?.invoke(next)
    }

    AnimatedContent(
        targetState = open,
        modifier = modifier,
        transitionSpec = {
            if (reduceMotion) {
                fadeIn(snap()) togetherWith fadeOut(snap())
            } else {
                fadeIn(rareUiVisualSpring(PANEL_DURATION, PANEL_BOUNCE)) togetherWith
                    fadeOut(rareUiVisualSpring(PANEL_DURATION, PANEL_BOUNCE))
            }
        },
        label = "github-footer",
    ) { open ->
        if (open) {
            Column(
                modifier =
                    Modifier
                        .background(colors.surface, RoundedCornerShape(14.dp))
                        .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    FooterLabel(label, colors, Modifier.weight(1f))
                    Chevron(expanded = true, colors = colors, reduceMotion = reduceMotion) {
                        setOpen(false)
                    }
                }
                repos.forEach { repo ->
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RepoAvatar(repo, colors)
                        BasicText(
                            text = repo.name,
                            style =
                                TextStyle(
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = colors.foreground,
                                ),
                            modifier = Modifier.weight(1f),
                        )
                        BasicText(
                            text = repo.count.toString(),
                            style =
                                TextStyle(
                                    fontSize = 13.sp,
                                    color = colors.glyph,
                                    fontFeatureSettings = "tnum",
                                ),
                        )
                    }
                }
            }
        } else {
            Row(
                modifier =
                    Modifier
                        .background(colors.surface, RoundedCornerShape(percent = 50))
                        .padding(horizontal = 10.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                FooterLabel(label, colors)
                Row(horizontalArrangement = Arrangement.spacedBy((-8).dp)) {
                    repos.take(STACK_LIMIT).forEach { repo -> RepoAvatar(repo, colors) }
                }
                Chevron(expanded = false, colors = colors, reduceMotion = reduceMotion) {
                    setOpen(true)
                }
            }
        }
    }
}

/** The footer's wording, which both states share. */
@Composable
private fun FooterLabel(
    label: String,
    colors: RareUiColors,
    modifier: Modifier = Modifier,
) {
    BasicText(
        text = label,
        style = TextStyle(fontSize = 12.sp, fontWeight = FontWeight.Medium, color = colors.glyph),
        modifier = modifier,
    )
}

/** A repository, as the first letter of its name in a ring. */
@Composable
private fun RepoAvatar(
    repo: GitHubRepoContribution,
    colors: RareUiColors,
) {
    Box(
        modifier =
            Modifier
                .size(28.dp)
                .background(colors.background, CircleShape)
                .border(1.dp, colors.border, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        BasicText(
            text = repo.name.take(1).uppercase(),
            style =
                TextStyle(
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = colors.foreground,
                ),
        )
    }
}

/** The caret that opens and closes the panel, drawn rather than bundled as an asset. */
@Composable
private fun Chevron(
    expanded: Boolean,
    colors: RareUiColors,
    reduceMotion: Boolean,
    onClick: () -> Unit,
) {
    val turn by animateFloatAsState(
        targetValue = if (expanded) 180f else 0f,
        animationSpec = if (reduceMotion) snap() else tween(200),
        label = "github-chevron",
    )

    Canvas(
        modifier =
            Modifier
                .size(22.dp)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onClick,
                ).graphicsLayer { rotationZ = turn }
                .semantics {
                    contentDescription = if (expanded) "Hide repositories" else "Show repositories"
                },
    ) {
        val width = 11.dp.toPx()
        val height = 5.5.dp.toPx()
        val middle = Offset(size.width / 2, size.height / 2)
        val stroke = 2.dp.toPx()
        drawLine(
            color = colors.glyph,
            start = Offset(middle.x - width / 2, middle.y + height / 2),
            end = middle.copy(y = middle.y - height / 2),
            strokeWidth = stroke,
            cap = StrokeCap.Round,
        )
        drawLine(
            color = colors.glyph,
            start = middle.copy(y = middle.y - height / 2),
            end = Offset(middle.x + width / 2, middle.y + height / 2),
            strokeWidth = stroke,
            cap = StrokeCap.Round,
        )
    }
}
