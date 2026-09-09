package io.github.dim971.rareui.showcase.entries

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.github.dim971.rareui.components.aikit.MatrixOrb
import io.github.dim971.rareui.components.aikit.MatrixOrbState
import io.github.dim971.rareui.showcase.CatalogEntry
import io.github.dim971.rareui.showcase.Demo

val matrixOrbEntry: CatalogEntry =
    CatalogEntry(
        name = "Matrix Orb",
        summary = "A grid of dots that breathes, ripples or thinks, depending on what it is doing.",
        demos =
            listOf(
                Demo(
                    title = "The three states",
                    note =
                        "Switching does not cut: the states crossfade, so an interruption blends " +
                            "from whatever is on screen.",
                    code = "MatrixOrb(state = MatrixOrbState.LISTENING)",
                ) { MatrixOrbStates() },
                Demo(
                    title = "Driven by a level",
                    note =
                        "With a level of your own the listening ripple follows it. Without one, " +
                            "the orb synthesises a breath.",
                    code = "MatrixOrb(state = MatrixOrbState.LISTENING, level = microphone.level)",
                ) { MatrixOrbLevel() },
                Demo(
                    title = "Grid and size",
                    note =
                        "The outline stays round because dots past 1.12 from the middle are " +
                            "dropped, rather than the square's own 1.41 corner.",
                    code = "MatrixOrb(state = MatrixOrbState.THINKING, size = 140.dp, dots = 7)",
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                        MatrixOrb(state = MatrixOrbState.THINKING, size = 120.dp, dots = 7)
                        MatrixOrb(state = MatrixOrbState.THINKING, size = 120.dp, dots = 15)
                    }
                },
            ),
    ) {
        MatrixOrb(
            state = MatrixOrbState.THINKING,
            size = 56.dp,
            dots = 9,
            labels = mapOf(MatrixOrbState.THINKING to ""),
        )
    }

@Composable
private fun MatrixOrbStates() {
    var state by remember { mutableStateOf(MatrixOrbState.IDLE) }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        MatrixOrb(state = state, size = 200.dp)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            MatrixOrbState.entries.forEach { candidate ->
                FilterChip(
                    selected = candidate == state,
                    onClick = { state = candidate },
                    label = { Text(candidate.name.lowercase().replaceFirstChar { it.uppercase() }) },
                )
            }
        }
    }
}

@Composable
private fun MatrixOrbLevel() {
    var level by remember { mutableFloatStateOf(0.5f) }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        MatrixOrb(state = MatrixOrbState.LISTENING, level = level, size = 200.dp)
        Slider(value = level, onValueChange = { level = it })
    }
}
