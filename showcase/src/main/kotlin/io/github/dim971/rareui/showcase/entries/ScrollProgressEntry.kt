package io.github.dim971.rareui.showcase.entries

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.unit.dp
import io.github.dim971.rareui.components.navigation.ScrollProgress
import io.github.dim971.rareui.components.navigation.ScrollProgressSection
import io.github.dim971.rareui.showcase.CatalogEntry
import io.github.dim971.rareui.showcase.Demo
import kotlinx.coroutines.launch

val scrollProgressEntry: CatalogEntry =
    CatalogEntry(
        name = "Scroll Progress",
        summary = "A floating pill showing how far down you are, which opens into the page's sections.",
        demos =
            listOf(
                Demo(
                    title = "Scroll the panel",
                    note =
                        "The ring follows a spring rather than the scroll itself, so a flick does " +
                            "not make it jump. Tapping the pill opens it into the sections, and " +
                            "the highlight travels between them rather than fading in and out.",
                    code =
                        "ScrollProgress(sections = sections, progress = read, selection = section,\n" +
                            "    onSelect = { scrollTo(it) })",
                ) { ScrollProgressDemo() },
            ),
    ) {
        ScrollProgress(
            sections = listOf(ScrollProgressSection("a", "Reading")),
            progress = 0.4f,
            selection = "a",
        )
    }

private val DemoSections =
    listOf(
        ScrollProgressSection("intro", "Introduction"),
        ScrollProgressSection("install", "Installing"),
        ScrollProgressSection("theming", "Theming"),
        ScrollProgressSection("motion", "Motion"),
    )

@Composable
private fun ScrollProgressDemo() {
    val scroll = rememberScrollState()
    val scope = rememberCoroutineScope()
    val tops = remember { mutableStateMapOf<String, Int>() }

    val progress by remember {
        derivedStateOf {
            if (scroll.maxValue <= 0) 0f else scroll.value.toFloat() / scroll.maxValue
        }
    }
    val selection by remember {
        derivedStateOf {
            DemoSections.lastOrNull { (tops[it.id] ?: Int.MAX_VALUE) - scroll.value <= 40 }?.id
                ?: DemoSections.first().id
        }
    }

    Box {
        Column(
            modifier =
                Modifier
                    .height(300.dp)
                    .verticalScroll(scroll),
            verticalArrangement = Arrangement.spacedBy(24.dp),
        ) {
            DemoSections.forEach { section ->
                Column(
                    modifier =
                        Modifier.onGloballyPositioned { coordinates ->
                            tops[section.id] = coordinates.positionInParent().y.toInt()
                        },
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(section.label, style = MaterialTheme.typography.titleSmall)
                    Text(
                        text = "Something to read. ".repeat(14),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Box(Modifier.height(60.dp).fillMaxWidth())
        }

        ScrollProgress(
            sections = DemoSections,
            progress = progress,
            selection = selection,
            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 12.dp),
            onSelect = { id -> scope.launch { scroll.animateScrollTo(tops[id] ?: 0) } },
        )
    }
}
