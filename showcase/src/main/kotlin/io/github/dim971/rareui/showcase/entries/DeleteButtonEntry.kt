package io.github.dim971.rareui.showcase.entries

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.github.dim971.rareui.components.inputs.DeleteButton
import io.github.dim971.rareui.showcase.CatalogEntry
import io.github.dim971.rareui.showcase.Demo

val deleteButtonEntry: CatalogEntry =
    CatalogEntry(
        name = "Delete Button",
        summary = "A bin that opens into its own confirmation rather than into a dialogue.",
        demos =
            listOf(
                Demo(
                    title = "Ask first",
                    note =
                        "The lid swings back past its open angle before settling, and the walls " +
                            "redraw shorter as it goes, so the bin appears to sink while the lid " +
                            "lifts clear of it.",
                    code = "DeleteButton(onConfirm = { remove(item) })",
                ) { DeleteButtonDemo() },
                Demo(
                    title = "In a row",
                    note = "The button widens in place, so whatever is beside it moves out of the way.",
                    code =
                        "Row {\n" +
                            "    Text(\"Draft\")\n" +
                            "    Spacer(Modifier.weight(1f))\n" +
                            "    DeleteButton(onConfirm = { remove(draft) })\n" +
                            "}",
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        listOf("Draft", "Archive", "Backup").forEach { name ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(name, style = MaterialTheme.typography.bodyLarge)
                                DeleteButton()
                            }
                        }
                    }
                },
            ),
    ) {
        DeleteButton()
    }

@Composable
private fun DeleteButtonDemo() {
    val log = remember { mutableStateListOf<String>() }

    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        DeleteButton(
            onCancel = { log += "kept" },
            onConfirm = { log += "deleted" },
        )
        Text(
            text = if (log.isEmpty()) "Nothing yet" else log.takeLast(6).joinToString(", "),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
