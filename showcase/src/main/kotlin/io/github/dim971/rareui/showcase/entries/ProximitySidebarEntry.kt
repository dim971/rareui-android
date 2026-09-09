package io.github.dim971.rareui.showcase.entries

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.github.dim971.rareui.components.navigation.ProximitySection
import io.github.dim971.rareui.components.navigation.ProximitySectionKind
import io.github.dim971.rareui.components.navigation.ProximitySidebar
import io.github.dim971.rareui.components.navigation.ProximitySidebarSide
import io.github.dim971.rareui.showcase.CatalogEntry
import io.github.dim971.rareui.showcase.Demo

val proximitySidebarEntry: CatalogEntry =
    CatalogEntry(
        name = "Proximity Sidebar",
        summary = "A page outline drawn as dashes, which swell as a pointer passes them.",
        demos =
            listOf(
                Demo(
                    title = "The outline",
                    note =
                        "A finger is not a pointer, so the dash for whatever is being read " +
                            "swells instead. Drag down the dashes, or move a mouse over them, " +
                            "and they follow.",
                    code =
                        "ProximitySidebar(sections = outline, selection = section,\n" +
                            "    onSelect = { scrollTo(it) })",
                ) { ProximityDemo() },
                Demo(
                    title = "On the right",
                    note = "The dashes grow from whichever edge they are anchored to.",
                    code =
                        "ProximitySidebar(sections = outline, side = ProximitySidebarSide.TRAILING,\n" +
                            "    selection = section)",
                ) { ProximityDemo(ProximitySidebarSide.TRAILING) },
            ),
    ) {
        ProximitySidebar(
            sections =
                listOf(
                    ProximitySection("a", "Title", ProximitySectionKind.TITLE),
                    ProximitySection("b", "Body"),
                    ProximitySection("c", "Body"),
                ),
            selection = "a",
        )
    }

private val OutlineSections =
    listOf(
        ProximitySection("title", "Rare UI", ProximitySectionKind.TITLE),
        ProximitySection("install", "Installing", ProximitySectionKind.SUBTITLE),
        ProximitySection("jitpack", "JitPack", ProximitySectionKind.SECTION),
        ProximitySection("gradle", "In Gradle", ProximitySectionKind.BODY),
        ProximitySection("theme", "Theming", ProximitySectionKind.SUBTITLE),
        ProximitySection("colours", "Colours", ProximitySectionKind.SECTION),
        ProximitySection("motion", "Motion", ProximitySectionKind.BODY),
    )

@Composable
private fun ProximityDemo(side: ProximitySidebarSide = ProximitySidebarSide.LEADING) {
    var selection by remember { mutableStateOf<String?>("title") }

    Row(
        horizontalArrangement = Arrangement.spacedBy(20.dp),
        verticalAlignment = Alignment.Top,
    ) {
        if (side == ProximitySidebarSide.TRAILING) {
            ProximityDetail(selection, Modifier.weight(1f))
        }
        ProximitySidebar(
            sections = OutlineSections,
            side = side,
            selection = selection,
            onSelect = { selection = it },
        )
        if (side == ProximitySidebarSide.LEADING) {
            ProximityDetail(selection, Modifier.weight(1f))
        }
    }
}

@Composable
private fun ProximityDetail(
    selection: String?,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = OutlineSections.firstOrNull { it.id == selection }?.label ?: "",
            style = MaterialTheme.typography.titleSmall,
        )
        Text(
            text = "Tap a dash to pick a section.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
