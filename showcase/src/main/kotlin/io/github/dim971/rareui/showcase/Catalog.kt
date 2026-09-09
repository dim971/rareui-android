package io.github.dim971.rareui.showcase

import androidx.compose.runtime.Composable
import io.github.dim971.rareui.showcase.entries.animatedCounterEntry
import io.github.dim971.rareui.showcase.entries.bounceSidebarEntry
import io.github.dim971.rareui.showcase.entries.deleteButtonEntry
import io.github.dim971.rareui.showcase.entries.durationPickerEntry
import io.github.dim971.rareui.showcase.entries.emojiReactionEntry
import io.github.dim971.rareui.showcase.entries.gitHubActivityEntry
import io.github.dim971.rareui.showcase.entries.gooeyNavEntry
import io.github.dim971.rareui.showcase.entries.hookSidebarEntry
import io.github.dim971.rareui.showcase.entries.matrixOrbEntry
import io.github.dim971.rareui.showcase.entries.notificationBellEntry
import io.github.dim971.rareui.showcase.entries.otpInputEntry
import io.github.dim971.rareui.showcase.entries.proximitySidebarEntry
import io.github.dim971.rareui.showcase.entries.scrollProgressEntry
import io.github.dim971.rareui.showcase.entries.stepPlayerEntry

/** One demo on a component's screen: a live sample and the code behind it. */
class Demo(
    val title: String,
    val note: String? = null,
    val code: String,
    val sample: @Composable () -> Unit,
)

/**
 * One component in the catalog.
 *
 * Adding a component means adding one of these under `entries` and listing it in
 * [catalog]. The home list, the detail screen and the screenshots all read from that one
 * place.
 */
class CatalogEntry(
    val name: String,
    val summary: String,
    val demos: List<Demo>,
    val preview: @Composable () -> Unit,
)

/**
 * A group of components, named the way rareui.com groups them.
 *
 * Keeping upstream's grouping means someone arriving from the website finds a component
 * where they expect it to be, rather than where a Kotlin author would have filed it.
 */
class CatalogSection(
    val name: String,
    val entries: List<CatalogEntry>,
)

/**
 * Every component the library ships, grouped as rareui.com groups them.
 *
 * The sections are declared up front and fill in as components land, so the shape of the
 * catalog matches the shape of the website from the first commit rather than being
 * rearranged as the port progresses.
 */
val catalog: List<CatalogSection> =
    listOf(
        CatalogSection(
            "Display",
            listOf(animatedCounterEntry, gitHubActivityEntry, stepPlayerEntry),
        ),
        CatalogSection("AI Kit", listOf(matrixOrbEntry)),
        CatalogSection(
            "Navigation",
            listOf(
                bounceSidebarEntry,
                hookSidebarEntry,
                gooeyNavEntry,
                proximitySidebarEntry,
                scrollProgressEntry,
            ),
        ),
        CatalogSection(
            "Inputs",
            listOf(deleteButtonEntry, durationPickerEntry, otpInputEntry),
        ),
        CatalogSection("Feedback", listOf(emojiReactionEntry, notificationBellEntry)),
    )

/** Every entry in the catalog, flattened, for lookup by name. */
val catalogEntries: List<CatalogEntry> = catalog.flatMap { it.entries }
