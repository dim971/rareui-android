package io.github.dim971.rareui.showcase

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/** Every component, each row showing the real thing rather than a screenshot. */
@Composable
fun CatalogHomeScreen(
    accent: Color,
    onAccentChange: (Color) -> Unit,
    onOpen: (CatalogEntry) -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxWidth(),
        contentPadding =
            androidx.compose.foundation.layout
                .PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            Text(
                "Rare UI",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 32.dp, bottom = 4.dp),
            )
        }
        item { AccentControls(accent = accent, onAccentChange = onAccentChange) }

        catalog.forEach { section ->
            if (section.entries.isNotEmpty()) {
                item {
                    Text(
                        section.name,
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 12.dp),
                    )
                }
                items(section.entries, key = { it.name }) { entry ->
                    CatalogRow(entry = entry, onOpen = onOpen)
                }
            }
        }

        if (catalogEntries.isEmpty()) {
            item {
                Text(
                    "No components yet. They land one at a time.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun CatalogRow(
    entry: CatalogEntry,
    onOpen: (CatalogEntry) -> Unit,
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        modifier =
            Modifier
                .fillMaxWidth()
                .clickable { onOpen(entry) },
    ) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(entry.name, style = MaterialTheme.typography.titleMedium)
                Text(
                    entry.summary,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Spacer(Modifier.width(12.dp))
            // A row is a navigation target, not a playground: the live preview is there to
            // be recognised, and a component that swallowed the tap would make the row
            // unreachable.
            Box(contentAlignment = Alignment.CenterEnd) { entry.preview() }
        }
    }
}
