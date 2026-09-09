package io.github.dim971.rareui.showcase.entries

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import io.github.dim971.rareui.components.aikit.FluidOrb
import io.github.dim971.rareui.showcase.CatalogEntry
import io.github.dim971.rareui.showcase.Demo

val fluidOrbEntry: CatalogEntry =
    CatalogEntry(
        name = "Fluid Orb",
        summary = "A circle filled with something that looks like it is being stirred.",
        demos =
            listOf(
                Demo(
                    title = "Watch it move",
                    note =
                        "Upstream's own fragment shader, transliterated from GLSL to AGSL: value " +
                            "noise over three octaves, sampled at a position that is itself " +
                            "noise, which is what turns a cloud into something stirred. Below " +
                            "Android 13 there is no runtime shader, so the orb falls back to a " +
                            "gradient drifting on the same clock.",
                    code = "FluidOrb()",
                ) { FluidOrb() },
                Demo(
                    title = "Any colour, any size",
                    note = "The colour is the shade the fluid settles to at its darkest.",
                    code = "FluidOrb(size = 120.dp, color = Color(0xFFAF52DE))",
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        FluidOrb(size = 120.dp, color = Color(0xFFAF52DE))
                        FluidOrb(size = 90.dp, color = Color(0xFF34C759))
                        FluidOrb(size = 60.dp, color = Color(0xFFFC4C01))
                    }
                },
            ),
    ) {
        FluidOrb(size = 72.dp)
    }
