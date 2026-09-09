package io.github.dim971.rareui.showcase

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import io.github.dim971.rareui.theme.rememberRareUiReduceMotion

/** One component: what it is, every variant of it live, and the code for each. */
@Composable
fun ComponentScreen(
    entry: CatalogEntry,
    accent: Color,
    onAccentChange: (Color) -> Unit,
    onBack: () -> Unit,
) {
    val reduced = rememberRareUiReduceMotion()

    LazyColumn(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item {
            Row(modifier = Modifier.padding(top = 32.dp)) {
                TextButton(onClick = onBack) { Text("Back") }
            }
        }
        item {
            Text(entry.name, style = MaterialTheme.typography.headlineSmall)
        }
        item {
            Text(
                entry.summary,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        if (reduced) {
            item {
                // That setting changes what these components do, deliberately and in every
                // case, and a reader who does not know it is on would file a bug about it.
                Text(
                    "Animations are turned off on this device, so these settle instead of animating.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        item { AccentControls(accent = accent, onAccentChange = onAccentChange) }

        items(entry.demos, key = { it.title }) { demo ->
            DemoCard(demo)
        }
    }
}

@Composable
private fun DemoCard(demo: Demo) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(demo.title, style = MaterialTheme.typography.titleSmall)
        demo.note?.let {
            Text(
                it,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Card(shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) { demo.sample() }
        }
        Card(shape = RoundedCornerShape(10.dp), modifier = Modifier.fillMaxWidth()) {
            Text(
                demo.code,
                fontFamily = FontFamily.Monospace,
                style = MaterialTheme.typography.bodySmall,
                modifier =
                    Modifier
                        .horizontalScroll(rememberScrollState())
                        .padding(12.dp),
            )
        }
    }
}
