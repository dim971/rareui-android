package io.github.dim971.rareui.showcase.entries

import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.github.dim971.rareui.components.navigation.HookSidebar
import io.github.dim971.rareui.showcase.CatalogEntry
import io.github.dim971.rareui.showcase.Demo

val hookSidebarEntry: CatalogEntry =
    CatalogEntry(
        name = "Hook Sidebar",
        summary = "A rail down the gutter that stops at the current row and hooks into it.",
        demos =
            listOf(
                Demo(
                    title = "Pick a row",
                    note =
                        "The rail stops a corner short of the row and the hook covers the last " +
                            "stretch, turning out toward the label.",
                    code = "HookSidebar(items = items, selection = section, onSelect = { section = it })",
                ) { HookSidebarDemo(listOf("Overview", "Installation", "Theming", "Motion", "Licence")) },
                Demo(
                    title = "Solid rather than dashed",
                    code =
                        "HookSidebar(items = items, selection = section, onSelect = { section = it },\n" +
                            "    dashed = false)",
                ) { HookSidebarDemo(listOf("Overview", "Installation", "Theming"), dashed = false) },
                Demo(
                    title = "With a heading",
                    note =
                        "A second, fainter rail follows a pointer. It appears with a mouse, a " +
                            "trackpad or a stylus, and never under a finger, which is not a " +
                            "pointer to follow. Upstream behaves the same way.",
                    code =
                        "HookSidebar(items = items, selection = section, onSelect = { section = it },\n" +
                            "    label = \"Docs\")",
                ) { HookSidebarDemo(listOf("Overview", "Installation", "Theming"), label = "Docs") },
            ),
    ) {
        HookSidebarDemo(listOf("One", "Two"), start = 1)
    }

@Composable
private fun HookSidebarDemo(
    items: List<String>,
    dashed: Boolean = true,
    label: String? = null,
    start: Int = 0,
) {
    var section by remember { mutableIntStateOf(start) }
    HookSidebar(
        items = items,
        selection = section,
        onSelect = { section = it },
        modifier = Modifier.widthIn(max = 260.dp),
        label = label,
        dashed = dashed,
    )
}
