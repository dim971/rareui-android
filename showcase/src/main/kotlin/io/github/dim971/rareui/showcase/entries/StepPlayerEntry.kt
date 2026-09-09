package io.github.dim971.rareui.showcase.entries

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.github.dim971.rareui.components.display.StepPlayer
import io.github.dim971.rareui.components.display.StepPlayerControlPosition
import io.github.dim971.rareui.components.display.StepPlayerStep
import io.github.dim971.rareui.showcase.CatalogEntry
import io.github.dim971.rareui.showcase.Demo

val stepPlayerEntry: CatalogEntry =
    CatalogEntry(
        name = "Step Player",
        summary = "A row of dots where the one being played stretches into a bar and fills.",
        demos =
            listOf(
                Demo(
                    title = "Play it",
                    note =
                        "The play triangle and the pause bars are the same four cornered shapes " +
                            "interpolated corner for corner, so one becomes the other exactly " +
                            "rather than approximately. Replay crossfades, since an arrow curled " +
                            "into a circle has no sensible corners in common with either.",
                    code =
                        "StepPlayer(steps = steps, index = step, playing = playing,\n" +
                            "    onIndexChange = { step = it }, onPlayingChange = { playing = it })",
                ) { StepPlayerDemo() },
                Demo(
                    title = "Seekable, looping, and on the other side",
                    code =
                        "StepPlayer(steps = steps, index = step, playing = playing, seekable = true,\n" +
                            "    loop = true, controlPosition = StepPlayerControlPosition.LEADING)",
                ) {
                    StepPlayerDemo(
                        count = 6,
                        seekable = true,
                        loop = true,
                        position = StepPlayerControlPosition.LEADING,
                    )
                },
                Demo(
                    title = "Any size",
                    note = "Every measurement is a fraction of the track's height.",
                    code = "StepPlayer(steps = steps, index = step, playing = playing, size = 32.dp)",
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        listOf(28.dp, 48.dp, 64.dp).forEach { size ->
                            StepPlayerDemo(count = 3, size = size, duration = 6.0)
                        }
                    }
                },
            ),
    ) {
        StepPlayerDemo(count = 3, size = 32.dp, playingToStart = false)
    }

@Composable
private fun StepPlayerDemo(
    count: Int = 4,
    size: Dp = 48.dp,
    duration: Double = 4.0,
    seekable: Boolean = false,
    loop: Boolean = false,
    position: StepPlayerControlPosition = StepPlayerControlPosition.TRAILING,
    playingToStart: Boolean = true,
) {
    var step by remember { mutableIntStateOf(0) }
    var playing by remember { mutableStateOf(playingToStart) }

    StepPlayer(
        steps = List(count) { StepPlayerStep(label = "Chapter ${it + 1}") },
        index = step,
        playing = playing,
        onIndexChange = { step = it },
        onPlayingChange = { playing = it },
        duration = duration,
        loop = loop,
        size = size,
        controlPosition = position,
        seekable = seekable,
    )
}
