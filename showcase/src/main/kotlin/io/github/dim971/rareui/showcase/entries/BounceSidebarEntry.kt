package io.github.dim971.rareui.showcase.entries

import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.github.dim971.rareui.components.navigation.BounceSidebar
import io.github.dim971.rareui.components.navigation.BounceSidebarItem
import io.github.dim971.rareui.components.navigation.bounceSidebarHeading
import io.github.dim971.rareui.showcase.CatalogEntry
import io.github.dim971.rareui.showcase.Demo

val bounceSidebarEntry: CatalogEntry =
    CatalogEntry(
        name = "Bounce Sidebar",
        summary = "A list with a dot in the gutter that arcs from one row to the next.",
        demos =
            listOf(
                Demo(
                    title = "Pick a row",
                    note =
                        "The dot does not travel in a straight line. It swings out to the left " +
                            "and back in, by about the same amount whatever the distance.",
                    code =
                        "BounceSidebar(items = items, selection = section, onSelect = { section = it })",
                ) {
                    BounceSidebarDemo(
                        listOf("Overview", "Installation", "Theming", "Motion", "Licence")
                            .map { BounceSidebarItem(it) },
                    )
                },
                Demo(
                    title = "With headings",
                    note = "A heading takes the dot's colour and opens a gap above itself.",
                    code =
                        "BounceSidebar(\n" +
                            "    items = listOf(\n" +
                            "        bounceSidebarHeading(\"Getting started\"),\n" +
                            "        BounceSidebarItem(\"Overview\"),\n" +
                            "    ),\n" +
                            "    selection = section,\n" +
                            "    onSelect = { section = it },\n" +
                            ")",
                ) {
                    BounceSidebarDemo(
                        listOf(
                            bounceSidebarHeading("Getting started"),
                            BounceSidebarItem("Overview"),
                            BounceSidebarItem("Installation"),
                            bounceSidebarHeading("Reference"),
                            BounceSidebarItem("Components"),
                            BounceSidebarItem("Theming"),
                        ),
                    )
                },
            ),
    ) {
        BounceSidebarDemo(listOf(BounceSidebarItem("One"), BounceSidebarItem("Two")))
    }

@Composable
private fun BounceSidebarDemo(items: List<BounceSidebarItem>) {
    var section by remember { mutableIntStateOf(0) }
    BounceSidebar(
        items = items,
        selection = section,
        onSelect = { section = it },
        modifier = Modifier.widthIn(max = 260.dp),
    )
}
