package io.github.dim971.rareui.showcase.entries

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.dim971.rareui.components.display.AnimatedCounter
import io.github.dim971.rareui.components.display.CounterGrouping
import io.github.dim971.rareui.showcase.CatalogEntry
import io.github.dim971.rareui.showcase.Demo

val animatedCounterEntry: CatalogEntry =
    CatalogEntry(
        name = "Animated Counter",
        summary = "An odometer. Each digit is a wheel that rolls to its new face.",
        demos =
            listOf(
                Demo(
                    title = "Up and down",
                    note = "The roll follows the value: up when it grows, down when it shrinks.",
                    code = "AnimatedCounter(value = total)",
                ) { CounterPlayground(start = 1234.0, step = 111.0) },
                Demo(
                    title = "Currency",
                    note = "Two decimal places, with a symbol in front.",
                    code = """AnimatedCounter(value = revenue, decimals = 2, prefix = "$")""",
                ) { CounterPlayground(start = 4820.5, step = 137.25, decimals = 2, prefix = "$") },
                Demo(
                    title = "Indian grouping",
                    note = "Three at the end, pairs above it: 12,34,567 rather than 1,234,567.",
                    code = "AnimatedCounter(value = population, grouping = CounterGrouping.INDIAN)",
                ) {
                    CounterPlayground(start = 1_234_567.0, step = 111_111.0, grouping = CounterGrouping.INDIAN)
                },
                Demo(
                    title = "Padded",
                    note = "A minimum width, so the number never changes size. Useful for a timer.",
                    code = """AnimatedCounter(value = count, padStart = 6, separator = "")""",
                ) { CounterPlayground(start = 42.0, step = 7.0, padStart = 6, separator = "") },
            ),
    ) {
        AnimatedCounter(value = 1234.0, style = TextStyle(fontSize = 18.sp, fontWeight = FontWeight.SemiBold))
    }

@Composable
private fun CounterPlayground(
    start: Double,
    step: Double,
    decimals: Int = 0,
    padStart: Int = 1,
    separator: String = ",",
    grouping: CounterGrouping = CounterGrouping.WESTERN,
    prefix: String? = null,
) {
    var value by remember { mutableStateOf(start) }

    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        AnimatedCounter(
            value = value,
            decimals = decimals,
            padStart = padStart,
            separator = separator,
            grouping = grouping,
            prefix = prefix,
            style = TextStyle(fontSize = 36.sp, fontWeight = FontWeight.SemiBold),
        )
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Button(onClick = { value -= step }) { Text("Less") }
            Button(onClick = { value += step }) { Text("More") }
            Button(onClick = { value = start }) { Text("Reset") }
        }
    }
}
