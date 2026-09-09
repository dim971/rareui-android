package io.github.dim971.rareui.showcase.entries

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.unit.dp
import io.github.dim971.rareui.components.inputs.DurationPicker
import io.github.dim971.rareui.components.inputs.DurationValue
import io.github.dim971.rareui.showcase.CatalogEntry
import io.github.dim971.rareui.showcase.Demo

val durationPickerEntry: CatalogEntry =
    CatalogEntry(
        name = "Duration Picker",
        summary = "Three touching panels that separate to be edited, and a pen that becomes a tick.",
        demos =
            listOf(
                Demo(
                    title = "Set a duration",
                    note =
                        "Pressing the pen opens the fields and morphs it into a tick. The morph " +
                            "is a real one: both outlines are walked, sampled to the same number " +
                            "of points, lined up and slid across.",
                    code =
                        "DurationPicker(value = duration, onValueChange = { duration = it },\n" +
                            "    onConfirm = ::schedule)",
                ) { DurationPickerDemo() },
                Demo(
                    title = "Its own limits",
                    note =
                        "Typing past the limit clamps the field and gives it a nudge, so the " +
                            "refusal is felt rather than read.",
                    code =
                        "DurationPicker(value = duration, onValueChange = { duration = it },\n" +
                            "    maxHours = 8, maxMinutes = 59)",
                ) { DurationPickerDemo(maxHours = 8, maxMinutes = 59, hours = 2, minutes = 30) },
            ),
    ) {
        DurationPicker(value = DurationValue(1, 15), onValueChange = {})
    }

@Composable
private fun DurationPickerDemo(
    maxHours: Int = 24,
    maxMinutes: Int = 60,
    hours: Int = 1,
    minutes: Int = 15,
) {
    var duration by remember { mutableStateOf(DurationValue(hours, minutes)) }
    var confirmed by remember { mutableStateOf<DurationValue?>(null) }

    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        DurationPicker(
            value = duration,
            onValueChange = { duration = it },
            maxHours = maxHours,
            maxMinutes = maxMinutes,
            onConfirm = { confirmed = it },
        )
        Text(
            text =
                confirmed?.let { "Confirmed ${it.hours} hr ${it.minutes} min" }
                    ?: "Press the pen to edit",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
