package io.github.dim971.rareui.showcase.entries

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.unit.dp
import io.github.dim971.rareui.components.aikit.GridReveal
import io.github.dim971.rareui.showcase.CatalogEntry
import io.github.dim971.rareui.showcase.Demo
import kotlinx.coroutines.delay
import kotlin.math.sin

val gridRevealEntry: CatalogEntry =
    CatalogEntry(
        name = "Grid Reveal",
        summary = "A placeholder that becomes a picture by dividing itself into it.",
        demos =
            listOf(
                Demo(
                    title = "Reveal it",
                    note =
                        "A hundred and eighty cells, each split sliding two halves out of where " +
                            "their parent was. The busiest parts of the picture come apart first, " +
                            "so detail arrives before flat colour does.",
                    code = "GridReveal(image = photo, caption = \"Generating\")",
                ) { GridRevealDemo() },
                Demo(
                    title = "Waiting for the picture",
                    note =
                        "With nothing to show yet the grid paces itself, creeping toward nine " +
                            "tenths and holding there, so a load that outruns its estimate still " +
                            "looks like it is working.",
                    code = "GridReveal(caption = \"Generating\", estimatedDuration = 4.0)",
                ) {
                    GridReveal(
                        modifier = Modifier.fillMaxWidth(),
                        aspect = 16f / 9f,
                        caption = "Generating",
                        estimatedDuration = 4.0,
                    )
                },
            ),
    ) {
        GridReveal(modifier = Modifier.fillMaxWidth(), image = SampleGradient)
    }

@Composable
private fun GridRevealDemo() {
    var image by remember { mutableStateOf<ImageBitmap?>(null) }
    var arrived by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        delay(2_500)
        image = SampleGradient
        arrived = true
    }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        GridReveal(
            modifier = Modifier.fillMaxWidth(),
            image = image,
            aspect = 4f / 3f,
            caption = if (arrived) null else "Generating",
        )
        Text(
            text = if (arrived) "The picture arrived after two and a half seconds." else "Working.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/**
 * Something to reveal, drawn rather than shipped: a band of colour with enough going on in
 * it that the ordering by detail has something to order.
 */
private val SampleGradient: ImageBitmap =
    ImageBitmap(256, 256).also { bitmap ->
        val canvas =
            androidx.compose.ui.graphics
                .Canvas(bitmap)
        val paint =
            androidx.compose.ui.graphics
                .Paint()
        for (y in 0 until 256) {
            for (x in 0 until 256) {
                val wave = (sin(x / 18.0) * sin(y / 26.0) + 1) / 2
                paint.color =
                    Color(
                        red = (0.15f + 0.7f * (x / 255f)).coerceIn(0f, 1f),
                        green = (0.2f + 0.6f * wave.toFloat()).coerceIn(0f, 1f),
                        blue = (0.9f - 0.6f * (y / 255f)).coerceIn(0f, 1f),
                    )
                canvas.drawRect(x.toFloat(), y.toFloat(), x + 1f, y + 1f, paint)
            }
        }
    }
