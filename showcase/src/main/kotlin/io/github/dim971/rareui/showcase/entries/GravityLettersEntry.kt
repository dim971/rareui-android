package io.github.dim971.rareui.showcase.entries

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import io.github.dim971.rareui.components.display.GravityGlyphs
import io.github.dim971.rareui.components.display.GravityLetters
import io.github.dim971.rareui.showcase.CatalogEntry
import io.github.dim971.rareui.showcase.Demo

val gravityLettersEntry: CatalogEntry =
    CatalogEntry(
        name = "Gravity Letters",
        summary = "Letters that fall out of your finger and pile up where they land.",
        demos =
            listOf(
                Demo(
                    title = "Drop some",
                    note =
                        "Touch to drop one, hold to pour, and drag to steer the pour. There is no " +
                            "physics engine: the pile is a height map, so hundreds of letters cost " +
                            "almost nothing.",
                    code = "GravityLetters(modifier = Modifier.fillMaxWidth().height(320.dp))",
                ) {
                    GravityLetters(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .height(320.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .background(MaterialTheme.colorScheme.surfaceContainerHighest),
                    )
                },
                Demo(
                    title = "Numbers, and your own strings",
                    note = "Tilt the device past ten degrees and the heap slides the way it leans.",
                    code =
                        "GravityLetters(glyphs = GravityGlyphs.NUMBERS)\n" +
                            "GravityLetters(items = listOf(\"Rare\", \"UI\"))",
                ) {
                    GravityLetters(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .height(260.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .background(MaterialTheme.colorScheme.surfaceContainerHighest),
                        glyphs = GravityGlyphs.BOTH,
                        size = 22.dp,
                    )
                },
            ),
    ) {
        // Sized rather than filled: the catalog row puts the preview beside the name, and
        // a preview that takes the whole width leaves the name one letter wide.
        GravityLetters(
            modifier = Modifier.size(width = 120.dp, height = 80.dp),
            glyphs = GravityGlyphs.NUMBERS,
            size = 14.dp,
        )
    }
