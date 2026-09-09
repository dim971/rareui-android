package io.github.dim971.rareui.showcase

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp

/** A row of accent swatches, shown above every list of samples. */
@Composable
fun AccentControls(
    accent: Color,
    onAccentChange: (Color) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            "Accent",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            ShowcaseAccents.forEach { (name, colour) ->
                Column(
                    modifier =
                        Modifier
                            .padding(3.dp)
                            .then(
                                if (colour == accent) {
                                    Modifier.border(2.dp, MaterialTheme.colorScheme.onSurface, CircleShape)
                                } else {
                                    Modifier
                                },
                            ).padding(3.dp),
                ) {
                    Column(
                        modifier =
                            Modifier
                                .size(26.dp)
                                .background(colour, CircleShape)
                                .clickable { onAccentChange(colour) }
                                .semantics { contentDescription = name },
                    ) {}
                }
            }
        }
    }
}
