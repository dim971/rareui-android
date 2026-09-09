package io.github.dim971.rareui.showcase.entries

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.github.dim971.rareui.components.navigation.GooeyNav
import io.github.dim971.rareui.components.navigation.GooeyNavSize
import io.github.dim971.rareui.showcase.CatalogEntry
import io.github.dim971.rareui.showcase.Demo

val gooeyNavEntry: CatalogEntry =
    CatalogEntry(
        name = "Gooey Nav",
        summary = "A segmented bar whose selected tile detaches, stretching the seams until they part.",
        demos =
            listOf(
                Demo(
                    title = "Pick a tile",
                    note =
                        "The seam is a drawn pair of curves, not a blur filter. It pinches to a " +
                            "waist and breaks once the gap passes 22% of the separation.",
                    code = "GooeyNav(items = listOf(\"Home\", \"Docs\"), selection = tab, onSelect = { tab = it })",
                ) { GooeyNavDemo(listOf("Home", "Docs", "Pricing")) },
                Demo(
                    title = "Sizes",
                    note = "Padding, type size, corner radius and separation all follow the size.",
                    code =
                        "GooeyNav(items = items, selection = tab, onSelect = { tab = it },\n" +
                            "    size = GooeyNavSize.SMALL)",
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        GooeyNavSize.entries.forEach { size ->
                            GooeyNavDemo(listOf("One", "Two", "Three"), size = size)
                        }
                    }
                },
                Demo(
                    title = "A wider pull",
                    note =
                        "A larger separation stretches the seam further before it breaks, since " +
                            "the break is a fraction of it.",
                    code = "GooeyNav(items = items, selection = tab, onSelect = { tab = it }, separation = 44.dp)",
                ) { GooeyNavDemo(listOf("Left", "Middle", "Right"), separation = 44.dp) },
            ),
    ) {
        GooeyNavDemo(listOf("A", "B"), size = GooeyNavSize.EXTRA_SMALL)
    }

@Composable
private fun GooeyNavDemo(
    items: List<String>,
    size: GooeyNavSize = GooeyNavSize.MEDIUM,
    separation: Dp? = null,
) {
    var tab by remember { mutableIntStateOf(0) }
    if (separation == null) {
        GooeyNav(items = items, selection = tab, onSelect = { tab = it }, size = size)
    } else {
        GooeyNav(
            items = items,
            selection = tab,
            onSelect = { tab = it },
            size = size,
            separation = separation,
        )
    }
}
